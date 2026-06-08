package com.apeligrate.data.repository

import android.util.Log
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.repository.IncidentReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "IncidentReportRepo"

class IncidentReportRepositoryImpl : IncidentReportRepository {
    // Temporal: almacenamiento en memoria
    // Cuando la tabla Room exista, cambiar esta implementación
    private val _reportsFlow = MutableStateFlow<List<IncidentReport>>(emptyList())
    private val reports = mutableListOf<IncidentReport>()

    override suspend fun submitReport(report: IncidentReport): Result<IncidentReport> {
        return try {
            // Generar ID si no existe
            val reportWithId = if (report.id.isEmpty()) {
                report.copy(id = generateId())
            } else {
                report
            }

            // Guardar en memoria (temporal)
            reports.add(reportWithId)
            _reportsFlow.value = reports.toList()

            Log.d(TAG, "Reporte guardado: ${reportWithId.id}")
            Result.success(reportWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar reporte", e)
            Result.failure(e)
        }
    }

    override fun getReports(): Flow<List<IncidentReport>> {
        return _reportsFlow.asStateFlow()
    }

    override fun getReportById(id: String): Flow<IncidentReport?> {
        return MutableStateFlow(reports.find { it.id == id }).asStateFlow()
    }

    private fun generateId(): String {
        return "report_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

