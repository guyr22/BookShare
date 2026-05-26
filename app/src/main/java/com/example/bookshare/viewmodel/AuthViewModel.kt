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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authResult = MutableLiveData<AppResult<User>>()
    val authResult: LiveData<AppResult<User>> = _authResult

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn()

    /** Local Room record of the signed-in user; null until login finishes or if signed out. */
    val currentUser: LiveData<User?> = authRepository.observeCurrentUser()

    /** Every cached user — the Feed maps ownerId → display name from this. */
    val users: LiveData<List<User>> = authRepository.getAllUsers()

    /** Delta-fetch all users into Room so the Feed can resolve owner names. */
    fun syncUsers() {
        viewModelScope.launch {
            authRepository.syncUsersFromFirebase()
        }
    }

    /** One-shot accessor for the FirebaseAuth user (e.g. for UID before the Room write lands). */
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

    /** Updates the signed-in user's display name and (optionally) avatar photo. */
    fun updateProfile(name: String, avatarBitmap: Bitmap?) {
        viewModelScope.launch {
            _updateProfileResult.value = authRepository.updateProfile(name, avatarBitmap)
        }
    }

    /** Clears the one-shot result so it isn't re-delivered after a config change. */
    fun clearUpdateProfileResult() {
        _updateProfileResult.value = null
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(authRepository) as T
    }
}
