package com.theblankstate.epmanager.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.theblankstate.epmanager.notifications.NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks and triggers notifications.
 * 
 * This worker:
 * - Runs periodically (every 6 hours)
 * - Checks user preferences before sending each notification type
 * - Triggers budget alerts, recurring reminders, and daily insights
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting notification check...")
        
        return try {
            // Check and send notifications based on user preferences
            notificationManager.checkAndSendNotifications()
            
            Log.d(TAG, "Notification check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Notification check failed: ${e.message}")
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "notification_check_work"
        
        /**
         * Schedule periodic notification checks with WorkManager
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            val notificationRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
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
                    notificationRequest
                )
            
            Log.d(TAG, "Notification worker scheduled")
        }
        
        /**
         * Cancel scheduled notification checks
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Notification worker cancelled")
        }
        
        /**
         * Trigger immediate notification check (one-time)
         */
        fun checkNow(context: Context) {
            val checkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(checkRequest)
            Log.d(TAG, "Immediate notification check triggered")
        }
    }
}
