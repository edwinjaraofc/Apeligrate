package com.apeligrate.data.repository

import com.apeligrate.data.local.dao.AlertDao
import com.apeligrate.data.local.entity.toDomain
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlertRepositoryImpl(
    private val api: SentinelApiService,
    private val dao: AlertDao
) : AlertRepository {
    override fun getLatestAlerts(): Flow<List<Alert>> {
        return dao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
