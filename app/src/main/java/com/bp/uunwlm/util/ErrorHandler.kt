package com.bp.uunwlm.util

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

object ErrorHandler {
    private const val TAG = "ErrorHandler"

    fun getErrorMessage(throwable: Throwable): String {
        Log.e(TAG, "Handling error: ${throwable.message}", throwable)
        
        return when (throwable) {
            is FirebaseAuthException -> {
                when (throwable.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address format."
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                    "ERROR_USER_DISABLED" -> "This account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                    else -> throwable.message ?: "Authentication failed."
                }
            }
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to perform this action."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Database is currently unavailable. Check your connection."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "Request timed out. Please try again."
                    else -> "Database error: ${throwable.code}"
                }
            }
            is FirebaseException -> throwable.message ?: "Firebase service error."
            is UnknownHostException, is ConnectException, is IOException -> 
                "Network unreachable. Please check your internet connection."
            else -> throwable.message ?: "An unexpected error occurred. Please try again."
        }
    }
}
