package com.taskgoapp.taskgo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val authRepository: FirebaseAuthRepository,
    val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // Observar accountType do usuário atual para usar na BottomNavigationBar
    val accountType: StateFlow<AccountType?> = userRepository
        .observeCurrentUser()
        .map { user -> user?.accountType }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )
}

