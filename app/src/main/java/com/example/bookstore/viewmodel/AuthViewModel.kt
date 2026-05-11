package com.example.bookstore.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookstore.local.User
import com.example.bookstore.repository.AppResult
import com.example.bookstore.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authResult = MutableLiveData<AppResult<User>>()
    val authResult: LiveData<AppResult<User>> = _authResult

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn()

    /** Local Room record of the signed-in user; null until login finishes or if signed out. */
    val currentUser: LiveData<User?> = authRepository.observeCurrentUser()

    /** One-shot accessor for the FirebaseAuth user (e.g. for UID before the Room write lands). */
    val firebaseUser: FirebaseUser? get() = authRepository.getCurrentUser()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = authRepository.signInWithEmail(email, password)
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authResult.value = authRepository.registerWithEmail(email, password, name)
        }
    }

    fun signOut() = authRepository.signOut()

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(authRepository) as T
    }
}
