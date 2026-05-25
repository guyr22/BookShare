package com.example.bookshare.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookshare.model.User
import com.example.bookshare.local.UserDao
import com.example.bookshare.network.toErrorResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val storageRepository: StorageRepository = StorageRepository(),
    db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val usersRef: DatabaseReference = db.getReference("users")

    fun observeCurrentUser(): LiveData<User?> {
        val uid = auth.currentUser?.uid ?: return MutableLiveData(null)
        return userDao.getUserById(uid)
    }

    fun getAllUsers(): LiveData<List<User>> = userDao.getAll()

    suspend fun updateProfile(name: String, avatarBitmap: Bitmap?): AppResult<User> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return AppResult.Error("No signed-in user to update.")
            val uid = firebaseUser.uid

            val avatarUrl = if (avatarBitmap != null) {
                val path = "avatars/$uid/${System.currentTimeMillis()}.jpg"
                when (val result = storageRepository.uploadBitmap(avatarBitmap, path)) {
                    is AppResult.Success -> result.data
                    is AppResult.Error -> return result
                }
            } else {
                userDao.getUserByIdOnce(uid)?.avatarUrl.orEmpty()
            }

            val changes = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .apply { if (avatarUrl.isNotBlank()) photoUri = Uri.parse(avatarUrl) }
                .build()
            firebaseUser.updateProfile(changes).await()

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

    suspend fun syncUsersFromFirebase(): AppResult<Int> {
        return try {
            val since = userDao.getMaxLastUpdated() ?: 0L
            val snapshot = usersRef
                .orderByChild("lastUpdated")
                .startAt((since + 1).toDouble())
                .get()
                .await()

            val newUsers = snapshot.children.mapNotNull { it.toUser() }
            if (newUsers.isNotEmpty()) userDao.insertAll(newUsers)

            AppResult.Success(newUsers.size)
        } catch (e: Exception) {
            e.toErrorResult("User sync failed.")
        }
    }

    // Called by AuthRepository after sign-in to cache user locally and optionally push to Firebase
    suspend fun cacheUser(user: User, pushToFirebase: Boolean = false) {
        userDao.insert(user)
        if (pushToFirebase) pushUserToFirebase(user)
    }

    // Called by AuthRepository after registration — uploads avatar and persists the new user
    suspend fun createUserAfterRegistration(
        uid: String,
        email: String,
        name: String,
        avatarBitmap: Bitmap?
    ): AppResult<User> {
        return try {
            val avatarUrl = if (avatarBitmap != null) {
                val path = "avatars/$uid/${System.currentTimeMillis()}.jpg"
                when (val upload = storageRepository.uploadBitmap(avatarBitmap, path)) {
                    is AppResult.Success -> upload.data
                    is AppResult.Error -> ""
                }
            } else ""

            val user = User(id = uid, email = email, name = name, avatarUrl = avatarUrl)
            userDao.insert(user)
            pushUserToFirebase(user)

            AppResult.Success(user)
        } catch (e: Exception) {
            e.toErrorResult("Failed to save user profile.")
        }
    }

    // Used by AuthRepository during sign-in to prefer existing remote profile data
    suspend fun fetchRemoteUser(uid: String): User? {
        return try {
            usersRef.child(uid).get().await().toUser()
        } catch (e: Exception) {
            null
        }
    }

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
