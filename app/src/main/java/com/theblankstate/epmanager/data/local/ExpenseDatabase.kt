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
import com.theblankstate.epmanager.data.local.dao.NotificationDao
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
import com.theblankstate.epmanager.data.model.AppNotification

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
        TermsAcceptance::class,
        AppNotification::class
    ],
    version = 16, // Added AppNotification for notification history
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
    abstract fun notificationDao(): NotificationDao
    
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
                
                val MIGRATION_15_16 = object : Migration(15, 16) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE IF NOT EXISTS app_notifications (
                                id TEXT PRIMARY KEY NOT NULL,
                                type TEXT NOT NULL,
                                title TEXT NOT NULL,
                                message TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                isRead INTEGER NOT NULL DEFAULT 0,
                                actionData TEXT
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_createdAt ON app_notifications(createdAt)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_isRead ON app_notifications(isRead)")
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
