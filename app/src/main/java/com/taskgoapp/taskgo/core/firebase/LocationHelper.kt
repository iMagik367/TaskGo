package com.taskgoapp.taskgo.core.firebase

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.flow.first

/**
 * Helper para organização de dados por localização
 * Dados públicos são salvos em coleções organizadas por cidade/estado
 * Estrutura: locations/{city}_{state}/{collection}/{documentId}
 */
object LocationHelper {
    private const val TAG = "LocationHelper"
    
    /**
     * Normaliza cidade e estado para criar ID válido para coleção
     * Remove espaços, caracteres especiais e converte para lowercase
     * Exemplo: "Osasco" + "SP" -> "osasco_sp"
     */
    fun normalizeLocationId(city: String, state: String): String {
        val normalize = { str: String ->
            java.text.Normalizer.normalize(str.lowercase().trim(), java.text.Normalizer.Form.NFD)
                .replace(Regex("[\\u0300-\\u036F]"), "") // Remove acentos
                .replace(Regex("[^a-z0-9]"), "_") // Substitui caracteres especiais por underscore
                .replace(Regex("_+"), "_") // Remove underscores duplicados
                .replace(Regex("^_|_\$"), "") // Remove underscores no início e fim
        }
        
        val normalizedCity = normalize(city)
        val normalizedState = normalize(state)
        
        val locationId = if (normalizedCity.isEmpty() && normalizedState.isEmpty()) {
            "unknown"
        } else if (normalizedCity.isEmpty()) {
            normalizedState
        } else if (normalizedState.isEmpty()) {
            normalizedCity
        } else {
            "${normalizedCity}_${normalizedState}"
        }
        
        // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de normalização (Frontend)
        Log.d("LocationTrace", """
            📍 FRONTEND LOCATION TRACE
            Function: normalizeLocationId
            RawCity: $city
            RawState: $state
            NormalizedCity: $normalizedCity
            NormalizedState: $normalizedState
            LocationId: $locationId
            Timestamp: ${java.util.Date()}
        """.trimIndent())
        
        return locationId
    }
    
    /**
     * Extrai cidade e estado de uma string de localização
     * Formatos suportados:
     * - "Cidade, Estado"
     * - "Endereço, Cidade, Estado"
     * - "Cidade"
     */
    fun parseLocation(location: String): Pair<String, String> {
        if (location.isBlank()) {
            return "" to ""
        }
        
        val parts = location.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (parts.isEmpty()) {
            return "" to ""
        }
        
        if (parts.size == 1) {
            // Apenas cidade fornecida
            return parts[0] to ""
        }
        
        // Assumir que os últimos dois elementos são cidade e estado
        val state = parts[parts.size - 1]
        val city = parts[parts.size - 2]
        
        return city to state
    }
    
    /**
     * Obtém referência da coleção por localização
     * @param firestore Instância do Firestore
     * @param collection Nome da coleção (orders, products, stories, posts)
     * @param city Cidade
     * @param state Estado
     */
    fun getLocationCollection(
        firestore: FirebaseFirestore,
        collection: String,
        city: String,
        state: String
    ): com.google.firebase.firestore.CollectionReference {
        val locationId = normalizeLocationId(city, state)
        val firestorePath = "locations/$locationId/$collection"
        
        // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de coleção (Frontend)
        Log.d("LocationTrace", """
            📍 FRONTEND LOCATION TRACE
            Function: getLocationCollection
            City: $city
            State: $state
            LocationId: $locationId
            Firestore Path: $firestorePath
            Collection: $collection
            Timestamp: ${java.util.Date()}
        """.trimIndent())
        
        return firestore.collection("locations").document(locationId).collection(collection)
    }
    
    /**
     * Obtém cidade e estado do usuário a partir do UserRepository
     * Retorna Pair(city, state)
     * CRÍTICO: UserProfile agora tem state diretamente (adicionado na versão 88)
     */
    suspend fun getUserLocation(
        userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
    ): Pair<String, String> {
        return try {
            val user = userRepository.observeCurrentUser().first()
            val city = user?.city?.takeIf { it.isNotBlank() } ?: ""
            val state = user?.state?.takeIf { it.isNotBlank() } ?: ""
            
            val locationId = normalizeLocationId(city, state)
            
            // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização do usuário (Frontend)
            Log.d("LocationTrace", """
                📍 FRONTEND LOCATION TRACE
                Function: getUserLocation
                RawCity: ${user?.city ?: "null"}
                RawState: ${user?.state ?: "null"}
                City: $city
                State: $state
                LocationId: $locationId
                Timestamp: ${java.util.Date()}
            """.trimIndent())
            
            city to state
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter localizacao do usuario: ${e.message}", e)
            // 📍 LOCATION TRACE: Erro ao obter localização
            Log.w("LocationTrace", """
                📍 FRONTEND LOCATION TRACE
                Function: getUserLocation
                Error: ${e.message}
                Timestamp: ${java.util.Date()}
            """.trimIndent())
            "" to ""
        }
    }
}
