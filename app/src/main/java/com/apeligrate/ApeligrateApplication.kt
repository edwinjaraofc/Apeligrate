package com.apeligrate

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.apeligrate.data.worker.AlertWorker
import java.util.concurrent.TimeUnit

class ApeligrateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupBackgroundAlerts()
    }

    private fun setupBackgroundAlerts() {
        val workRequest = PeriodicWorkRequestBuilder<AlertWorker>(
            15, TimeUnit.MINUTES // Intervalo mínimo permitido por Android
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SecurityAlertsWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
