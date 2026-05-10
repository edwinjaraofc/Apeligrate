package com.apeligrate.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "apeligrate_prefs"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class DataStoreManager(private val context: Context) {
    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
    }

    suspend fun saveUserId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = id
        }
    }

    val userIdFlow = context.dataStore.data.map { prefs -> prefs[KEY_USER_ID] }

    suspend fun clearUserId() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
        }
    }
}

