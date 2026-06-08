package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.use_case.SubmitIncidentReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val lastReportId: String? = null
)

class ReportViewModel(
    private val submitIncidentReportUseCase: SubmitIncidentReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun submitReport(
        category: String,
        description: String,
        isAnonymous: Boolean,
        userId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        address: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)

            val report = IncidentReport(
                category = category,
                description = description,
                isAnonymous = isAnonymous,
                userId = if (isAnonymous) null else userId,
                latitude = latitude,
                longitude = longitude,
                address = address,
                reportedAt = System.currentTimeMillis(),
                status = "pending"
            )

            val result = submitIncidentReportUseCase(report)

            result.onSuccess { submittedReport ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    lastReportId = submittedReport.id
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Error al enviar el reporte"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState()
    }
}

