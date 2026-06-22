package com.apeligrate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_cooldowns")
data class NotificationCooldownEntity(
    @PrimaryKey val id: String,
    val lastNotifiedAt: Long
)
