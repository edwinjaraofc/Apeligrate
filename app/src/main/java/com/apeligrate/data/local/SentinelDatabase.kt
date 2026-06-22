package com.apeligrate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apeligrate.data.local.dao.AlertDao
import com.apeligrate.data.local.dao.NotificationCooldownDao
import com.apeligrate.data.local.entity.AlertEntity
import com.apeligrate.data.local.entity.NotificationCooldownEntity

@Database(
    entities = [AlertEntity::class, NotificationCooldownEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun notificationCooldownDao(): NotificationCooldownDao
}
