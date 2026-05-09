package com.apeligrate.data.remote

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User

/**
 * Placeholder for Retrofit or Ktor API Service
 */
interface SentinelApiService {
    suspend fun login(credentials: AuthCredentials): User
    suspend fun getAlerts(): List<Alert>
}
