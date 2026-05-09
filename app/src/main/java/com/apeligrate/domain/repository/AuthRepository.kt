package com.apeligrate.domain.repository

import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(credentials: AuthCredentials): Result<User>
    suspend fun register(name: String, credentials: AuthCredentials): Result<User>
    suspend fun loginWithGoogle(): Result<User>
    suspend fun loginWithApple(): Result<User>
    fun getCurrentUser(): Flow<User?>
    suspend fun logout()
}
