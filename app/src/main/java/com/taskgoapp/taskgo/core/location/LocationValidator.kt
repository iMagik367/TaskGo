package com.taskgoapp.taskgo.core.location

import android.location.Address
import android.location.Location
import android.util.Log

/**
 * Validador robusto de localização
 * Garante que city e state sejam sempre válidos antes de salvar
 */
object LocationValidator {
    private const val TAG = "LocationValidator"
    
    // Estados válidos do Brasil (siglas de 2 caracteres)
    private val VALID_BRAZILIAN_STATES = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
        "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
        "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    )
    
    /**
     * Valida se uma localização GPS tem qualidade suficiente
     */
    fun isValidLocationQuality(location: Location?): Boolean {
        if (location == null) {
            Log.w(TAG, "📍 Localização GPS é null")
            return false
        }
        
        // Verificar se tem coordenadas válidas
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.w(TAG, "📍 Coordenadas GPS são (0,0) - inválidas")
            return false
        }
        
        // Verificar se está dentro dos limites do Brasil (com margem de erro)
        // Brasil: aproximadamente -35 a 5 de latitude, -75 a -30 de longitude
        // Adicionar margem de 2 graus para evitar falsas rejeições
        if (location.latitude < -37.0 || location.latitude > 7.0 ||
            location.longitude < -77.0 || location.longitude > -28.0) {
            Log.w(TAG, "📍 Coordenadas GPS fora dos limites do Brasil: (${location.latitude}, ${location.longitude})")
            // Não rejeitar imediatamente - pode ser um erro de GPS temporário
            // Aceitar se a precisão for boa
            if (location.hasAccuracy() && location.accuracy < 100) {
                Log.d(TAG, "📍 Aceitando GPS fora dos limites devido à boa precisão: ${location.accuracy}m")
                return true
            }
            return false
        }
        
        // Verificar precisão (se disponível)
        if (location.hasAccuracy() && location.accuracy > 1000) {
            Log.w(TAG, "📍 Precisão GPS muito baixa: ${location.accuracy}m")
            // Não rejeitar, mas avisar
        }
        
        return true
    }
    
    /**
     * Valida e normaliza city
     */
    fun validateAndNormalizeCity(city: String?): String? {
        if (city.isNullOrBlank()) {
            Log.w(TAG, "📍 City é null ou vazio")
            return null
        }
        
        val normalized = city.trim()
        
        // Verificar tamanho mínimo
        if (normalized.length < 2) {
            Log.w(TAG, "📍 City muito curto: '$normalized'")
            return null
        }
        
        // Verificar se não é um valor genérico/inválido
        val invalidValues = setOf(
            "unknown", "desconhecido", "null", "undefined", "n/a", "na",
            "cidade", "city", "local", "location", "endereço", "address"
        )
        
        if (invalidValues.contains(normalized.lowercase())) {
            Log.w(TAG, "📍 City é um valor genérico/inválido: '$normalized'")
            return null
        }
        
        // Verificar se contém apenas caracteres válidos (letras, espaços, hífens, acentos)
        if (!normalized.matches(Regex("^[a-zA-ZÀ-ÿ\\s\\-']+$"))) {
            Log.w(TAG, "📍 City contém caracteres inválidos: '$normalized'")
            return null
        }
        
        return normalized
    }
    
    /**
     * Valida e normaliza state (deve ser sigla de 2 caracteres)
     */
    fun validateAndNormalizeState(state: String?): String? {
        if (state.isNullOrBlank()) {
            Log.w(TAG, "📍 State é null ou vazio")
            return null
        }
        
        val normalized = state.trim().uppercase()
        
        // Verificar se tem exatamente 2 caracteres
        if (normalized.length != 2) {
            Log.w(TAG, "📍 State não tem 2 caracteres: '$normalized' (${normalized.length} caracteres)")
            return null
        }
        
        // Verificar se é uma sigla válida do Brasil
        if (!VALID_BRAZILIAN_STATES.contains(normalized)) {
            Log.w(TAG, "📍 State não é uma sigla válida do Brasil: '$normalized'")
            return null
        }
        
        return normalized
    }
    
    /**
     * Valida Address completo do Geocoder
     * 
     * ⚠️ ATENÇÃO: Este método é usado APENAS para validação de Address obtido via geocoding reverso
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar este método para obter city/state do usuário - apenas para validar Address de geocoding
     * 
     * @deprecated Este método não deve ser usado para obter city/state do usuário
     */
    fun validateAddress(address: Address?): Pair<String?, String?> {
        if (address == null) {
            Log.w(TAG, "📍 Address do Geocoder é null")
            return null to null
        }
        
        // Extrair city e state
        val rawCity = address.locality
        val rawState = address.adminArea
        
        Log.d(TAG, """
            📍 Validando Address do Geocoder:
            Locality (raw): $rawCity
            AdminArea (raw): $rawState
            CountryCode: ${address.countryCode}
            CountryName: ${address.countryName}
            FeatureName: ${address.featureName}
            SubAdminArea: ${address.subAdminArea}
        """.trimIndent())
        
        // Verificar se é do Brasil
        val countryCode = address.countryCode?.uppercase() ?: ""
        val countryName = address.countryName?.uppercase() ?: ""
        
        if (countryCode != "BR" && !countryName.contains("BRASIL", ignoreCase = true)) {
            Log.w(TAG, "📍 Address não é do Brasil: countryCode=$countryCode, countryName=$countryName")
            // Continuar mesmo assim, pode ser um erro do Geocoder
        }
        
        // Tentar obter city de diferentes campos se locality estiver vazio
        var city = rawCity
        if (city.isNullOrBlank()) {
            city = address.subLocality ?: address.featureName
            Log.d(TAG, "📍 Usando subLocality ou featureName como city: $city")
        }
        
        // Tentar obter state de diferentes campos se adminArea estiver vazio
        var state = rawState
        if (state.isNullOrBlank()) {
            state = address.subAdminArea
            Log.d(TAG, "📍 Usando subAdminArea como state: $state")
        }
        
        // Validar e normalizar
        val validatedCity = validateAndNormalizeCity(city)
        val validatedState = validateAndNormalizeState(state)
        
        if (validatedCity == null || validatedState == null) {
            Log.e(TAG, """
                ❌ Validação de Address falhou:
                City: '$city' -> $validatedCity
                State: '$state' -> $validatedState
            """.trimIndent())
            return null to null
        }
        
        Log.d(TAG, """
            ✅ Address validado com sucesso:
            City: '$validatedCity'
            State: '$validatedState'
        """.trimIndent())
        
        return validatedCity to validatedState
    }
    
    /**
     * Valida se city e state são válidos juntos
     */
    fun validateCityAndState(city: String?, state: String?): Boolean {
        val validatedCity = validateAndNormalizeCity(city)
        val validatedState = validateAndNormalizeState(state)
        
        val isValid = validatedCity != null && validatedState != null
        
        if (!isValid) {
            Log.e(TAG, """
                ❌ Validação de city e state falhou:
                City: '$city' -> $validatedCity
                State: '$state' -> $validatedState
            """.trimIndent())
        }
        
        return isValid
    }
}
