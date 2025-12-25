package com.theblankstate.epmanager.util

import androidx.lifecycle.ViewModel
import com.theblankstate.epmanager.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel for accessing currency information across the app.
 */
@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    val currencySymbol: Flow<String> = userPreferencesRepository.currencySymbol
    
    val currencyCode: Flow<String> = userPreferencesRepository.selectedCurrencyCode
}
