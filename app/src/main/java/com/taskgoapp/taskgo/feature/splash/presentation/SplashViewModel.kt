package com.taskgoapp.taskgo.feature.splash.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authRepository: FirebaseAuthRepository,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager
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
                    // Verificar se o token do usuário ainda é válido fazendo um refresh
                    // Se o token estiver expirado, getCurrentUser() pode retornar null ou um usuário inválido
                    try {
                        // Fazer refresh do token para garantir que está válido
                        currentUser.getIdToken(true).await()
                        
                        // Se estiver logado e o token é válido, verificar se precisa fazer sync inicial
                        val needsSync = !preferencesManager.isInitialSyncCompleted(currentUser.uid)
                        if (needsSync) {
                            Log.d("SplashViewModel", "Primeiro acesso do usuário, iniciando sincronização inicial...")
                            try {
                                initialDataSyncManager.syncAllUserData()
                                preferencesManager.setInitialSyncCompleted(currentUser.uid)
                                Log.d("SplashViewModel", "Sincronização inicial concluída")
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao sincronizar dados iniciais: ${e.message}", e)
                                // Continuar mesmo se o sync falhar
                            }
                        }
                        
                        // Ir para home
                        Log.d("SplashViewModel", "Usuário logado e token válido, navegando para home")
                        onNavigateToHome()
                    } catch (e: Exception) {
                        // Se o refresh do token falhar, o usuário não está realmente autenticado
                        Log.w("SplashViewModel", "Token do usuário inválido ou expirado: ${e.message}")
                        Log.d("SplashViewModel", "Navegando para login")
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

