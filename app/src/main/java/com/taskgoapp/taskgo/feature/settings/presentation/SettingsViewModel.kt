package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    val state: StateFlow<com.taskgoapp.taskgo.domain.usecase.SettingsState> =
        settingsUseCase.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            com.taskgoapp.taskgo.domain.usecase.SettingsState(
                promosEnabled = true,
                soundEnabled = true,
                pushEnabled = true,
                lockscreenEnabled = true,
                emailNotificationsEnabled = false,
                smsNotificationsEnabled = false,
                language = "pt",
                theme = "system",
                categories = "[]",
                locationSharingEnabled = true,
                profileVisible = true,
                contactInfoSharingEnabled = false,
                analyticsEnabled = true,
                personalizedAdsEnabled = false,
                dataCollectionEnabled = true,
                thirdPartySharingEnabled = false,
                currentUser = null
            )
        )
    
    init {
        viewModelScope.launch {
            settingsUseCase.syncRemoteSettings()
        }
    }

    fun saveNotifications(
        promos: Boolean,
        sound: Boolean,
        push: Boolean,
        lockscreen: Boolean,
        email: Boolean,
        sms: Boolean
    ) {
        viewModelScope.launch {
            settingsUseCase.updateNotificationSettings(
                promos = promos,
                sound = sound,
                push = push,
                lockscreen = lockscreen,
                email = email,
                sms = sms
            )
        }
    }

    fun saveLanguage(language: String) {
        viewModelScope.launch { settingsUseCase.updateLanguage(language) }
    }

    fun saveTheme(theme: String) {
        viewModelScope.launch { settingsUseCase.updateTheme(theme) }
    }

    fun saveCategories(json: String) {
        viewModelScope.launch { settingsUseCase.updateCategories(json) }
    }
    
    fun savePrivacySettings(
        locationSharing: Boolean,
        profileVisible: Boolean,
        contactInfoSharing: Boolean,
        analytics: Boolean,
        personalizedAds: Boolean,
        dataCollection: Boolean,
        thirdPartySharing: Boolean
    ) {
        viewModelScope.launch {
            settingsUseCase.updatePrivacySettings(
                locationSharing = locationSharing,
                profileVisible = profileVisible,
                contactInfoSharing = contactInfoSharing,
                analytics = analytics,
                personalizedAds = personalizedAds,
                dataCollection = dataCollection,
                thirdPartySharing = thirdPartySharing
            )
        }
    }
    
    fun refreshSettings() {
        viewModelScope.launch {
            settingsUseCase.syncRemoteSettings()
        }
    }
}


