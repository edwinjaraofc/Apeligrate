package com.apeligrate.domain.use_case

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow

class GetLatestAlertsUseCase(
    private val repository: AlertRepository,
) {
    operator fun invoke(): Flow<List<Alert>> {
        return repository.getLatestAlerts()
    }
}
