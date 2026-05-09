package com.apeligrate.domain.use_case

import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.AuthRepository

class RegisterUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String, credentials: AuthCredentials): Result<User> {
        return repository.register(name, credentials)
    }
}
