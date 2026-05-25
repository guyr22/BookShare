package com.example.bookshare.repository

import android.graphics.Bitmap
import com.example.bookshare.model.User
import com.example.bookshare.network.toErrorResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithEmail(email: String, password: String): AppResult<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AppResult.Error("Sign-in succeeded but no user was returned.")
            val uid = firebaseUser.uid

            val remote = userRepository.fetchRemoteUser(uid)
            val user = remote ?: User(
                id = uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName.orEmpty()
            )
            userRepository.cacheUser(user, pushToFirebase = remote == null)

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

            userRepository.createUserAfterRegistration(firebaseUser.uid, email, name, avatarBitmap)
        } catch (e: Exception) {
            e.toErrorResult("Registration failed.")
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
