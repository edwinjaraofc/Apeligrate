package com.apeligrate.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.use_case.SubmitIncidentReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val category: String = "",
    val description: String = "",
    val isAnonymous: Boolean = false,
    val selectedImages: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val lastReportId: String? = null,
    val pickedLocation: DeviceCoordinates? = null
)

class ReportViewModel(
    private val submitIncidentReportUseCase: SubmitIncidentReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category, error = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, error = null) }
    }

    fun onAnonymousChange(isAnonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = isAnonymous) }
    }

    fun onLocationPicked(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(pickedLocation = DeviceCoordinates(latitude, longitude)) }
    }

    fun addImages(uris: List<Uri>) {
        _uiState.update { state ->
            val newList = (state.selectedImages + uris).distinct().take(5)
            state.copy(selectedImages = newList)
        }
    }

    fun removeImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages.filter { it != uri })
        }
    }

    fun submitReport(
        userId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        address: String? = null
    ) {
        val currentState = _uiState.value
        
        if (currentState.category.isEmpty()) {
            _uiState.update { it.copy(error = "Selecciona una categoría") }
            return
        }
        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa una descripción") }
            return
        }

        val finalLat = currentState.pickedLocation?.latitude ?: latitude
        val finalLng = currentState.pickedLocation?.longitude ?: longitude

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }

            val report = IncidentReport(
                category = currentState.category,
                description = currentState.description,
                isAnonymous = currentState.isAnonymous,
                userId = if (currentState.isAnonymous) null else userId,
                latitude = finalLat,
                longitude = finalLng,
                address = address,
                reportedAt = System.currentTimeMillis(),
                status = if (currentState.category == "Robo a mano armada") "critical" else "warning",
                images = currentState.selectedImages.map { it.toString() }
            )

            val result = submitIncidentReportUseCase(report)

            result.onSuccess { submittedReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        lastReportId = submittedReport.id
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al enviar el reporte"
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState()
    }
}
