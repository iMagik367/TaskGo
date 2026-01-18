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
        
        if (normalizedCity.isEmpty() && normalizedState.isEmpty()) {
            return "unknown"
        }
        
        if (normalizedCity.isEmpty()) {
            return normalizedState
        }
        
        if (normalizedState.isEmpty()) {
            return normalizedCity
        }
        
        return "${normalizedCity}_${normalizedState}"
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
        Log.d(TAG, "🔵 Acessando coleção por localização: locations/$locationId/$collection (city=$city, state=$state)")
        return firestore.collection("locations").document(locationId).collection(collection)
    }
    
    /**
     * Obtém cidade e estado do usuário a partir do UserRepository
     * Retorna Pair(city, state)
     * CRÍTICO: UserProfile não tem state diretamente, precisa acessar via UserFirestore.address.state
     */
    suspend fun getUserLocation(
        userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
    ): Pair<String, String> {
        return try {
            val user = userRepository.observeCurrentUser().first()
            val city = user?.city ?: ""
            // CRÍTICO: UserProfile não tem state, retornar vazio (será obtido via FirestoreUserRepository quando necessário)
            val state = ""
            Log.d(TAG, "📍 Localização do usuário obtida: city=$city, state=$state (state obtido separadamente via FirestoreUserRepository)")
            city to state
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter localização do usuário: ${e.message}", e)
            "" to ""
        }
    }
}
