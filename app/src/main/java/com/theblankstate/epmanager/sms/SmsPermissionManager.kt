package com.theblankstate.epmanager.sms

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manages SMS-related permissions for the app
 */
object SmsPermissionManager {
    
    const val SMS_PERMISSION_REQUEST_CODE = 1001
    
    val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )
    
    /**
     * Check if all SMS permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if we should show rationale for permissions
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return requiredPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Request SMS permissions
     */
    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            requiredPermissions,
            SMS_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if notification permission is needed (Android 13+) 
     */
    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    /**
     * Get notification permission (for Android 13+)
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
    }
}
