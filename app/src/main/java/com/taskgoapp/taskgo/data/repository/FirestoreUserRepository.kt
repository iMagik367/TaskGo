package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.core.firebase.LocationHelper
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
    // REMOVIDO: usersCollection global - usuários devem ser salvos em locations/{locationId}/users
    // private val usersCollection = firestore.collection("users")

    suspend fun getUser(uid: String): UserFirestore? {
        return try {
            android.util.Log.d("FirestoreUserRepository", "Buscando usuário no Firestore: uid=$uid")
            
            // ESTRATÉGIA HÍBRIDA: Buscar primeiro em users global (legacy), depois em locations/{locationId}/users
            // 1. Tentar buscar na coleção global "users" (legacy/migração)
            val globalDoc = firestore.collection("users").document(uid).get().await()
            if (globalDoc.exists()) {
                val user = globalDoc.data?.let { mapUser(globalDoc.id, it) }
                if (user != null) {
                    android.util.Log.d("FirestoreUserRepository", "✅ Usuário encontrado em users global: ${user.displayName}, email: ${user.email}, role: ${user.role}")
                    
                    // Se o usuário tem city/state, também buscar em locations/{locationId}/users para verificar se existe lá
                    val userCity = user.city?.takeIf { it.isNotBlank() }
                    val userState = user.state?.takeIf { it.isNotBlank() }
                    if (userCity != null && userState != null) {
                        try {
                            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
                            val locationDoc = firestore.collection("locations").document(locationId)
                                .collection("users").document(uid).get().await()
                            if (locationDoc.exists()) {
                                android.util.Log.d("FirestoreUserRepository", "✅ Usuário também encontrado em locations/$locationId/users")
                                // Retornar o da coleção locations (mais atualizado)
                                val locationUser = locationDoc.data?.let { mapUser(locationDoc.id, it) }
                                if (locationUser != null) {
                                    return locationUser
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("FirestoreUserRepository", "Erro ao buscar em locations: ${e.message}")
                        }
                    }
                    
                    return user
                }
            }
            
            // 2. Se não encontrou na coleção global, tentar buscar em todas as locations conhecidas
            // (Ineficiente, mas necessário para migração)
            android.util.Log.d("FirestoreUserRepository", "Usuário não encontrado em users global, tentando locations...")
            
            // Por enquanto, retornar null se não encontrou na coleção global
            // TODO: Implementar busca em locations/{locationId}/users quando tivermos lista de locations
            android.util.Log.d("FirestoreUserRepository", "Usuário não encontrado no Firestore: uid=$uid")
            null
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao buscar usuário: ${e.message}", e)
            null
        }
    }
    
    /**
     * Observa mudanças do usuário no Firestore em tempo real
     * CRÍTICO: Não falha se usuário não existe ainda (permite criação durante login)
     * LEI MÁXIMA DO TASKGO: Observa TANTO em users global QUANTO em locations/{locationId}/users
     */
    fun observeUser(uid: String): Flow<UserFirestore?> = callbackFlow {
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        
        try {
            // ESTRATÉGIA HÍBRIDA: Observar em ambas as coleções para garantir sincronização
            // 1. Observar na coleção global "users" (legacy/compatibilidade)
            val globalListener = firestore.collection("users").document(uid)
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
                            android.util.Log.d("FirestoreUserRepository", "🔄 Usuário atualizado em users global: ${user?.displayName}, role: ${user?.role}")
                            
                            // Se o usuário tem city/state, também observar em locations/{locationId}/users
                            val userCity = user?.city?.takeIf { it.isNotBlank() }
                            val userState = user?.state?.takeIf { it.isNotBlank() }
                            if (userCity != null && userState != null && listeners.size == 1) {
                                try {
                                    val locationId = LocationHelper.normalizeLocationId(userCity, userState)
                                    val locationListener = firestore.collection("locations").document(locationId)
                                        .collection("users").document(uid)
                                        .addSnapshotListener { locationSnapshot, locationError ->
                                            if (locationError != null) {
                                                android.util.Log.w("FirestoreUserRepository", "Erro ao observar em locations: ${locationError.message}")
                                                return@addSnapshotListener
                                            }
                                            
                                            if (locationSnapshot != null && locationSnapshot.exists()) {
                                                try {
                                                    val locationUser = locationSnapshot.data?.let { mapUser(locationSnapshot.id, it) }
                                                    if (locationUser != null) {
                                                        android.util.Log.d("FirestoreUserRepository", "🔄 Usuário atualizado em locations/$locationId/users: ${locationUser.displayName}")
                                                        // Priorizar dados de locations/{locationId}/users (mais atualizado)
                                                        trySend(locationUser)
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("FirestoreUserRepository", "Erro ao converter usuário de locations: ${e.message}", e)
                                                }
                                            }
                                        }
                                    listeners.add(locationListener)
                                    android.util.Log.d("FirestoreUserRepository", "✅ Observando também em locations/$locationId/users")
                                } catch (e: Exception) {
                                    android.util.Log.w("FirestoreUserRepository", "Erro ao configurar listener de locations: ${e.message}")
                                }
                            }
                            
                            trySend(user)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreUserRepository", "Erro ao converter usuário: ${e.message}", e)
                            trySend(null)
                        }
                    } else {
                        // CRÍTICO: Se o documento não existe mas já existia antes (foi deletado), forçar logout
                        // Verificar se o usuário está autenticado - se sim e documento não existe, foi deletado
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null && currentUser.uid == uid) {
                            android.util.Log.w("FirestoreUserRepository", "⚠️ Usuário autenticado mas documento não existe no Firestore - conta foi deletada, forçando logout")
                            // Fazer logout imediatamente
                            try {
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                android.util.Log.d("FirestoreUserRepository", "✅ Logout realizado com sucesso após detecção de conta deletada")
                            } catch (e: Exception) {
                                android.util.Log.e("FirestoreUserRepository", "Erro ao fazer logout: ${e.message}", e)
                            }
                            // Emitir null para indicar que o usuário foi deletado
                            // O componente que observa este Flow deve detectar e forçar logout
                            trySend(null)
                        } else {
                            android.util.Log.d("FirestoreUserRepository", "Usuário não existe no Firestore ainda (será criado)")
                            trySend(null)
                        }
                    }
                }
            listeners.add(globalListener)
            
            awaitClose { 
                listeners.forEach { it.remove() }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao configurar listener de usuário: ${e.message}", e)
            trySend(null)
            // Não fechar o channel imediatamente, permitir retry
            awaitClose { 
                listeners.forEach { it.remove() }
            }
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
            
            // LEI MÁXIMA DO TASKGO: Buscar em locations/{locationId}/users quando temos city/state
            // Se não tiver city/state, buscar em users global (legacy)
            val users = mutableListOf<UserFirestore>()
            
            if (city != null && state != null) {
                // Buscar em locations/{locationId}/users (correto)
                try {
                    val locationId = LocationHelper.normalizeLocationId(city, state)
                    val locationQuery = firestore.collection("locations").document(locationId)
                        .collection("users")
                        .whereEqualTo("role", role)
                        .get()
                        .await()
                    
                    android.util.Log.d("FirestoreUserRepository", "Buscando em locations/$locationId/users: ${locationQuery.size()} documentos")
                    
                    locationQuery.documents.forEach { doc ->
                        try {
                            val user = doc.data?.let { mapUser(doc.id, it) }
                            if (user != null) {
                                // Verificar categorias se necessário
                                val matchesCategories = if (categories != null && role == "partner") {
                                    user.preferredCategories?.any { cat -> 
                                        categories.any { searchCat -> 
                                            cat.equals(searchCat, ignoreCase = true) 
                                        }
                                    } ?: false
                                } else {
                                    true
                                }
                                
                                if (matchesCategories) {
                                    users.add(user)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreUserRepository", "Erro ao converter documento: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreUserRepository", "Erro ao buscar em locations: ${e.message}, tentando users global")
                }
            }
            
            // Fallback: Buscar em users global (legacy) se não encontrou em locations ou se não tem city/state
            if (users.isEmpty() || city == null || state == null) {
                android.util.Log.d("FirestoreUserRepository", "Buscando em users global (legacy)...")
                var query = firestore.collection("users").whereEqualTo("role", role)
                val snapshot = query.get().await()
                
                snapshot.documents.forEach { doc ->
                    try {
                        val user = doc.toObject(UserFirestore::class.java)
                        if (user != null) {
                            // Verificar se corresponde aos critérios de busca
                            // Lei 1: Ler city/state APENAS da raiz do documento
                            val matchesLocation = when {
                                city != null && state != null -> {
                                    user.city?.equals(city, ignoreCase = true) == true &&
                                    user.state?.equals(state, ignoreCase = true) == true
                                }
                                latitude != null && longitude != null -> {
                                    // Verificar se está dentro de um raio (será feito em camada superior)
                                    true // Por enquanto, retornar todos e filtrar depois
                                }
                                else -> true
                            }
                            
                            val matchesCategories = if (categories != null && role == "partner") {
                                user.preferredCategories?.any { cat -> 
                                    categories.any { searchCat -> 
                                        cat.equals(searchCat, ignoreCase = true) 
                                    }
                                } ?: false
                            } else {
                                true
                            }
                            
                            if (matchesLocation && matchesCategories) {
                                users.add(user)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreUserRepository", "Erro ao converter documento: ${e.message}", e)
                    }
                }
            }
            
            android.util.Log.d("FirestoreUserRepository", "Total de usuários encontrados: ${users.size}")
            return users
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
                // CRÍTICO: Buscar em users global (legacy) - TODO: Migrar para locations/{locationId}/users
                val cpfQuery = firestore.collection("users")
                    .whereEqualTo("cpf", cleanDocument)
                    .limit(1)
                    .get()
                    .await()
                
                if (!cpfQuery.isEmpty) {
                    val user = cpfQuery.documents[0].data?.let { mapUser(cpfQuery.documents[0].id, it) }
                    android.util.Log.d("FirestoreUserRepository", "✅ Usuário encontrado por CPF: ${user?.email}, role: ${user?.role}")
                    return user
                }
                
                // CRÍTICO: Buscar em users global (legacy) - TODO: Migrar para locations/{locationId}/users
                val cnpjQuery = firestore.collection("users")
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
                // CRÍTICO: Buscar em users global (legacy) - TODO: Migrar para locations/{locationId}/users
                val allUsersSnapshot = firestore.collection("users")
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
            // CRÍTICO: O role SEMPRE será "partner" ou "client" - garantido pelo sistema
            role = (data["role"] as String).lowercase(),
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
                // PADRÃO ÚNICO: Ler city/state APENAS dos campos diretos do documento
                // Backend salva em user.city e user.state, não em address
                val city = (data["city"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                val state = (data["state"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                
                com.taskgoapp.taskgo.core.model.Address(
                    id = addr["id"] as? String ?: "",
                    name = addr["name"] as? String ?: "",
                    phone = addr["phone"] as? String ?: "",
                    cep = addr["cep"] as? String ?: (addr["zipCode"] as? String ?: ""),
                    street = addr["street"] as? String ?: "",
                    district = addr["district"] as? String ?: "",
                    city = city,
                    state = state,
                    number = addr["number"] as? String ?: "",
                    complement = addr["complement"] as? String,
                    neighborhood = addr["neighborhood"] as? String ?: "",
                    zipCode = addr["zipCode"] as? String ?: "",
                    country = addr["country"] as? String ?: "Brasil"
                )
            } ?: run {
                // Se não tem address, criar um básico com city/state diretos do documento
                val city = (data["city"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                val state = (data["state"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                if (city.isNotBlank() && state.isNotBlank()) {
                    com.taskgoapp.taskgo.core.model.Address(
                        id = "",
                        name = "",
                        phone = "",
                        cep = "",
                        street = "",
                        district = "",
                        city = city,
                        state = state,
                        number = "",
                        complement = null,
                        neighborhood = "",
                        zipCode = "",
                        country = "Brasil"
                    )
                } else null
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
            // Lei 1: city e state DEVEM estar na raiz do documento users/{userId}
            city = (data["city"] as? String)?.takeIf { it.isNotBlank() },
            state = (data["state"] as? String)?.takeIf { it.isNotBlank() },
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
            if (user.uid.isBlank()) {
                return Result.failure(Exception("UID não pode estar vazio"))
            }
            
            // CRÍTICO: O role DEVE ser definido pelo usuário (partner ou client)
            // NUNCA aceitar "user" como válido
            if (user.role.isNullOrBlank()) {
                return Result.failure(Exception("Role não pode estar vazio. O usuário deve ter um role válido (partner ou client)."))
            }
            
            val validRoles = listOf("partner", "client")
            if (!validRoles.contains(user.role.lowercase())) {
                return Result.failure(Exception("Role inválido: ${user.role}. Role deve ser 'partner' ou 'client'."))
            }
            
            if (user.city.isNullOrBlank() || user.state.isNullOrBlank()) {
                return Result.failure(Exception("City e state são obrigatórios e não podem estar vazios."))
            }
            
            val userIdentifier = com.taskgoapp.taskgo.core.utils.UserIdentifier.generateUserId(user)
            
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
            
            val finalCity = user.city?.takeIf { it.isNotBlank() }
                ?: return Result.failure(Exception("City é obrigatório e não pode estar vazio. O usuário deve ter city definido no cadastro."))
            
            val finalState = user.state?.takeIf { it.isNotBlank() }
                ?: return Result.failure(Exception("State é obrigatório e não pode estar vazio. O usuário deve ter state definido no cadastro."))
            
            dataMap["city"] = finalCity
            dataMap["state"] = finalState
            
            // Converter Address se existir
            // CRÍTICO: NÃO salvar city/state em address - backend lê de user.city/user.state
            user.address?.let { address ->
                dataMap["address"] = mapOf(
                    "street" to (address.street ?: ""),
                    "number" to (address.number ?: ""),
                    "complement" to (address.complement ?: ""),
                    "neighborhood" to (address.neighborhood ?: ""),
                    // REMOVIDO: city e state de address - usar APENAS campos diretos user.city/user.state
                    "zipCode" to (address.zipCode ?: ""),
                    "country" to (address.country ?: "Brasil")
                )
            }
            
            // CRÍTICO: Salvar em locations/{locationId}/users/{userId} em vez de users global
            // Obter locationId de finalCity e finalState (sempre válidos - validação acima garante)
            val locationId = try {
                LocationHelper.normalizeLocationId(finalCity, finalState)
            } catch (e: Exception) {
                return Result.failure(Exception("Erro ao normalizar locationId para city=$finalCity, state=$finalState: ${e.message}"))
            }
            
            val locationUsersCollection = firestore.collection("locations").document(locationId).collection("users")
            locationUsersCollection.document(user.uid).set(dataMap, com.google.firebase.firestore.SetOptions.merge()).await()
            
            val privateUsersCollection = firestore.collection("users").document(locationId).collection("users")
            privateUsersCollection.document(user.uid).set(dataMap, com.google.firebase.firestore.SetOptions.merge()).await()
            
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
            val user = getUser(uid)
            val userCity = user?.city?.takeIf { it.isNotBlank() }
            val userState = user?.state?.takeIf { it.isNotBlank() }
            
            if (userCity == null || userState == null) {
                return Result.failure(Exception("Usuário não tem city/state no perfil"))
            }
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            
            // CRÍTICO: Atualizar em DUAS coleções
            val updateData = mapOf(
                field to value,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // 1. Atualizar em locations/{locationId}/users/{userId} (pública)
            firestore.collection("locations").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
                .await()
            
            // 2. Atualizar em users/{locationId}/users/{userId} (privada)
            firestore.collection("users").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
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
            val updateData = mapOf(
                "documents" to documents,
                "documentsApproved" to true,
                "documentsApprovedAt" to FieldValue.serverTimestamp(),
                "documentsApprovedBy" to approvedBy,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            val user = getUser(uid)
            val userCity = user?.city?.takeIf { it.isNotBlank() }
            val userState = user?.state?.takeIf { it.isNotBlank() }
            
            if (userCity == null || userState == null) {
                return Result.failure(Exception("Usuário não tem city/state no perfil"))
            }
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            
            // CRÍTICO: Atualizar em DUAS coleções
            // 1. Atualizar em locations/{locationId}/users/{userId} (pública)
            firestore.collection("locations").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
                .await()
            
            // 2. Atualizar em users/{locationId}/users/{userId} (privada)
            firestore.collection("users").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStripeAccount(uid: String, accountId: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "stripeAccountId" to accountId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            val user = getUser(uid)
            val userCity = user?.city?.takeIf { it.isNotBlank() }
            val userState = user?.state?.takeIf { it.isNotBlank() }
            
            if (userCity == null || userState == null) {
                return Result.failure(Exception("Usuário não tem city/state no perfil"))
            }
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            
            // CRÍTICO: Atualizar em DUAS coleções
            // 1. Atualizar em locations/{locationId}/users/{userId} (pública)
            firestore.collection("locations").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
                .await()
            
            // 2. Atualizar em users/{locationId}/users/{userId} (privada)
            firestore.collection("users").document(locationId)
                .collection("users").document(uid)
                .update(updateData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





