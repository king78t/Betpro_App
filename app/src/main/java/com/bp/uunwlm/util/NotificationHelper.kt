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
import com.bp.uunwlm.model.UserAccount

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "transaction_updates_channel"
    private const val CHANNEL_NAME = "Transaction Updates"
    private const val CHANNEL_DESC = "Alerts for deposit and withdrawal status changes"

    const val ADMIN_CHANNEL_ID = "admin_alerts_channel"
    private const val ADMIN_CHANNEL_NAME = "Admin Realtime Alerts"
    private const val ADMIN_CHANNEL_DESC = "Instant alerts for new deposits, withdrawals, and user account registrations"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // User channel
            val userChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(userChannel)

            // Admin channel
            val adminChannel = NotificationChannel(
                ADMIN_CHANNEL_ID,
                ADMIN_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ADMIN_CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(adminChannel)

            Log.d(TAG, "Notification channels initialized successfully.")
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted. Skipping notification.")
                return false
            }
        }
        return true
    }

    fun showTransactionStatusNotification(context: Context, tx: TransactionRequest) {
        try {
            if (!hasNotificationPermission(context)) return

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
                .setDefaults(NotificationCompat.DEFAULT_ALL)
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

    fun showAdminNewDepositNotification(context: Context, tx: TransactionRequest) {
        try {
            if (!hasNotificationPermission(context)) return

            val title = "📥 New Deposit Request (${tx.currency} ${"%.0f".format(tx.amount)})"
            val message = "User: ${tx.userName}\nAmount: ${tx.currency} ${"%.2f".format(tx.amount)} via ${tx.gatewayName}\nRef: ${tx.referenceNumber}"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(("admin_dep_" + tx.id).hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText("New deposit of ${tx.currency} ${tx.amount} from ${tx.userName}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = 100000 + (Math.abs(tx.id.hashCode()) % 80000)
            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Admin deposit notification posted for transaction ${tx.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send admin deposit notification: ${e.message}", e)
        }
    }

    fun showAdminNewWithdrawalNotification(context: Context, tx: TransactionRequest) {
        try {
            if (!hasNotificationPermission(context)) return

            val title = "📤 New Withdrawal Request (${tx.currency} ${"%.0f".format(tx.amount)})"
            val message = "User: ${tx.userName}\nAmount: ${tx.currency} ${"%.2f".format(tx.amount)} to ${tx.gatewayName}\nAccount Title: ${tx.accountTitle}"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(("admin_wd_" + tx.id).hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle(title)
                .setContentText("Withdrawal request of ${tx.currency} ${tx.amount} from ${tx.userName}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = 200000 + (Math.abs(tx.id.hashCode()) % 80000)
            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Admin withdrawal notification posted for transaction ${tx.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send admin withdrawal notification: ${e.message}", e)
        }
    }

    fun showAdminNewAccountNotification(context: Context, user: UserAccount) {
        try {
            if (!hasNotificationPermission(context)) return

            val title = "👤 New Account Registration"
            val message = "Name: ${user.fullName}\nMobile: ${user.mobileNumber}\nEmail: ${user.email}\nCountry: ${user.country}"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(("admin_usr_" + user.id).hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_myplaces)
                .setContentTitle(title)
                .setContentText("New user registered: ${user.fullName} (${user.mobileNumber})")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = 300000 + (Math.abs(user.id.hashCode()) % 80000)
            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Admin new user notification posted for user ${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send admin user notification: ${e.message}", e)
        }
    }
}
