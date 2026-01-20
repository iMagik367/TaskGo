package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getUser(uid: String): UserFirestore? {
        return try {
            android.util.Log.d("FirestoreUserRepository", "Buscando usuário no Firestore: uid=$uid")
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.data?.let { mapUser(document.id, it) }
                android.util.Log.d("FirestoreUserRepository", "Usuário encontrado: ${user?.displayName}, email: ${user?.email}, role: ${user?.role}")
                user
            } else {
                android.util.Log.d("FirestoreUserRepository", "Usuário não encontrado no Firestore: uid=$uid")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao buscar usuário: ${e.message}", e)
            null
        }
    }
    
    /**
     * Observa mudanças do usuário no Firestore em tempo real
     * CRÍTICO: Não falha se usuário não existe ainda (permite criação durante login)
     */
    fun observeUser(uid: String): Flow<UserFirestore?> = callbackFlow {
        try {
            val listenerRegistration = usersCollection.document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Se for erro de permissão, pode ser que o usuário não existe ainda
                        // Não fechar o channel, apenas logar e enviar null
                        if (error is com.google.firebase.firestore.FirebaseFirestoreException) {
                            val firestoreError = error as com.google.firebase.firestore.FirebaseFirestoreException
                            if (firestoreError.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                android.util.Log.w("FirestoreUserRepository", "Permissão negada ao observar usuário (pode não existir ainda): ${error.message}")
                                trySend(null)
                                return@addSnapshotListener
                            }
                        }
                        android.util.Log.e("FirestoreUserRepository", "Erro ao observar usuário: ${error.message}", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val user = snapshot.data?.let { mapUser(snapshot.id, it) }
                            android.util.Log.d("FirestoreUserRepository", "Usuário atualizado no Firestore: ${user?.displayName}, role: ${user?.role}")
                            trySend(user)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreUserRepository", "Erro ao converter usuário: ${e.message}", e)
                            trySend(null)
                        }
                    } else {
                        android.util.Log.d("FirestoreUserRepository", "Usuário não existe no Firestore ainda (será criado)")
                        trySend(null)
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao configurar listener de usuário: ${e.message}", e)
            trySend(null)
            // Não fechar o channel imediatamente, permitir retry
        }
    }
    
    /**
     * Busca usuários por localização, role e categorias usando userIdentifier
     * @param role Role do usuário (client, partner, etc.)
     * @param city Cidade (opcional)
     * @param state Estado (opcional)
     * @param latitude Latitude (opcional, usado se cidade/estado não fornecidos)
     * @param longitude Longitude (opcional, usado se cidade/estado não fornecidos)
     * @param categories Categorias de serviços (opcional, apenas para partners)
     * @return Lista de usuários encontrados
     */
    suspend fun findUsersByLocationAndRole(
        role: String,
        city: String? = null,
        state: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        categories: List<String>? = null
    ): List<UserFirestore> {
        return try {
            val searchId = com.taskgoapp.taskgo.core.utils.UserIdentifier.generateSearchId(
                role = role,
                latitude = latitude,
                longitude = longitude,
                city = city,
                state = state,
                categories = categories
            )
            
            android.util.Log.d("FirestoreUserRepository", "Buscando usuários com searchId: $searchId")
            
            // Buscar usuários que correspondem ao searchId
            // Nota: Como userIdentifier é um hash, precisamos buscar por componentes individuais
            // ou usar uma abordagem diferente. Por enquanto, vamos buscar por role e localização.
            var query = usersCollection.whereEqualTo("role", role)
            
            // Se tiver cidade e estado, podemos filtrar por endereço (requer índice composto)
            // Por enquanto, vamos buscar todos e filtrar em memória
            val snapshot = query.get().await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val user = doc.toObject(UserFirestore::class.java)
                    if (user != null) {
                        // Verificar se corresponde aos critérios de busca
                        val matchesLocation = when {
                            city != null && state != null -> {
                                user.address?.city?.equals(city, ignoreCase = true) == true &&
                                user.address?.state?.equals(state, ignoreCase = true) == true
                            }
                            latitude != null && longitude != null -> {
                                // Verificar se está dentro de um raio (será feito em camada superior)
                                true // Por enquanto, retornar todos e filtrar depois
                            }
                            else -> true
                        }
                        
                        val matchesCategories = if (categories != null && (role == "partner" || role == "provider" || role == "seller")) {
                            user.preferredCategories?.any { cat -> 
                                categories.any { searchCat -> 
                                    cat.equals(searchCat, ignoreCase = true) 
                                }
                            } ?: false
                        } else {
                            true
                        }
                        
                        if (matchesLocation && matchesCategories) {
                            user
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreUserRepository", "Erro ao converter documento: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao buscar usuários: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Busca usuário por CPF ou CNPJ
     * @param document CPF ou CNPJ (com ou sem formatação)
     * @return UserFirestore se encontrado, null caso contrário
     * 
     * CRÍTICO: Esta função precisa buscar sem autenticação, então tenta ambas as formas:
     * 1. Busca direta no Firestore (pode falhar por regras de segurança)
     * 2. Fallback: busca todos os documentos e filtra em memória (menos eficiente, mas funciona)
     */
    suspend fun getUserByDocument(document: String): UserFirestore? {
        return try {
            // Remove formatação do documento
            val cleanDocument = document.replace(Regex("[^0-9]"), "")
            android.util.Log.d("FirestoreUserRepository", "Buscando usuário por documento: $cleanDocument (limpo)")
            
            // Tentar busca direta primeiro (mais eficiente se permitido pelas regras)
            try {
                // Buscar por CPF
                val cpfQuery = usersCollection
                    .whereEqualTo("cpf", cleanDocument)
                    .limit(1)
                    .get()
                    .await()
                
                if (!cpfQuery.isEmpty) {
                    val user = cpfQuery.documents[0].data?.let { mapUser(cpfQuery.documents[0].id, it) }
                    android.util.Log.d("FirestoreUserRepository", "✅ Usuário encontrado por CPF: ${user?.email}, role: ${user?.role}")
                    return user
                }
                
                // Buscar por CNPJ
                val cnpjQuery = usersCollection
                    .whereEqualTo("cnpj", cleanDocument)
                    .limit(1)
                    .get()
                    .await()
                
                if (!cnpjQuery.isEmpty) {
                    val user = cnpjQuery.documents[0].data?.let { mapUser(cnpjQuery.documents[0].id, it) }
                    android.util.Log.d("FirestoreUserRepository", "✅ Usuário encontrado por CNPJ: ${user?.email}, role: ${user?.role}")
                    return user
                }
            } catch (queryError: Exception) {
                // Se a query direta falhar (provavelmente por regras de segurança), usar fallback
                android.util.Log.w("FirestoreUserRepository", "Query direta falhou (provavelmente regras de segurança), usando fallback: ${queryError.message}")
            }
            
            // FALLBACK: Buscar todos os usuários e filtrar em memória
            // Isso é necessário porque as regras do Firestore podem bloquear queries não autenticadas
            android.util.Log.d("FirestoreUserRepository", "Tentando busca por fallback (buscando todos e filtrando)...")
            try {
                // Buscar todos os usuários (com limite razoável)
                // Nota: Isso pode ser lento, mas funciona mesmo sem autenticação
                val allUsersSnapshot = usersCollection
                    .limit(1000) // Limite razoável para não sobrecarregar
                    .get()
                    .await()
                
                android.util.Log.d("FirestoreUserRepository", "Buscando em ${allUsersSnapshot.size()} documentos...")
                
                // Filtrar em memória por CPF ou CNPJ
                for (doc in allUsersSnapshot.documents) {
                    try {
                        val data = doc.data
                        val docCpf = (data?.get("cpf") as? String)?.replace(Regex("[^0-9]"), "")
                        val docCnpj = (data?.get("cnpj") as? String)?.replace(Regex("[^0-9]"), "")
                        
                        if ((docCpf != null && docCpf == cleanDocument) || 
                            (docCnpj != null && docCnpj == cleanDocument)) {
                            val user = mapUser(doc.id, data)
                            android.util.Log.d("FirestoreUserRepository", "✅ Usuário encontrado por fallback: ${user.email}, role: ${user.role}")
                            return user
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FirestoreUserRepository", "Erro ao processar documento ${doc.id}: ${e.message}")
                    }
                }
            } catch (fallbackError: Exception) {
                android.util.Log.e("FirestoreUserRepository", "Erro no fallback: ${fallbackError.message}", fallbackError)
            }
            
            android.util.Log.d("FirestoreUserRepository", "❌ Usuário não encontrado por documento: $cleanDocument")
            null
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao buscar usuário por documento: ${e.message}", e)
            null
        }
    }

    private fun mapUser(id: String, data: Map<String, Any?>): UserFirestore {
        fun parseDate(value: Any?): java.util.Date? = when (value) {
            is java.util.Date -> value
            is com.google.firebase.Timestamp -> value.toDate()
            is Long -> java.util.Date(value)
            is Int -> java.util.Date(value.toLong())
            else -> null
        }

        return UserFirestore(
            uid = id,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String,
            photoURL = data["photoURL"] as? String,
            phone = data["phone"] as? String,
            role = data["role"] as? String ?: "client",
            pendingAccountType = data["pendingAccountType"] as? Boolean ?: false,
            profileComplete = data["profileComplete"] as? Boolean ?: false,
            verified = data["verified"] as? Boolean ?: false,
            cpf = data["cpf"] as? String,
            rg = data["rg"] as? String,
            cnpj = data["cnpj"] as? String,
            birthDate = parseDate(data["birthDate"]),
            documentFront = data["documentFront"] as? String,
            documentBack = data["documentBack"] as? String,
            selfie = data["selfie"] as? String,
            address = (data["address"] as? Map<*, *>)?.let { addr ->
                com.taskgoapp.taskgo.core.model.Address(
                    id = addr["id"] as? String ?: "",
                    name = addr["name"] as? String ?: "",
                    phone = addr["phone"] as? String ?: "",
                    cep = addr["cep"] as? String ?: (addr["zipCode"] as? String ?: ""),
                    street = addr["street"] as? String ?: "",
                    district = addr["district"] as? String ?: "",
                    city = addr["city"] as? String ?: "",
                    state = addr["state"] as? String ?: "",
                    number = addr["number"] as? String ?: "",
                    complement = addr["complement"] as? String,
                    neighborhood = addr["neighborhood"] as? String ?: "",
                    zipCode = addr["zipCode"] as? String ?: "",
                    country = addr["country"] as? String ?: "Brasil"
                )
            },
            addressProof = data["addressProof"] as? String,
            verifiedAt = parseDate(data["verifiedAt"]),
            verifiedBy = data["verifiedBy"] as? String,
            biometricEnabled = data["biometricEnabled"] as? Boolean ?: false,
            twoFactorEnabled = data["twoFactorEnabled"] as? Boolean ?: false,
            twoFactorMethod = data["twoFactorMethod"] as? String,
            stripeAccountId = data["stripeAccountId"] as? String,
            stripeChargesEnabled = data["stripeChargesEnabled"] as? Boolean ?: false,
            stripePayoutsEnabled = data["stripePayoutsEnabled"] as? Boolean ?: false,
            stripeDetailsSubmitted = data["stripeDetailsSubmitted"] as? Boolean ?: false,
            documents = (data["documents"] as? List<*>)?.mapNotNull { it as? String },
            documentsApproved = data["documentsApproved"] as? Boolean ?: false,
            documentsApprovedAt = parseDate(data["documentsApprovedAt"]),
            documentsApprovedBy = data["documentsApprovedBy"] as? String,
            preferredCategories = (data["preferredCategories"] as? List<*>)?.mapNotNull { it as? String },
            userIdentifier = data["userIdentifier"] as? String,
            notificationSettings = (data["notificationSettings"] as? Map<*, *>)?.let {
                com.taskgoapp.taskgo.data.firestore.models.NotificationSettingsFirestore(
                    push = it["push"] as? Boolean ?: true,
                    promos = it["promos"] as? Boolean ?: true,
                    sound = it["sound"] as? Boolean ?: true,
                    lockscreen = it["lockscreen"] as? Boolean ?: true,
                    email = it["email"] as? Boolean ?: false,
                    sms = it["sms"] as? Boolean ?: false
                )
            },
            privacySettings = (data["privacySettings"] as? Map<*, *>)?.let {
                com.taskgoapp.taskgo.data.firestore.models.PrivacySettingsFirestore(
                    locationSharing = it["locationSharing"] as? Boolean ?: true,
                    profileVisible = it["profileVisible"] as? Boolean ?: true,
                    contactInfoSharing = it["contactInfoSharing"] as? Boolean ?: false,
                    analytics = it["analytics"] as? Boolean ?: true,
                    personalizedAds = it["personalizedAds"] as? Boolean ?: false,
                    dataCollection = it["dataCollection"] as? Boolean ?: true,
                    thirdPartySharing = it["thirdPartySharing"] as? Boolean ?: false
                )
            },
            language = data["language"] as? String ?: "pt",
            rating = (data["rating"] as? Number)?.toDouble(),
            createdAt = parseDate(data["createdAt"]),
            updatedAt = parseDate(data["updatedAt"])
        )
    }

    suspend fun updateUser(user: UserFirestore): Result<Unit> {
        return try {
            // Validar que uid não está vazio
            if (user.uid.isBlank()) {
                android.util.Log.e("FirestoreUserRepository", "Erro: uid está vazio ao tentar salvar usuário")
                return Result.failure(Exception("UID não pode estar vazio"))
            }
            
            android.util.Log.d("FirestoreUserRepository", "Salvando usuário no Firestore: uid=${user.uid}, email=${user.email}, displayName=${user.displayName}")
            
            // Calcular userIdentifier automaticamente
            val userIdentifier = com.taskgoapp.taskgo.core.utils.UserIdentifier.generateUserId(user)
            android.util.Log.d("FirestoreUserRepository", "UserIdentifier calculado: $userIdentifier para role=${user.role}")
            
            // Converter UserFirestore para Map, tratando Date corretamente
            val dataMap = mutableMapOf<String, Any?>(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoURL" to user.photoURL,
                "phone" to user.phone,
                "role" to user.role,
                "pendingAccountType" to user.pendingAccountType,
                "profileComplete" to user.profileComplete,
                "verified" to user.verified,
                "cpf" to user.cpf,
                "rg" to user.rg,
                "cnpj" to user.cnpj,
                "birthDate" to (user.birthDate?.let { com.google.firebase.Timestamp(it) }),
                "documentFront" to user.documentFront,
                "documentBack" to user.documentBack,
                "selfie" to user.selfie,
                "addressProof" to user.addressProof,
                "verifiedAt" to (user.verifiedAt?.let { com.google.firebase.Timestamp(it) }),
                "verifiedBy" to user.verifiedBy,
                "biometricEnabled" to user.biometricEnabled,
                "twoFactorEnabled" to user.twoFactorEnabled,
                "twoFactorMethod" to user.twoFactorMethod,
                "stripeAccountId" to user.stripeAccountId,
                "stripeChargesEnabled" to user.stripeChargesEnabled,
                "stripePayoutsEnabled" to user.stripePayoutsEnabled,
                "stripeDetailsSubmitted" to user.stripeDetailsSubmitted,
                "documents" to user.documents,
                "documentsApproved" to user.documentsApproved,
                "documentsApprovedAt" to (user.documentsApprovedAt?.let { com.google.firebase.Timestamp(it) }),
                "documentsApprovedBy" to user.documentsApprovedBy,
                "preferredCategories" to user.preferredCategories,
                "userIdentifier" to userIdentifier,
                "notificationSettings" to user.notificationSettings?.let { settings ->
                    mapOf(
                        "push" to settings.push,
                        "promos" to settings.promos,
                        "sound" to settings.sound,
                        "lockscreen" to settings.lockscreen,
                        "email" to settings.email,
                        "sms" to settings.sms
                    )
                },
                "privacySettings" to user.privacySettings?.let { settings ->
                    mapOf(
                        "locationSharing" to settings.locationSharing,
                        "profileVisible" to settings.profileVisible,
                        "contactInfoSharing" to settings.contactInfoSharing,
                        "analytics" to settings.analytics,
                        "personalizedAds" to settings.personalizedAds,
                        "dataCollection" to settings.dataCollection,
                        "thirdPartySharing" to settings.thirdPartySharing
                    )
                },
                "language" to user.language,
                "rating" to user.rating,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Adicionar createdAt apenas se não existir (para não sobrescrever)
            if (user.createdAt != null) {
                dataMap["createdAt"] = com.google.firebase.Timestamp(user.createdAt)
            }
            // Se createdAt for null, não adicionar ao map - o merge não vai sobrescrever se já existir
            
            // Remover campos null para não sobrescrever dados existentes
            dataMap.entries.removeAll { it.value == null }
            
            // Converter Address se existir
            user.address?.let { address ->
                dataMap["address"] = mapOf(
                    "street" to (address.street ?: ""),
                    "number" to (address.number ?: ""),
                    "complement" to (address.complement ?: ""),
                    "neighborhood" to (address.neighborhood ?: ""),
                    "city" to (address.city ?: ""),
                    "state" to (address.state ?: ""),
                    "zipCode" to (address.zipCode ?: ""),
                    "country" to (address.country ?: "Brasil")
                )
            }
            
            // Usar set() com merge para não sobrescrever campos existentes
            usersCollection.document(user.uid).set(dataMap, com.google.firebase.firestore.SetOptions.merge()).await()
            
            android.util.Log.d("FirestoreUserRepository", "Usuário salvo com sucesso no Firestore: ${user.uid}")
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.w("FirestoreUserRepository", "Operação de salvamento cancelada: ${e.message}")
            throw e // Re-lançar CancellationException para propagar corretamente
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao salvar usuário no Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateField(uid: String, field: String, value: Any): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update(field, value, "updatedAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun promoteToProvider(uid: String): Result<Unit> {
        // Atualizar para "partner" em vez de "provider" (legacy)
        return updateField(uid, "role", "partner")
    }

    suspend fun approveDocuments(uid: String, documents: List<String>, approvedBy: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                "documents", documents,
                "documentsApproved", true,
                "documentsApprovedAt", FieldValue.serverTimestamp(),
                "documentsApprovedBy", approvedBy,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStripeAccount(uid: String, accountId: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                "stripeAccountId", accountId,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





