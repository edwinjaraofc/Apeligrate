package com.apeligrate.data.repository

import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

class MockAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)

    private val registerEndpoint =
        "https://gb72d0482c8537e-upchmovilbb2026.adb.us-phoenix-1.oraclecloudapps.com/ords/admin/users/"

    // Simple in-memory database
    private val registeredUsers = mutableMapOf<String, String>(
        "admin@apeligrate.com" to hashPassword("123456")
    )

    override suspend fun login(credentials: AuthCredentials): Result<User> {
        val storedPassword = registeredUsers[credentials.email]
        val providedPasswordHash = hashPassword(credentials.password)
        return if (storedPassword != null && storedPassword == providedPasswordHash) {
            val user = User("1", "Usuario Sentinel", credentials.email)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        }
    }

    override suspend fun register(name: String, credentials: AuthCredentials): Result<User> {
        if (registeredUsers.containsKey(credentials.email)) {
            return Result.failure(Exception("El usuario ya existe"))
        }

        val passwordHash = hashPassword(credentials.password)

        return runCatching {
            val responseUser = postUserToOracle(
                name = name,
                email = credentials.email,
                passwordHash = passwordHash,
                profileImageUrl = credentials.profileImageUrl,
            )

            registeredUsers[credentials.email] = passwordHash
            _currentUser.value = responseUser
            responseUser
        }
    }

    override suspend fun loginWithGoogle(): Result<User> {
        val user = User("3", "Google User", "google@example.com")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun loginWithApple(): Result<User> {
        val user = User("4", "Apple User", "apple@example.com")
        _currentUser.value = user
        return Result.success(user)
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun logout() {
        _currentUser.value = null
    }

    private suspend fun postUserToOracle(
        name: String,
        email: String,
        passwordHash: String,
        profileImageUrl: String?,
    ): User = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password_hash", passwordHash)
            if (!profileImageUrl.isNullOrBlank()) {
                put("profile_image_url", profileImageUrl)
            }
        }

        val connection = (URL(registerEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            connection.outputStream.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, UTF_8)).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            val responseBody = runCatching {
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader(UTF_8).use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader(UTF_8)?.use { it.readText() }.orEmpty()
                }
            }.getOrDefault("")

            if (responseCode !in 200..299) {
                throw IllegalStateException(parseOracleError(responseBody, responseCode))
            }

            parseCreatedUser(
                responseBody = responseBody,
                fallbackName = name,
                fallbackEmail = email,
                fallbackProfileImageUrl = profileImageUrl,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCreatedUser(
        responseBody: String,
        fallbackName: String,
        fallbackEmail: String,
        fallbackProfileImageUrl: String?,
    ): User {
        if (responseBody.isBlank()) {
            return User(
                id = fallbackEmail,
                name = fallbackName,
                email = fallbackEmail,
                profileImageUrl = fallbackProfileImageUrl,
            )
        }

        return runCatching {
            val json = JSONObject(responseBody)
            User(
                id = json.optStringOrNull("id") ?: fallbackEmail,
                name = json.optStringOrNull("name") ?: fallbackName,
                email = json.optStringOrNull("email") ?: fallbackEmail,
                profileImageUrl = json.optStringOrNull("profile_image_url") ?: fallbackProfileImageUrl,
            )
        }.getOrElse {
            User(
                id = fallbackEmail,
                name = fallbackName,
                email = fallbackEmail,
                profileImageUrl = fallbackProfileImageUrl,
            )
        }
    }

    private fun parseOracleError(responseBody: String, responseCode: Int): String {
        if (responseBody.isBlank()) {
            return "Error HTTP $responseCode al registrar el usuario"
        }

        return runCatching {
            val json = JSONObject(responseBody)
            json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Error HTTP $responseCode al registrar el usuario"
        }.getOrElse {
            responseBody
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun hashPassword(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray(UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
