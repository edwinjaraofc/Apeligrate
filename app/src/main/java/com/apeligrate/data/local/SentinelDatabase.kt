package com.apeligrate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apeligrate.data.local.dao.AlertDao
import com.apeligrate.data.local.entity.AlertEntity

@Database(entities = [AlertEntity::class], version = 1, exportSchema = false)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}
