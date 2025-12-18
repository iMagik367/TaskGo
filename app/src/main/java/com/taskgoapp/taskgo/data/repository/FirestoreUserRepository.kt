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
                val user = document.toObject(UserFirestore::class.java)
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
                        val user = snapshot.toObject(UserFirestore::class.java)
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
     * Busca usuário por CPF ou CNPJ
     * @param document CPF ou CNPJ (com ou sem formatação)
     * @return UserFirestore se encontrado, null caso contrário
     */
    suspend fun getUserByDocument(document: String): UserFirestore? {
        return try {
            // Remove formatação do documento
            val cleanDocument = document.replace(Regex("[^0-9]"), "")
            android.util.Log.d("FirestoreUserRepository", "Buscando usuário por documento: $cleanDocument")
            
            // Buscar por CPF
            val cpfQuery = usersCollection
                .whereEqualTo("cpf", cleanDocument)
                .limit(1)
                .get()
                .await()
            
            if (!cpfQuery.isEmpty) {
                val user = cpfQuery.documents[0].toObject(UserFirestore::class.java)
                android.util.Log.d("FirestoreUserRepository", "Usuário encontrado por CPF: ${user?.email}")
                return user
            }
            
            // Buscar por CNPJ
            val cnpjQuery = usersCollection
                .whereEqualTo("cnpj", cleanDocument)
                .limit(1)
                .get()
                .await()
            
            if (!cnpjQuery.isEmpty) {
                val user = cnpjQuery.documents[0].toObject(UserFirestore::class.java)
                android.util.Log.d("FirestoreUserRepository", "Usuário encontrado por CNPJ: ${user?.email}")
                return user
            }
            
            android.util.Log.d("FirestoreUserRepository", "Usuário não encontrado por documento: $cleanDocument")
            null
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserRepository", "Erro ao buscar usuário por documento: ${e.message}", e)
            null
        }
    }

    suspend fun updateUser(user: UserFirestore): Result<Unit> {
        return try {
            // Validar que uid não está vazio
            if (user.uid.isBlank()) {
                android.util.Log.e("FirestoreUserRepository", "Erro: uid está vazio ao tentar salvar usuário")
                return Result.failure(Exception("UID não pode estar vazio"))
            }
            
            android.util.Log.d("FirestoreUserRepository", "Salvando usuário no Firestore: uid=${user.uid}, email=${user.email}, displayName=${user.displayName}")
            
            // Converter UserFirestore para Map, tratando Date corretamente
            val dataMap = mutableMapOf<String, Any?>(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoURL" to user.photoURL,
                "phone" to user.phone,
                "role" to user.role,
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
        return updateField(uid, "role", "provider")
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





