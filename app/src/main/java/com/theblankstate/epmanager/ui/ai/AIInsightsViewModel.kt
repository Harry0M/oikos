package com.theblankstate.epmanager.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.ai.GeminiAIService
import com.theblankstate.epmanager.ai.SpendingInsight
import com.theblankstate.epmanager.ai.SpendingPrediction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIInsightsUiState(
    val isLoading: Boolean = false,
    val insights: List<SpendingInsight> = emptyList(),
    val prediction: SpendingPrediction? = null,
    val error: String? = null
)

@HiltViewModel
class AIInsightsViewModel @Inject constructor(
    private val aiService: GeminiAIService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIInsightsUiState())
    val uiState: StateFlow<AIInsightsUiState> = _uiState.asStateFlow()
    
    init {
        // Auto-load insights on init - no API key needed with Firebase AI
        loadInsights()
    }
    
    fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Load insights
            aiService.generateSpendingInsights()
                .onSuccess { insights ->
                    _uiState.update { it.copy(insights = insights) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            // Load prediction
            aiService.predictNextMonthSpending()
                .onSuccess { prediction ->
                    _uiState.update { it.copy(prediction = prediction) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun suggestCategory(note: String, amount: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            aiService.suggestCategory(note, amount)
                .onSuccess { category ->
                    onResult(category)
                }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
