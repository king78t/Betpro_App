package com.bp.uunwlm.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bp.uunwlm.MainActivity
import com.bp.uunwlm.model.TransactionRequest

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "transaction_updates_channel"
    private const val CHANNEL_NAME = "Transaction Updates"
    private const val CHANNEL_DESC = "Alerts for deposit and withdrawal status changes"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val descriptionText = CHANNEL_DESC
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel initialized.")
        }
    }

    fun showTransactionStatusNotification(context: Context, tx: TransactionRequest) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted. Skipping notification.")
                    return
                }
            }

            val isApproved = tx.status.equals("Approved", ignoreCase = true)
            val isRejected = tx.status.equals("Rejected", ignoreCase = true)

            if (!isApproved && !isRejected) return

            val statusSymbol = if (isApproved) "Approved 🎉" else "Rejected ❌"
            val typeTitle = if (tx.type.equals("DEPOSIT", ignoreCase = true)) "Deposit Request" else "Withdrawal Request"
            val title = "$typeTitle $statusSymbol"

            val amountFormatted = "${tx.currency} ${"%.2f".format(tx.amount)}"
            val notesSuffix = if (tx.adminNotes.isNotBlank()) " Note: ${tx.adminNotes}" else ""
            val message = if (isApproved) {
                "Your $typeTitle of $amountFormatted has been Approved.$notesSuffix"
            } else {
                "Your $typeTitle of $amountFormatted was Rejected.$notesSuffix"
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(tx.id.hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = ((System.currentTimeMillis() % 10000).toInt() + Math.abs(tx.id.hashCode())) % 100000
            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Notification posted for transaction ${tx.id}: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
        }
    }
}
