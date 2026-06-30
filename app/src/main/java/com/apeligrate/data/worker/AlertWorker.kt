package com.apeligrate.data.worker

import android.content.Context
import android.location.LocationManager
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.apeligrate.data.local.SentinelDatabase
import com.apeligrate.data.local.entity.NotificationCooldownEntity
import com.apeligrate.data.repository.IncidentReportRepositoryImpl
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.model.isCritical
import com.apeligrate.util.NotificationHelper
import com.apeligrate.util.DangerZoneAggregator
import com.apeligrate.util.GeofenceManager
import kotlinx.coroutines.flow.first

class AlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database by lazy {
        Room.databaseBuilder(applicationContext, SentinelDatabase::class.java, "sentinel_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val repository = IncidentReportRepositoryImpl()
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result {
        // 1. Obtener ubicación (usamos la última conocida para ahorrar batería)
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastLocation = try {
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) ?:
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?:
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        } ?: return Result.success()

        // 2. Obtener reportes
        val reports = repository.getReports().first()
        val zones = DangerZoneAggregator.buildDangerZones(reports.map { it.toAlert() })
        if (zones.isEmpty()) return Result.success()

        val activeZone = GeofenceManager(applicationContext).findContainingZone(
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            zones = zones,
            bufferMeters = 25f
        ) ?: return Result.success()

        val cooldownDao = database.notificationCooldownDao()
        val lastNotified = cooldownDao.getLastNotifiedAt(activeZone.id)
        val threeHoursMillis = 3 * 60 * 60 * 1000

        if (lastNotified == null || (System.currentTimeMillis() - lastNotified) > threeHoursMillis) {
            val title = if (activeZone.grouped) "¡ZONA AGRUPADA!" else "¡ALERTA CRÍTICA!"
            val message = if (activeZone.grouped) {
                "Hay ${activeZone.reportCount} incidentes cercanos dentro de una misma zona."
            } else {
                "Se reportó ${activeZone.primaryReport.title} cerca de tu ubicación."
            }

            notificationHelper.showProximityAlert(title, message)

            cooldownDao.updateLastNotifiedAt(NotificationCooldownEntity(activeZone.id, System.currentTimeMillis()))
            cooldownDao.clearOldCooldowns(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        }

        return Result.success()
    }
}

private fun IncidentReport.toAlert(): Alert {
    return Alert(
        id = id,
        title = category,
        description = description,
        severity = if (category.isCritical()) Severity.CRITICAL else Severity.WARNING,
        timestamp = reportedAt,
        latitude = latitude,
        longitude = longitude
    )
}
