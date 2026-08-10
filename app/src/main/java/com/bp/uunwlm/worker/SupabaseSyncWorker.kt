package com.bp.uunwlm.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bp.uunwlm.data.BPWalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SupabaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing automated WorkManager background Supabase cloud sync...")
            BPWalletRepository.syncWithSupabaseCloud()
            Log.d(TAG, "Automated WorkManager background Supabase cloud sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager background Supabase sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SupabaseSyncWorker"
        const val WORK_NAME = "supabase_periodic_cloud_sync_worker"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<SupabaseSyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
                Log.i(TAG, "Enqueued periodic Supabase cloud sync WorkManager task every 15 minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule WorkManager periodic sync: ${e.message}")
            }
        }

        fun triggerOneTimeSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueue(syncRequest)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger WorkManager one-time sync: ${e.message}")
            }
        }
    }
}
