package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.data.local.SessionManager
import com.apeligrate.data.remote.BackendConfig
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.AuthRepository
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
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer ${BackendConfig.SUPABASE_ANON_KEY}")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BackendConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SentinelApiService::class.java)
    }

    override suspend fun login(credentials: AuthCredentials): Result<User> = withContext(Dispatchers.IO) {
        // Handle test accounts
        if (credentials.email == "1" && credentials.password == "1") {
            sessionManager.setSession("1")
            val user = User(id = "1", name = "Juan Pérez", email = "1")
            _currentUser.value = user
            return@withContext Result.success(user)
        }
        if (credentials.email == "2" && credentials.password == "2") {
            sessionManager.setSession("2")
            val user = User(id = "2", name = "María García", email = "2")
            _currentUser.value = user
            return@withContext Result.success(user)
        }

        try {
            val passwordHash = hashPassword(credentials.password)
            val users = api.queryUsers(
                email = "eq.${credentials.email}",
                passwordHash = "eq.$passwordHash",
            )

            if (users.isNotEmpty()) {
                val first = users[0]
                val id = first["id"]?.toString() ?: credentials.email
                sessionManager.setSession(id)

                val name = first["name"]?.toString() ?: ""
                val email = first["email"]?.toString() ?: credentials.email
                val profile = first["profile_image_url"]?.toString()
                val city = first["city"]?.toString().orEmpty()
                val isVerified = first["is_verified"] as? Boolean ?: false
                val experience = first["experience"]?.toString()?.toIntOrNull() ?: 0
                val level = first["level"]?.toString()?.toIntOrNull() ?: 1
                val nextLevelExperience = first["next_level_experience"]?.toString()?.toIntOrNull() ?: 100
                val reputationTitle = first["reputation_title"]?.toString() ?: "Iniciante"
                val reportsCount = first["reports_count"]?.toString()?.toIntOrNull() ?: 0
                val validationsCount = first["validations_count"]?.toString()?.toIntOrNull() ?: 0

                val user = User(
                    id = id,
                    name = if (name.isNotBlank()) name else "Usuario",
                    email = email,
                    profileImageUrl = profile,
                    city = city,
                    isVerified = isVerified,
                    experience = experience,
                    level = level,
                    nextLevelExperience = nextLevelExperience,
                    reputationTitle = reputationTitle,
                    reportsCount = reportsCount,
                    validationsCount = validationsCount
                )
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Credenciales inválidas"))
            }
        } catch (e: Exception) {
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

                val response = api.createUser(request)
                if (response.isEmpty()) {
                    return@withContext Result.failure(Exception("Usuario no creado"))
                }

                val created = response[0]
                val id = created["id"]?.toString() ?: credentials.email
                sessionManager.setSession(id)

                val respName = created["name"]?.toString() ?: name
                val respEmail = created["email"]?.toString() ?: credentials.email
                val profile = created["profile_image_url"]?.toString() ?: credentials.profileImageUrl
                val city = created["city"]?.toString().orEmpty()
                val isVerified = created["is_verified"] as? Boolean ?: false
                val experience = created["experience"]?.toString()?.toIntOrNull() ?: 0
                val level = created["level"]?.toString()?.toIntOrNull() ?: 1
                val nextLevelExperience = created["next_level_experience"]?.toString()?.toIntOrNull() ?: 100
                val reputationTitle = created["reputation_title"]?.toString() ?: "Iniciante"
                val reportsCount = created["reports_count"]?.toString()?.toIntOrNull() ?: 0
                val validationsCount = created["validations_count"]?.toString()?.toIntOrNull() ?: 0

                val user = User(
                    id = id,
                    name = respName,
                    email = respEmail,
                    profileImageUrl = profile,
                    city = city,
                    isVerified = isVerified,
                    experience = experience,
                    level = level,
                    nextLevelExperience = nextLevelExperience,
                    reputationTitle = reputationTitle,
                    reportsCount = reportsCount,
                    validationsCount = validationsCount
                )
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
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
        sessionManager.clearSession()
        _currentUser.value = null
    }

    private fun hashPassword(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray(UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
