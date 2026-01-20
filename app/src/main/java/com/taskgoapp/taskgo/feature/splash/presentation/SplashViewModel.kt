package com.taskgoapp.taskgo.feature.splash.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.core.location.LocationStateManager
import com.taskgoapp.taskgo.core.location.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authRepository: FirebaseAuthRepository,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val locationStateManager: LocationStateManager
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
                
                // Usar getCurrentUser() diretamente - mais confiável no primeiro acesso
                // observeAuthState() pode retornar usuário em cache que não é válido
                val currentUser = authRepository.getCurrentUser()
                
                Log.d("SplashViewModel", "currentUser: ${currentUser?.email}, uid: ${currentUser?.uid}")
                
                if (currentUser != null) {
                    // CRÍTICO: Validar token antes de considerar usuário autenticado
                    // getCurrentUser() pode retornar usuário em cache que não é mais válido
                    var tokenValid = false
                    try {
                        // Tentar obter token com timeout curto
                        val token = withTimeoutOrNull(3.seconds) {
                            currentUser.getIdToken(false).await()
                        }
                        if (token != null) {
                            tokenValid = true
                            Log.d("SplashViewModel", "Token do usuário válido")
                        } else {
                            // Se falhar, tentar com refresh (pode demorar mais)
                            Log.w("SplashViewModel", "Token inicial falhou, tentando refresh...")
                            val refreshedToken = withTimeoutOrNull(5.seconds) {
                                currentUser.getIdToken(true).await()
                            }
                            if (refreshedToken != null) {
                                tokenValid = true
                                Log.d("SplashViewModel", "Token do usuário validado após refresh")
                            } else {
                                Log.w("SplashViewModel", "Refresh do token falhou (timeout)")
                                tokenValid = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SplashViewModel", "Erro ao validar token: ${e.message}")
                        // Se houver erro específico de token inválido/expirado, fazer signOut
                        if (e.message?.contains("auth/invalid-user-token") == true ||
                            e.message?.contains("auth/user-token-expired") == true ||
                            e.message?.contains("auth/user-disabled") == true) {
                            Log.w("SplashViewModel", "Token inválido/expirado, fazendo signOut e navegando para login")
                            try {
                                authRepository.signOut()
                            } catch (signOutError: Exception) {
                                Log.e("SplashViewModel", "Erro ao fazer signOut: ${signOutError.message}")
                            }
                            onNavigateToLogin()
                            return@launch
                        }
                        tokenValid = false
                    }
                    
                    // CRÍTICO: Só navegar para home se o token for válido E localização estiver pronta
                    if (tokenValid) {
                        // ⚠️ ETAPA 4: Garantir localização no login/primeiro uso
                        // Aguardar localização estar pronta antes de navegar para home
                        Log.d("SplashViewModel", "Aguardando localização estar pronta...")
                        
                        // Aguardar localização com timeout de 10 segundos
                        val locationState = withTimeoutOrNull(10.seconds) {
                            locationStateManager.locationState.first { it is LocationState.Ready || it is LocationState.Error }
                        }
                        
                        when (locationState) {
                            is LocationState.Ready -> {
                                Log.d("SplashViewModel", """
                                    Localização pronta:
                                    City: ${locationState.city}
                                    State: ${locationState.state}
                                    LocationId: ${locationState.locationId}
                                """.trimIndent())
                                
                                // Verificar se precisa fazer sync inicial (em background, não bloqueia navegação)
                                val needsSync = !preferencesManager.isInitialSyncCompleted(currentUser.uid)
                                if (needsSync) {
                                    Log.d("SplashViewModel", "Primeiro acesso do usuário, iniciando sincronização em background...")
                                    // Executar sync em background sem bloquear navegação
                                    viewModelScope.launch {
                                        try {
                                            initialDataSyncManager.syncAllUserData()
                                            preferencesManager.setInitialSyncCompleted(currentUser.uid)
                                            Log.d("SplashViewModel", "Sincronização inicial concluída")
                                        } catch (e: Exception) {
                                            Log.e("SplashViewModel", "Erro ao sincronizar dados iniciais: ${e.message}", e)
                                            // Continuar mesmo se o sync falhar - não é crítico para login
                                        }
                                    }
                                }
                                
                                // Localização pronta - navegar para home
                                Log.d("SplashViewModel", "Usuário autenticado e localização pronta, navegando para home")
                                onNavigateToHome()
                            }
                            is LocationState.Error -> {
                                Log.e("SplashViewModel", "Erro ao obter localização: ${locationState.reason}")
                                // Continuar para home mesmo sem localização (pode ser corrigido depois)
                                // Mas isso não deve acontecer se o usuário tem city/state no perfil
                                Log.w("SplashViewModel", "Navegando para home sem localização válida (pode causar problemas)")
                                onNavigateToHome()
                            }
                            else -> {
                                Log.w("SplashViewModel", "Timeout aguardando localização (10s), navegando para home mesmo assim")
                                // Continuar para home mesmo sem localização (pode ser corrigido depois)
                                onNavigateToHome()
                            }
                        }
                    } else {
                        // Token inválido - fazer signOut e ir para login
                        Log.w("SplashViewModel", "Token inválido, fazendo signOut e navegando para login")
                        try {
                            authRepository.signOut()
                        } catch (signOutError: Exception) {
                            Log.e("SplashViewModel", "Erro ao fazer signOut: ${signOutError.message}")
                        }
                        onNavigateToLogin()
                    }
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

