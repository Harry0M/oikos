package com.theblankstate.epmanager.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs settings data to Firebase.
 * 
 * This worker:
 * - Runs periodically (every 6 hours) when device has network
 * - Only syncs if user is logged in
 * - Only syncs settings data (NOT transactions)
 * - Uses offline-first approach - never blocks main app functionality
 */
@HiltWorker
class BackgroundSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val syncManager: FirebaseSyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background sync...")
        
        // Check if user is logged in
        if (!syncManager.isLoggedIn) {
            Log.d(TAG, "User not logged in, skipping sync")
            return Result.success()
        }
        
        return try {
            val result = syncManager.syncAllData()
            
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Background sync completed successfully")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Background sync failed: ${error.message}")
                    // Retry on failure (network issues, etc.)
                    if (runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Background sync exception: ${e.message}")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "BackgroundSyncWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "settings_sync_work"
        
        /**
         * Schedule periodic sync with WorkManager
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
            
            Log.d(TAG, "Background sync scheduled")
        }
        
        /**
         * Cancel scheduled sync
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Background sync cancelled")
        }
        
        /**
         * Trigger immediate sync (one-time)
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "Immediate sync triggered")
        }
    }
}
