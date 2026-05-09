package com.apeligrate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val severity: String,
    val timestamp: Long
)

fun AlertEntity.toDomain() = Alert(
    id = id,
    title = title,
    description = description,
    severity = Severity.valueOf(severity),
    timestamp = timestamp
)

fun Alert.toEntity() = AlertEntity(
    id = id,
    title = title,
    description = description,
    severity = severity.name,
    timestamp = timestamp
)
