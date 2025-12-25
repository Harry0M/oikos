package com.theblankstate.epmanager.di

import android.content.Context
import androidx.room.Room
import com.theblankstate.epmanager.data.local.ExpenseDatabase
import com.theblankstate.epmanager.data.local.dao.AccountDao
import com.theblankstate.epmanager.data.local.dao.BudgetDao
import com.theblankstate.epmanager.data.local.dao.CategoryDao
import com.theblankstate.epmanager.data.local.dao.DebtDao
import com.theblankstate.epmanager.data.local.dao.FriendDao
import com.theblankstate.epmanager.data.local.dao.RecurringExpenseDao
import com.theblankstate.epmanager.data.local.dao.SavingsGoalDao
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.local.dao.SplitDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideExpenseDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase {
        return Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            ExpenseDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - use proper migrations in production
            .build()
    }
    
    @Provides
    @Singleton
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    @Singleton
    fun provideAccountDao(database: ExpenseDatabase): AccountDao {
        return database.accountDao()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: ExpenseDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideBudgetDao(database: ExpenseDatabase): BudgetDao {
        return database.budgetDao()
    }
    
    @Provides
    @Singleton
    fun provideRecurringExpenseDao(database: ExpenseDatabase): RecurringExpenseDao {
        return database.recurringExpenseDao()
    }
    
    @Provides
    @Singleton
    fun provideSavingsGoalDao(database: ExpenseDatabase): SavingsGoalDao {
        return database.savingsGoalDao()
    }
    
    @Provides
    @Singleton
    fun provideSplitDao(database: ExpenseDatabase): SplitDao {
        return database.splitDao()
    }
    
    @Provides
    @Singleton
    fun provideSmsTemplateDao(database: ExpenseDatabase): SmsTemplateDao {
        return database.smsTemplateDao()
    }
    
    @Provides
    @Singleton
    fun provideFriendDao(database: ExpenseDatabase): FriendDao {
        return database.friendDao()
    }
    
    @Provides
    @Singleton
    fun provideDebtDao(database: ExpenseDatabase): DebtDao {
        return database.debtDao()
    }
    
    @Provides
    @Singleton
    fun provideTermsAcceptanceDao(database: ExpenseDatabase): com.theblankstate.epmanager.data.local.dao.TermsAcceptanceDao {
        return database.termsAcceptanceDao()
    }
}
