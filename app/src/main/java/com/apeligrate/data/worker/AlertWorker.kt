package com.apeligrate.data.worker

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.apeligrate.data.local.SentinelDatabase
import com.apeligrate.data.local.entity.NotificationCooldownEntity
import com.apeligrate.data.repository.IncidentReportRepositoryImpl
import com.apeligrate.domain.model.isCritical
import com.apeligrate.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

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
        
        // 3. Filtrar por cercanía (200 metros)
        val nearbyReports = reports.filter { report ->
            val lat = report.latitude ?: return@filter false
            val lng = report.longitude ?: return@filter false
            val results = FloatArray(1)
            Location.distanceBetween(lastLocation.latitude, lastLocation.longitude, lat, lng, results)
            results[0] < 200f
        }

        if (nearbyReports.isEmpty()) return Result.success()

        // 4. Lógica de Agrupación y Cooldown
        // Usamos una "ZoneKey" basada en coordenadas redondeadas para agrupar incidentes cercanos
        val mostCritical = nearbyReports.maxByOrNull { if (it.category.isCritical()) 2 else 1 } ?: return Result.success()
        val zoneKey = "${(mostCritical.latitude!! * 1000).roundToInt()}_${(mostCritical.longitude!! * 1000).roundToInt()}"
        
        val cooldownDao = database.notificationCooldownDao()
        val lastNotified = cooldownDao.getLastNotifiedAt(zoneKey)
        val threeHoursMillis = 3 * 60 * 60 * 1000

        if (lastNotified == null || (System.currentTimeMillis() - lastNotified) > threeHoursMillis) {
            // Enviar Notificación
            val title = if (mostCritical.category.isCritical()) "¡ALERTA CRÍTICA!" else "Incidente cercano"
            val message = if (nearbyReports.size > 1) {
                "${mostCritical.category} y ${nearbyReports.size - 1} incidentes más en tu zona."
            } else {
                "Se reportó ${mostCritical.category} cerca de tu ubicación."
            }

            notificationHelper.showProximityAlert(title, message)

            // Actualizar Cooldown
            cooldownDao.updateLastNotifiedAt(NotificationCooldownEntity(zoneKey, System.currentTimeMillis()))
            
            // Limpiar tabla de registros muy viejos (más de 24h)
            cooldownDao.clearOldCooldowns(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        }

        return Result.success()
    }
}
