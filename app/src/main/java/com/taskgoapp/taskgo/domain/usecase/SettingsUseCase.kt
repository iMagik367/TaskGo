package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.domain.repository.PreferencesRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class SettingsUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository
) {
    
    fun observeSettings(): Flow<SettingsState> {
        return combine(
            preferencesRepository.observePromosEnabled(),
            preferencesRepository.observeSoundEnabled(),
            preferencesRepository.observePushEnabled(),
            preferencesRepository.observeLockscreenEnabled(),
            preferencesRepository.observeLanguage(),
            preferencesRepository.observeTheme(),
            preferencesRepository.observeCategories(),
            userRepository.observeCurrentUser()
        ) { values ->
            SettingsState(
                promosEnabled = values[0] as Boolean,
                soundEnabled = values[1] as Boolean,
                pushEnabled = values[2] as Boolean,
                lockscreenEnabled = values[3] as Boolean,
                language = values[4] as String,
                theme = values[5] as String,
                categories = values[6] as String,
                currentUser = values[7] as com.taskgoapp.taskgo.core.model.UserProfile?
            )
        }
    }
    
    suspend fun updateNotificationSettings(
        promos: Boolean,
        sound: Boolean,
        push: Boolean,
        lockscreen: Boolean
    ) {
        preferencesRepository.updatePromosEnabled(promos)
        preferencesRepository.updateSoundEnabled(sound)
        preferencesRepository.updatePushEnabled(push)
        preferencesRepository.updateLockscreenEnabled(lockscreen)
    }
    
    suspend fun updateLanguage(language: String) {
        preferencesRepository.updateLanguage(language)
    }
    
    suspend fun updateTheme(theme: String) {
        preferencesRepository.updateTheme(theme)
    }
    
    suspend fun updateCategories(categories: String) {
        preferencesRepository.updateCategories(categories)
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
    val language: String,
    val theme: String,
    val categories: String,
    val currentUser: com.taskgoapp.taskgo.core.model.UserProfile?
)
