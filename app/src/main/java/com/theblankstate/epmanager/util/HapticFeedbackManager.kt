package com.theblankstate.epmanager.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haptic feedback manager for providing tactile responses to user actions.
 * Supports different vibration patterns for different action types.
 */
@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Light tick for button presses and selections
     */
    fun tick() {
        vibrate(VibrationPattern.TICK)
    }
    
    /**
     * Click feedback for confirming actions
     */
    fun click() {
        vibrate(VibrationPattern.CLICK)
    }
    
    /**
     * Heavy click for important confirmations
     */
    fun heavyClick() {
        vibrate(VibrationPattern.HEAVY_CLICK)
    }
    
    /**
     * Success feedback for completed actions
     */
    fun success() {
        vibrate(VibrationPattern.SUCCESS)
    }
    
    /**
     * Error feedback for failed actions
     */
    fun error() {
        vibrate(VibrationPattern.ERROR)
    }
    
    /**
     * Warning feedback for cautionary actions
     */
    fun warning() {
        vibrate(VibrationPattern.WARNING)
    }
    
    private fun vibrate(pattern: VibrationPattern) {
        if (vibrator?.hasVibrator() != true) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (pattern) {
                VibrationPattern.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                VibrationPattern.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                VibrationPattern.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                VibrationPattern.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1)
                VibrationPattern.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                VibrationPattern.WARNING -> VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1)
            }
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern.fallbackDuration)
        }
    }
    
    private enum class VibrationPattern(val fallbackDuration: Long) {
        TICK(10L),
        CLICK(20L),
        HEAVY_CLICK(50L),
        SUCCESS(100L),
        ERROR(200L),
        WARNING(150L)
    }
}
