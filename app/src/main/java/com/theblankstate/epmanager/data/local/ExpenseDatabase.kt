package com.theblankstate.epmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.theblankstate.epmanager.data.local.dao.AccountDao
import com.theblankstate.epmanager.data.local.dao.BudgetDao
import com.theblankstate.epmanager.data.local.dao.CategoryDao
import com.theblankstate.epmanager.data.local.dao.RecurringExpenseDao
import com.theblankstate.epmanager.data.local.dao.SavingsGoalDao
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.local.dao.SplitDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Budget
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.ExpenseShare
import com.theblankstate.epmanager.data.model.GroupMember
import com.theblankstate.epmanager.data.model.PendingSms
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.model.Settlement
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.data.model.SplitExpense
import com.theblankstate.epmanager.data.model.SplitGroup
import com.theblankstate.epmanager.data.model.Transaction

@Database(
    entities = [
        Category::class,
        Account::class,
        Transaction::class,
        Budget::class,
        RecurringExpense::class,
        SavingsGoal::class,
        SplitGroup::class,
        GroupMember::class,
        SplitExpense::class,
        ExpenseShare::class,
        Settlement::class,
        SmsTemplate::class,
        PendingSms::class
    ],
    version = 6, // Added SmsTemplate and PendingSms entities
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun splitDao(): SplitDao
    abstract fun smsTemplateDao(): SmsTemplateDao
    
    companion object {
        const val DATABASE_NAME = "expense_database"
        
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null
        
        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
