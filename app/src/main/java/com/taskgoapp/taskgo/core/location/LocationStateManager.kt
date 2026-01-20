package com.taskgoapp.taskgo.core.location

import android.util.Log
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de estado global de localização
 * Singleton responsável por:
 * - Observar usuário logado
 * - Extrair city/state
 * - Normalizar locationId
 * - Emitir LocationState.Ready SOMENTE quando os 3 valores estiverem válidos
 * 
 * ⚠️ FONTE ÚNICA DE VERDADE DA LOCALIZAÇÃO NO FRONTEND
 */
@Singleton
class LocationStateManager @Inject constructor(
    private val userRepository: UserRepository
) {
    private val TAG = "LocationStateManager"
    
    /**
     * Estado atual de localização
     * Observe este Flow para saber quando a localização está pronta
     */
    val locationState: Flow<LocationState> = userRepository.observeCurrentUser()
        .map { user ->
            when {
                user == null -> {
                    Log.w(TAG, "📍 User is null, location state: Loading")
                    LocationState.Loading
                }
                else -> {
                    val city = user.city?.takeIf { it.isNotBlank() } ?: ""
                    val state = user.state?.takeIf { it.isNotBlank() } ?: ""
                    
                    when {
                        city.isBlank() || state.isBlank() -> {
                            Log.w(TAG, """
                                📍 Location not ready:
                                UserId: ${user.id}
                                City: ${user.city ?: "null"}
                                State: ${user.state ?: "null"}
                            """.trimIndent())
                            LocationState.Loading
                        }
                        else -> {
                            val locationId = LocationHelper.normalizeLocationId(city, state)
                            
                            // 🚨 PROTEÇÃO: Nunca permitir "unknown" como locationId válido
                            if (locationId == "unknown" || locationId.isBlank()) {
                                Log.e(TAG, """
                                    📍 FATAL_LOCATION: Attempted to create LocationState.Ready with invalid locationId
                                    City: $city
                                    State: $state
                                    LocationId: $locationId
                                """.trimIndent())
                                LocationState.Error("Invalid locationId: $locationId")
                            } else {
                                Log.d(TAG, """
                                    📍 Location ready:
                                    City: $city
                                    State: $state
                                    LocationId: $locationId
                                """.trimIndent())
                                LocationState.Ready(
                                    city = city,
                                    state = state,
                                    locationId = locationId
                                )
                            }
                        }
                    }
                }
            }
        }
        .catch { exception ->
            Log.e(TAG, "📍 Error observing user location: ${exception.message}", exception)
            emit(LocationState.Error("Failed to get user location: ${exception.message}"))
        }
        .distinctUntilChanged()
        .shareIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000L),
            replay = 1
        )
    
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
}
