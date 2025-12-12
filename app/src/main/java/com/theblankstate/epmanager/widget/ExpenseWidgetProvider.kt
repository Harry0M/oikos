package com.theblankstate.epmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.theblankstate.epmanager.MainActivity
import com.theblankstate.epmanager.R
import com.theblankstate.epmanager.data.local.ExpenseDatabase
import com.theblankstate.epmanager.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ExpenseWidgetProvider : AppWidgetProvider() {
    
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        job.cancel()
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Create RemoteViews
        val views = RemoteViews(context.packageName, R.layout.expense_widget_layout)
        
        // Set click intent to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set today's date
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        views.setTextViewText(R.id.widget_date, dateFormat.format(Date()))
        
        // Fetch data asynchronously
        coroutineScope.launch {
            try {
                val db = ExpenseDatabase.getInstance(context)
                val transactionDao = db.transactionDao()
                
                // Get today's start and end timestamps
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val todayEnd = calendar.timeInMillis
                
                // Get today's transactions
                val todayTransactions = transactionDao.getTransactionsBetweenDatesSync(
                    todayStart, todayEnd
                )
                
                var todayExpense = 0.0
                var todayIncome = 0.0
                
                todayTransactions.forEach { transaction ->
                    when (transaction.type) {
                        TransactionType.EXPENSE -> todayExpense += transaction.amount
                        TransactionType.INCOME -> todayIncome += transaction.amount
                    }
                }
                
                val balance = todayIncome - todayExpense
                
                // Update views on main thread
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                views.setTextViewText(R.id.widget_expense_amount, currencyFormat.format(todayExpense))
                views.setTextViewText(R.id.widget_income_amount, currencyFormat.format(todayIncome))
                views.setTextViewText(R.id.widget_balance_amount, currencyFormat.format(balance))
                
                // Update widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // Set default values on error
                views.setTextViewText(R.id.widget_expense_amount, "₹0")
                views.setTextViewText(R.id.widget_income_amount, "₹0")
                views.setTextViewText(R.id.widget_balance_amount, "₹0")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
        
        // Update with placeholder first (will be updated when data loads)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    companion object {
        fun requestUpdate(context: Context) {
            val intent = Intent(context, ExpenseWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
