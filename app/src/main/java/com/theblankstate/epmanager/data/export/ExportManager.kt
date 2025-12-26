package com.theblankstate.epmanager.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filter options for export
 */
data class ExportFilters(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val categoryIds: Set<String>? = null,
    val accountIds: Set<String>? = null,
    val transactionTypes: Set<TransactionType>? = null,
    val includeNotes: Boolean = true,
    val includeAccountInfo: Boolean = true
)

@Singleton
class ExportManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    
    /**
     * Export transactions to CSV format with filters
     */
    suspend fun exportToCSV(
        context: Context, 
        filters: ExportFilters = ExportFilters()
    ): Result<Uri> {
        return try {
            // Get all transactions first
            var transactions = if (filters.startDate != null && filters.endDate != null) {
                transactionRepository.getTransactionsByDateRange(filters.startDate, filters.endDate).first()
            } else {
                transactionRepository.getAllTransactions().first()
            }
            
            // Apply category filter
            filters.categoryIds?.let { categoryIds ->
                if (categoryIds.isNotEmpty()) {
                    transactions = transactions.filter { 
                        it.categoryId in categoryIds || (it.categoryId == null && "uncategorized" in categoryIds)
                    }
                }
            }
            
            // Apply account filter
            filters.accountIds?.let { accountIds ->
                if (accountIds.isNotEmpty()) {
                    transactions = transactions.filter { 
                        it.accountId in accountIds || (it.accountId == null && "default" in accountIds)
                    }
                }
            }
            
            // Apply transaction type filter
            filters.transactionTypes?.let { types ->
                if (types.isNotEmpty()) {
                    transactions = transactions.filter { it.type in types }
                }
            }
            
            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            val accounts = accountRepository.getAllAccounts().first()
            val accountMap = accounts.associateBy { it.id }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            
            val fileName = "expenses_${fileNameFormat.format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header
                val headers = mutableListOf("Date", "Time", "Type", "Category", "Amount")
                if (filters.includeAccountInfo) {
                    headers.add("Account")
                }
                if (filters.includeNotes) {
                    headers.add("Note")
                }
                writer.append(headers.joinToString(",") + "\n")
                
                // Data rows
                transactions.forEach { transaction ->
                    val date = Date(transaction.date)
                    val category = transaction.categoryId?.let { categoryMap[it]?.name } ?: "Uncategorized"
                    val account = transaction.accountId?.let { accountMap[it]?.name } ?: "Default"
                    val type = if (transaction.type == TransactionType.EXPENSE) "Expense" else "Income"
                    val note = transaction.note?.replace(",", ";")?.replace("\n", " ") ?: ""
                    
                    val row = mutableListOf(
                        dateFormat.format(date),
                        timeFormat.format(date),
                        type,
                        category,
                        transaction.amount.toString()
                    )
                    if (filters.includeAccountInfo) {
                        row.add(account)
                    }
                    if (filters.includeNotes) {
                        row.add("\"$note\"")
                    }
                    writer.append(row.joinToString(",") + "\n")
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get export statistics for preview
     */
    suspend fun getExportPreview(filters: ExportFilters): ExportPreview {
        var transactions = if (filters.startDate != null && filters.endDate != null) {
            transactionRepository.getTransactionsByDateRange(filters.startDate, filters.endDate).first()
        } else {
            transactionRepository.getAllTransactions().first()
        }
        
        // Apply category filter
        filters.categoryIds?.let { categoryIds ->
            if (categoryIds.isNotEmpty()) {
                transactions = transactions.filter { 
                    it.categoryId in categoryIds || (it.categoryId == null && "uncategorized" in categoryIds)
                }
            }
        }
        
        // Apply account filter
        filters.accountIds?.let { accountIds ->
            if (accountIds.isNotEmpty()) {
                transactions = transactions.filter { 
                    it.accountId in accountIds || (it.accountId == null && "default" in accountIds)
                }
            }
        }
        
        // Apply transaction type filter
        filters.transactionTypes?.let { types ->
            if (types.isNotEmpty()) {
                transactions = transactions.filter { it.type in types }
            }
        }
        
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME && it.categoryId != "adjustment" }.sumOf { it.amount }
        
        return ExportPreview(
            transactionCount = transactions.size,
            totalExpenses = totalExpenses,
            totalIncome = totalIncome
        )
    }
    
    /**
     * Export summary report as text (simpler than PDF)
     */
    suspend fun exportSummaryReport(context: Context, month: Int, year: Int): Result<Uri> {
        return try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val endDate = calendar.timeInMillis
            
            val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }
            val accounts = accountRepository.getAllAccounts().first()
            val accountMap = accounts.associateBy { it.id }
            
            val totalExpenses = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            val totalIncome = transactions
                .filter { it.type == TransactionType.INCOME && it.categoryId != "adjustment" }
                .sumOf { it.amount }
            
            val categorySpending = transactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
            
            val accountSpending = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.accountId }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
            
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(startDate))
            
            val fileName = "report_${monthName.replace(" ", "_")}.txt"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.append("═══════════════════════════════════════\n")
                writer.append("     EP MANAGER - MONTHLY REPORT\n")
                writer.append("            $monthName\n")
                writer.append("═══════════════════════════════════════\n\n")
                
                writer.append("📊 SUMMARY\n")
                writer.append("───────────────────────────────────────\n")
                writer.append("Total Income:    ₹${String.format("%,.2f", totalIncome)}\n")
                writer.append("Total Expenses:  ₹${String.format("%,.2f", totalExpenses)}\n")
                writer.append("Net Savings:     ₹${String.format("%,.2f", totalIncome - totalExpenses)}\n")
                writer.append("Transactions:    ${transactions.size}\n\n")
                
                writer.append("📈 EXPENSES BY CATEGORY\n")
                writer.append("───────────────────────────────────────\n")
                
                categorySpending.forEach { (categoryId, amount) ->
                    val categoryName = categoryId?.let { categoryMap[it]?.name } ?: "Other"
                    val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100) else 0.0
                    writer.append("${categoryName.padEnd(20)} ₹${String.format("%,.2f", amount).padStart(12)} (${String.format("%.1f", percentage)}%)\n")
                }
                
                writer.append("\n💳 EXPENSES BY ACCOUNT\n")
                writer.append("───────────────────────────────────────\n")
                
                accountSpending.forEach { (accountId, amount) ->
                    val accountName = accountId?.let { accountMap[it]?.name } ?: "Default"
                    val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100) else 0.0
                    writer.append("${accountName.padEnd(20)} ₹${String.format("%,.2f", amount).padStart(12)} (${String.format("%.1f", percentage)}%)\n")
                }
                
                writer.append("\n═══════════════════════════════════════\n")
                writer.append("Generated by EP Manager\n")
                writer.append("${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}\n")
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Share file via Android share sheet
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

/**
 * Preview data for export
 */
data class ExportPreview(
    val transactionCount: Int,
    val totalExpenses: Double,
    val totalIncome: Double
)
