package com.taskgoapp.taskgo.core.location

/**
 * Estado global de localização do usuário
 * Fonte única de verdade para localização operacional
 * 
 * ✅ Ready: Localização está pronta e válida (contém OperationalLocation)
 * ⏳ Loading: Localização ainda não está disponível
 * ❌ Error: Erro ao obter localização (não bloqueia o app)
 */
sealed class LocationState {
    /**
     * Localização ainda não está disponível
     * O app pode iniciar em Loading e rapidamente mudar para Ready
     */
    object Loading : LocationState()
    
    /**
     * Localização está pronta e válida
     * Contém OperationalLocation que é a fonte única de verdade
     */
    data class Ready(
        val location: OperationalLocation
    ) : LocationState() {
        init {
            require(location.locationId.isNotBlank()) { 
                "LocationId cannot be blank in LocationState.Ready" 
            }
            require(location.locationId != "unknown") { 
                "LocationId cannot be 'unknown' in LocationState.Ready" 
            }
            require(location.locationId != "unknown_unknown") { 
                "LocationId cannot be 'unknown_unknown' in LocationState.Ready" 
            }
        }
    }
    
    /**
     * Erro ao obter localização
     * NÃO bloqueia o app - apenas informa que houve problema
     */
    data class Error(val reason: String) : LocationState()
}
