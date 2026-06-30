package com.apeligrate.domain.model

data class DangerZone(
    val id: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Float,
    val reports: List<Alert>,
    val grouped: Boolean
) {
    val reportCount: Int get() = reports.size
    val primaryReport: Alert get() = reports.first()
}
