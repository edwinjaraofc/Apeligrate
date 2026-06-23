package com.apeligrate.domain.repository

import com.apeligrate.domain.model.IncidentReport
import kotlinx.coroutines.flow.Flow

interface IncidentReportRepository {
    suspend fun submitReport(report: IncidentReport): Result<IncidentReport>
    suspend fun voteReport(reportId: String, userId: String, isReal: Boolean): VoteReportResult
    fun getReports(): Flow<List<IncidentReport>>
    fun getReportById(id: String): Flow<IncidentReport?>
}

data class VoteReportResult(
    val accepted: Boolean,
    val reportCancelled: Boolean = false,
    val rewardedUserIds: List<String> = emptyList(),
    val errorMessage: String? = null
)

