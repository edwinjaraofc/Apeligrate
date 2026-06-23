package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.data.remote.BackendConfig
import com.apeligrate.data.remote.CreateReportVoteRequest
import com.apeligrate.data.remote.CreateIncidentReportRequest
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.data.remote.UpdateIncidentReportRequest
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.VoteReportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "IncidentReportRepo"
private const val FALSE_REPORT_THRESHOLD = 10

class IncidentReportRepositoryImpl : IncidentReportRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reportsFlow = MutableStateFlow<List<IncidentReport>>(emptyList())

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

    init {
        repositoryScope.launch {
            refreshReports()
        }
    }

    override suspend fun submitReport(report: IncidentReport): Result<IncidentReport> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createIncidentReport(
                    CreateIncidentReportRequest(
                        category = report.category,
                        description = report.description,
                        latitude = report.latitude,
                        longitude = report.longitude,
                        address = report.address,
                        is_anonymous = report.isAnonymous,
                        reported_at = report.reportedAt,
                        user_id = report.userId?.toLongOrNull(),
                        status = report.status,
                        images = report.images,
                        validation_count = report.validationCount,
                        false_count = report.falseCount,
                        persistence_message = report.persistenceMessage
                    )
                )

                val createdReport = response.firstOrNull()?.toIncidentReport() ?: report.copy(
                    id = generateId()
                )
                reportsFlow.value = listOf(createdReport) + reportsFlow.value
                Log.d(TAG, "Reporte guardado remotamente: ${createdReport.id}")
                Result.success(createdReport)
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar reporte", e)
                Result.failure(e)
            }
        }
    }

    override fun getReports(): Flow<List<IncidentReport>> {
        if (reportsFlow.value.isEmpty()) {
            repositoryScope.launch {
                refreshReports()
            }
        }
        return reportsFlow.asStateFlow()
    }

    override fun getReportById(id: String): Flow<IncidentReport?> {
        return reportsFlow.map { reports -> reports.find { it.id == id } }
    }

    override suspend fun voteReport(reportId: String, userId: String, isReal: Boolean): VoteReportResult {
        val normalizedUserId = userId.normalizeUserId()
        if (normalizedUserId.isBlank()) {
            return VoteReportResult(accepted = false, errorMessage = "Debes iniciar sesión para votar.")
        }

        val numericUserId = normalizedUserId.toLongOrNull()
            ?: return VoteReportResult(accepted = false, errorMessage = "Tu sesión es inválida. Vuelve a iniciar sesión.")

        return withContext(Dispatchers.IO) {
            try {
                val currentReport = reportsFlow.value.find { it.id == reportId }
                if (currentReport?.status == "cancelled") {
                    return@withContext VoteReportResult(
                        accepted = false,
                        errorMessage = "Este reporte ya fue cancelado."
                    )
                }

                val existingVotes = api.getReportVotes(
                    reportId = "eq.$reportId",
                    userId = "eq.$numericUserId"
                )
                if (existingVotes.isNotEmpty()) {
                    return@withContext VoteReportResult(
                        accepted = false,
                        errorMessage = "Ya votaste en este reporte."
                    )
                }

                api.createReportVote(
                    CreateReportVoteRequest(
                        report_id = reportId,
                        user_id = numericUserId,
                        vote_type = if (isReal) "real" else "false"
                    )
                )

                val votes = api.getReportVotes(reportId = "eq.$reportId")
                val falseVoterIds = votes
                    .filter { it["vote_type"]?.toString() == "false" }
                    .mapNotNull { it["user_id"]?.toString()?.normalizeUserId() }
                    .distinct()
                val validationCount = votes.count { it["vote_type"]?.toString() == "real" }
                val falseCount = falseVoterIds.size
                val reportCancelled = falseCount >= FALSE_REPORT_THRESHOLD

                api.updateIncidentReport(
                    id = "eq.$reportId",
                    body = UpdateIncidentReportRequest(
                        status = if (reportCancelled) "cancelled" else null,
                        validation_count = validationCount,
                        false_count = falseCount
                    )
                )

                refreshReports()

                VoteReportResult(
                    accepted = true,
                    reportCancelled = reportCancelled,
                    rewardedUserIds = if (reportCancelled) falseVoterIds else emptyList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al votar reporte $reportId", e)
                VoteReportResult(
                    accepted = false,
                    errorMessage = "No se pudo registrar tu voto. Intenta otra vez."
                )
            }
        }
    }

    private suspend fun refreshReports() {
        runCatching {
            val reports = api.getIncidentReports().mapNotNull { raw ->
                runCatching { raw.toIncidentReport() }.getOrNull()
            }
            val votesByReport = api.getReportVotes()
                .groupBy { it["report_id"]?.toString().orEmpty() }

            reports.map { report ->
                val reportVotes = votesByReport[report.id].orEmpty()
                val validationVoterIds = reportVotes
                    .filter { it["vote_type"]?.toString() == "real" }
                    .mapNotNull { it["user_id"]?.toString()?.normalizeUserId() }
                    .distinct()
                val falseVoterIds = reportVotes
                    .filter { it["vote_type"]?.toString() == "false" }
                    .mapNotNull { it["user_id"]?.toString()?.normalizeUserId() }
                    .distinct()

                report.copy(
                    validationCount = validationVoterIds.size,
                    falseCount = falseVoterIds.size,
                    validationVoterIds = validationVoterIds,
                    falseVoterIds = falseVoterIds
                )
            }
        }.onSuccess { reports ->
            reportsFlow.value = reports
            Log.d(TAG, "Reportes cargados: ${reports.size}")
        }.onFailure { error ->
            Log.e(TAG, "Error al cargar reportes", error)
        }
    }

    private fun generateId(): String {
        return "report_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

private fun Map<String, Any?>.toIncidentReport(): IncidentReport {
    return IncidentReport(
        id = this["id"]?.toString().orEmpty(),
        category = this["category"]?.toString().orEmpty(),
        description = this["description"]?.toString().orEmpty(),
        latitude = this["latitude"]?.toString()?.toDoubleOrNull(),
        longitude = this["longitude"]?.toString()?.toDoubleOrNull(),
        address = this["address"]?.toString(),
        isAnonymous = this["is_anonymous"] as? Boolean ?: false,
        reportedAt = this["reported_at"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
        userId = this["user_id"]?.toString(),
        status = this["status"]?.toString() ?: "warning",
        images = (this["images"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        validationCount = this["validation_count"]?.toString()?.toIntOrNull() ?: 0,
        falseCount = this["false_count"]?.toString()?.toIntOrNull() ?: 0,
        persistenceMessage = this["persistence_message"]?.toString().orEmpty()
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
