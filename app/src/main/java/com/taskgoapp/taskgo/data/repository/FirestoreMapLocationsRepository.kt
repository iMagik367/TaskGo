package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.maps.ProviderLocation
import com.taskgoapp.taskgo.core.maps.StoreLocation
import com.taskgoapp.taskgo.core.location.GeocodingService
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para buscar prestadores e lojas em tempo real para exibição no mapa
 */
@Singleton
class FirestoreMapLocationsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val geocodingService: GeocodingService
) {
    private val usersCollection = firestore.collection("users")
    
    // Cache de geocoding para evitar múltiplas chamadas à API
    private val geocodingCache = mutableMapOf<String, Pair<Double, Double>>()
    
    /**
     * Observa prestadores de serviço com localização em tempo real
     * Usa cache de geocoding e processamento assíncrono para otimizar performance
     */
    fun observeProvidersWithLocation(): Flow<List<ProviderLocation>> = callbackFlow {
        val listenerRegistration = usersCollection
            .whereEqualTo("role", "partner")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreMapLocationsRepository", "Erro ao observar providers: ${error.message}", error)
                    // Não fecha o flow, apenas envia lista vazia para evitar crash
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Processar geocoding de forma assíncrona para não bloquear o listener
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val providers = coroutineScope {
                        snapshot?.documents?.mapNotNull { doc ->
                            async {
                                val user = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                                user?.let { provider ->
                                    // Obter localização via geocoding do endereço (com cache)
                                    val latitude: Double?
                                    val longitude: Double?
                                    
                                    if (provider.address != null) {
                                        // Criar chave de cache baseada no CEP ou endereço completo
                                        val cacheKey = provider.address.zipCode.ifEmpty { 
                                            "${provider.address.street}, ${provider.address.city}, ${provider.address.state}"
                                        }
                                        
                                        // Verificar cache primeiro
                                        val cached = geocodingCache[cacheKey]
                                        if (cached != null) {
                                            latitude = cached.first
                                            longitude = cached.second
                                        } else {
                                            // Fazer geocoding apenas se não estiver em cache
                                            val geocodingResult = geocodingService.geocodeAddress(provider.address)
                                            if (geocodingResult is com.taskgoapp.taskgo.core.location.GeocodingResult.Success) {
                                                latitude = geocodingResult.latitude
                                                longitude = geocodingResult.longitude
                                                // Armazenar no cache
                                                geocodingCache[cacheKey] = Pair(latitude, longitude)
                                            } else {
                                                latitude = null
                                                longitude = null
                                            }
                                        }
                                    } else {
                                        latitude = null
                                        longitude = null
                                    }
                                    
                                    if (latitude != null && longitude != null) {
                                        ProviderLocation(
                                            id = provider.uid,
                                            name = provider.displayName ?: "Prestador",
                                            category = provider.preferredCategories?.firstOrNull() ?: "Serviços",
                                            latitude = latitude,
                                            longitude = longitude,
                                            rating = provider.rating?.toFloat(),
                                            isOnline = true
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                        }?.awaitAll()?.filterNotNull() ?: emptyList()
                    }
                    
                    trySend(providers)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Observa lojas com localização em tempo real
     */
    fun observeStoresWithLocation(): Flow<List<StoreLocation>> = callbackFlow {
        val listenerRegistration = usersCollection
            .whereEqualTo("role", "partner")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreMapLocationsRepository", "Erro ao observar stores: ${error.message}", error)
                    // Não fecha o flow, apenas envia lista vazia para evitar crash
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Processar geocoding de forma assíncrona para não bloquear o listener
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val stores = coroutineScope {
                        snapshot?.documents?.mapNotNull { doc ->
                            async {
                                val user = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                                user?.let { store ->
                                    // Obter localização via geocoding do endereço (com cache)
                                    val latitude: Double?
                                    val longitude: Double?
                                    
                                    if (store.address != null) {
                                        // Criar chave de cache baseada no CEP ou endereço completo
                                        val cacheKey = store.address.zipCode.ifEmpty { 
                                            "${store.address.street}, ${store.address.city}, ${store.address.state}"
                                        }
                                        
                                        // Verificar cache primeiro
                                        val cached = geocodingCache[cacheKey]
                                        if (cached != null) {
                                            latitude = cached.first
                                            longitude = cached.second
                                        } else {
                                            // Fazer geocoding apenas se não estiver em cache
                                            val geocodingResult = geocodingService.geocodeAddress(store.address)
                                            if (geocodingResult is com.taskgoapp.taskgo.core.location.GeocodingResult.Success) {
                                                latitude = geocodingResult.latitude
                                                longitude = geocodingResult.longitude
                                                // Armazenar no cache
                                                geocodingCache[cacheKey] = Pair(latitude, longitude)
                                            } else {
                                                latitude = null
                                                longitude = null
                                            }
                                        }
                                    } else {
                                        latitude = null
                                        longitude = null
                                    }
                                    
                                    if (latitude != null && longitude != null) {
                                        StoreLocation(
                                            id = store.uid,
                                            name = store.displayName ?: "Loja",
                                            type = store.preferredCategories?.firstOrNull() ?: "Loja",
                                            latitude = latitude,
                                            longitude = longitude,
                                            rating = store.rating?.toFloat(),
                                            isOpen = true
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                        }?.awaitAll()?.filterNotNull() ?: emptyList()
                    }
                    
                    trySend(stores)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Busca prestadores e lojas próximos a uma localização
     */
    suspend fun getNearbyProvidersAndStores(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 50.0
    ): Pair<List<ProviderLocation>, List<StoreLocation>> {
        return try {
            // Buscar todos os prestadores e lojas
            val providersSnapshot = usersCollection
                .whereEqualTo("role", "partner")
                .get()
                .await()
            
            val storesSnapshot = usersCollection
                .whereEqualTo("role", "partner")
                .get()
                .await()
            
            // Filtrar por distância
            // Fazer geocoding de todos os endereços em paralelo
            val nearbyProviders = coroutineScope {
                providersSnapshot.documents.map { doc ->
                    async<ProviderLocation?> {
                        val user = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                        user?.let { provider ->
                            if (provider.address != null) {
                                val geocodingResult = geocodingService.geocodeAddress(provider.address)
                                if (geocodingResult is com.taskgoapp.taskgo.core.location.GeocodingResult.Success) {
                                    val distance = calculateDistance(
                                        latitude, longitude,
                                        geocodingResult.latitude, geocodingResult.longitude
                                    )
                                    
                                    if (distance <= radiusKm) {
                                        ProviderLocation(
                                            id = provider.uid,
                                            name = provider.displayName ?: "Prestador",
                                            category = provider.preferredCategories?.firstOrNull() ?: "Serviços",
                                            latitude = geocodingResult.latitude,
                                            longitude = geocodingResult.longitude,
                                            rating = provider.rating?.toFloat(),
                                            isOnline = true
                                        )
                                    } else null
                                } else null
                            } else null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            
            val nearbyStores = coroutineScope {
                storesSnapshot.documents.map { doc ->
                    async<StoreLocation?> {
                        val user = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                        user?.let { store ->
                            if (store.address != null) {
                                val geocodingResult = geocodingService.geocodeAddress(store.address)
                                if (geocodingResult is com.taskgoapp.taskgo.core.location.GeocodingResult.Success) {
                                    val distance = calculateDistance(
                                        latitude, longitude,
                                        geocodingResult.latitude, geocodingResult.longitude
                                    )
                                    
                                    if (distance <= radiusKm) {
                                        StoreLocation(
                                            id = store.uid,
                                            name = store.displayName ?: "Loja",
                                            type = store.preferredCategories?.firstOrNull() ?: "Loja",
                                            latitude = geocodingResult.latitude,
                                            longitude = geocodingResult.longitude,
                                            rating = store.rating?.toFloat(),
                                            isOpen = true
                                        )
                                    } else null
                                } else null
                            } else null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            
            Pair(nearbyProviders, nearbyStores)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreMapLocationsRepository", "Erro ao buscar locais próximos: ${e.message}", e)
            Pair(emptyList(), emptyList())
        }
    }
    
    /**
     * Calcula distância entre duas coordenadas (Haversine)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}

