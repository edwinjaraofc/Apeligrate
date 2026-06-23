package com.apeligrate.domain.repository

import com.apeligrate.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserProgressRepository {
    fun getUser(userId: String): Flow<User?>
    suspend fun ensureUser(userId: String)
    suspend fun recordSubmittedReport(userId: String?)
    suspend fun recordValidation(userId: String?)
    suspend fun awardJusticePoints(userIds: List<String>)
    suspend fun saveProfile(userId: String, name: String, city: String)
}
