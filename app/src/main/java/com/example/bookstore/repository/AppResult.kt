package com.example.bookstore.repository

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : AppResult<Nothing>()
}
