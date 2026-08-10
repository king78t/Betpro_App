package com.bp.uunwlm.util

import android.util.Log
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

object ErrorHandler {
    private const val TAG = "ErrorHandler"

    fun getErrorMessage(throwable: Throwable): String {
        Log.e(TAG, "Handling error: ${throwable.message}", throwable)
        
        return when (throwable) {
            is UnknownHostException, is ConnectException, is IOException -> 
                "Network unreachable. Please check your internet connection."
            else -> throwable.message ?: "An unexpected error occurred. Please try again."
        }
    }
}
