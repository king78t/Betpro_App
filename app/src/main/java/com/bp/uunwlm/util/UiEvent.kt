package com.bp.uunwlm.util

import androidx.compose.material3.SnackbarDuration

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent()

    data class ShowToast(
        val message: String,
        val isLong: Boolean = false
    ) : UiEvent()
}
