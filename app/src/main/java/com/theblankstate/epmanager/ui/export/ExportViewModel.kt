package com.theblankstate.epmanager.ui.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.export.ExportFilters
import com.theblankstate.epmanager.data.export.ExportManager
import com.theblankstate.epmanager.data.export.ExportPreview
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Date range presets for export
 */
enum class DateRangeType {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportSuccess: String? = null,
    val exportError: String? = null,
    
    // Date range
    val dateRangeType: DateRangeType = DateRangeType.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    
    // Filters
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedAccountIds: Set<String> = emptySet(),
    val includeExpenses: Boolean = true,
    val includeIncome: Boolean = true,
    
    // Export options
    val includeNotes: Boolean = true,
    val includeAccountInfo: Boolean = true,
    
    // Preview
    val preview: ExportPreview? = null,
    
    // Monthly report
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportManager: ExportManager,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    init {
        loadFilters()
    }
    
    private fun loadFilters() {
        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            val accounts = accountRepository.getAllAccounts().first()
            
            _uiState.update { 
                it.copy(
                    categories = categories,
                    accounts = accounts,
                    // Select all by default
                    selectedCategoryIds = categories.map { c -> c.id }.toSet() + "uncategorized",
                    selectedAccountIds = accounts.map { a -> a.id }.toSet() + "default"
                ) 
            }
            
            updatePreview()
        }
    }
    
    // ==================== DATE RANGE ====================
    
    fun setDateRangeType(type: DateRangeType) {
        _uiState.update { it.copy(dateRangeType = type) }
        updatePreview()
    }
    
    fun setCustomDateRange(startDate: Long, endDate: Long) {
        _uiState.update { 
            it.copy(
                dateRangeType = DateRangeType.CUSTOM,
                customStartDate = startDate,
                customEndDate = endDate
            ) 
        }
        updatePreview()
    }
    
    // ==================== CATEGORY FILTERS ====================
    
    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.selectedCategoryIds
        val updated = if (categoryId in current) current - categoryId else current + categoryId
        _uiState.update { it.copy(selectedCategoryIds = updated) }
        updatePreview()
    }
    
    fun selectAllCategories() {
        val allIds = _uiState.value.categories.map { it.id }.toSet() + "uncategorized"
        _uiState.update { it.copy(selectedCategoryIds = allIds) }
        updatePreview()
    }
    
    fun clearAllCategories() {
        _uiState.update { it.copy(selectedCategoryIds = emptySet()) }
        updatePreview()
    }
    
    // ==================== ACCOUNT FILTERS ====================
    
    fun toggleAccount(accountId: String) {
        val current = _uiState.value.selectedAccountIds
        val updated = if (accountId in current) current - accountId else current + accountId
        _uiState.update { it.copy(selectedAccountIds = updated) }
        updatePreview()
    }
    
    fun selectAllAccounts() {
        val allIds = _uiState.value.accounts.map { it.id }.toSet() + "default"
        _uiState.update { it.copy(selectedAccountIds = allIds) }
        updatePreview()
    }
    
    fun clearAllAccounts() {
        _uiState.update { it.copy(selectedAccountIds = emptySet()) }
        updatePreview()
    }
    
    // ==================== TRANSACTION TYPE FILTERS ====================
    
    fun toggleExpenses() {
        _uiState.update { it.copy(includeExpenses = !it.includeExpenses) }
        updatePreview()
    }
    
    fun toggleIncome() {
        _uiState.update { it.copy(includeIncome = !it.includeIncome) }
        updatePreview()
    }
    
    // ==================== EXPORT OPTIONS ====================
    
    fun toggleIncludeNotes() {
        _uiState.update { it.copy(includeNotes = !it.includeNotes) }
    }
    
    fun toggleIncludeAccountInfo() {
        _uiState.update { it.copy(includeAccountInfo = !it.includeAccountInfo) }
    }
    
    // ==================== MONTHLY REPORT ====================
    
    fun setMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
    }
    
    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }
    
    // ==================== PREVIEW ====================
    
    private fun updatePreview() {
        viewModelScope.launch {
            val filters = buildFilters()
            val preview = exportManager.getExportPreview(filters)
            _uiState.update { it.copy(preview = preview) }
        }
    }
    
    // ==================== EXPORT ====================
    
    private fun buildFilters(): ExportFilters {
        val state = _uiState.value
        val (startDate, endDate) = getDateRange(state.dateRangeType)
        
        val transactionTypes = mutableSetOf<TransactionType>()
        if (state.includeExpenses) transactionTypes.add(TransactionType.EXPENSE)
        if (state.includeIncome) transactionTypes.add(TransactionType.INCOME)
        
        return ExportFilters(
            startDate = startDate,
            endDate = endDate,
            categoryIds = state.selectedCategoryIds.takeIf { it.isNotEmpty() },
            accountIds = state.selectedAccountIds.takeIf { it.isNotEmpty() },
            transactionTypes = transactionTypes.takeIf { it.isNotEmpty() },
            includeNotes = state.includeNotes,
            includeAccountInfo = state.includeAccountInfo
        )
    }
    
    private fun getDateRange(type: DateRangeType): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        return when (type) {
            DateRangeType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeType.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeType.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeType.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            DateRangeType.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeType.ALL_TIME -> Pair(null, null)
            DateRangeType.CUSTOM -> Pair(
                _uiState.value.customStartDate,
                _uiState.value.customEndDate
            )
        }
    }
    
    fun exportFiltered() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null) }
            
            val filters = buildFilters()
            
            exportManager.exportToCSV(context, filters)
                .onSuccess { uri ->
                    exportManager.shareFile(context, uri, "text/csv")
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportSuccess = "CSV exported successfully!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportError = e.message ?: "Export failed"
                        ) 
                    }
                }
        }
    }
    
    fun exportMonthlyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null) }
            
            val month = _uiState.value.selectedMonth
            val year = _uiState.value.selectedYear
            
            exportManager.exportSummaryReport(context, month, year)
                .onSuccess { uri ->
                    exportManager.shareFile(context, uri, "text/plain")
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportSuccess = "Report exported successfully!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportError = e.message ?: "Export failed"
                        ) 
                    }
                }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(exportSuccess = null, exportError = null) }
    }
}
