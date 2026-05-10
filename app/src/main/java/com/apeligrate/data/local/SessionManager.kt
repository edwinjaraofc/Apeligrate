package com.apeligrate.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SESSION_DATASTORE = "apeligrate_session"
private val Context.sessionDataStore by preferencesDataStore(name = SESSION_DATASTORE)
private const val TAG = "SessionManager"

class SessionManager(private val context: Context) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val isLoggedInFlow: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        val isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
        Log.d(TAG, "Reading isLoggedIn from DataStore: $isLoggedIn")
        isLoggedIn
    }

    val userIdFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        Log.d(TAG, "Reading userId from DataStore: $userId")
        userId
    }

    suspend fun setSession(userId: String) {
        Log.d(TAG, "Saving session: userId=$userId, isLoggedIn=true")
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.IS_LOGGED_IN] = true
            Log.d(TAG, "Session saved successfully")
        }
    }

    suspend fun clearSession() {
        Log.d(TAG, "Clearing session")
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs[Keys.IS_LOGGED_IN] = false
            Log.d(TAG, "Session cleared successfully")
        }
    }
}

