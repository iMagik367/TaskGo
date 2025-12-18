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
    val EMAIL_NOTIFICATIONS = booleanPreferencesKey("notif_email")
    val SMS_NOTIFICATIONS = booleanPreferencesKey("notif_sms")
    val LANGUAGE = stringPreferencesKey("language") // "pt","en","es","fr","it","de"
    val THEME = stringPreferencesKey("theme") // "light","dark","system"
    val CATEGORIES = stringPreferencesKey("categories_json")
    
    // Privacy
    val PRIVACY_LOCATION = booleanPreferencesKey("privacy_location")
    val PRIVACY_PROFILE_VISIBLE = booleanPreferencesKey("privacy_profile_visible")
    val PRIVACY_CONTACT_INFO = booleanPreferencesKey("privacy_contact_info")
    val PRIVACY_ANALYTICS = booleanPreferencesKey("privacy_analytics")
    val PRIVACY_PERSONALIZED_ADS = booleanPreferencesKey("privacy_personalized_ads")
    val PRIVACY_DATA_COLLECTION = booleanPreferencesKey("privacy_data_collection")
    val PRIVACY_THIRD_PARTY = booleanPreferencesKey("privacy_third_party")
    
    // Biometric & 2FA
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val TWO_FACTOR_ENABLED = booleanPreferencesKey("two_factor_enabled")
    val TWO_FACTOR_METHOD = stringPreferencesKey("two_factor_method") // "sms", "email", "authenticator"
    val SAVED_EMAIL_FOR_BIOMETRIC = stringPreferencesKey("saved_email_biometric")
    
    // Permissions
    val PERMISSIONS_REQUESTED = booleanPreferencesKey("permissions_requested")
    
    // Initial sync tracking (por usuário)
    // Usa stringPreferencesKey para armazenar JSON com userId -> timestamp
    val INITIAL_SYNC_COMPLETED = stringPreferencesKey("initial_sync_completed")
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
    
    val emailNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.EMAIL_NOTIFICATIONS] ?: false
    }
    
    val smsNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.SMS_NOTIFICATIONS] ?: false
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
    
    // Privacy preferences
    val privacyLocationSharing: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_LOCATION] ?: true
    }
    
    val privacyProfileVisible: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_PROFILE_VISIBLE] ?: true
    }
    
    val privacyContactInfo: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_CONTACT_INFO] ?: false
    }
    
    val privacyAnalytics: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_ANALYTICS] ?: true
    }
    
    val privacyPersonalizedAds: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_PERSONALIZED_ADS] ?: false
    }
    
    val privacyDataCollection: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_DATA_COLLECTION] ?: true
    }
    
    val privacyThirdPartySharing: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PRIVACY_THIRD_PARTY] ?: false
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
    
    suspend fun updateEmailNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.EMAIL_NOTIFICATIONS] = enabled
        }
    }
    
    suspend fun updateSmsNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.SMS_NOTIFICATIONS] = enabled
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
    
    suspend fun updatePrivacyLocationSharing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_LOCATION] = enabled
        }
    }
    
    suspend fun updatePrivacyProfileVisible(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_PROFILE_VISIBLE] = enabled
        }
    }
    
    suspend fun updatePrivacyContactInfo(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_CONTACT_INFO] = enabled
        }
    }
    
    suspend fun updatePrivacyAnalytics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_ANALYTICS] = enabled
        }
    }
    
    suspend fun updatePrivacyPersonalizedAds(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_PERSONALIZED_ADS] = enabled
        }
    }
    
    suspend fun updatePrivacyDataCollection(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_DATA_COLLECTION] = enabled
        }
    }
    
    suspend fun updatePrivacyThirdPartySharing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PRIVACY_THIRD_PARTY] = enabled
        }
    }
    
    // Permissions tracking
    val permissionsRequested: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.PERMISSIONS_REQUESTED] ?: false
    }
    
    suspend fun setPermissionsRequested(requested: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.PERMISSIONS_REQUESTED] = requested
        }
    }
    
    // Initial sync tracking
    suspend fun isInitialSyncCompleted(userId: String): Boolean {
        val syncData = dataStore.data.first()[PrefsKeys.INITIAL_SYNC_COMPLETED] ?: "{}"
        return try {
            val json = org.json.JSONObject(syncData)
            json.has(userId) && json.getLong(userId) > 0
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun setInitialSyncCompleted(userId: String) {
        dataStore.edit { preferences ->
            val syncData = preferences[PrefsKeys.INITIAL_SYNC_COMPLETED] ?: "{}"
            try {
                val json = org.json.JSONObject(syncData)
                json.put(userId, System.currentTimeMillis())
                preferences[PrefsKeys.INITIAL_SYNC_COMPLETED] = json.toString()
            } catch (e: Exception) {
                // Se houver erro, criar novo JSON
                val json = org.json.JSONObject()
                json.put(userId, System.currentTimeMillis())
                preferences[PrefsKeys.INITIAL_SYNC_COMPLETED] = json.toString()
            }
        }
    }
    
    suspend fun clearInitialSyncForUser(userId: String) {
        dataStore.edit { preferences ->
            val syncData = preferences[PrefsKeys.INITIAL_SYNC_COMPLETED] ?: "{}"
            try {
                val json = org.json.JSONObject(syncData)
                json.remove(userId)
                preferences[PrefsKeys.INITIAL_SYNC_COMPLETED] = json.toString()
            } catch (e: Exception) {
                // Ignorar erro
            }
        }
    }
}
