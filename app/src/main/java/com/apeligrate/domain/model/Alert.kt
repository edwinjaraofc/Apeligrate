package com.apeligrate.domain.model

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

enum class Severity {
    SAFE, WARNING, CRITICAL
}
