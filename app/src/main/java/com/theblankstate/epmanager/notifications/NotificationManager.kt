package com.theblankstate.epmanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.theblankstate.epmanager.MainActivity
import com.theblankstate.epmanager.R
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.repository.BudgetRepository
import com.theblankstate.epmanager.data.repository.RecurringExpenseRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRepository: RecurringExpenseRepository
) {
    
    companion object {
        const val CHANNEL_BUDGET = "budget_alerts"
        const val CHANNEL_RECURRING = "recurring_reminders"
        const val CHANNEL_INSIGHTS = "spending_insights"
        
        const val NOTIFICATION_BUDGET_WARNING = 1001
        const val NOTIFICATION_BUDGET_EXCEEDED = 1002
        const val NOTIFICATION_RECURRING_DUE = 1003
        const val NOTIFICATION_DAILY_INSIGHT = 1004
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Budget Alerts Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you're approaching or exceeding budget limits"
            }
            
            // Recurring Reminders Channel
            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING,
                "Bill Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for upcoming recurring expenses"
            }
            
            // Spending Insights Channel
            val insightsChannel = NotificationChannel(
                CHANNEL_INSIGHTS,
                "Spending Insights",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily and weekly spending summaries"
            }
            
            notificationManager.createNotificationChannels(
                listOf(budgetChannel, recurringChannel, insightsChannel)
            )
        }
    }
    
    /**
     * Check budgets and send alerts if needed
     */
    suspend fun checkBudgetAlerts() {
        val budgets = budgetRepository.getBudgetsWithSpending().first()
        
        budgets.forEach { budgetWithSpending ->
            when {
                budgetWithSpending.percentage >= 1.0f -> {
                    sendBudgetExceededNotification(budgetWithSpending)
                }
                budgetWithSpending.percentage >= budgetWithSpending.budget.alertThreshold -> {
                    sendBudgetWarningNotification(budgetWithSpending)
                }
            }
        }
    }
    
    /**
     * Check for upcoming recurring expenses
     */
    suspend fun checkRecurringReminders() {
        val upcoming = recurringRepository.getUpcomingReminders()
        
        upcoming.forEach { recurring ->
            sendRecurringReminderNotification(recurring)
        }
    }
    
    /**
     * Generate daily spending insight
     */
    suspend fun sendDailyInsight() {
        val today = transactionRepository.getTodayTransactions().first()
        val todaySpending = today.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
        
        if (todaySpending > 0) {
            val monthlyExpenses = transactionRepository.getMonthlyExpenses().first() ?: 0.0
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val avgDailyBudget = monthlyExpenses / dayOfMonth
            
            val message = when {
                todaySpending > avgDailyBudget * 1.5 -> 
                    "You spent ${formatCurrency(todaySpending)} today - that's higher than your daily average!"
                todaySpending < avgDailyBudget * 0.5 -> 
                    "Great job! Only ${formatCurrency(todaySpending)} spent today. Keep it up! 🎉"
                else -> 
                    "Today's spending: ${formatCurrency(todaySpending)}. You're on track!"
            }
            
            sendNotification(
                channelId = CHANNEL_INSIGHTS,
                notificationId = NOTIFICATION_DAILY_INSIGHT,
                title = "Daily Spending Update",
                message = message,
                icon = android.R.drawable.ic_menu_info_details
            )
        }
    }
    
    private fun sendBudgetWarningNotification(budget: BudgetWithSpending) {
        val categoryName = budget.category?.name ?: "Unknown"
        val percentage = (budget.percentage * 100).toInt()
        
        sendNotification(
            channelId = CHANNEL_BUDGET,
            notificationId = NOTIFICATION_BUDGET_WARNING + categoryName.hashCode(),
            title = "⚠️ Budget Warning",
            message = "You've used $percentage% of your $categoryName budget. ${formatCurrency(budget.remaining)} remaining.",
            icon = android.R.drawable.ic_dialog_alert
        )
    }
    
    private fun sendBudgetExceededNotification(budget: BudgetWithSpending) {
        val categoryName = budget.category?.name ?: "Unknown"
        val overAmount = budget.spent - budget.budget.amount
        
        sendNotification(
            channelId = CHANNEL_BUDGET,
            notificationId = NOTIFICATION_BUDGET_EXCEEDED + categoryName.hashCode(),
            title = "🚨 Budget Exceeded!",
            message = "You've exceeded your $categoryName budget by ${formatCurrency(overAmount)}",
            icon = android.R.drawable.ic_dialog_alert
        )
    }
    
    private fun sendRecurringReminderNotification(recurring: RecurringExpense) {
        sendNotification(
            channelId = CHANNEL_RECURRING,
            notificationId = NOTIFICATION_RECURRING_DUE + recurring.id.hashCode(),
            title = "📅 Upcoming Bill",
            message = "${recurring.name}: ${formatCurrency(recurring.amount)} due soon",
            icon = android.R.drawable.ic_menu_my_calendar
        )
    }
    
    private fun sendNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted - ignore
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }
}
