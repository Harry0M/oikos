package com.theblankstate.epmanager.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized analytics tracking for user behavior and app events.
 * Uses Firebase Analytics for event logging.
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    
    // ==================== Screen Tracking ====================
    
    fun logScreenView(screenName: String, screenClass: String? = null) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        })
    }
    
    // ==================== Transaction Events ====================
    
    fun logTransactionAdded(type: String, amount: Double, categoryName: String?) {
        firebaseAnalytics.logEvent("transaction_added", Bundle().apply {
            putString("transaction_type", type)
            putDouble("amount", amount)
            categoryName?.let { putString("category", it) }
        })
    }
    
    fun logTransactionDeleted(type: String) {
        firebaseAnalytics.logEvent("transaction_deleted", Bundle().apply {
            putString("transaction_type", type)
        })
    }
    
    // ==================== Feature Usage ====================
    
    fun logFeatureUsed(featureName: String) {
        firebaseAnalytics.logEvent("feature_used", Bundle().apply {
            putString("feature_name", featureName)
        })
    }
    
    fun logBudgetCreated(categoryName: String, amount: Double) {
        firebaseAnalytics.logEvent("budget_created", Bundle().apply {
            putString("category", categoryName)
            putDouble("amount", amount)
        })
    }
    
    fun logGoalCreated(goalName: String, targetAmount: Double) {
        firebaseAnalytics.logEvent("goal_created", Bundle().apply {
            putString("goal_name", goalName)
            putDouble("target_amount", targetAmount)
        })
    }
    
    fun logExportCompleted(format: String) {
        firebaseAnalytics.logEvent("export_completed", Bundle().apply {
            putString("format", format)
        })
    }
    
    // ==================== User Properties ====================
    
    fun setUserCurrency(currencyCode: String) {
        firebaseAnalytics.setUserProperty("currency", currencyCode)
    }
    
    fun setUserTheme(themeName: String) {
        firebaseAnalytics.setUserProperty("app_theme", themeName)
    }
    
    fun setUserAccountCount(count: Int) {
        firebaseAnalytics.setUserProperty("account_count", count.toString())
    }
    
    // ==================== Session Tracking ====================
    
    fun logAppOpen() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    
    fun logOnboardingCompleted() {
        firebaseAnalytics.logEvent("onboarding_completed", null)
    }
    
    fun logLogin(method: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }
    
    // ==================== Error Tracking ====================
    
    fun logError(errorType: String, errorMessage: String?) {
        firebaseAnalytics.logEvent("app_error", Bundle().apply {
            putString("error_type", errorType)
            errorMessage?.let { putString("error_message", it.take(100)) }
        })
    }
}
