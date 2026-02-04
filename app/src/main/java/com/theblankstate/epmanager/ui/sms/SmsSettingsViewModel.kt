package com.theblankstate.epmanager.ui.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.AvailableBank
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.data.repository.AvailableBankRepository
import com.theblankstate.epmanager.sms.AiSmsParser
import com.theblankstate.epmanager.sms.BankDiscoveryResult
import com.theblankstate.epmanager.sms.BankDiscoveryScanner
import com.theblankstate.epmanager.sms.SmsInboxScanner
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsSettingsUiState(
    val customTemplates: List<SmsTemplate> = emptyList(),
    val availableBanks: List<AvailableBank> = emptyList(), // Banks from AvailableBankRepository
    val uncategorizedSenders: List<String> = emptyList(),
    val isLearning: Boolean = false,
    val learnResult: String? = null,
    val error: String? = null,
    // Scan State
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress? = null,
    // Bank Discovery State
    val isDiscovering: Boolean = false,
    val discoveryResult: BankDiscoveryResult? = null
)

data class ScanProgress(
    val scanned: Int,
    val found: Int,
    val new: Int,
    val isComplete: Boolean = false
)

@HiltViewModel
class SmsSettingsViewModel @Inject constructor(
    private val smsTemplateDao: SmsTemplateDao,
    private val transactionDao: TransactionDao,
    private val aiSmsParser: AiSmsParser,
    private val smsInboxScanner: SmsInboxScanner,
    private val bankDiscoveryScanner: BankDiscoveryScanner,
    private val availableBankRepository: AvailableBankRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val PREFS_NAME = "sms_settings_prefs"
        private const val KEY_LAST_SCAN_TIMESTAMP = "last_scan_timestamp"
        private const val SCAN_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SmsSettingsUiState())
    val uiState: StateFlow<SmsSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCustomTemplates()
        loadAvailableBanks()
        loadUncategorizedSenders()
    }
    
    /**
     * Check if auto-scan should run:
     * - Returns true if never scanned before (first install)
     * - Returns true if more than 7 days since last scan
     */
    fun shouldAutoScan(): Boolean {
        val lastScanTimestamp = prefs.getLong(KEY_LAST_SCAN_TIMESTAMP, 0L)
        if (lastScanTimestamp == 0L) {
            return true // Never scanned - first install
        }
        val timeSinceLastScan = System.currentTimeMillis() - lastScanTimestamp
        return timeSinceLastScan >= SCAN_INTERVAL_MS
    }
    
    /**
     * Save the current timestamp as last scan time
     */
    private fun saveLastScanTimestamp() {
        prefs.edit().putLong(KEY_LAST_SCAN_TIMESTAMP, System.currentTimeMillis()).apply()
    }
    
    private fun loadCustomTemplates() {
        viewModelScope.launch {
            smsTemplateDao.getAllTemplates().collect { templates ->
                _uiState.update { it.copy(customTemplates = templates) }
            }
        }
    }
    
    /**
     * Load available banks from repository (persisted banks)
     */
    private fun loadAvailableBanks() {
        viewModelScope.launch {
            availableBankRepository.getAllBanks().collect { banks ->
                _uiState.update { it.copy(availableBanks = banks) }
                
                // Also create a BankDiscoveryResult for UI compatibility if banks exist
                if (banks.isNotEmpty()) {
                    val result = availableBankRepository.toBankDiscoveryResult()
                    _uiState.update { it.copy(discoveryResult = result) }
                }
            }
        }
    }
    
    private fun loadUncategorizedSenders() {
        viewModelScope.launch {
            transactionDao.getUncategorizedSmsSenders().collect { senders ->
                _uiState.update { it.copy(uncategorizedSenders = senders) }
            }
        }
    }

    /**
     * Add a custom bank template or update existing one.
     * Saves to both SmsTemplate (for parsing) and AvailableBankRepository (for UI).
     */
    fun addCustomBank(bankName: String, senderIds: String) {
        viewModelScope.launch {
            try {
                val newSenderIds = senderIds.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                
                if (newSenderIds.isEmpty()) return@launch
                
                // Save to AvailableBankRepository (primary storage)
                availableBankRepository.addCustomBank(
                    bankName = bankName.trim(),
                    senderIds = newSenderIds.joinToString(",")
                )
                
                // Also save to SmsTemplate for backward compatibility with parsing
                val existingTemplate = smsTemplateDao.getTemplateByBankName(bankName.trim())
                
                if (existingTemplate != null) {
                    val existingSenderIds = existingTemplate.senderIds.split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    
                    val mergedSenderIds = (existingSenderIds + newSenderIds).joinToString(",")
                    
                    val updatedTemplate = existingTemplate.copy(
                        senderIds = mergedSenderIds
                    )
                    smsTemplateDao.updateTemplate(updatedTemplate)
                } else {
                    val template = SmsTemplate(
                        bankName = bankName.trim(),
                        senderIds = newSenderIds.joinToString(","),
                        isCustom = true,
                        isActive = true
                    )
                    smsTemplateDao.insertTemplate(template)
                }
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
    
    /**
     * Update an existing custom bank template
     */
    fun updateCustomBank(templateId: String, bankName: String, senderIds: String) {
        viewModelScope.launch {
            try {
                val existingTemplate = smsTemplateDao.getTemplateById(templateId) ?: return@launch
                
                val cleanedSenderIds = senderIds.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(",")
                
                if (cleanedSenderIds.isEmpty()) {
                    _uiState.update { it.copy(error = "At least one sender ID is required") }
                    return@launch
                }
                
                val updatedTemplate = existingTemplate.copy(
                    bankName = bankName.trim(),
                    senderIds = cleanedSenderIds
                )
                smsTemplateDao.updateTemplate(updatedTemplate)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update bank: ${e.message}") }
            }
        }
    }
    
    /**
     * Add a sender ID to an existing custom bank template
     */
    fun addSenderToExistingBank(senderId: String, templateId: String) {
        viewModelScope.launch {
            try {
                val existingTemplate = smsTemplateDao.getTemplateById(templateId) ?: return@launch
                
                val existingSenderIds = existingTemplate.senderIds.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                
                val newSenderId = senderId.trim().uppercase()
                if (newSenderId.isEmpty() || existingSenderIds.contains(newSenderId)) {
                    return@launch // Already exists or empty
                }
                
                val mergedSenderIds = (existingSenderIds + newSenderId).joinToString(",")
                
                val updatedTemplate = existingTemplate.copy(
                    senderIds = mergedSenderIds
                )
                smsTemplateDao.updateTemplate(updatedTemplate)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add sender: ${e.message}") }
            }
        }
    }
    
    // ========== AVAILABLE BANK OPERATIONS ==========
    
    /**
     * Update an existing available bank
     */
    fun updateAvailableBank(bankId: String, bankName: String, senderIds: String) {
        viewModelScope.launch {
            try {
                val cleanedSenderIds = senderIds.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(",")
                
                if (cleanedSenderIds.isEmpty()) {
                    _uiState.update { it.copy(error = "At least one sender ID is required") }
                    return@launch
                }
                
                availableBankRepository.updateBank(
                    bankId = bankId,
                    bankName = bankName.trim(),
                    senderIds = cleanedSenderIds
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update bank: ${e.message}") }
            }
        }
    }
    
    /**
     * Delete an available bank
     */
    fun deleteAvailableBank(bankId: String) {
        viewModelScope.launch {
            try {
                availableBankRepository.deleteBankById(bankId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete bank: ${e.message}") }
            }
        }
    }
    
    /**
     * Add a sender ID to an existing available bank
     */
    fun addSenderToAvailableBank(senderId: String, bankId: String) {
        viewModelScope.launch {
            try {
                val success = availableBankRepository.addSenderIdToBank(bankId, senderId)
                if (!success) {
                    _uiState.update { it.copy(error = "Bank not found or sender already exists") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add sender: ${e.message}") }
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

    // New Scan Methods
    
    fun scanSms(scanOption: ScanOption, customDetails: Long? = null) {
        val startTime = when (scanOption) {
            ScanOption.LAST_WEEK -> System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            ScanOption.SINCE_INSTALL -> {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
                } catch (e: Exception) {
                    System.currentTimeMillis() - 365L * 24 * 3600 * 1000 // Fallback to 1 year
                }
            }
            ScanOption.CUSTOM -> customDetails ?: (System.currentTimeMillis() - 30L * 24 * 3600 * 1000)
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = ScanProgress(0, 0, 0)) }
            
            try {
                smsInboxScanner.scanMessages(startTime).collect { result ->
                    val progress = ScanProgress(
                        scanned = result.scannedCount,
                        found = result.foundCount,
                        new = result.newTransactionsCount,
                        isComplete = result.isComplete
                    )
                    
                    _uiState.update { 
                        it.copy(
                            scanProgress = progress,
                            isScanning = !result.isComplete
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        error = "Scan failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearScanProgress() {
        _uiState.update { it.copy(scanProgress = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearResult() {
        _uiState.update { it.copy(learnResult = null) }
    }
    
    // Bank Discovery Methods
    
    /**
     * Discover banks from SMS by scanning for transaction patterns.
     * This scans ALL SMS (not just linked accounts) to find potential banks.
     * Results are saved to AvailableBankRepository for persistence.
     */
    fun discoverBanks() {
        // Use startTime of 0 to scan ALL SMS in inbox
        val startTime = 0L
        
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, discoveryResult = null, error = null) }
            
            try {
                bankDiscoveryScanner.discoverBanks(startTime).collect { result ->
                    _uiState.update {
                        it.copy(
                            discoveryResult = result,
                            isDiscovering = !result.isComplete
                        )
                    }
                    
                    // When discovery completes, save to repository and update timestamp
                    if (result.isComplete) {
                        // Save discovered banks to repository for persistence
                        availableBankRepository.saveDiscoveredBanks(result, clearOldScannedBanks = true)
                        saveLastScanTimestamp()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDiscovering = false,
                        error = "Discovery failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearDiscoveryResult() {
        _uiState.update { it.copy(discoveryResult = null) }
    }
}

enum class ScanOption {
    LAST_WEEK,
    SINCE_INSTALL,
    CUSTOM
}
