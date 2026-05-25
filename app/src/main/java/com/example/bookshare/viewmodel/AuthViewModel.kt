package com.example.bookshare.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookshare.model.User
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authResult = MutableLiveData<AppResult<User>>()
    val authResult: LiveData<AppResult<User>> = _authResult

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn()

    val currentUser: LiveData<User?> = userRepository.observeCurrentUser()

    val users: LiveData<List<User>> = userRepository.getAllUsers()

    fun syncUsers() {
        viewModelScope.launch {
            userRepository.syncUsersFromFirebase()
        }
    }

    val firebaseUser: FirebaseUser? get() = authRepository.getCurrentUser()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = authRepository.signInWithEmail(email, password)
        }
    }

    fun register(email: String, password: String, name: String, avatarBitmap: Bitmap? = null) {
        viewModelScope.launch {
            _authResult.value = authRepository.registerWithEmail(email, password, name, avatarBitmap)
        }
    }

    fun signOut() = authRepository.signOut()

    private val _updateProfileResult = MutableLiveData<AppResult<User>?>()
    val updateProfileResult: LiveData<AppResult<User>?> = _updateProfileResult

    fun updateProfile(name: String, avatarBitmap: Bitmap?) {
        viewModelScope.launch {
            _updateProfileResult.value = userRepository.updateProfile(name, avatarBitmap)
        }
    }

    fun clearUpdateProfileResult() {
        _updateProfileResult.value = null
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(authRepository, userRepository) as T
    }
}
