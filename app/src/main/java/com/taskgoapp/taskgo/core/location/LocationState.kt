package com.taskgoapp.taskgo.core.location

/**
 * Estado global de localização do usuário
 * Fonte única de verdade para city, state e locationId
 * 
 * ✅ Ready: Localização está pronta e válida
 * ⏳ Loading: Localização ainda não está disponível
 * ❌ Error: Erro ao obter localização
 */
sealed class LocationState {
    /**
     * Localização ainda não está disponível
     * Nenhuma query Firestore por localização deve ocorrer
     */
    object Loading : LocationState()
    
    /**
     * Localização está pronta e válida
     * Todos os valores (city, state, locationId) estão resolvidos
     */
    data class Ready(
        val city: String,
        val state: String,
        val locationId: String
    ) : LocationState() {
        init {
            require(city.isNotBlank()) { "City cannot be blank in LocationState.Ready" }
            require(state.isNotBlank()) { "State cannot be blank in LocationState.Ready" }
            require(locationId.isNotBlank() && locationId != "unknown") { 
                "LocationId cannot be blank or 'unknown' in LocationState.Ready" 
            }
        }
    }
    
    /**
     * Erro ao obter localização
     * Nenhuma query Firestore por localização deve ocorrer
     */
    data class Error(val reason: String) : LocationState()
}
