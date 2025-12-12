package com.theblankstate.epmanager.ui.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.export.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportSuccess: String? = null,
    val exportError: String? = null,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportManager: ExportManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    fun setMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
    }
    
    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }
    
    fun exportAllToCSV() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null) }
            
            exportManager.exportToCSV(context)
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
