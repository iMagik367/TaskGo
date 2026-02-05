package com.taskgoapp.taskgo.core.location

/**
 * Localização operacional válida do app
 * 
 * Esta é a única fonte de verdade para localização em runtime.
 * Representa a localização atual que o app deve usar para todas as operações.
 */
data class OperationalLocation(
    val city: String,
    val state: String,
    val locationId: String,
    val source: LocationSource,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(city.isNotBlank()) { "City não pode ser vazio" }
        require(state.isNotBlank()) { "State não pode ser vazio" }
        require(locationId.isNotBlank()) { "LocationId não pode ser vazio" }
        require(locationId != "unknown") { "LocationId não pode ser 'unknown'" }
        require(locationId != "unknown_unknown") { "LocationId não pode ser 'unknown_unknown'" }
    }
}

/**
 * Fonte da localização operacional
 */
enum class LocationSource {
    /**
     * Localização obtida via GPS + Geocoder
     */
    GPS,
    
    /**
     * Localização obtida do perfil do usuário (cadastro)
     */
    PROFILE
}
