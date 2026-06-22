package com.apeligrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apeligrate.data.local.entity.NotificationCooldownEntity

@Dao
interface NotificationCooldownDao {

    @Query("SELECT * FROM notification_cooldown WHERE geofenceId = :geofenceId")
    suspend fun getCooldown(geofenceId: String): NotificationCooldownEntity?

    @Query("SELECT lastNotificationTime FROM notification_cooldown WHERE geofenceId = :geofenceId")
    suspend fun getLastNotificationTime(geofenceId: String): Long?

    @Query("SELECT lastNotificationTime FROM notification_cooldown WHERE geofenceId = :zoneKey")
    suspend fun getLastNotifiedAt(zoneKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCooldown(cooldown: NotificationCooldownEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateLastNotifiedAt(cooldown: NotificationCooldownEntity)

    @Query("DELETE FROM notification_cooldown WHERE lastNotificationTime < :oldTimestamp")
    suspend fun deleteOldCooldowns(oldTimestamp: Long)

    @Query("DELETE FROM notification_cooldown WHERE lastNotificationTime < :oldTimestamp")
    suspend fun clearOldCooldowns(oldTimestamp: Long)

    @Query("DELETE FROM notification_cooldown")
    suspend fun clearAll()
}
