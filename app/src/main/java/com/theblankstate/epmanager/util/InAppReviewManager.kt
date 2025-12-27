package com.theblankstate.epmanager.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-app review prompts using Google Play In-App Review API.
 * 
 * Best practices:
 * - Don't prompt too frequently (max once per session or after significant actions)
 * - Don't prompt after negative experiences
 * - Prompt after positive experiences (completed transactions, achieved goals, etc.)
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    
    /**
     * Requests and launches the in-app review flow.
     * 
     * Note: Google Play controls when the review dialog is actually shown.
     * The API may not show the dialog every time (rate limiting, user already reviewed, etc.)
     * 
     * @param activity The activity to launch the review flow from
     * @return true if the flow was launched successfully, false otherwise
     */
    suspend fun requestReview(activity: Activity): Boolean {
        return try {
            val reviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(activity, reviewInfo).await()
            true
        } catch (e: Exception) {
            // Silently fail - review prompts are non-critical
            false
        }
    }
    
    companion object {
        // Constants for tracking when to show review prompts
        const val TRANSACTIONS_BEFORE_REVIEW = 20
        const val DAYS_BEFORE_REVIEW = 7
        const val MIN_SESSIONS_BEFORE_REVIEW = 5
    }
}
