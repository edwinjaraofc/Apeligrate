package com.apeligrate.domain.model

data class RiskMetric(
    val level: Int, // 0-100
    val label: String, // e.g., "ALTO", "MODERADO"
    val areaName: String,
    val trend: Trend
)

enum class Trend {
    RISING, STABLE, FALLING
}
