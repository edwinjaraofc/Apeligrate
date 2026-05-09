package com.apeligrate.domain.use_case

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.repository.AlertRepository

class ReportIncidentUseCase(private val repository: AlertRepository) {
    suspend operator fun invoke(alert: Alert): Result<Unit> {
        // Business logic for reporting could go here
        return Result.success(Unit)
    }
}
