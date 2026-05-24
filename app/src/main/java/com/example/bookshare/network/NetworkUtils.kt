package com.example.bookshare.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.bookshare.repository.AppResult
import java.io.IOException

const val NO_INTERNET_MESSAGE = "No internet connection. Please check your connection and try again."

/**
 * Returns true for any exception that indicates a connectivity problem rather
 * than a server-side or logic error:
 *   - IOException covers UnknownHostException, ConnectException, SocketTimeoutException, etc.
 *   - Walking the cause chain catches cases where a Firebase or Retrofit exception
 *     wraps the underlying IO error.
 *   - Class-name check catches FirebaseNetworkException without importing the class.
 */
fun isNetworkError(e: Exception): Boolean {
    var cause: Throwable? = e
    while (cause != null) {
        if (cause is IOException) return true
        if (cause.javaClass.simpleName.contains("Network", ignoreCase = true)) return true
        cause = cause.cause
    }
    return false
}

fun Exception.toErrorResult(fallback: String): AppResult.Error =
    if (isNetworkError(this)) AppResult.Error(NO_INTERNET_MESSAGE, this)
    else AppResult.Error(message ?: fallback, this)

fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
