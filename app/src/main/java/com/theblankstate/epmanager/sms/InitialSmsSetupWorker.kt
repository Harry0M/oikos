package com.theblankstate.epmanager.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.theblankstate.epmanager.data.repository.AvailableBankRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time background worker that discovers banks from SMS inbox on first install.
 * 
 * This worker runs automatically after the user grants SMS permission during
 * or after onboarding. It:
 * 1. Checks if a scan has already been performed (to avoid duplicate work)
 * 2. Scans ALL SMS to discover banks/financial institutions
 * 3. Saves discovered banks to AvailableBankRepository
 * 4. Updates the last scan timestamp so the scan doesn't repeat
 */
@HiltWorker
class InitialSmsSetupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val bankDiscoveryScanner: BankDiscoveryScanner,
    private val availableBankRepository: AvailableBankRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting initial SMS bank discovery scan...")
        
        // Check if scan already happened (avoid duplicate work)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastScanTimestamp = prefs.getLong(KEY_LAST_SCAN_TIMESTAMP, 0L)
        if (lastScanTimestamp != 0L) {
            Log.d(TAG, "Scan already completed at $lastScanTimestamp, skipping.")
            return Result.success()
        }
        
        return try {
            // Scan ALL SMS (startTime = 0) to discover banks
            bankDiscoveryScanner.discoverBanks(startTime = 0L).collect { result ->
                if (result.isComplete) {
                    // Save discovered banks to repository for persistence
                    availableBankRepository.saveDiscoveredBanks(
                        result, 
                        clearOldScannedBanks = true
                    )
                    
                    // Save scan timestamp
                    prefs.edit()
                        .putLong(KEY_LAST_SCAN_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                    
                    Log.d(TAG, "Initial bank discovery complete. " +
                        "Found ${result.detectedBanks.size} banks, " +
                        "scanned ${result.scannedCount} messages.")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Initial SMS scan failed: ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "InitialSmsSetupWorker"
        private const val MAX_RETRIES = 2
        private const val WORK_NAME = "initial_sms_bank_discovery"
        
        // Same SharedPreferences keys as SmsSettingsViewModel for consistency
        private const val PREFS_NAME = "sms_settings_prefs"
        private const val KEY_LAST_SCAN_TIMESTAMP = "last_scan_timestamp"
        
        /**
         * Enqueue the initial SMS setup worker if a scan hasn't been done yet.
         * This is safe to call multiple times — WorkManager ensures uniqueness.
         */
        fun enqueueIfNeeded(context: Context) {
            // Quick check: skip enqueue if scan already done
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastScan = prefs.getLong(KEY_LAST_SCAN_TIMESTAMP, 0L)
            if (lastScan != 0L) {
                Log.d(TAG, "Scan already done, not enqueueing worker.")
                return
            }
            
            val workRequest = OneTimeWorkRequestBuilder<InitialSmsSetupWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP, // Don't replace if already running
                    workRequest
                )
            
            Log.d(TAG, "Initial SMS bank discovery worker enqueued.")
        }
    }
}
