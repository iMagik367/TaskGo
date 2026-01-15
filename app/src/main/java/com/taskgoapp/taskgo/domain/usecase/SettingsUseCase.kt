package com.taskgoapp.taskgo.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.domain.repository.PreferencesRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class SettingsUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository,
    private val firebaseFunctionsService: FirebaseFunctionsService,
    private val firestoreUserRepository: com.taskgoapp.taskgo.data.repository.FirestoreUserRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) {
    
    fun observeSettings(): Flow<SettingsState> {
        return combine(
            preferencesRepository.observePromosEnabled(),
            preferencesRepository.observeSoundEnabled(),
            preferencesRepository.observePushEnabled(),
            preferencesRepository.observeLockscreenEnabled(),
            preferencesRepository.observeEmailNotificationsEnabled(),
            preferencesRepository.observeSmsNotificationsEnabled(),
            preferencesRepository.observeLanguage(),
            preferencesRepository.observeTheme(),
            preferencesRepository.observeCategories(),
            preferencesRepository.observePrivacyLocationSharing(),
            preferencesRepository.observePrivacyProfileVisible(),
            preferencesRepository.observePrivacyContactInfo(),
            preferencesRepository.observePrivacyAnalytics(),
            preferencesRepository.observePrivacyPersonalizedAds(),
            preferencesRepository.observePrivacyDataCollection(),
            preferencesRepository.observePrivacyThirdPartySharing(),
            userRepository.observeCurrentUser()
        ) { values ->
            SettingsState(
                promosEnabled = values[0] as Boolean,
                soundEnabled = values[1] as Boolean,
                pushEnabled = values[2] as Boolean,
                lockscreenEnabled = values[3] as Boolean,
                emailNotificationsEnabled = values[4] as Boolean,
                smsNotificationsEnabled = values[5] as Boolean,
                language = values[6] as String,
                theme = values[7] as String,
                categories = values[8] as String,
                locationSharingEnabled = values[9] as Boolean,
                profileVisible = values[10] as Boolean,
                contactInfoSharingEnabled = values[11] as Boolean,
                analyticsEnabled = values[12] as Boolean,
                personalizedAdsEnabled = values[13] as Boolean,
                dataCollectionEnabled = values[14] as Boolean,
                thirdPartySharingEnabled = values[15] as Boolean,
                currentUser = values[16] as com.taskgoapp.taskgo.core.model.UserProfile?
            )
        }
    }
    
    suspend fun updateNotificationSettings(
        promos: Boolean,
        sound: Boolean,
        push: Boolean,
        lockscreen: Boolean,
        email: Boolean,
        sms: Boolean
    ) {
        // Salvar localmente primeiro
        preferencesRepository.updatePromosEnabled(promos)
        preferencesRepository.updateSoundEnabled(sound)
        preferencesRepository.updatePushEnabled(push)
        preferencesRepository.updateLockscreenEnabled(lockscreen)
        preferencesRepository.updateEmailNotificationsEnabled(email)
        preferencesRepository.updateSmsNotificationsEnabled(sms)
        
        // Salvar diretamente no Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val user = firestoreUserRepository.getUser(currentUser.uid)
                if (user != null) {
                    val notificationSettings = com.taskgoapp.taskgo.data.firestore.models.NotificationSettingsFirestore(
                        push = push,
                        promos = promos,
                        sound = sound,
                        lockscreen = lockscreen,
                        email = email,
                        sms = sms
                    )
                    val updatedUser = user.copy(notificationSettings = notificationSettings)
                    val updateResult = firestoreUserRepository.updateUser(updatedUser)
                    updateResult.fold(
                        onSuccess = {
                            Log.d("SettingsUseCase", "Configurações de notificação salvas no Firestore")
                        },
                        onFailure = { error ->
                            Log.e("SettingsUseCase", "Erro ao salvar notificações no Firestore: ${error.message}", error)
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w("SettingsUseCase", "Operação de salvamento de notificações cancelada")
                throw e // Re-lançar CancellationException para propagar corretamente
            } catch (e: Exception) {
                Log.e("SettingsUseCase", "Erro ao salvar notificações no Firestore: ${e.message}", e)
            }
        }
        
        // Sync with backend via Cloud Functions (fallback)
        try {
            val result = firebaseFunctionsService.updateNotificationSettings(
                mapOf(
                    "promos" to promos,
                    "sound" to sound,
                    "push" to push,
                    "lockscreen" to lockscreen,
                    "email" to email,
                    "sms" to sms
                )
            )
            result.onSuccess { data ->
                Log.d("SettingsUseCase", "Configurações de notificação sincronizadas via Cloud Function: $data")
            }.onFailure { error ->
                Log.e("SettingsUseCase", "Erro ao sincronizar notificações via Cloud Function: ${error.message}", error)
                // Tratar erro específico do Secure Token API bloqueado
                if (error.message?.contains("SecureToken") == true || error.message?.contains("securetoken") == true) {
                    Log.w("SettingsUseCase", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsUseCase", "Erro ao chamar função de notificações: ${e.message}", e)
            if (e.message?.contains("SecureToken") == true || e.message?.contains("securetoken") == true) {
                Log.w("SettingsUseCase", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
            }
        }
    }
    
    suspend fun updateLanguage(language: String) {
        preferencesRepository.updateLanguage(language)
        
        try {
            val result = firebaseFunctionsService.updateLanguagePreference(language)
            result.onFailure { error ->
                Log.e("SettingsUseCase", "Erro ao sincronizar idioma: ${error.message}", error)
            }
        } catch (e: Exception) {
            Log.e("SettingsUseCase", "Erro ao chamar função de idioma: ${e.message}", e)
        }
    }
    
    suspend fun updateTheme(theme: String) {
        preferencesRepository.updateTheme(theme)
    }
    
    suspend fun updateCategories(categories: String) {
        // Save to local storage
        preferencesRepository.updateCategories(categories)
        
        // Sync to Firestore via Cloud Function
        try {
            // Parse JSON string to List<String>
            val categoriesList = try {
                val gson = Gson()
                val jsonArray = categories.removePrefix("[").removeSuffix("]")
                if (jsonArray.isBlank()) {
                    emptyList()
                } else {
                    jsonArray.split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotBlank() }
                }
            } catch (e: Exception) {
                Log.e("SettingsUseCase", "Error parsing categories JSON: ${e.message}", e)
                emptyList()
            }
            
            // Call Cloud Function to update preferences in Firestore
            val result = firebaseFunctionsService.updateUserPreferences(categoriesList)
            result.fold(
                onSuccess = {
                    Log.d("SettingsUseCase", "Preferences synced to Firestore successfully")
                },
                onFailure = { error ->
                    Log.e("SettingsUseCase", "Error syncing preferences to Firestore: ${error.message}", error)
                    // Don't throw - local save was successful, sync can fail silently
                }
            )
        } catch (e: Exception) {
            Log.e("SettingsUseCase", "Error syncing preferences: ${e.message}", e)
            // Don't throw - local save was successful
        }
    }
    
    suspend fun updatePrivacySettings(
        locationSharing: Boolean,
        profileVisible: Boolean,
        contactInfoSharing: Boolean,
        analytics: Boolean,
        personalizedAds: Boolean,
        dataCollection: Boolean,
        thirdPartySharing: Boolean
    ) {
        // Salvar localmente primeiro
        preferencesRepository.updatePrivacyLocationSharing(locationSharing)
        preferencesRepository.updatePrivacyProfileVisible(profileVisible)
        preferencesRepository.updatePrivacyContactInfo(contactInfoSharing)
        preferencesRepository.updatePrivacyAnalytics(analytics)
        preferencesRepository.updatePrivacyPersonalizedAds(personalizedAds)
        preferencesRepository.updatePrivacyDataCollection(dataCollection)
        preferencesRepository.updatePrivacyThirdPartySharing(thirdPartySharing)
        
        // Salvar diretamente no Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val user = firestoreUserRepository.getUser(currentUser.uid)
                if (user != null) {
                    val privacySettings = com.taskgoapp.taskgo.data.firestore.models.PrivacySettingsFirestore(
                        locationSharing = locationSharing,
                        profileVisible = profileVisible,
                        contactInfoSharing = contactInfoSharing,
                        analytics = analytics,
                        personalizedAds = personalizedAds,
                        dataCollection = dataCollection,
                        thirdPartySharing = thirdPartySharing
                    )
                    val updatedUser = user.copy(privacySettings = privacySettings)
                    firestoreUserRepository.updateUser(updatedUser)
                    Log.d("SettingsUseCase", "Configurações de privacidade salvas no Firestore")
                }
            } catch (e: Exception) {
                Log.e("SettingsUseCase", "Erro ao salvar privacidade no Firestore: ${e.message}", e)
            }
        }
        
        // Sync with backend via Cloud Functions (fallback)
        try {
            val result = firebaseFunctionsService.updatePrivacySettings(
                mapOf(
                    "locationSharing" to locationSharing,
                    "profileVisible" to profileVisible,
                    "contactInfoSharing" to contactInfoSharing,
                    "analytics" to analytics,
                    "personalizedAds" to personalizedAds,
                    "dataCollection" to dataCollection,
                    "thirdPartySharing" to thirdPartySharing
                )
            )
            result.onFailure { error ->
                Log.e("SettingsUseCase", "Erro ao sincronizar privacidade via Cloud Function: ${error.message}", error)
            }
        } catch (e: Exception) {
            Log.e("SettingsUseCase", "Erro ao chamar função de privacidade: ${e.message}", e)
        }
    }
    
    suspend fun syncRemoteSettings() {
        try {
            val result = firebaseFunctionsService.getUserSettings()
            result.onSuccess { data ->
                val notificationSettings = data["notificationSettings"] as? Map<*, *>
                val privacySettings = data["privacySettings"] as? Map<*, *>
                val language = data["language"] as? String
                val preferredCategories = data["preferredCategories"] as? List<*>
                
                notificationSettings?.let { settings ->
                    (settings["promos"] as? Boolean)?.let { preferencesRepository.updatePromosEnabled(it) }
                    (settings["sound"] as? Boolean)?.let { preferencesRepository.updateSoundEnabled(it) }
                    (settings["push"] as? Boolean)?.let { preferencesRepository.updatePushEnabled(it) }
                    (settings["lockscreen"] as? Boolean)?.let { preferencesRepository.updateLockscreenEnabled(it) }
                    (settings["email"] as? Boolean)?.let { preferencesRepository.updateEmailNotificationsEnabled(it) }
                    (settings["sms"] as? Boolean)?.let { preferencesRepository.updateSmsNotificationsEnabled(it) }
                }
                
                privacySettings?.let { settings ->
                    (settings["locationSharing"] as? Boolean)?.let { preferencesRepository.updatePrivacyLocationSharing(it) }
                    (settings["profileVisible"] as? Boolean)?.let { preferencesRepository.updatePrivacyProfileVisible(it) }
                    (settings["contactInfoSharing"] as? Boolean)?.let { preferencesRepository.updatePrivacyContactInfo(it) }
                    (settings["analytics"] as? Boolean)?.let { preferencesRepository.updatePrivacyAnalytics(it) }
                    (settings["personalizedAds"] as? Boolean)?.let { preferencesRepository.updatePrivacyPersonalizedAds(it) }
                    (settings["dataCollection"] as? Boolean)?.let { preferencesRepository.updatePrivacyDataCollection(it) }
                    (settings["thirdPartySharing"] as? Boolean)?.let { preferencesRepository.updatePrivacyThirdPartySharing(it) }
                }
                
                language?.let { preferencesRepository.updateLanguage(it) }
                
                preferredCategories?.let { list ->
                    val json = list.filterIsInstance<String>()
                        .joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                    preferencesRepository.updateCategories(json)
                }
            }.onFailure { error ->
                Log.e("SettingsUseCase", "Erro ao buscar configurações do usuário: ${error.message}", error)
                // Tratar erro específico do Secure Token API bloqueado
                if (error.message?.contains("SecureToken") == true || error.message?.contains("securetoken") == true) {
                    Log.w("SettingsUseCase", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w("SettingsUseCase", "Sincronização de configurações remotas cancelada")
            throw e // Re-lançar CancellationException para propagar corretamente
        } catch (e: Exception) {
            Log.e("SettingsUseCase", "Erro ao sincronizar configurações remotas: ${e.message}", e)
            if (e.message?.contains("SecureToken") == true || e.message?.contains("securetoken") == true) {
                Log.w("SettingsUseCase", "Firebase Secure Token API bloqueado. Verifique configurações do Google Cloud.")
            }
        }
    }
    
    suspend fun updateUserProfile(user: com.taskgoapp.taskgo.core.model.UserProfile) {
        userRepository.updateUser(user)
    }
    
    suspend fun updateUserAvatar(avatarUri: String) {
        userRepository.updateAvatar(avatarUri)
    }
}

data class SettingsState(
    val promosEnabled: Boolean,
    val soundEnabled: Boolean,
    val pushEnabled: Boolean,
    val lockscreenEnabled: Boolean,
    val emailNotificationsEnabled: Boolean,
    val smsNotificationsEnabled: Boolean,
    val language: String,
    val theme: String,
    val categories: String,
    val locationSharingEnabled: Boolean,
    val profileVisible: Boolean,
    val contactInfoSharingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val personalizedAdsEnabled: Boolean,
    val dataCollectionEnabled: Boolean,
    val thirdPartySharingEnabled: Boolean,
    val currentUser: com.taskgoapp.taskgo.core.model.UserProfile?
)
