package com.example.bookstore.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookstore.local.User
import com.example.bookstore.local.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
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

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName ?: ""
            )
            userDao.insert(user)

            AppResult.Success(user)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Sign-in failed.", e)
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        name: String
    ): AppResult<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AppResult.Error("Registration succeeded but no user was returned.")

            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name
            )
            userDao.insert(user)

            AppResult.Success(user)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Registration failed.", e)
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
            AppResult.Error(e.message ?: "Profile update failed.", e)
        }
    }

    private fun User.toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "lastUpdated" to lastUpdated
    )
}
