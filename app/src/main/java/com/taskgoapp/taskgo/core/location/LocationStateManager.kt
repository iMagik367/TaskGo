package com.taskgoapp.taskgo.core.location

import android.util.Log
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de estado global de localização
 * 
 * Singleton responsável por:
 * - Gerenciar o estado de localização operacional
 * - Usar LocationResolver para obter localização
 * - Emitir LocationState.Ready com OperationalLocation
 * - Atualizar localização em background quando GPS resolver
 * 
 * ⚠️ FONTE ÚNICA DE VERDADE DA LOCALIZAÇÃO NO FRONTEND
 * 
 * FLUXO DE ESTADOS:
 * 1. Loading (inicial)
 * 2. Tenta cache → Ready(cache) se existir
 * 3. Em paralelo: tenta GPS → atualiza cache → Ready(GPS)
 * 4. Se GPS falhar: tenta perfil → cria cache → Ready(PROFILE)
 * 5. Se tudo falhar: Error (não bloqueia app)
 */
@Singleton
class LocationStateManager @Inject constructor(
    private val userRepository: UserRepository,
    private val locationResolver: LocationResolver,
    private val operationalLocationStore: OperationalLocationStore
) {
    private val TAG = "LocationStateManager"
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Estado atual de localização
     * Observe este Flow para saber quando a localização está pronta
     */
    val locationState: Flow<LocationState> = userRepository.observeCurrentUser()
        .flatMapLatest { user ->
            when {
                user == null -> {
                    Log.w(TAG, "📍 User is null, location state: Loading")
                    flowOf(LocationState.Loading)
                }
                else -> {
                    // Tentar resolver localização
                    resolveLocationFlow()
                }
            }
        }
        .catch { exception ->
            Log.e(TAG, "📍 Error observing user location: ${exception.message}", exception)
            emit(LocationState.Error("Failed to get user location: ${exception.message}"))
        }
        .distinctUntilChanged()
        .shareIn(
            scope = backgroundScope,
            started = SharingStarted.WhileSubscribed(5000L),
            replay = 1
        )
    
    /**
     * Resolve localização e emite estados
     * CRÍTICO: Sempre tenta resolver do perfil do usuário primeiro
     */
    private fun resolveLocationFlow(): Flow<LocationState> = flow {
        // 1. Emitir Loading primeiro
        emit(LocationState.Loading)
        
        // 2. Tentar carregar cache (mais rápido)
        val cachedLocation = operationalLocationStore.get()
        if (cachedLocation != null) {
            Log.d(TAG, """
                ✅ Localização obtida do cache:
                City: ${cachedLocation.city}
                State: ${cachedLocation.state}
                LocationId: ${cachedLocation.locationId}
                Source: ${cachedLocation.source}
            """.trimIndent())
            emit(LocationState.Ready(cachedLocation))
        }
        
        // 3. CRÍTICO: Tentar resolver localização do perfil do usuário
        // Se o usuário tem city/state no perfil, SEMPRE deve conseguir resolver
        try {
            val resolvedLocation = locationResolver.resolve()
            if (resolvedLocation != null) {
                // Se cache não existia ou locationId mudou, atualizar
                if (cachedLocation == null || resolvedLocation.locationId != cachedLocation.locationId) {
                    Log.d(TAG, """
                        ✅ Localização resolvida:
                        City: ${resolvedLocation.city}
                        State: ${resolvedLocation.state}
                        LocationId: ${resolvedLocation.locationId}
                        Source: ${resolvedLocation.source}
                    """.trimIndent())
                    emit(LocationState.Ready(resolvedLocation))
                }
            } else {
                // Se tudo falhou e não temos cache, tentar obter do perfil diretamente
                if (cachedLocation == null) {
                    Log.w(TAG, "⚠️ LocationResolver retornou null, tentando obter do perfil diretamente...")
                    // Última tentativa: obter do perfil diretamente
                    val currentUser = userRepository.observeCurrentUser().first()
                    val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
                    val userState = currentUser?.state?.takeIf { it.isNotBlank() }
                    
                    if (!userCity.isNullOrBlank() && !userState.isNullOrBlank()) {
                        try {
                            val locationId = com.taskgoapp.taskgo.core.firebase.LocationHelper.normalizeLocationId(userCity, userState)
                            val fallbackLocation = OperationalLocation(
                                city = userCity,
                                state = userState,
                                locationId = locationId,
                                source = LocationSource.PROFILE
                            )
                            operationalLocationStore.save(fallbackLocation)
                            Log.d(TAG, """
                                ✅ Localização obtida do perfil (fallback):
                                City: ${fallbackLocation.city}
                                State: ${fallbackLocation.state}
                                LocationId: ${fallbackLocation.locationId}
                            """.trimIndent())
                            emit(LocationState.Ready(fallbackLocation))
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Erro ao criar OperationalLocation do perfil: ${e.message}", e)
                            emit(LocationState.Error("Localização indisponível: ${e.message}"))
                        }
                    } else {
                        Log.e(TAG, "❌ Não foi possível resolver localização: usuário não tem city/state no perfil")
                        emit(LocationState.Error("Localização indisponível: usuário não tem city/state no perfil"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao resolver localização: ${e.message}", e)
            // Se temos cache, usar ele mesmo com erro
            if (cachedLocation != null) {
                emit(LocationState.Ready(cachedLocation))
            } else {
                // Última tentativa: obter do perfil diretamente
                try {
                    val currentUser = userRepository.observeCurrentUser().first()
                    val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
                    val userState = currentUser?.state?.takeIf { it.isNotBlank() }
                    
                    if (!userCity.isNullOrBlank() && !userState.isNullOrBlank()) {
                        val locationId = com.taskgoapp.taskgo.core.firebase.LocationHelper.normalizeLocationId(userCity, userState)
                        val fallbackLocation = OperationalLocation(
                            city = userCity,
                            state = userState,
                            locationId = locationId,
                            source = LocationSource.PROFILE
                        )
                        operationalLocationStore.save(fallbackLocation)
                        Log.d(TAG, "✅ Localização obtida do perfil (fallback após erro): ${userCity}/${userState}")
                        emit(LocationState.Ready(fallbackLocation))
                    } else {
                        emit(LocationState.Error("Erro ao obter localização: ${e.message}"))
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Erro ao obter localização do perfil (fallback): ${e2.message}", e2)
                    emit(LocationState.Error("Erro ao obter localização: ${e.message}"))
                }
            }
        }
    }
    
    /**
     * Obtém o estado atual de localização (one-shot)
     */
    suspend fun getCurrentLocationState(): LocationState {
        return locationState.first()
    }
    
    /**
     * Verifica se a localização está pronta
     */
    suspend fun isLocationReady(): Boolean {
        return getCurrentLocationState() is LocationState.Ready
    }
    
    /**
     * Força atualização da localização via GPS
     * 
     * Usado quando o usuário muda de cidade ou quando precisamos atualizar
     */
    suspend fun refreshLocation() {
        try {
            val updatedLocation = locationResolver.refreshFromProfile()
            if (updatedLocation != null) {
                Log.d(TAG, "✅ Localização atualizada: ${updatedLocation.city}/${updatedLocation.state}")
            } else {
                Log.w(TAG, "⚠️ Não foi possível atualizar localização via GPS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar localização: ${e.message}", e)
        }
    }
}
