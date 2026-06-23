package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.data.remote.BackendConfig
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.data.remote.UpdateUserRequest
import com.apeligrate.domain.model.Achievement
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.UserProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "UserProgressRepo"
private const val JUSTICE_POINTS_PER_CONFIRMED_FALSE_REPORT = 100
private const val EXPERIENCE_PER_LEVEL = 100

class SupabaseUserProgressRepository : UserProgressRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersFlow = MutableStateFlow<Map<String, User>>(emptyMap())

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

    override fun getUser(userId: String): Flow<User?> {
        return usersFlow.map { users -> users[userId] }
    }

    override suspend fun ensureUser(userId: String) {
        refreshUser(userId)
    }

    override suspend fun recordSubmittedReport(userId: String?) {
        if (userId.isNullOrBlank()) return
        val user = refreshUser(userId) ?: return
        updateUser(userId, user.copy(reportsCount = user.reportsCount + 1))
    }

    override suspend fun recordValidation(userId: String?) {
        if (userId.isNullOrBlank()) return
        val user = refreshUser(userId) ?: return
        updateUser(userId, user.copy(validationsCount = user.validationsCount + 1))
    }

    override suspend fun awardJusticePoints(userIds: List<String>) {
        userIds.filter { it.isNotBlank() }.distinct().forEach { userId ->
            val user = refreshUser(userId) ?: return@forEach
            updateUser(userId, user.withExperience(user.experience + JUSTICE_POINTS_PER_CONFIRMED_FALSE_REPORT))
        }
    }

    override suspend fun saveProfile(userId: String, name: String, city: String) {
        runCatching {
            api.updateUser(
                id = "eq.$userId",
                body = UpdateUserRequest(name = name, city = city)
            )
        }.onSuccess { response ->
            response.firstOrNull()?.toUser()?.let { user ->
                usersFlow.update { it + (userId to user) }
            }
        }.onFailure { error ->
            Log.e(TAG, "Error al guardar perfil de usuario $userId", error)
        }
    }

    private suspend fun refreshUser(userId: String): User? = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val normalizedUserId = userId.normalizeUserId()
            val baseUser = api.getUsers(id = "eq.$normalizedUserId").firstOrNull()?.toUser()
            if (baseUser == null) {
                null
            } else {
                val syncedUser = syncUserStats(baseUser)
                if (syncedUser.reportsCount != baseUser.reportsCount || syncedUser.validationsCount != baseUser.validationsCount) {
                    api.updateUser(
                        id = "eq.$normalizedUserId",
                        body = UpdateUserRequest(
                            reports_count = syncedUser.reportsCount,
                            validations_count = syncedUser.validationsCount
                        )
                    )
                }
                syncedUser
            }
        }.onSuccess { user ->
            if (user != null) {
                usersFlow.update { it + (userId to user) }
            }
        }.onFailure { error ->
            Log.e(TAG, "Error al cargar usuario $userId", error)
        }.getOrNull()
    }

    private suspend fun updateUser(userId: String, user: User) {
        withContext(Dispatchers.IO) {
            runCatching {
                api.updateUser(
                    id = "eq.$userId",
                    body = UpdateUserRequest(
                        name = user.name,
                        city = user.city,
                        is_verified = user.isVerified,
                        experience = user.experience,
                        level = user.level,
                        next_level_experience = user.nextLevelExperience,
                        reputation_title = user.reputationTitle,
                        reports_count = user.reportsCount,
                        validations_count = user.validationsCount
                    )
                )
            }.onSuccess { response ->
                val updatedUser = response.firstOrNull()?.toUser() ?: user
                usersFlow.update { it + (userId to updatedUser) }
            }.onFailure { error ->
                Log.e(TAG, "Error al actualizar usuario $userId", error)
            }
        }
    }

    private suspend fun syncUserStats(user: User): User {
        val normalizedUserId = user.id.normalizeUserId()
        val submittedReports = api.getIncidentReports(
            select = "id",
            status = null,
            userId = "eq.$normalizedUserId"
        )
        val validations = api.getReportVotes(
            select = "report_id",
            userId = "eq.$normalizedUserId"
        )

        return user.copy(
            reportsCount = submittedReports.size,
            validationsCount = validations.size
        )
    }
}

private fun Map<String, Any?>.toUser(): User {
    val experience = this["experience"]?.toString()?.toIntOrNull() ?: 0
    val level = this["level"]?.toString()?.toIntOrNull() ?: 1
    val nextLevelExperience = this["next_level_experience"]?.toString()?.toIntOrNull()
        ?: (level * EXPERIENCE_PER_LEVEL)

    return User(
        id = this["id"]?.toString().orEmpty(),
        name = this["name"]?.toString().orEmpty().ifBlank { "Usuario" },
        email = this["email"]?.toString().orEmpty(),
        profileImageUrl = this["profile_image_url"]?.toString(),
        city = this["city"]?.toString().orEmpty(),
        isVerified = this["is_verified"] as? Boolean ?: false,
        level = level,
        experience = experience,
        nextLevelExperience = nextLevelExperience,
        reputationTitle = this["reputation_title"]?.toString().orEmpty().ifBlank { reputationTitleFor(experience) },
        reportsCount = this["reports_count"]?.toString()?.toIntOrNull() ?: 0,
        validationsCount = this["validations_count"]?.toString()?.toIntOrNull() ?: 0,
        achievements = defaultAchievements()
    )
}

private fun User.withExperience(totalExperience: Int): User {
    val normalizedExperience = totalExperience.coerceAtLeast(0)
    val level = (normalizedExperience / EXPERIENCE_PER_LEVEL) + 1
    return copy(
        experience = normalizedExperience,
        level = level,
        nextLevelExperience = level * EXPERIENCE_PER_LEVEL,
        reputationTitle = reputationTitleFor(normalizedExperience)
    )
}

private fun String.normalizeUserId(): String {
    val numericValue = this.toDoubleOrNull() ?: return this
    return if (numericValue % 1.0 == 0.0) {
        numericValue.toLong().toString()
    } else {
        this
    }
}

private fun reputationTitleFor(experience: Int): String {
    return when {
        experience >= 1000 -> "Justiciero de Elite"
        experience >= 600 -> "Justiciero de Barrio"
        experience >= 300 -> "Justiciero Urbano"
        experience >= 100 -> "Justiciero Novato"
        else -> "Iniciante"
    }
}

private fun defaultAchievements(): List<Achievement> {
    return listOf(
        Achievement(
            id = "1",
            title = "Vigilante",
            description = "Participa activamente validando reportes de la comunidad.",
            iconName = "visibility",
            colorHex = "#FF5252"
        ),
        Achievement(
            id = "2",
            title = "Justiciero",
            description = "Recibe puntos cuando ayudas a cancelar denuncias falsas.",
            iconName = "star",
            colorHex = "#FFC107"
        )
    )
}
