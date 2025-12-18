package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCategoriesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoriesRepository {
    
    private val productCategoriesCollection = firestore.collection("product_categories")
    private val serviceCategoriesCollection = firestore.collection("service_categories")
    
    override fun observeProductCategories(): Flow<List<String>> = callbackFlow {
        // Primeiro, tentar buscar do cache (instantâneo)
        try {
            val cachedSnapshot = productCategoriesCollection
                .orderBy("name")
                .get(Source.CACHE)
                .await()
            
            val cachedCategories = cachedSnapshot.documents.mapNotNull { doc ->
                doc.getString("name")
            }
            
            if (cachedCategories.isNotEmpty()) {
                trySend(cachedCategories)
            } else {
                trySend(getDefaultProductCategories())
            }
        } catch (e: Exception) {
            // Se não houver cache, usar categorias padrão
            trySend(getDefaultProductCategories())
        }
        
        // Depois, escutar mudanças em tempo real
        val listenerRegistration = productCategoriesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Se não existir a coleção, retorna categorias padrão
                    trySend(getDefaultProductCategories())
                    return@addSnapshotListener
                }
                
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("name")
                } ?: getDefaultProductCategories()
                
                trySend(categories)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override fun observeServiceCategories(): Flow<List<ServiceCategory>> = callbackFlow {
        var hasSentInitialData = false
        
        // Primeiro, tentar buscar do cache (instantâneo)
        try {
            val cachedSnapshot = serviceCategoriesCollection
                .orderBy("name")
                .get(Source.CACHE)
                .await()
            
            val cachedCategories = cachedSnapshot.documents.mapNotNull { doc ->
                ServiceCategory(
                    id = doc.id.toLongOrNull() ?: 0L,
                    name = doc.getString("name") ?: "",
                    icon = doc.getString("icon") ?: "",
                    description = doc.getString("description") ?: ""
                )
            }
            
            if (cachedCategories.isNotEmpty()) {
                trySend(cachedCategories)
                hasSentInitialData = true
            } else {
                val defaultCategories = getDefaultServiceCategories()
                trySend(defaultCategories)
                hasSentInitialData = true
            }
        } catch (e: Exception) {
            // Se não houver cache, usar categorias padrão
            val defaultCategories = getDefaultServiceCategories()
            trySend(defaultCategories)
            hasSentInitialData = true
        }
        
        // Depois, escutar mudanças em tempo real
        val listenerRegistration = serviceCategoriesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Se não existir a coleção e ainda não enviamos dados, retorna categorias padrão
                    if (!hasSentInitialData) {
                        trySend(getDefaultServiceCategories())
                    }
                    return@addSnapshotListener
                }
                
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    ServiceCategory(
                        id = doc.id.toLongOrNull() ?: 0L,
                        name = doc.getString("name") ?: "",
                        icon = doc.getString("icon") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                } ?: getDefaultServiceCategories()
                
                // Só enviar se houver dados válidos ou se ainda não enviamos nada
                if (categories.isNotEmpty() || !hasSentInitialData) {
                    trySend(categories)
                    hasSentInitialData = true
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override suspend fun getProductCategories(): List<String> {
        return try {
            val snapshot = productCategoriesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.getString("name")
            }.ifEmpty { getDefaultProductCategories() }
        } catch (e: Exception) {
            getDefaultProductCategories()
        }
    }
    
    override suspend fun getServiceCategories(): List<ServiceCategory> {
        return try {
            val snapshot = serviceCategoriesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                ServiceCategory(
                    id = doc.id.toLongOrNull() ?: 0L,
                    name = doc.getString("name") ?: "",
                    icon = doc.getString("icon") ?: "",
                    description = doc.getString("description") ?: ""
                )
            }.ifEmpty { getDefaultServiceCategories() }
        } catch (e: Exception) {
            getDefaultServiceCategories()
        }
    }
    
    private fun getDefaultProductCategories(): List<String> {
        return listOf(
            "Todos",
            "Eletrônicos",
            "Casa e Decoração",
            "Ferramentas",
            "Móveis",
            "Roupas",
            "Esportes",
            "Livros",
            "Brinquedos",
            "Beleza e Cuidados"
        )
    }
    
    private fun getDefaultServiceCategories(): List<ServiceCategory> {
        return listOf(
            ServiceCategory(1, "Montagem", "build", "Serviços de montagem de móveis e equipamentos"),
            ServiceCategory(2, "Reforma", "home", "Reformas e construções"),
            ServiceCategory(3, "Jardinagem", "eco", "Serviços de jardinagem e paisagismo"),
            ServiceCategory(4, "Elétrica", "flash_on", "Serviços elétricos"),
            ServiceCategory(5, "Encanamento", "plumbing", "Serviços de encanamento"),
            ServiceCategory(6, "Pintura", "format_paint", "Serviços de pintura"),
            ServiceCategory(7, "Limpeza", "cleaning_services", "Serviços de limpeza"),
            ServiceCategory(8, "Outros", "more_horiz", "Outros serviços")
        )
    }
}

