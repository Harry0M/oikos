package com.theblankstate.epmanager.ui.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.sms.AiSmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsSettingsUiState(
    val customTemplates: List<SmsTemplate> = emptyList(),
    val isLearning: Boolean = false,
    val learnResult: String? = null,
    val error: String? = null
)

@HiltViewModel
class SmsSettingsViewModel @Inject constructor(
    private val smsTemplateDao: SmsTemplateDao,
    private val aiSmsParser: AiSmsParser
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SmsSettingsUiState())
    val uiState: StateFlow<SmsSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCustomTemplates()
    }
    
    private fun loadCustomTemplates() {
        viewModelScope.launch {
            smsTemplateDao.getAllTemplates().collect { templates ->
                _uiState.update { it.copy(customTemplates = templates) }
            }
        }
    }
    
    fun addCustomBank(bankName: String, senderIds: String) {
        viewModelScope.launch {
            try {
                val template = SmsTemplate(
                    bankName = bankName.trim(),
                    senderIds = senderIds.split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                        .joinToString(","),
                    isCustom = true,
                    isActive = true
                )
                smsTemplateDao.insertTemplate(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add bank: ${e.message}") }
            }
        }
    }
    
    fun deleteTemplate(template: SmsTemplate) {
        viewModelScope.launch {
            try {
                smsTemplateDao.deleteTemplate(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }
    
    fun learnFromSample(smsText: String, senderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLearning = true, error = null) }
            
            try {
                // First, parse the SMS to verify AI can understand it
                val parseResult = aiSmsParser.parseSms(smsText, senderId)
                
                if (parseResult?.amount != null && parseResult.amount > 0) {
                    // SMS was successfully parsed, now learn patterns
                    val suggestedPatterns = aiSmsParser.learnPatternsFromSample(smsText, senderId)
                    
                    // Create or update template for this sender
                    val existingTemplate = smsTemplateDao.getTemplateBySenderId(senderId.uppercase())
                    
                    if (existingTemplate != null) {
                        // Update existing template with new patterns
                        val updatedPattern = suggestedPatterns?.amountPattern
                        
                        smsTemplateDao.updateTemplate(
                            existingTemplate.copy(
                                amountPattern = updatedPattern ?: existingTemplate.amountPattern,
                                merchantPattern = suggestedPatterns?.merchantPattern ?: existingTemplate.merchantPattern,
                                accountPattern = suggestedPatterns?.accountPattern ?: existingTemplate.accountPattern,
                                usageCount = existingTemplate.usageCount + 1,
                                lastUsedAt = System.currentTimeMillis()
                            )
                        )
                        
                        val txType = if (parseResult.isDebit) "Debit" else "Credit"
                        _uiState.update { 
                            it.copy(
                                isLearning = false,
                                learnResult = "Updated patterns for ${existingTemplate.bankName}! " +
                                    "Detected: $txType of ₹${parseResult.amount}"
                            )
                        }
                    } else {
                        // Create new template
                        val bankName = parseResult.bankName?.ifBlank { null } 
                            ?: senderId.replace(Regex("[^A-Za-z]"), "").take(10) + " Bank"
                        
                        val newTemplate = SmsTemplate(
                            bankName = bankName,
                            senderIds = senderId.uppercase(),
                            amountPattern = suggestedPatterns?.amountPattern,
                            accountPattern = suggestedPatterns?.accountPattern,
                            merchantPattern = suggestedPatterns?.merchantPattern,
                            debitKeywords = suggestedPatterns?.debitKeywords?.joinToString(","),
                            creditKeywords = suggestedPatterns?.creditKeywords?.joinToString(","),
                            sampleSms = smsText.take(500),
                            isCustom = true,
                            isActive = true,
                            usageCount = 1,
                            lastUsedAt = System.currentTimeMillis()
                        )
                        
                        smsTemplateDao.insertTemplate(newTemplate)
                        
                        val txType = if (parseResult.isDebit) "Debit" else "Credit"
                        _uiState.update { 
                            it.copy(
                                isLearning = false,
                                learnResult = "Created template for $bankName! " +
                                    "Detected: $txType of ₹${parseResult.amount}"
                            )
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLearning = false,
                            error = "Could not detect amount from SMS. Please verify it's a transaction SMS."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLearning = false,
                        error = "Learning failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearResult() {
        _uiState.update { it.copy(learnResult = null) }
    }
}
