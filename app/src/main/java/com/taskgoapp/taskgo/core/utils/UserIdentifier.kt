package com.taskgoapp.taskgo.core.utils

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import java.security.MessageDigest
import kotlin.math.round

/**
 * Sistema de ID único por usuário baseado em:
 * - Modo de conta (role)
 * - Geolocalização (cidade/estado ou latitude/longitude)
 * - Categorias de serviços (preferredCategories)
 * 
 * Este ID é usado para indexação e busca eficiente no Firestore,
 * permitindo filtrar usuários por localização, tipo de conta e categorias.
 */
object UserIdentifier {
    
    /**
     * Gera um ID único para o usuário baseado em seus atributos
     * @param user Usuário do Firestore
     * @return ID único calculado
     */
    fun generateUserId(user: UserFirestore): String {
        val components = mutableListOf<String>()
        
        // 1. Modo de conta (role)
        components.add("role:${user.role}")
        
        // 2. Geolocalização (cidade/estado ou coordenadas)
        val locationId = generateLocationId(user)
        if (locationId.isNotEmpty()) {
            components.add("loc:$locationId")
        }
        
        // 3. Categorias de serviços (apenas para parceiros)
        if (user.role == "partner" || user.role == "provider" || user.role == "seller") {
            val categoriesId = generateCategoriesId(user.preferredCategories)
            if (categoriesId.isNotEmpty()) {
                components.add("cats:$categoriesId")
            }
        }
        
        // Gerar hash MD5 do ID composto para garantir unicidade e tamanho fixo
        val compositeId = components.joinToString("|")
        return generateHash(compositeId)
    }
    
    /**
     * Gera ID de localização baseado em cidade/estado ou coordenadas
     */
    private fun generateLocationId(user: UserFirestore): String {
        // Priorizar cidade/estado do endereço
        val address = user.address
        if (address != null) {
            val city = address.city?.trim()?.lowercase() ?: ""
            val state = address.state?.trim()?.lowercase() ?: ""
            if (city.isNotEmpty() && state.isNotEmpty()) {
                return "${city}_${state}"
            }
        }
        
        // Se não tiver endereço, retornar vazio (não usar coordenadas para ID de localização)
        // Coordenadas são muito específicas e mudam frequentemente
        return ""
    }
    
    /**
     * Gera ID de categorias ordenadas e normalizadas
     */
    private fun generateCategoriesId(categories: List<String>?): String {
        if (categories.isNullOrEmpty()) {
            return ""
        }
        
        // Ordenar e normalizar categorias
        val normalized = categories
            .map { it.trim().lowercase() }
            .sorted()
            .joinToString(",")
        
        return generateHash(normalized).take(8) // Usar apenas 8 caracteres do hash
    }
    
    /**
     * Gera hash MD5 de uma string
     */
    private fun generateHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Gera ID de localização baseado em coordenadas GPS (para busca por raio)
     * Usa geohash aproximado para agrupar coordenadas próximas
     */
    fun generateGeohashId(latitude: Double, longitude: Double, precision: Int = 5): String {
        // Geohash simplificado: arredondar coordenadas para agrupar áreas próximas
        val latRounded = round(latitude * 100.0) / 100.0 // Precisão de ~1km
        val lngRounded = round(longitude * 100.0) / 100.0
        
        return "${latRounded}_${lngRounded}"
    }
    
    /**
     * Gera ID composto para busca eficiente de usuários por:
     * - Localização (geohash)
     * - Role
     * - Categorias
     */
    fun generateSearchId(
        role: String,
        latitude: Double?,
        longitude: Double?,
        city: String?,
        state: String?,
        categories: List<String>?
    ): String {
        val components = mutableListOf<String>()
        
        components.add("role:$role")
        
        // Priorizar cidade/estado, senão usar geohash
        if (city != null && state != null && city.isNotEmpty() && state.isNotEmpty()) {
            components.add("loc:${city.lowercase()}_${state.lowercase()}")
        } else if (latitude != null && longitude != null) {
            components.add("geo:${generateGeohashId(latitude, longitude)}")
        }
        
        if (role == "partner" || role == "provider" || role == "seller") {
            val categoriesId = generateCategoriesId(categories)
            if (categoriesId.isNotEmpty()) {
                components.add("cats:$categoriesId")
            }
        }
        
        return components.joinToString("|")
    }
    
    /**
     * Extrai componentes do ID de busca para facilitar queries
     */
    data class SearchIdComponents(
        val role: String?,
        val location: String?,
        val geohash: String?,
        val categories: List<String>?
    )
    
    fun parseSearchId(searchId: String): SearchIdComponents {
        val parts = searchId.split("|")
        var role: String? = null
        var location: String? = null
        var geohash: String? = null
        var categories: List<String>? = null
        
        parts.forEach { part ->
            when {
                part.startsWith("role:") -> role = part.substringAfter("role:")
                part.startsWith("loc:") -> location = part.substringAfter("loc:")
                part.startsWith("geo:") -> geohash = part.substringAfter("geo:")
                part.startsWith("cats:") -> {
                    // Categories hash não pode ser revertido, mas podemos usar para busca
                    // Em uma implementação futura, podemos manter um índice reverso
                }
            }
        }
        
        return SearchIdComponents(role, location, geohash, categories)
    }
}
