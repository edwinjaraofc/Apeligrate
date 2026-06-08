package com.apeligrate.domain.use_case

import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository

class SubmitIncidentReportUseCase(
    private val repository: IncidentReportRepository
) {
    suspend operator fun invoke(report: IncidentReport): Result<IncidentReport> {
        return repository.submitReport(report)
    }
}

