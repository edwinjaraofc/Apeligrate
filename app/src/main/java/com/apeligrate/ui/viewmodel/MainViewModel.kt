package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.use_case.GetLatestAlertsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class MainViewModel(
    private val getLatestAlertsUseCase: GetLatestAlertsUseCase? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Mocking data for now as we don't have Repository implementation yet
            val mockAlerts = listOf(
                Alert(
                    id = "1",
                    title = "SISTEMA ACTIVO",
                    description = "Vigilancia comunitaria en curso.",
                    severity = Severity.SAFE,
                    timestamp = System.currentTimeMillis(),
                ),
                Alert(
                    id = "2",
                    title = "INTRUSIÓN DETECTADA",
                    description = "Sector Noroeste - Zona 4.",
                    severity = Severity.CRITICAL,
                    timestamp = System.currentTimeMillis(),
                ),
            )
            _uiState.value = _uiState.value.copy(alerts = mockAlerts, isLoading = false)
        }
    }
}
