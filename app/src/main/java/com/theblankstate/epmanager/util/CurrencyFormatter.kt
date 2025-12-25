package com.theblankstate.epmanager.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.CurrencyProvider
import com.theblankstate.epmanager.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for formatting currency amounts across the app.
 */
@Singleton
class CurrencyFormatter @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Get the current currency symbol.
     */
    val symbolFlow: Flow<String> = userPreferencesRepository.currencySymbol
    
    /**
     * Get the current currency code.
     */
    val codeFlow: Flow<String> = userPreferencesRepository.selectedCurrencyCode
    
    /**
     * Synchronously get the currency symbol (use sparingly, prefer flow version).
     */
    fun getSymbolBlocking(): String = runBlocking {
        userPreferencesRepository.currencySymbol.first()
    }
    
    /**
     * Format amount with currency symbol.
     */
    suspend fun format(amount: Double): String {
        val symbol = userPreferencesRepository.currencySymbol.first()
        return "$symbol${String.format("%.0f", amount)}"
    }
    
    /**
     * Format amount with currency symbol (with decimals).
     */
    suspend fun formatWithDecimals(amount: Double): String {
        val symbol = userPreferencesRepository.currencySymbol.first()
        return "$symbol${String.format("%.2f", amount)}"
    }
}

/**
 * Composable to get the current currency symbol.
 */
@Composable
fun rememberCurrencySymbol(
    currencyViewModel: CurrencyViewModel = hiltViewModel()
): String {
    val symbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    return symbol
}
