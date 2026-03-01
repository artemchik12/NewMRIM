package org.artemchik.newmrim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import org.artemchik.newmrim.protocol.data.ServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mrim_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HOST = stringPreferencesKey("server_host")
        val PORT = intPreferencesKey("server_port")
        val USE_REDIRECTOR = booleanPreferencesKey("use_redirector")
        val AVATAR_HOST = stringPreferencesKey("avatar_host")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        val REMEMBER_PASSWORD = booleanPreferencesKey("remember_password")
    }

    val serverConfig: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        ServerConfig(
            host = prefs[Keys.HOST] ?: ServerConfig.DEFAULT_HOST,
            port = prefs[Keys.PORT] ?: ServerConfig.DEFAULT_PORT,
            useRedirector = prefs[Keys.USE_REDIRECTOR] ?: true,
            avatarHost = prefs[Keys.AVATAR_HOST] ?: ServerConfig.DEFAULT_AVATAR_HOST
        )
    }

    val lastEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_EMAIL] ?: ""
    }

    val savedPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SAVED_PASSWORD] ?: ""
    }

    val rememberPassword: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMEMBER_PASSWORD] ?: false
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOST] = config.host
            prefs[Keys.PORT] = config.port
            prefs[Keys.USE_REDIRECTOR] = config.useRedirector
            prefs[Keys.AVATAR_HOST] = config.avatarHost
        }
    }

    suspend fun saveCredentials(email: String, password: String, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_EMAIL] = email
            prefs[Keys.REMEMBER_PASSWORD] = remember
            prefs[Keys.SAVED_PASSWORD] = if (remember) password else ""
        }
    }

    suspend fun clearPassword() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_PASSWORD] = ""
            prefs[Keys.REMEMBER_PASSWORD] = false
        }
    }
}