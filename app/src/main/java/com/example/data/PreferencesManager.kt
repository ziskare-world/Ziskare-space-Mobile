package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ziskare_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val CSRF_TOKEN_KEY = stringPreferencesKey("csrf_token")
        val SESSION_ID_KEY = stringPreferencesKey("session_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
        val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")

        // Default points directly to user's real AI Studio Development/Preview instance
        const val DEFAULT_SERVER_URL = "https://ais-dev-ikkrirhwqbdyccrwxjy4k5-966729313146.asia-southeast1.run.app"
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY] ?: DEFAULT_SERVER_URL
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN_KEY]
    }

    val csrfToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CSRF_TOKEN_KEY]
    }

    val sessionId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SESSION_ID_KEY]
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY] ?: "Anonymous Administrator"
    }

    val userRole: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_ROLE_KEY] ?: "Administrator"
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: true
    }

    suspend fun saveServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL_KEY] = cleanUrl
        }
    }

    suspend fun saveAuthSession(token: String?, csrf: String?, sessionId: String?, name: String?, role: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) prefs[AUTH_TOKEN_KEY] = token else prefs.remove(AUTH_TOKEN_KEY)
            if (csrf != null) prefs[CSRF_TOKEN_KEY] = csrf else prefs.remove(CSRF_TOKEN_KEY)
            if (sessionId != null) prefs[SESSION_ID_KEY] = sessionId else prefs.remove(SESSION_ID_KEY)
            if (name != null) prefs[USER_NAME_KEY] = name else prefs.remove(USER_NAME_KEY)
            if (role != null) prefs[USER_ROLE_KEY] = role else prefs.remove(USER_ROLE_KEY)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(CSRF_TOKEN_KEY)
            prefs.remove(SESSION_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_ROLE_KEY)
        }
    }
}
