package com.mario.tanamin.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object SessionKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val TOKEN = stringPreferencesKey("token")
}

// DataStore instance attached to Context
val Context.dataStore by preferencesDataStore(name = "session")

/**
 * In-memory token holder for use by interceptors. Updated when saveSession/clearSession is called.
 * Keeps a volatile copy of the latest token and userId so OkHttp interceptors can access them synchronously.
 */
object InMemorySessionHolder {
    @Volatile
    var userId: String? = null

    @Volatile
    var token: String? = null
}

class SessionManager(private val context: Context) {
    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[SessionKeys.USER_ID] }
    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[SessionKeys.TOKEN] }

    // Convenient suspend helpers to get the current values once
    suspend fun getUserId(): String? = userIdFlow.first()
    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun saveSession(userId: String, token: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[SessionKeys.USER_ID] = userId
            prefs[SessionKeys.TOKEN] = token
        }
        // update in-memory holder for synchronous access (interceptor)
        InMemorySessionHolder.userId = userId
        InMemorySessionHolder.token = token
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs.remove(SessionKeys.USER_ID)
            prefs.remove(SessionKeys.TOKEN)
        }
        InMemorySessionHolder.userId = null
        InMemorySessionHolder.token = null
    }
}
