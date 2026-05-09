package com.apeligrate.domain.model

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val timestamp: Long,
)

enum class Severity {
    SAFE, WARNING, CRITICAL
}
