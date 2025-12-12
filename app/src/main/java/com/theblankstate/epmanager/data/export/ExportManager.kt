package com.theblankstate.epmanager.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    
    /**
     * Export transactions to CSV format
     */
    suspend fun exportToCSV(context: Context, startDate: Long? = null, endDate: Long? = null): Result<Uri> {
        return try {
            val transactions = if (startDate != null && endDate != null) {
                transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
            } else {
                transactionRepository.getAllTransactions().first()
            }
            
            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            
            val fileName = "expenses_${fileNameFormat.format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Time,Type,Category,Amount,Note\n")
                
                // Data rows
                transactions.forEach { transaction ->
                    val date = Date(transaction.date)
                    val category = transaction.categoryId?.let { categoryMap[it]?.name } ?: "Uncategorized"
                    val type = if (transaction.type == TransactionType.EXPENSE) "Expense" else "Income"
                    val note = transaction.note?.replace(",", ";")?.replace("\n", " ") ?: ""
                    
                    writer.append("${dateFormat.format(date)},")
                    writer.append("${timeFormat.format(date)},")
                    writer.append("$type,")
                    writer.append("$category,")
                    writer.append("${transaction.amount},")
                    writer.append("\"$note\"\n")
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
            
            val totalExpenses = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            val totalIncome = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            
            val categorySpending = transactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId }
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
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }
}
