package com.taskgoapp.taskgo.feature.splash.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
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
                    
                    // CRÍTICO: Só navegar para home se o token for válido
                    if (tokenValid) {
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
                            
                            // ✅ LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
                            // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
                            Log.d("SplashViewModel", "✅ Usuário autenticado - city/state vêm do perfil do Firestore")
                            
                            // Localização válida - navegar para home
                            Log.d("SplashViewModel", "Usuário autenticado e localização válida, navegando para home")
                            onNavigateToHome()
                        } else {
                            Log.w("SplashViewModel", "Usuário não tem city/state no perfil. City: ${userCity ?: "null"}, State: ${userState ?: "null"}")
                            // Continuar para home mesmo assim - o usuário pode completar o cadastro depois
                            Log.w("SplashViewModel", "Navegando para home sem city/state válido (usuário deve completar cadastro)")
                            onNavigateToHome()
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

