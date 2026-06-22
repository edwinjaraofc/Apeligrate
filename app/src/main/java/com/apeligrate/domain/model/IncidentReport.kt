package com.apeligrate.domain.model

data class IncidentReport(
    val id: String = "",
    val category: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val isAnonymous: Boolean = false,
    val reportedAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val status: String = "pending",
    val images: List<String> = emptyList(),
    
    // Feed specifics
    val validationCount: Int = 0,
    val falseCount: Int = 0,
    val persistenceMessage: String = "" // e.g., "42 reportes similares"
)

fun String.isCritical(): Boolean {
    return this == "Robo a mano armada" || this == "Hurto/Arrebato"
}

fun IncidentReport.toAlert(): Alert {
    return Alert(
        id = this.id,
        title = this.category,
        description = this.description,
        severity = if (this.category.isCritical()) Severity.CRITICAL else Severity.WARNING,
        timestamp = this.reportedAt,
        latitude = this.latitude,
        longitude = this.longitude
    )
}

