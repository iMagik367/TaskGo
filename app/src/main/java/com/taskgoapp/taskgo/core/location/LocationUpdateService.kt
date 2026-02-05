package com.taskgoapp.taskgo.core.location

import android.util.Log
import com.taskgoapp.taskgo.core.model.UserProfile
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.taskgoapp.taskgo.core.location.LocationValidator

/**
 * ⚠️ DESABILITADO: Este serviço NÃO deve mais atualizar city/state via GPS
 * 
 * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
 * NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
 * 
 * Este serviço foi desabilitado porque usava GPS para atualizar city/state do perfil.
 * City/state agora vêm APENAS do cadastro do usuário no Firestore.
 * 
 * @deprecated Este serviço não deve mais ser usado para atualizar city/state
 */
@Singleton
class LocationUpdateService @Inject constructor(
    private val locationManager: LocationManager,
    private val userRepository: UserRepository
) {
    private val TAG = "LocationUpdateService"
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null
    
    // Cache da última localização atualizada para evitar atualizações duplicadas
    private var lastUpdatedCity: String? = null
    private var lastUpdatedState: String? = null
    
    /**
     * ⚠️ DESABILITADO: Este método não faz mais nada
     * 
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar GPS para city/state
     */
    fun startLocationMonitoring() {
        Log.d(TAG, "⚠️ LocationUpdateService.startLocationMonitoring() foi desabilitado - city/state vêm apenas do perfil")
        // Não fazer nada - city/state vêm apenas do perfil do Firestore
        return
        
        // ✅ DESABILITADO: Este serviço não deve mais monitorar GPS para atualizar city/state
        // LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
        // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
        Log.d(TAG, "⚠️ LocationUpdateService.startLocationMonitoring() foi desabilitado - city/state vêm apenas do perfil")
        // Não fazer nada - city/state vêm apenas do perfil do Firestore
    }
    
    /**
     * Para o monitoramento de localização
     * Deve ser chamado quando o app fecha ou quando o usuário faz logout
     */
    fun stopLocationMonitoring() {
        updateJob?.cancel()
        updateJob = null
        lastUpdatedCity = null
        lastUpdatedState = null
        Log.d(TAG, "📍 Monitoramento de localização parado")
    }
    
    /**
     * ⚠️ DESABILITADO: Este método não deve mais atualizar city/state via GPS
     * 
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
     */
    private suspend fun updateUserLocation(city: String, state: String) {
        Log.d(TAG, "⚠️ LocationUpdateService.updateUserLocation() foi desabilitado - city/state vêm apenas do perfil")
        // Não fazer nada - city/state vêm apenas do perfil do Firestore
        return
        try {
            // CRÍTICO: Validar city e state ANTES de salvar
            val validatedCity = LocationValidator.validateAndNormalizeCity(city)
            val validatedState = LocationValidator.validateAndNormalizeState(state)
            
            if (validatedCity == null || validatedState == null) {
                Log.e(TAG, """
                    ❌ FALHA CRÍTICA: Tentativa de salvar localização inválida bloqueada:
                    City: '$city' -> $validatedCity
                    State: '$state' -> $validatedState
                """.trimIndent())
                return
            }
            
            // Obter usuário atual
            val currentUser = userRepository.observeCurrentUser().first()
            
            if (currentUser == null) {
                Log.w(TAG, "📍 Usuário não encontrado, não é possível atualizar localização")
                return
            }
            
            // Verificar se a localização realmente mudou
            val currentCity = currentUser.city ?: ""
            val currentState = currentUser.state ?: ""
            
            if (currentCity.equals(validatedCity, ignoreCase = true) && 
                currentState.equals(validatedState, ignoreCase = true)) {
                Log.d(TAG, "📍 Localização já está atualizada: $validatedCity, $validatedState")
                lastUpdatedCity = validatedCity
                lastUpdatedState = validatedState
                return
            }
            
            Log.d(TAG, """
                📍 Atualizando localização do usuário:
                Usuário: ${currentUser.id}
                Antiga: $currentCity, $currentState
                Nova: $validatedCity, $validatedState
            """.trimIndent())
            
            // Atualizar perfil com nova localização VALIDADA
            val updatedUser = currentUser.copy(
                city = validatedCity,
                state = validatedState
            )
            
            try {
                userRepository.updateUser(updatedUser)
                // Atualizar cache apenas após sucesso
                lastUpdatedCity = validatedCity
                lastUpdatedState = validatedState
                Log.d(TAG, "✅ Localização do usuário atualizada com sucesso: $validatedCity, $validatedState")
            } catch (error: Exception) {
                Log.e(TAG, "❌ Erro ao salvar localização no Firestore: ${error.message}", error)
                // Não atualizar cache em caso de erro
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar localização do usuário: ${e.message}", e)
        }
    }
    
    /**
     * Força uma atualização imediata da localização
     * Útil quando o app inicia ou quando o usuário solicita manualmente
     * CRÍTICO: Esta função tenta múltiplas vezes até obter uma localização válida
     */
    /**
     * ⚠️ DESABILITADO: Este método não faz mais nada
     * 
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar GPS para city/state
     */
    suspend fun updateLocationNow(): Boolean {
        Log.d(TAG, "⚠️ LocationUpdateService.updateLocationNow() foi desabilitado - city/state vêm apenas do perfil")
        // Não fazer nada - city/state vêm apenas do perfil do Firestore
        return false
    }
    
    /**
     * Força atualização imediata e aguarda até que a localização seja atualizada
     * CRÍTICO: Esta função bloqueia até que city e state sejam preenchidos
     */
    /**
     * ⚠️ DESABILITADO: Este método verifica apenas o perfil do Firestore
     * 
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar GPS para city/state
     */
    suspend fun updateLocationAndWait(timeoutMillis: Long = 30000): Boolean {
        return try {
            // Verificar se já tem localização válida no perfil
            val currentUser = userRepository.observeCurrentUser().first()
            val currentCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val currentState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (currentCity.isNotBlank() && currentState.isNotBlank()) {
                Log.d(TAG, "✅ Localização já está disponível no perfil: $currentCity, $currentState")
                lastUpdatedCity = currentCity
                lastUpdatedState = currentState
                return true
            }
            
            // Aguardar até que o perfil seja atualizado (não via GPS, mas pelo usuário)
            Log.d(TAG, "📍 Localização não disponível no perfil, aguardando atualização...")
            val startTime = System.currentTimeMillis()
            var locationFound = false
            
            userRepository.observeCurrentUser()
                .drop(1) // Pular o primeiro valor (já verificamos acima)
                .takeWhile { 
                    val elapsed = System.currentTimeMillis() - startTime
                    val city = it?.city?.takeIf { it.isNotBlank() } ?: ""
                    val state = it?.state?.takeIf { it.isNotBlank() } ?: ""
                    val hasLocation = city.isNotBlank() && state.isNotBlank()
                    
                    if (hasLocation) {
                        locationFound = true
                        Log.d(TAG, "✅ Localização obtida do perfil após espera: $city, $state")
                        lastUpdatedCity = city
                        lastUpdatedState = state
                        false // Parar o takeWhile
                    } else if (elapsed < timeoutMillis) {
                        Log.d(TAG, "📍 Aguardando localização no perfil... (${elapsed}ms/${timeoutMillis}ms)")
                        true
                    } else {
                        false
                    }
                }
                .collect { 
                    // Apenas coletar, a lógica está no takeWhile
                }
            
            if (locationFound) {
                return true
            }
            
            // Verificar novamente após o timeout
            val finalUser = userRepository.observeCurrentUser().first()
            val finalCity = finalUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val finalState = finalUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (finalCity.isNotBlank() && finalState.isNotBlank()) {
                Log.d(TAG, "✅ Localização obtida do perfil após timeout: $finalCity, $finalState")
                lastUpdatedCity = finalCity
                lastUpdatedState = finalState
                return true
            }
            
            Log.e(TAG, "❌ Timeout aguardando localização no perfil")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aguardar localização do perfil: ${e.message}", e)
            false
        }
    }
}
