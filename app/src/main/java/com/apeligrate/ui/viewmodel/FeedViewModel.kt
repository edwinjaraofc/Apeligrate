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

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getReports().collect { reports ->
                // If repository is empty (mocking), let's add some initial professional mock data
                val displayReports = if (reports.isEmpty()) getMockReports() else reports
                _uiState.update { it.copy(reports = displayReports, isLoading = false) }
            }
        }
    }

    fun validateReport(reportId: String, isReal: Boolean) {
        // In a real app, this would call a use case to update the backend
        _uiState.update { state ->
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

    private fun getMockReports(): List<IncidentReport> {
        return listOf(
            IncidentReport(
                id = "1",
                category = "Asalto a Mano Armada",
                description = "Reportado cerca de Av. Reforma. Sujeto en motocicleta negra.",
                reportedAt = System.currentTimeMillis() - 7200000, // 2h ago
                validationCount = 42,
                persistenceMessage = "42 reportes similares",
                status = "critical"
            ),
            IncidentReport(
                id = "2",
                category = "Actividad Sospechosa",
                description = "Grupo de personas merodeando vehículos estacionados en Calle 5.",
                reportedAt = System.currentTimeMillis() - 900000, // 15min ago
                validationCount = 8,
                persistenceMessage = "8 validaciones",
                status = "warning"
            ),
            IncidentReport(
                id = "3",
                category = "Falla de Alumbrado Público",
                description = "Calle oscura facilita actos delictivos. Reportado para mantenimiento.",
                reportedAt = System.currentTimeMillis() - 18000000, // 5h ago
                validationCount = 15,
                persistenceMessage = "15 vecinos",
                status = "safe"
            )
        )
    }
}
