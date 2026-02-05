package com.taskgoapp.taskgo.core.location

import android.util.Log
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolvedor de localização operacional
 * 
 * LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
 * NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
 * 
 * Decide QUAL localização usar na ordem correta:
 * 1. Cache local (OperationalLocationStore)
 * 2. Cidade/estado do perfil do usuário (Firestore)
 * 3. Se tudo falhar → ERRO CONTROLADO
 * 
 * ⚠️ REGRA DE OURO: Nenhum repository decide localização
 * Tudo passa por LocationResolver → OperationalLocation
 */
@Singleton
class LocationResolver @Inject constructor(
    private val operationalLocationStore: OperationalLocationStore,
    private val locationManager: LocationManager,
    private val userRepository: UserRepository
) {
    private val TAG = "LocationResolver"
    
    /**
     * Resolve a localização operacional atual
     * 
     * LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
     * NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
     * 
     * Ordem de tentativas:
     * 1. Cache local (mais rápido)
     * 2. Perfil do usuário (Firestore) - FONTE DE VERDADE
     * 3. Erro controlado (nunca bloqueia app)
     * 
     * @return OperationalLocation válida ou null se tudo falhar
     */
    suspend fun resolve(): OperationalLocation? {
        // 1. Tentar cache local primeiro (mais rápido)
        val cachedLocation = operationalLocationStore.get()
        if (cachedLocation != null) {
            Log.d(TAG, "✅ Localização obtida do cache: ${cachedLocation.city}/${cachedLocation.state} (${cachedLocation.source})")
            return cachedLocation
        }
        
        // 2. LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
        // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa)
        try {
            Log.d(TAG, "📍 Cache não disponível, obtendo city/state do perfil do usuário...")
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
            val userState = currentUser?.state?.takeIf { it.isNotBlank() }
            
            if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                Log.e(TAG, "❌ ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                        "City: ${currentUser?.city ?: "null"}, State: ${currentUser?.state ?: "null"}")
                return null // Retornar null em vez de fallback
            }
            
            val validatedCity = LocationValidator.validateAndNormalizeCity(userCity) ?: userCity
            val validatedState = LocationValidator.validateAndNormalizeState(userState) ?: userState
            
            val locationId = try {
                LocationHelper.normalizeLocationId(validatedCity, validatedState)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao normalizar locationId: ${e.message}", e)
                throw Exception("Erro ao normalizar locationId para city=$validatedCity, state=$validatedState: ${e.message}")
            }
            
            val operationalLocation = OperationalLocation(
                city = validatedCity,
                state = validatedState,
                locationId = locationId,
                source = LocationSource.PROFILE // Mudado de GPS para PROFILE
            )
            
            // Salvar no cache para próxima vez
            operationalLocationStore.save(operationalLocation)
            
            Log.d(TAG, "✅ Localização obtida do perfil: $validatedCity/$validatedState")
            return operationalLocation
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter localização do perfil: ${e.message}", e)
        }
        
        // 4. Tudo falhou - retornar null (LocationStateManager tratará como Error)
        Log.e(TAG, "❌ Todas as tentativas falharam - localização indisponível")
        return null
    }
    
    /**
     * Força atualização da localização do perfil do usuário
     * 
     * LEI MÁXIMA DO TASKGO: NUNCA usar GPS para city/state
     * Usado quando o usuário atualiza seu perfil
     * 
     * @return OperationalLocation atualizada ou null se falhar
     */
    suspend fun refreshFromProfile(): OperationalLocation? {
        try {
            Log.d(TAG, "🔄 Forçando atualização do perfil do usuário...")
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
            val userState = currentUser?.state?.takeIf { it.isNotBlank() }
            
            if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                Log.e(TAG, "❌ ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                        "City: ${currentUser?.city ?: "null"}, State: ${currentUser?.state ?: "null"}")
                return null
            }
            
            val validatedCity = LocationValidator.validateAndNormalizeCity(userCity) ?: userCity
            val validatedState = LocationValidator.validateAndNormalizeState(userState) ?: userState
            
            val locationId = try {
                LocationHelper.normalizeLocationId(validatedCity, validatedState)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao normalizar locationId: ${e.message}", e)
                throw Exception("Erro ao normalizar locationId para city=$validatedCity, state=$validatedState: ${e.message}")
            }
            
            val operationalLocation = OperationalLocation(
                city = validatedCity,
                state = validatedState,
                locationId = locationId,
                source = LocationSource.PROFILE
            )
            
            // Salvar no cache
            operationalLocationStore.save(operationalLocation)
            
            Log.d(TAG, "✅ Localização atualizada do perfil: $validatedCity/$validatedState")
            return operationalLocation
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar localização do perfil: ${e.message}", e)
            return null
        }
    }
}
