package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.data.remote.BackendConfig
import com.apeligrate.data.remote.CreateIncidentReportRequest
import com.apeligrate.data.remote.SentinelApiService
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository
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

    private suspend fun refreshReports() {
        runCatching {
            api.getIncidentReports().mapNotNull { raw ->
                runCatching { raw.toIncidentReport() }.getOrNull()
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
