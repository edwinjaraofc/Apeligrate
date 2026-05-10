package com.apeligrate.domain.model

data class AuthCredentials(
    val email: String,
    val password: String,
    val profileImageUrl: String? = null
)
