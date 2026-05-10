package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.data.remote.OrdsUsersResponse
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.AuthRepository
import com.apeligrate.data.local.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

private const val TAG = "RemoteAuthRepository"

class RemoteAuthRepository(private val sessionManager: SessionManager) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)

    // Build Retrofit
    private val api: SentinelApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        Retrofit.Builder()
            .baseUrl("https://gb72d0482c8537e-upchmovilbb2026.adb.us-phoenix-1.oraclecloudapps.com/ords/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SentinelApiService::class.java)
    }

    override suspend fun login(credentials: AuthCredentials): Result<User> = withContext(Dispatchers.IO) {
        try {
            val passwordHash = hashPassword(credentials.password)
            val qObj = mapOf("email" to credentials.email, "password_hash" to passwordHash)
            val qJson = Gson().toJson(qObj)

            val response: OrdsUsersResponse = api.queryUsers(qJson)

            val items = response.items
            if (items.isNotEmpty()) {
                val first = items[0]
                val id = first["id"]?.toString() ?: first["ID"]?.toString() ?: credentials.email
                Log.d(TAG, "Login successful. Extracted id: $id")
                Log.d(TAG, "Saving session with userId: $id")
                sessionManager.setSession(id)
                Log.d(TAG, "Session saved successfully")

                val name = first["name"]?.toString() ?: first["NAME"]?.toString() ?: ""
                val email = first["email"]?.toString() ?: first["EMAIL"]?.toString() ?: credentials.email
                val profile = first["profile_image_url"]?.toString() ?: first["PROFILE_IMAGE_URL"]?.toString()

                val user = User(id = id, name = if (name.isNotBlank()) name else "Usuario", email = email, profileImageUrl = profile)
                _currentUser.value = user
                Result.success(user)
            } else {
                Log.w(TAG, "Login failed: items list is empty")
                Result.failure(Exception("Credenciales inválidas"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, credentials: AuthCredentials): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val passwordHash = hashPassword(credentials.password)
                val request = com.apeligrate.data.remote.CreateUserRequest(
                    name = name,
                    email = credentials.email,
                    password_hash = passwordHash,
                    profile_image_url = credentials.profileImageUrl
                )

                Log.d(TAG, "Sending register request for email: ${credentials.email}")
                val response: com.apeligrate.data.remote.CreateUserResponse = api.createUser(request)

                // Try to extract id/name/email/profile from response map
                val id = response["id"]?.toString() ?: response["ID"]?.toString() ?: credentials.email
                Log.d(TAG, "Register successful. Extracted id: $id")
                Log.d(TAG, "Saving session with userId: $id")
                sessionManager.setSession(id)
                Log.d(TAG, "Session saved successfully")

                val respName = response["name"]?.toString() ?: response["NAME"]?.toString() ?: name
                val respEmail = response["email"]?.toString() ?: response["EMAIL"]?.toString() ?: credentials.email
                val profile = response["profile_image_url"]?.toString() ?: response["PROFILE_IMAGE_URL"]?.toString() ?: credentials.profileImageUrl

                val user = User(id = id, name = respName, email = respEmail, profileImageUrl = profile)
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Register error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun loginWithGoogle(): Result<User> {
        return Result.failure(Exception("Social login no implementado"))
    }

    override suspend fun loginWithApple(): Result<User> {
        return Result.failure(Exception("Social login no implementado"))
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun logout() {
        Log.d(TAG, "User logout requested")
        sessionManager.clearSession()
        _currentUser.value = null
    }

    private fun hashPassword(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray(UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

