package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.UserProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val reports: List<IncidentReport> = emptyList(),
    val currentUserId: String? = null,
    val submittingReportIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeedViewModel(
    private val repository: IncidentReportRepository,
    private val userProgressRepository: UserProgressRepository,
    private val currentUserId: String?
) : ViewModel() {

    private val uiStateMutable = MutableStateFlow(FeedUiState(currentUserId = currentUserId?.normalizeUserId()))
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
        val userId = currentUserId?.normalizeUserId() ?: run {
            uiStateMutable.update { it.copy(error = "Debes iniciar sesion para votar.") }
            return
        }

        val currentState = uiStateMutable.value
        val targetReport = currentState.reports.find { it.id == reportId } ?: return
        val alreadyVoted = userId in targetReport.validationVoterIds || userId in targetReport.falseVoterIds
        if (alreadyVoted || reportId in currentState.submittingReportIds) {
            return
        }

        viewModelScope.launch {
            uiStateMutable.update {
                it.copy(
                    submittingReportIds = it.submittingReportIds + reportId,
                    error = null
                )
            }

            val voteResult = repository.voteReport(reportId, userId, isReal)
            if (!voteResult.accepted) {
                uiStateMutable.update {
                    it.copy(
                        submittingReportIds = it.submittingReportIds - reportId,
                        error = voteResult.errorMessage ?: "No se pudo registrar tu voto."
                    )
                }
                return@launch
            }

            userProgressRepository.recordValidation(userId)
            if (voteResult.reportCancelled) {
                userProgressRepository.awardJusticePoints(voteResult.rewardedUserIds)
            }

            uiStateMutable.update { state ->
                state.copy(
                    reports = state.reports.map { report ->
                        if (report.id != reportId) {
                            report
                        } else if (isReal) {
                            report.copy(
                                validationCount = report.validationCount + 1,
                                validationVoterIds = (report.validationVoterIds + userId).distinct()
                            )
                        } else {
                            report.copy(
                                falseCount = report.falseCount + 1,
                                falseVoterIds = (report.falseVoterIds + userId).distinct()
                            )
                        }
                    },
                    submittingReportIds = state.submittingReportIds - reportId,
                    error = null
                )
            }
        }
    }
}

private fun String.normalizeUserId(): String {
    val numericValue = this.toDoubleOrNull() ?: return this
    return if (numericValue % 1.0 == 0.0) {
        numericValue.toLong().toString()
    } else {
        this
    }
}
