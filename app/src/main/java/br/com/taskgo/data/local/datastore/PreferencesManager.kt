package br.com.taskgo.taskgo.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskgo_preferences")

object PrefsKeys {
    val PROMOS = booleanPreferencesKey("notif_promotions")
    val SOUND = booleanPreferencesKey("notif_sound")
    val PUSH = booleanPreferencesKey("notif_push")
    val LOCKSCREEN = booleanPreferencesKey("notif_lockscreen")
    val LANGUAGE = stringPreferencesKey("language") // "pt","en","es","fr","it","de"
    val THEME = stringPreferencesKey("theme") // "light","dark","system"
    val CATEGORIES = stringPreferencesKey("categories_json")
}

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    // Notification preferences
    val promosEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PROMOS] ?: true
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.SOUND] ?: true
    }

    val pushEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PUSH] ?: true
    }

    val lockscreenEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.LOCKSCREEN] ?: true
    }

    // Language preference
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.LANGUAGE] ?: "pt"
    }

    // Theme preference
    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.THEME] ?: "system"
    }

    // Categories preference
    val categories: Flow<String> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.CATEGORIES] ?: "[]"
    }

    suspend fun updatePromosEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PROMOS] = enabled
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.SOUND] = enabled
        }
    }

    suspend fun updatePushEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PUSH] = enabled
        }
    }

    suspend fun updateLockscreenEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.LOCKSCREEN] = enabled
        }
    }

    suspend fun updateLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.LANGUAGE] = language
        }
    }

    suspend fun updateTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.THEME] = theme
        }
    }

    suspend fun updateCategories(categories: String) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.CATEGORIES] = categories
        }
    }
}
