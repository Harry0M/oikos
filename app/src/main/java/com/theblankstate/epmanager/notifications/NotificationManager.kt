package com.theblankstate.epmanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.theblankstate.epmanager.MainActivity
import com.theblankstate.epmanager.R
import com.theblankstate.epmanager.data.model.AppNotification
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtType
import com.theblankstate.epmanager.data.model.NotificationType
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.repository.BudgetRepository
import com.theblankstate.epmanager.data.repository.DebtRepository
import com.theblankstate.epmanager.data.repository.NotificationRepository
import com.theblankstate.epmanager.data.repository.RecurringExpenseRepository
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import com.theblankstate.epmanager.data.repository.SplitRepository
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
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val debtRepository: DebtRepository,
    private val splitRepository: SplitRepository,
    private val notificationRepository: NotificationRepository
) {
    
    companion object {
        private const val TAG = "NotificationManager"
        
        // Notification Channels
        const val CHANNEL_BUDGET = "budget_alerts"
        const val CHANNEL_RECURRING = "recurring_reminders"
        const val CHANNEL_INSIGHTS = "spending_insights"
        const val CHANNEL_SAVINGS = "savings_milestones"
        const val CHANNEL_DEBT = "debt_reminders"
        const val CHANNEL_SPLIT = "split_payments"
        
        // Notification IDs
        const val NOTIFICATION_BUDGET_WARNING = 1001
        const val NOTIFICATION_BUDGET_EXCEEDED = 1002
        const val NOTIFICATION_RECURRING_DUE = 1003
        const val NOTIFICATION_DAILY_INSIGHT = 1004
        const val NOTIFICATION_WEEKLY_SUMMARY = 1005
        const val NOTIFICATION_SAVINGS_MILESTONE = 1006
        const val NOTIFICATION_DEBT_DUE = 1007
        const val NOTIFICATION_CREDIT_DUE = 1008
        const val NOTIFICATION_DEBT_OVERDUE = 1009
        const val NOTIFICATION_SPLIT_BALANCE = 1010
        const val NOTIFICATION_OVERSPENDING = 1011
        const val NOTIFICATION_GOAL_DEADLINE = 1012
        
        private const val PREFS_NAME = "notification_prefs"
        private const val PREF_BUDGET_ALERTS = "budget_alerts"
        private const val PREF_RECURRING_REMINDERS = "recurring_reminders"
        private const val PREF_DEBT_REMINDERS = "debt_reminders"
        private const val PREF_SAVINGS_REMINDERS = "savings_reminders"
        private const val PREF_SPLIT_REMINDERS = "split_reminders"
        private const val PREF_DAILY_INSIGHTS = "daily_insights"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_BUDGET,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when you're approaching or exceeding budget limits"
                },
                NotificationChannel(
                    CHANNEL_RECURRING,
                    "Bill Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for upcoming recurring expenses"
                },
                NotificationChannel(
                    CHANNEL_INSIGHTS,
                    "Spending Insights",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Daily and weekly spending summaries"
                },
                NotificationChannel(
                    CHANNEL_SAVINGS,
                    "Savings Milestones",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when you reach savings goals"
                },
                NotificationChannel(
                    CHANNEL_DEBT,
                    "Debt & Credit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for debts you owe and credits due to you"
                },
                NotificationChannel(
                    CHANNEL_SPLIT,
                    "Split Payments",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for split expense settlements"
                }
            )
            
            notificationManager.createNotificationChannels(channels)
        }
    }
    
    // ====================== MAIN ENTRY POINT ======================
    
    /**
     * Main entry point called by NotificationWorker - checks preferences and sends all notifications
     */
    suspend fun checkAndSendNotifications() {
        Log.d(TAG, "Starting comprehensive notification check...")
        
        // === BUDGET NOTIFICATIONS ===
        if (isBudgetAlertsEnabled()) {
            checkBudgetAlerts()
            checkOverspendingAlerts()
        }
        
        // === RECURRING BILL REMINDERS ===
        if (isRecurringRemindersEnabled()) {
            checkRecurringReminders()
        }
        
        // === DEBT & CREDIT NOTIFICATIONS ===
        if (isDebtRemindersEnabled()) {
            checkDebtDueReminders()
            checkDebtOverdueAlerts()
            checkCreditDueReminders()
        }
        
        // === SAVINGS GOAL NOTIFICATIONS ===
        if (isSavingsRemindersEnabled()) {
            checkSavingsMilestones()
            checkGoalDeadlines()
        }
        
        // === SPLIT PAYMENT NOTIFICATIONS ===
        if (isSplitRemindersEnabled()) {
            checkSplitBalanceReminders()
        }
        
        // === DAILY/WEEKLY INSIGHTS ===
        if (isDailyInsightsEnabled()) {
            sendDailyInsight()
        }
        
        // Weekly summary on Sundays
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            sendWeeklySummary()
        }
        
        // Cleanup old notifications (older than 30 days)
        notificationRepository.cleanupOldNotifications()
        
        Log.d(TAG, "Notification check completed")
    }
    
    // ====================== PREFERENCE CHECKS ======================
    
    fun isBudgetAlertsEnabled(): Boolean = prefs.getBoolean(PREF_BUDGET_ALERTS, true)
    fun isRecurringRemindersEnabled(): Boolean = prefs.getBoolean(PREF_RECURRING_REMINDERS, true)
    fun isDebtRemindersEnabled(): Boolean = prefs.getBoolean(PREF_DEBT_REMINDERS, true)
    fun isSavingsRemindersEnabled(): Boolean = prefs.getBoolean(PREF_SAVINGS_REMINDERS, true)
    fun isSplitRemindersEnabled(): Boolean = prefs.getBoolean(PREF_SPLIT_REMINDERS, true)
    fun isDailyInsightsEnabled(): Boolean = prefs.getBoolean(PREF_DAILY_INSIGHTS, false)
    
    // ====================== BUDGET NOTIFICATIONS ======================
    
    suspend fun checkBudgetAlerts() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error checking budget alerts: ${e.message}")
        }
    }
    
    private suspend fun checkOverspendingAlerts() {
        try {
            val monthlyExpenses = transactionRepository.getMonthlyExpenses().first() ?: 0.0
            val monthlyIncome = transactionRepository.getMonthlyIncome().first() ?: 0.0
            
            if (monthlyIncome > 0 && monthlyExpenses > monthlyIncome * 0.9) {
                val key = "overspending_${Calendar.getInstance().get(Calendar.MONTH)}"
                if (!prefs.getBoolean(key, false)) {
                    sendNotificationWithHistory(
                        channelId = CHANNEL_BUDGET,
                        notificationId = NOTIFICATION_OVERSPENDING,
                        title = "⚠️ Overspending Alert",
                        message = "You've spent ${formatCurrency(monthlyExpenses)} this month, which is ${((monthlyExpenses / monthlyIncome) * 100).toInt()}% of your income!",
                        icon = android.R.drawable.ic_dialog_alert,
                        notificationType = NotificationType.BUDGET_EXCEEDED
                    )
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overspending: ${e.message}")
        }
    }
    
    private suspend fun sendBudgetWarningNotification(budget: BudgetWithSpending) {
        val categoryName = budget.category?.name ?: "Unknown"
        val percentage = (budget.percentage * 100).toInt()
        val title = "⚠️ Budget Warning: $categoryName"
        val message = "You've used $percentage% of your budget. ${formatCurrency(budget.remaining)} remaining."
        
        sendNotificationWithHistory(
            channelId = CHANNEL_BUDGET,
            notificationId = NOTIFICATION_BUDGET_WARNING + categoryName.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_dialog_alert,
            notificationType = NotificationType.BUDGET_WARNING,
            actionData = """{"categoryId": "${budget.budget.categoryId}"}"""
        )
    }
    
    private suspend fun sendBudgetExceededNotification(budget: BudgetWithSpending) {
        val categoryName = budget.category?.name ?: "Unknown"
        val overAmount = budget.spent - budget.budget.amount
        val title = "🚨 Budget Exceeded: $categoryName"
        val message = "You've exceeded your budget by ${formatCurrency(overAmount)}!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_BUDGET,
            notificationId = NOTIFICATION_BUDGET_EXCEEDED + categoryName.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_dialog_alert,
            notificationType = NotificationType.BUDGET_EXCEEDED,
            actionData = """{"categoryId": "${budget.budget.categoryId}"}"""
        )
    }
    
    // ====================== RECURRING EXPENSE NOTIFICATIONS ======================
    
    suspend fun checkRecurringReminders() {
        try {
            val upcoming = recurringRepository.getUpcomingReminders()
            
            upcoming.forEach { recurring ->
                sendRecurringReminderNotification(recurring)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking recurring reminders: ${e.message}")
        }
    }
    
    private suspend fun sendRecurringReminderNotification(recurring: RecurringExpense) {
        val title = "📅 Bill Due: ${recurring.name}"
        val message = "${formatCurrency(recurring.amount)} due soon. Don't forget to pay!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_RECURRING,
            notificationId = NOTIFICATION_RECURRING_DUE + recurring.id.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_menu_my_calendar,
            notificationType = NotificationType.RECURRING_REMINDER,
            actionData = """{"recurringId": "${recurring.id}"}"""
        )
    }
    
    // ====================== DEBT NOTIFICATIONS ======================
    
    private suspend fun checkDebtDueReminders() {
        try {
            val debts = debtRepository.getActiveDebtsByType(DebtType.DEBT).first()
            val now = System.currentTimeMillis()
            val twoDaysAhead = now + (2 * 24 * 60 * 60 * 1000)
            
            debts.filter { it.dueDate != null && it.dueDate in now..twoDaysAhead && !it.isSettled }
                .forEach { debt ->
                    sendDebtDueNotification(debt)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking debt due reminders: ${e.message}")
        }
    }
    
    private suspend fun checkDebtOverdueAlerts() {
        try {
            val debts = debtRepository.getActiveDebtsByType(DebtType.DEBT).first()
            
            debts.filter { it.isOverdue }
                .forEach { debt ->
                    sendDebtOverdueNotification(debt)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overdue debts: ${e.message}")
        }
    }
    
    private suspend fun sendDebtDueNotification(debt: Debt) {
        val daysText = when (debt.daysUntilDue) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in ${debt.daysUntilDue} days"
        }
        
        val title = "💳 Debt Due: ${debt.personName}"
        val message = "You owe ${formatCurrency(debt.remainingAmount)} to ${debt.personName}, due $daysText"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_DEBT,
            notificationId = NOTIFICATION_DEBT_DUE + debt.id.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_dialog_alert,
            notificationType = NotificationType.DEBT_REMINDER,
            actionData = """{"debtId": "${debt.id}"}"""
        )
    }
    
    private suspend fun sendDebtOverdueNotification(debt: Debt) {
        val title = "🚨 Overdue Debt: ${debt.personName}"
        val message = "You owe ${formatCurrency(debt.remainingAmount)} to ${debt.personName}. This payment is overdue!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_DEBT,
            notificationId = NOTIFICATION_DEBT_OVERDUE + debt.id.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_dialog_alert,
            notificationType = NotificationType.DEBT_REMINDER,
            actionData = """{"debtId": "${debt.id}"}"""
        )
    }
    
    // ====================== CREDIT NOTIFICATIONS ======================
    
    private suspend fun checkCreditDueReminders() {
        try {
            val credits = debtRepository.getActiveDebtsByType(DebtType.CREDIT).first()
            val now = System.currentTimeMillis()
            val twoDaysAhead = now + (2 * 24 * 60 * 60 * 1000)
            
            credits.filter { it.dueDate != null && it.dueDate in now..twoDaysAhead && !it.isSettled }
                .forEach { credit ->
                    sendCreditDueNotification(credit)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking credit due reminders: ${e.message}")
        }
    }
    
    private suspend fun sendCreditDueNotification(credit: Debt) {
        val daysText = when (credit.daysUntilDue) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in ${credit.daysUntilDue} days"
        }
        
        val title = "💰 Credit Due: ${credit.personName}"
        val message = "${credit.personName} owes you ${formatCurrency(credit.remainingAmount)}, due $daysText. Send a reminder!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_DEBT,
            notificationId = NOTIFICATION_CREDIT_DUE + credit.id.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_menu_today,
            notificationType = NotificationType.DEBT_REMINDER,
            actionData = """{"debtId": "${credit.id}"}"""
        )
    }
    
    // ====================== SAVINGS GOAL NOTIFICATIONS ======================
    
    private suspend fun checkSavingsMilestones() {
        try {
            val goals = savingsGoalRepository.getActiveGoals().first()
            
            goals.forEach { goal ->
                val percentage = (goal.savedAmount / goal.targetAmount).toFloat()
                
                // Check for milestone achievements (25%, 50%, 75%, 100%)
                val milestones = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                
                for (milestone in milestones) {
                    if (percentage >= milestone) {
                        val milestoneKey = "savings_milestone_${goal.id}_${(milestone * 100).toInt()}"
                        val alreadyNotified = prefs.getBoolean(milestoneKey, false)
                        
                        if (!alreadyNotified) {
                            sendSavingsMilestoneNotification(goal.name, (milestone * 100).toInt())
                            prefs.edit().putBoolean(milestoneKey, true).apply()
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking savings milestones: ${e.message}")
        }
    }
    
    private suspend fun checkGoalDeadlines() {
        try {
            val goals = savingsGoalRepository.getActiveGoals().first()
            val now = System.currentTimeMillis()
            val oneWeekAhead = now + (7 * 24 * 60 * 60 * 1000)
            
            goals.filter { goal ->
                goal.targetDate != null && 
                goal.targetDate in now..oneWeekAhead && 
                !goal.isCompleted &&
                goal.progress < 1.0f
            }.forEach { goal ->
                sendGoalDeadlineNotification(goal)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking goal deadlines: ${e.message}")
        }
    }
    
    private suspend fun sendSavingsMilestoneNotification(goalName: String, percentage: Int) {
        val emoji = when (percentage) {
            25 -> "🌱"
            50 -> "🌿"
            75 -> "🌳"
            100 -> "🎉"
            else -> "✨"
        }
        
        val title = "$emoji Savings Milestone!"
        val message = when (percentage) {
            100 -> "Congratulations! You've reached your $goalName goal!"
            else -> "You're $percentage% of the way to your $goalName goal! Keep going!"
        }
        
        sendNotificationWithHistory(
            channelId = CHANNEL_SAVINGS,
            notificationId = NOTIFICATION_SAVINGS_MILESTONE + goalName.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.star_big_on,
            notificationType = NotificationType.SAVINGS_MILESTONE
        )
    }
    
    private suspend fun sendGoalDeadlineNotification(goal: SavingsGoal) {
        val daysLeft = ((goal.targetDate!! - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
        val remaining = goal.targetAmount - goal.savedAmount
        
        val title = "⏰ Goal Deadline Approaching: ${goal.name}"
        val message = "$daysLeft days left! You still need ${formatCurrency(remaining)} to reach your ${goal.name} goal."
        
        sendNotificationWithHistory(
            channelId = CHANNEL_SAVINGS,
            notificationId = NOTIFICATION_GOAL_DEADLINE + goal.id.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_menu_recent_history,
            notificationType = NotificationType.SAVINGS_MILESTONE,
            actionData = """{"goalId": "${goal.id}"}"""
        )
    }
    
    // ====================== SPLIT PAYMENT NOTIFICATIONS ======================
    
    private suspend fun checkSplitBalanceReminders() {
        try {
            val groupsWithSummary = splitRepository.getGroupsWithSummary()
            
            groupsWithSummary.forEach { splitGroup ->
                // yourBalance: Positive = owed to you, Negative = you owe others
                val balance = splitGroup.yourBalance
                
                // Notify if you owe more than a threshold (negative balance)
                if (balance < -100) {
                    val key = "split_owe_${splitGroup.group.id}_${Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)}"
                    if (!prefs.getBoolean(key, false)) {
                        sendSplitOwingNotification(splitGroup.group.name, -balance)
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
                
                // Notify if others owe you (positive balance)
                if (balance > 100) {
                    val key = "split_collect_${splitGroup.group.id}_${Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)}"
                    if (!prefs.getBoolean(key, false)) {
                        sendSplitCollectNotification(splitGroup.group.name, balance)
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking split balances: ${e.message}")
        }
    }
    
    private suspend fun sendSplitOwingNotification(groupName: String, amount: Double) {
        val title = "💸 Split Payment Due: $groupName"
        val message = "You owe ${formatCurrency(amount)} in the $groupName group. Time to settle up!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_SPLIT,
            notificationId = NOTIFICATION_SPLIT_BALANCE + groupName.hashCode(),
            title = title,
            message = message,
            icon = android.R.drawable.ic_menu_share,
            notificationType = NotificationType.SETTLEMENT_RECEIVED
        )
    }
    
    private suspend fun sendSplitCollectNotification(groupName: String, amount: Double) {
        val title = "💰 Collect Split Payment: $groupName"
        val message = "You're owed ${formatCurrency(amount)} in the $groupName group. Send a reminder to settle up!"
        
        sendNotificationWithHistory(
            channelId = CHANNEL_SPLIT,
            notificationId = NOTIFICATION_SPLIT_BALANCE + groupName.hashCode() + 1,
            title = title,
            message = message,
            icon = android.R.drawable.ic_menu_share,
            notificationType = NotificationType.SETTLEMENT_RECEIVED
        )
    }
    
    // ====================== DAILY/WEEKLY INSIGHTS ======================
    
    suspend fun sendDailyInsight() {
        try {
            val today = transactionRepository.getTodayTransactions().first()
            val todaySpending = today.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
            
            if (todaySpending > 0) {
                val monthlyExpenses = transactionRepository.getMonthlyExpenses().first() ?: 0.0
                val calendar = Calendar.getInstance()
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                val avgDailyBudget = if (dayOfMonth > 0) monthlyExpenses / dayOfMonth else 0.0
                
                val (emoji, message) = when {
                    todaySpending > avgDailyBudget * 2.0 -> 
                        "🔥" to "High spending day! You spent ${formatCurrency(todaySpending)} - more than double your daily average!"
                    todaySpending > avgDailyBudget * 1.5 -> 
                        "⚠️" to "You spent ${formatCurrency(todaySpending)} today - that's higher than your daily average."
                    todaySpending < avgDailyBudget * 0.5 -> 
                        "🎉" to "Great job! Only ${formatCurrency(todaySpending)} spent today. Keep it up!"
                    else -> 
                        "📊" to "Today's spending: ${formatCurrency(todaySpending)}. You're on track!"
                }
                
                sendNotificationWithHistory(
                    channelId = CHANNEL_INSIGHTS,
                    notificationId = NOTIFICATION_DAILY_INSIGHT,
                    title = "$emoji Daily Spending Update",
                    message = message,
                    icon = android.R.drawable.ic_menu_info_details,
                    notificationType = NotificationType.DAILY_INSIGHT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending daily insight: ${e.message}")
        }
    }
    
    private suspend fun sendWeeklySummary() {
        try {
            val weeklyExpenses = transactionRepository.getWeeklyExpenses().first() ?: 0.0
            val weeklyIncome = transactionRepository.getWeeklyIncome().first() ?: 0.0
            val savings = weeklyIncome - weeklyExpenses
            
            val (emoji, message) = if (savings >= 0) {
                "💰" to "This week: Spent ${formatCurrency(weeklyExpenses)}, Saved ${formatCurrency(savings)}. Great work!"
            } else {
                "⚠️" to "This week: Spent ${formatCurrency(weeklyExpenses)}, Over by ${formatCurrency(-savings)}. Watch your spending!"
            }
            
            sendNotificationWithHistory(
                channelId = CHANNEL_INSIGHTS,
                notificationId = NOTIFICATION_WEEKLY_SUMMARY,
                title = "$emoji Weekly Summary",
                message = message,
                icon = android.R.drawable.ic_menu_recent_history,
                notificationType = NotificationType.WEEKLY_SUMMARY
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending weekly summary: ${e.message}")
        }
    }
    
    // ====================== HELPER METHODS ======================
    
    /**
     * Send notification and save to history
     */
    private suspend fun sendNotificationWithHistory(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
        notificationType: NotificationType,
        actionData: String? = null
    ) {
        // Save to notification history
        val appNotification = AppNotification(
            type = notificationType,
            title = title,
            message = message,
            actionData = actionData
        )
        notificationRepository.saveNotification(appNotification)
        
        // Send system notification
        sendNotification(channelId, notificationId, title, message, icon)
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
        
        // Use app icon for all notifications
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
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
            Log.w(TAG, "Notification permission not granted")
        }
    }
    
    /**
     * Send a test notification for verification
     */
    suspend fun sendTestNotification() {
        sendNotificationWithHistory(
            channelId = CHANNEL_INSIGHTS,
            notificationId = 9999,
            title = "🔔 Test Notification",
            message = "Notifications are working correctly! You'll receive alerts for budgets, bills, debts, credits, goals, and splits.",
            icon = android.R.drawable.ic_dialog_info,
            notificationType = NotificationType.SYSTEM
        )
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }
}
