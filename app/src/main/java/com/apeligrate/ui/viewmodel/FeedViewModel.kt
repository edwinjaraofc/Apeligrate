package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val reports: List<IncidentReport> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeedViewModel(
    private val repository: IncidentReportRepository
) : ViewModel() {

    private val uiStateMutable = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = uiStateMutable.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            uiStateMutable.update { it.copy(isLoading = true) }
            repository.getReports().collect { reports ->
                uiStateMutable.update { it.copy(reports = reports, isLoading = false) }
            }
        }
    }

    fun validateReport(reportId: String, isReal: Boolean) {
        uiStateMutable.update { state ->
            val updatedReports = state.reports.map { report ->
                if (report.id == reportId) {
                    if (isReal) {
                        report.copy(validationCount = report.validationCount + 1)
                    } else {
                        report.copy(falseCount = report.falseCount + 1)
                    }
                } else {
                    report
                }
            }
            state.copy(reports = updatedReports)
        }
    }
}
