package com.bp.wallet.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.SnackbarDuration
import androidx.core.app.NotificationCompat
import com.bp.wallet.model.TransactionRequest
import org.mindrot.jbcrypt.BCrypt

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

object SecurityUtils {
    fun hashPassword(raw: String): String {
        return try {
            BCrypt.hashpw(raw, BCrypt.gensalt(10))
        } catch (e: Exception) {
            raw
        }
    }

    fun checkPassword(raw: String, hashed: String): Boolean {
        return try {
            if (hashed.startsWith("$2a$") || hashed.startsWith("$2b$") || hashed.startsWith("$2y$")) {
                BCrypt.checkpw(raw, hashed)
            } else {
                raw == hashed
            }
        } catch (e: Exception) {
            raw == hashed
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "bp_wallet_notifications"
    private const val CHANNEL_NAME = "BP Wallet Alerts"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates for deposits, withdrawals, and account status"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showTransactionStatusNotification(context: Context, tx: TransactionRequest) {
        val title = "Transaction ${tx.status}: ${tx.type}"
        val message = "Your ${tx.type.lowercase()} request of ${tx.currency} ${tx.amount} has been ${tx.status.lowercase()}."
        sendNotification(context, (tx.id.hashCode() and 0x7FFFFFFF), title, message)
    }

    fun showDepositSubmittedNotification(context: Context, tx: TransactionRequest) {
        val title = "Deposit Request Submitted"
        val message = "Deposit of ${tx.currency} ${tx.amount} submitted via ${tx.gatewayName}. Awaiting verification."
        sendNotification(context, (tx.id.hashCode() and 0x7FFFFFFF), title, message)
    }

    fun showAdminNewDepositNotification(context: Context, tx: TransactionRequest) {
        val title = "Admin Alert: New Deposit"
        val message = "User ${tx.userName} deposited ${tx.currency} ${tx.amount} via ${tx.gatewayName}."
        sendNotification(context, ((tx.id + "_admin").hashCode() and 0x7FFFFFFF), title, message)
    }

    fun showWithdrawalSubmittedNotification(context: Context, tx: TransactionRequest) {
        val title = "Withdrawal Request Submitted"
        val message = "Withdrawal of ${tx.currency} ${tx.amount} submitted to ${tx.accountTitle}. Processing soon."
        sendNotification(context, (tx.id.hashCode() and 0x7FFFFFFF), title, message)
    }

    fun showAdminNewWithdrawalNotification(context: Context, tx: TransactionRequest) {
        val title = "Admin Alert: New Withdrawal"
        val message = "User ${tx.userName} requested withdrawal of ${tx.currency} ${tx.amount}."
        sendNotification(context, ((tx.id + "_admin").hashCode() and 0x7FFFFFFF), title, message)
    }

    private fun sendNotification(context: Context, notifId: Int, title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notifId, builder.build())
        } catch (e: Exception) {
            // Ignored if permissions not granted
        }
    }
}

object ErrorHandler {
    fun getErrorMessage(throwable: Throwable): String {
        return throwable.localizedMessage ?: throwable.message ?: "An unexpected error occurred. Please try again."
    }
}

