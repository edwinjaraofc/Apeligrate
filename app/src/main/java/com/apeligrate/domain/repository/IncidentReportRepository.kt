package com.apeligrate.domain.repository

import com.apeligrate.domain.model.IncidentReport
import kotlinx.coroutines.flow.Flow

interface IncidentReportRepository {
    suspend fun submitReport(report: IncidentReport): Result<IncidentReport>
    fun getReports(): Flow<List<IncidentReport>>
    fun getReportById(id: String): Flow<IncidentReport?>
}

