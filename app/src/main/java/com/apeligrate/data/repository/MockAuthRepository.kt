package com.apeligrate.data.repository

import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    
    // Simple in-memory database
    private val registeredUsers = mutableMapOf<String, String>(
        "admin@apeligrate.com" to "123456"
    )

    override suspend fun login(credentials: AuthCredentials): Result<User> {
        delay(1000)
        val storedPassword = registeredUsers[credentials.email]
        return if (storedPassword != null && storedPassword == credentials.password) {
            val user = User("1", "Usuario Sentinel", credentials.email)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        }
    }

    override suspend fun register(name: String, credentials: AuthCredentials): Result<User> {
        delay(1000)
        if (registeredUsers.containsKey(credentials.email)) {
            return Result.failure(Exception("El usuario ya existe"))
        }
        registeredUsers[credentials.email] = credentials.password
        val user = User("2", name, credentials.email)
        return Result.success(user)
    }

    override suspend fun loginWithGoogle(): Result<User> {
        delay(800)
        val user = User("3", "Google User", "google@example.com")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun loginWithApple(): Result<User> {
        delay(800)
        val user = User("4", "Apple User", "apple@example.com")
        _currentUser.value = user
        return Result.success(user)
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun logout() {
        _currentUser.value = null
    }
}
