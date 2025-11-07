package com.taskgoapp.taskgo.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    
    // Biometric & 2FA
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val TWO_FACTOR_ENABLED = booleanPreferencesKey("two_factor_enabled")
    val TWO_FACTOR_METHOD = stringPreferencesKey("two_factor_method") // "sms", "email", "authenticator"
    val SAVED_EMAIL_FOR_BIOMETRIC = stringPreferencesKey("saved_email_biometric")
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

    // Biometric & 2FA preferences
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.BIOMETRIC_ENABLED] ?: false
    }

    val twoFactorEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.TWO_FACTOR_ENABLED] ?: false
    }

    val twoFactorMethod: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.TWO_FACTOR_METHOD]
    }

    suspend fun updateBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun updateTwoFactorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.TWO_FACTOR_ENABLED] = enabled
        }
    }

    suspend fun updateTwoFactorMethod(method: String?) {
        dataStore.edit { preferences ->
            if (method != null) {
                preferences[PrefsKeys.TWO_FACTOR_METHOD] = method
            } else {
                preferences.remove(PrefsKeys.TWO_FACTOR_METHOD)
            }
        }
    }

    suspend fun saveEmailForBiometric(email: String) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.SAVED_EMAIL_FOR_BIOMETRIC] = email
        }
    }

    suspend fun getEmailForBiometric(): String? {
        return dataStore.data.map { preferences ->
            preferences[PrefsKeys.SAVED_EMAIL_FOR_BIOMETRIC]
        }.first()
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
