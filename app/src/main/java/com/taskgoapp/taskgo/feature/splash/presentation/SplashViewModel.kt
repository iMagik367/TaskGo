package com.taskgoapp.taskgo.feature.splash.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firebaseAuth: FirebaseAuth,
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
                // Verificar se há usuário logado
                val currentUser = firebaseAuth.currentUser
                
                Log.d("SplashViewModel", "currentUser: ${currentUser?.email}")
                
                if (currentUser != null) {
                    // Se estiver logado, verificar se precisa fazer sync inicial
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
                    Log.d("SplashViewModel", "Usuário logado, navegando para home")
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

