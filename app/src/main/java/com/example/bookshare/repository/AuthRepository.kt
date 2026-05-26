package com.example.bookshare.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookshare.model.User
import com.example.bookshare.local.UserDao
import com.example.bookshare.network.toErrorResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val storageRepository: StorageRepository = StorageRepository(),
    db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val usersRef: DatabaseReference = db.getReference("users")

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Observable view of the currently-signed-in user record stored in Room.
     * Returns a LiveData that emits null when no one is signed in or when the
     * local cache hasn't been populated yet.
     */
    fun observeCurrentUser(): LiveData<User?> {
        val uid = auth.currentUser?.uid ?: return MutableLiveData(null)
        return userDao.getUserById(uid)
    }

    suspend fun signInWithEmail(email: String, password: String): AppResult<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AppResult.Error("Sign-in succeeded but no user was returned.")
            val uid = firebaseUser.uid

            // Prefer the existing remote profile (real name + avatar from registration
            // or a previous edit) over the sparse FirebaseAuth fields. If there is no
            // remote record yet (legacy account), create one from what we have.
            val remote = fetchRemoteUser(uid)
            val user = remote ?: User(
                id = uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName.orEmpty()
            )
            userDao.insert(user)
            if (remote == null) pushUserToFirebase(user)

            AppResult.Success(user)
        } catch (e: Exception) {
            e.toErrorResult("Sign-in failed.")
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        name: String,
        avatarBitmap: Bitmap? = null
    ): AppResult<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AppResult.Error("Registration succeeded but no user was returned.")

            val uid = firebaseUser.uid
            val avatarUrl = if (avatarBitmap != null) {
                val path = "avatars/$uid/${System.currentTimeMillis()}.jpg"
                when (val upload = storageRepository.uploadBitmap(avatarBitmap, path)) {
                    is AppResult.Success -> upload.data
                    is AppResult.Error -> ""
                }
            } else ""

            val user = User(
                id = uid,
                email = email,
                name = name,
                avatarUrl = avatarUrl
            )
            userDao.insert(user)
            pushUserToFirebase(user)

            AppResult.Success(user)
        } catch (e: Exception) {
            e.toErrorResult("Registration failed.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Updates the signed-in user's display name and (optionally) avatar photo.
     *
     *   1. If [avatarBitmap] is provided, upload it to Firebase Storage and use
     *      the returned URL; otherwise keep the existing avatarUrl from Room.
     *   2. Mirror name + photo onto the FirebaseAuth profile (so it survives a
     *      fresh install / second device even before user-sync lands).
     *   3. Write the updated record to Room first (UI reacts at once), then push
     *      it to Firebase Realtime Database under users/{uid}.
     */
    suspend fun updateProfile(name: String, avatarBitmap: Bitmap?): AppResult<User> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return AppResult.Error("No signed-in user to update.")
            val uid = firebaseUser.uid

            // Step 1 – resolve the avatar URL (upload new bitmap or keep existing)
            val avatarUrl = if (avatarBitmap != null) {
                val path = "avatars/$uid/${System.currentTimeMillis()}.jpg"
                when (val result = storageRepository.uploadBitmap(avatarBitmap, path)) {
                    is AppResult.Success -> result.data
                    is AppResult.Error -> return result
                }
            } else {
                userDao.getUserByIdOnce(uid)?.avatarUrl.orEmpty()
            }

            // Step 2 – mirror onto the FirebaseAuth profile
            val changes = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .apply { if (avatarUrl.isNotBlank()) photoUri = Uri.parse(avatarUrl) }
                .build()
            firebaseUser.updateProfile(changes).await()

            // Step 3 – build the canonical record and persist (Room first, then Firebase)
            val user = User(
                id = uid,
                name = name,
                email = firebaseUser.email.orEmpty(),
                avatarUrl = avatarUrl
            )
            userDao.insert(user)
            usersRef.child(uid).setValue(user.toFirebaseMap()).await()

            AppResult.Success(user)
        } catch (e: Exception) {
            e.toErrorResult("Profile update failed.")
        }
    }

    // ── Multi-user persistence (so the Feed can show real names) ─────────────

    /** Observe every cached user — Feed maps ownerId → display name from this. */
    fun getAllUsers(): LiveData<List<User>> = userDao.getAll()

    /**
     * Delta-fetch every user whose lastUpdated is newer than the highest timestamp
     * already in Room, then cache them. Mirrors BookRepository.syncFromFirebase.
     * Returns the number of new/updated records written. Call on app/Feed start.
     */
    suspend fun syncUsersFromFirebase(): AppResult<Int> {
        return try {
            val since = userDao.getMaxLastUpdated() ?: 0L
            val snapshot = usersRef
                .orderByChild("lastUpdated")
                .startAt((since + 1).toDouble())   // +1 excludes records we already have
                .get()
                .await()

            val newUsers = snapshot.children.mapNotNull { it.toUser() }
            if (newUsers.isNotEmpty()) userDao.insertAll(newUsers)

            AppResult.Success(newUsers.size)
        } catch (e: Exception) {
            e.toErrorResult("User sync failed.")
        }
    }

    /** One-shot read of a single remote user record (null if absent or on error). */
    private suspend fun fetchRemoteUser(uid: String): User? {
        return try {
            usersRef.child(uid).get().await().toUser()
        } catch (e: Exception) {
            null
        }
    }

    /** Best-effort write of users/{uid}; never fails the surrounding auth flow. */
    private suspend fun pushUserToFirebase(user: User) {
        try {
            usersRef.child(user.id).setValue(user.toFirebaseMap()).await()
        } catch (e: Exception) {
            // Offline or transient — the next profile edit / sync reconciles it.
        }
    }

    private fun DataSnapshot.toUser(): User? {
        val uid = child("id").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: key
            ?: return null
        return User(
            id = uid,
            name = child("name").getValue(String::class.java).orEmpty(),
            email = child("email").getValue(String::class.java).orEmpty(),
            avatarUrl = child("avatarUrl").getValue(String::class.java).orEmpty(),
            lastUpdated = child("lastUpdated").getValue(Long::class.java)
                ?: System.currentTimeMillis()
        )
    }

    private fun User.toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "lastUpdated" to lastUpdated
    )
}
