package com.apeligrate.domain.repository

import com.apeligrate.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getLatestAlerts(): Flow<List<Alert>>
}
