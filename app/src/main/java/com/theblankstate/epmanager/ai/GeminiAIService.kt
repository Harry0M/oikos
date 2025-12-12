package com.theblankstate.epmanager.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.GenerativeModel
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered insights and categorization using Firebase AI (Gemini)
 * Uses Firebase's managed Gemini API - no user API key required!
 */
@Singleton
class GeminiAIService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    // Firebase AI model - automatically uses your Firebase project's Gemini quota
    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")
    }
    
    // Always initialized when using Firebase AI
    fun isInitialized(): Boolean = true
    
    /**
     * Suggest a category for a transaction based on note/description
     */
    suspend fun suggestCategory(note: String, amount: Double): Result<String> {
        val categories = categoryRepository.getAllCategories().first()
        val categoryNames = categories.map { "${it.name} (${it.type.name})" }.joinToString(", ")
        
        val prompt = """
            You are a personal finance assistant. Based on the transaction description, suggest the most appropriate category.
            
            Transaction: "$note"
            Amount: ₹$amount
            
            Available categories: $categoryNames
            
            Reply with ONLY the category name, nothing else. Pick the most relevant one.
        """.trimIndent()
        
        return try {
            val response = generativeModel.generateContent(prompt)
            val suggestedCategory = response.text?.trim() ?: ""
            
            // Find matching category
            val match = categories.find { 
                it.name.equals(suggestedCategory, ignoreCase = true) ||
                suggestedCategory.contains(it.name, ignoreCase = true)
            }
            
            Result.success(match?.name ?: categories.firstOrNull()?.name ?: "Other")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate personalized spending insights
     */
    suspend fun generateSpendingInsights(): Result<List<SpendingInsight>> {
        // Get recent transactions
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.timeInMillis
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
        val categories = categoryRepository.getAllCategories().first()
        val categoryMap = categories.associateBy { it.id }
        
        if (transactions.isEmpty()) {
            return Result.success(listOf(
                SpendingInsight(
                    title = "Start Tracking!",
                    description = "Add some transactions to get personalized AI insights about your spending patterns.",
                    type = InsightType.TIP,
                    emoji = "📊"
                )
            ))
        }
        
        // Build spending summary
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalSpent = expenses.sumOf { it.amount }
        val categorySpending = expenses
            .groupBy { categoryMap[it.categoryId]?.name ?: "Other" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val topCategories = categorySpending.take(5).joinToString("\n") { 
            "- ${it.first}: ₹${String.format("%.0f", it.second)}" 
        }
        
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val recentTransactions = expenses.take(10).joinToString("\n") {
            "- ${dateFormat.format(Date(it.date))}: ${categoryMap[it.categoryId]?.name ?: "Other"} - ₹${String.format("%.0f", it.amount)} (${it.note ?: "no note"})"
        }
        
        val prompt = """
            You are a helpful personal finance advisor. Analyze this spending data and provide 3-4 actionable insights.
            
            SPENDING SUMMARY (Last 30 days):
            Total Spent: ₹${String.format("%.0f", totalSpent)}
            Number of transactions: ${expenses.size}
            
            TOP SPENDING CATEGORIES:
            $topCategories
            
            RECENT TRANSACTIONS:
            $recentTransactions
            
            Provide insights in this JSON format (no markdown, just raw JSON):
            [
                {"emoji": "🎯", "title": "insight title", "description": "2-3 sentence insight", "type": "WARNING|TIP|ACHIEVEMENT|PREDICTION"},
                ...
            ]
            
            Be specific with numbers. Be encouraging but honest. Focus on:
            1. Unusual spending patterns
            2. Savings opportunities
            3. Positive progress (if any)
            4. Predictions for next month
        """.trimIndent()
        
        return try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim() ?: "[]"
            
            // Parse the JSON response
            val insights = parseInsightsFromJson(text)
            Result.success(insights)
        } catch (e: Exception) {
            // Return fallback insights on error
            Result.success(generateFallbackInsights(transactions, categorySpending))
        }
    }
    
    /**
     * Predict next month's spending
     */
    suspend fun predictNextMonthSpending(): Result<SpendingPrediction> {
        // Get last 3 months data
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -3)
        val startDate = calendar.timeInMillis
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        
        if (expenses.size < 10) {
            return Result.success(SpendingPrediction(
                predictedAmount = 0.0,
                confidence = "Low",
                reasoning = "Not enough transaction history for accurate prediction. Keep tracking for better insights!"
            ))
        }
        
        val monthlySpending = expenses
            .groupBy { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        
        val avgMonthly = monthlySpending.values.average()
        val trend = if (monthlySpending.size >= 2) {
            val values = monthlySpending.values.toList()
            if (values.last() > values.first()) "increasing" else "decreasing"
        } else "stable"
        
        val prompt = """
            Based on spending trends, predict next month's spending.
            
            Monthly spending history:
            ${monthlySpending.entries.joinToString("\n") { "- ${it.key}: ₹${String.format("%.0f", it.value)}" }}
            
            Average: ₹${String.format("%.0f", avgMonthly)}
            Trend: $trend
            
            Reply with ONLY this JSON format:
            {"predicted_amount": 12345, "confidence": "High|Medium|Low", "reasoning": "brief explanation"}
        """.trimIndent()
        
        return try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim() ?: ""
            
            // Simple parsing
            val amountRegex = """"predicted_amount"\s*:\s*(\d+(?:\.\d+)?)""".toRegex()
            val confidenceRegex = """"confidence"\s*:\s*"(\w+)"""".toRegex()
            val reasoningRegex = """"reasoning"\s*:\s*"([^"]+)"""".toRegex()
            
            val amount = amountRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: avgMonthly
            val confidence = confidenceRegex.find(text)?.groupValues?.get(1) ?: "Medium"
            val reasoning = reasoningRegex.find(text)?.groupValues?.get(1) ?: "Based on your average spending pattern"
            
            Result.success(SpendingPrediction(amount, confidence, reasoning))
        } catch (e: Exception) {
            Result.success(SpendingPrediction(
                avgMonthly,
                "Medium",
                "Based on your 3-month average spending of ₹${String.format("%.0f", avgMonthly)}"
            ))
        }
    }
    
    private fun parseInsightsFromJson(json: String): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        
        // Simple regex-based parsing
        val pattern = """\{"emoji"\s*:\s*"([^"]+)"\s*,\s*"title"\s*:\s*"([^"]+)"\s*,\s*"description"\s*:\s*"([^"]+)"\s*,\s*"type"\s*:\s*"(\w+)"\s*\}""".toRegex()
        
        pattern.findAll(json).forEach { match ->
            val (emoji, title, description, type) = match.destructured
            try {
                insights.add(SpendingInsight(
                    emoji = emoji,
                    title = title,
                    description = description,
                    type = InsightType.valueOf(type.uppercase())
                ))
            } catch (e: Exception) {
                // Skip invalid insight type
            }
        }
        
        return insights.ifEmpty {
            listOf(SpendingInsight(
                emoji = "💡",
                title = "Keep Going!",
                description = "Continue tracking your expenses to unlock more personalized insights.",
                type = InsightType.TIP
            ))
        }
    }
    
    private fun generateFallbackInsights(
        transactions: List<Transaction>,
        categorySpending: List<Pair<String, Double>>
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        
        val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        
        // Top category insight
        categorySpending.firstOrNull()?.let { (category, amount) ->
            val percentage = (amount / totalSpent * 100).toInt()
            insights.add(SpendingInsight(
                emoji = "📊",
                title = "Top Spending: $category",
                description = "You spent ₹${String.format("%.0f", amount)} on $category this month, which is $percentage% of your total spending.",
                type = InsightType.TIP
            ))
        }
        
        // Transaction count insight
        insights.add(SpendingInsight(
            emoji = "📈",
            title = "${transactions.size} Transactions",
            description = "You made ${transactions.size} transactions this month averaging ₹${String.format("%.0f", totalSpent / transactions.size.coerceAtLeast(1))} each.",
            type = InsightType.TIP
        ))
        
        return insights
    }
}

data class SpendingInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val emoji: String = "💡"
)

enum class InsightType {
    WARNING, TIP, ACHIEVEMENT, PREDICTION
}

data class SpendingPrediction(
    val predictedAmount: Double,
    val confidence: String,
    val reasoning: String
)
