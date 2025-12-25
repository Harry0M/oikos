package com.theblankstate.epmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.theblankstate.epmanager.data.local.dao.AccountDao
import com.theblankstate.epmanager.data.local.dao.BudgetDao
import com.theblankstate.epmanager.data.local.dao.CategoryDao
import com.theblankstate.epmanager.data.local.dao.DebtDao
import com.theblankstate.epmanager.data.local.dao.FriendDao
import com.theblankstate.epmanager.data.local.dao.RecurringExpenseDao
import com.theblankstate.epmanager.data.local.dao.SavingsGoalDao
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.local.dao.SplitDao
import com.theblankstate.epmanager.data.local.dao.TermsAcceptanceDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Budget
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtPayment
import com.theblankstate.epmanager.data.model.ExpenseShare
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.data.model.FriendRequest
import com.theblankstate.epmanager.data.model.GroupMember
import com.theblankstate.epmanager.data.model.PendingSms
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.model.Settlement
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.data.model.SplitExpense
import com.theblankstate.epmanager.data.model.SplitGroup
import com.theblankstate.epmanager.data.model.TermsAcceptance
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
        PendingSms::class,
        Friend::class,
        FriendRequest::class,
        Debt::class,
        DebtPayment::class,
        TermsAcceptance::class
    ],
    version = 15, // Added TermsAcceptance for T&C tracking
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
    abstract fun friendDao(): FriendDao
    abstract fun debtDao(): DebtDao
    abstract fun termsAcceptanceDao(): TermsAcceptanceDao
    
    companion object {
        const val DATABASE_NAME = "expense_database"
        
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null
        
        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val MIGRATION_13_14 = object : Migration(13, 14) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE transactions ADD COLUMN goalId TEXT DEFAULT NULL")
                        database.execSQL("ALTER TABLE transactions ADD COLUMN debtId TEXT DEFAULT NULL")
                    }
                }
                
                val MIGRATION_14_15 = object : Migration(14, 15) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE IF NOT EXISTS terms_acceptances (
                                id TEXT PRIMARY KEY NOT NULL,
                                termsVersion TEXT NOT NULL,
                                acceptedAt INTEGER NOT NULL,
                                userId TEXT,
                                deviceId TEXT
                            )
                        """.trimIndent())
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
