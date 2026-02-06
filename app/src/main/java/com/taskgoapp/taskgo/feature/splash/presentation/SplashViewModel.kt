package com.taskgoapp.taskgo.feature.splash.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.data.repository.AuthRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val userRepository: UserRepository,
    private val locationUpdateService: com.taskgoapp.taskgo.core.location.LocationUpdateService
) : ViewModel() {
    
    val permissionsRequested = preferencesManager.permissionsRequested
    
    fun setPermissionsRequested(requested: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPermissionsRequested(requested)
        }
    }
    
    fun checkAuthState(
        onNavigateToBiometricAuth: () -> Unit,
        onNavigateToHome: () -> Unit,
        onNavigateToLogin: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("SplashViewModel", "=== Verificando estado de autenticação ===")
                
                // Verificar se usuário está autenticado
                val isAuthenticated = authRepository.isAuthenticated()
                val currentUser = authRepository.getCurrentUser()
                
                Log.d("SplashViewModel", "isAuthenticated: $isAuthenticated, currentUser: ${currentUser?.email}, id: ${currentUser?.id}")
                
                if (isAuthenticated && currentUser != null) {
                        // Verificar se o usuário tem city/state no perfil (obrigatório)
                        val currentUserProfile = withTimeoutOrNull(5.seconds) {
                            userRepository.observeCurrentUser().first()
                        }
                        
                        val userCity = currentUserProfile?.city?.takeIf { it.isNotBlank() }
                        val userState = currentUserProfile?.state?.takeIf { it.isNotBlank() }
                        
                        if (userCity != null && userState != null) {
                            Log.d("SplashViewModel", """
                                Usuário tem localização no perfil:
                                City: $userCity
                                State: $userState
                            """.trimIndent())
                            
                        // Verificar se precisa fazer sync inicial (em background, não bloqueia navegação)
                    val needsSync = !preferencesManager.isInitialSyncCompleted(currentUser.id)
                    if (needsSync) {
                        Log.d("SplashViewModel", "Primeiro acesso do usuário, iniciando sincronização em background...")
                        // Executar sync em background sem bloquear navegação
                        viewModelScope.launch {
                            try {
                                initialDataSyncManager.syncAllUserData()
                                preferencesManager.setInitialSyncCompleted(currentUser.id)
                                Log.d("SplashViewModel", "Sincronização inicial concluída")
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao sincronizar dados iniciais: ${e.message}", e)
                                // Continuar mesmo se o sync falhar - não é crítico para login
                            }
                        }
                    }
                    
                    // Usuário autenticado - navegar para home
                    Log.d("SplashViewModel", "Usuário autenticado, navegando para home")
                    onNavigateToHome()
                } else {
                    // Se não estiver logado, ir para login
                    Log.d("SplashViewModel", "Usuário não logado, navegando para login")
                    onNavigateToLogin()
                }
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Erro ao verificar credenciais", e)
                // Em caso de erro, ir para login
                onNavigateToLogin()
            }
        }
    }
}

