package com.example.bookstore.repository

import com.example.bookstore.local.User
import com.example.bookstore.local.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val userDao: UserDao
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithEmail(email: String, password: String): AuthResult<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Sign-in succeeded but no user was returned.")

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName ?: ""
            )
            userDao.insert(user)

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign-in failed.", e)
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        name: String
    ): AuthResult<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Registration succeeded but no user was returned.")

            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name
            )
            userDao.insert(user)

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed.", e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
