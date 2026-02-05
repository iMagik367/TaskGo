package com.taskgoapp.taskgo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.taskgoapp.taskgo.data.firestore.models.BankAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBankAccountRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    
    /**
     * Observa todas as contas bancárias do usuário atual
     * CRÍTICO: Dados privados - salva em users/{userId}/bank_accounts (não em locations)
     */
    fun observeUserBankAccounts(): Flow<List<BankAccount>> = callbackFlow {
        // CRÍTICO: Sempre obter userId do usuário autenticado
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.w("BankAccountRepo", "Usuário não autenticado ao observar contas bancárias")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val authenticatedUserId = currentUser.uid
        
        // Dados privados: usar users/{userId}/bank_accounts (não locations)
        val collection = firestore.collection("users")
            .document(authenticatedUserId)
            .collection("bank_accounts")
        
        Log.d("BankAccountRepo", "📍 Observando contas bancárias do usuário: $authenticatedUserId em users/$authenticatedUserId/bank_accounts")
        
        val listener = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BankAccountRepo", "Erro ao observar contas bancárias: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val accounts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val account = doc.toBankAccount(doc.id)
                        // Validação adicional: garantir que a conta pertence ao usuário autenticado
                        if (account.userId != authenticatedUserId) {
                            Log.w("BankAccountRepo", "Conta bancária ${doc.id} pertence a outro usuário (${account.userId} != $authenticatedUserId), ignorando")
                            null
                        } else {
                            account
                        }
                    } catch (e: Exception) {
                        Log.e("BankAccountRepo", "Erro ao converter conta bancária: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d("BankAccountRepo", "✅ Contas bancárias observadas: ${accounts.size} em users/$authenticatedUserId/bank_accounts")
                trySend(accounts)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Obtém uma conta bancária por ID
     * CRÍTICO: Dados privados - busca em users/{userId}/bank_accounts (não em locations)
     */
    suspend fun getBankAccount(accountId: String): BankAccount? {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.w("BankAccountRepo", "Usuário não autenticado")
                return null
            }
            
            // Dados privados: usar users/{userId}/bank_accounts (não locations)
            val collection = firestore.collection("users")
                .document(currentUser.uid)
                .collection("bank_accounts")
            
            Log.d("BankAccountRepo", "📍 Buscando conta bancária: $accountId em users/${currentUser.uid}/bank_accounts")
            
            val doc = collection.document(accountId).get().await()
            if (doc.exists()) {
                val account = doc.toBankAccount(doc.id)
                Log.d("BankAccountRepo", "✅ Conta bancária encontrada: $accountId")
                account
            } else {
                Log.w("BankAccountRepo", "Conta bancária não encontrada: $accountId")
                null
            }
        } catch (e: Exception) {
            Log.e("BankAccountRepo", "Erro ao obter conta bancária: ${e.message}", e)
            null
        }
    }
    
    /**
     * Cria ou atualiza uma conta bancária
     * CRÍTICO: Dados privados - salva em users/{userId}/bank_accounts (não em locations)
     */
    suspend fun saveBankAccount(account: BankAccount): Result<String> {
        return try {
            // CRÍTICO: Sempre obter userId do usuário autenticado
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.e("BankAccountRepo", "Usuário não autenticado")
                return Result.failure(Exception("Usuário não autenticado"))
            }
            
            // Usar sempre o userId do usuário autenticado para garantir permissões corretas
            val authenticatedUserId = currentUser.uid
            
            // Dados privados: usar users/{userId}/bank_accounts (não locations)
            val collection = firestore.collection("users")
                .document(authenticatedUserId)
                .collection("bank_accounts")
            
            // Se estiver editando, validar que a conta pertence ao usuário autenticado
            if (account.id.isNotBlank()) {
                val existingDoc = collection.document(account.id).get().await()
                if (!existingDoc.exists()) {
                    Log.e("BankAccountRepo", "Conta bancária não encontrada: ${account.id}")
                    return Result.failure(Exception("Conta bancária não encontrada"))
                }
                val existingAccount = existingDoc.toBankAccount(existingDoc.id)
                if (existingAccount.userId != authenticatedUserId) {
                    Log.e("BankAccountRepo", "Permissão negada: conta pertence a outro usuário (${existingAccount.userId} != $authenticatedUserId)")
                    return Result.failure(Exception("Permissão negada: você não pode editar esta conta bancária"))
                }
            }
            
            // Garantir que o userId sempre seja o do usuário autenticado
            val accountToSave = account.copy(
                userId = authenticatedUserId
            )
            val accountData = accountToSave.toMap().toMutableMap()
            
            Log.d("BankAccountRepo", "📍 Salvando conta bancária em users/$authenticatedUserId/bank_accounts")
            
            val docRef = if (account.id.isBlank()) {
                // Nova conta - criar novo documento
                collection.document()
            } else {
                // Editar conta existente - usar o ID existente
                collection.document(account.id)
            }
            
            if (account.id.isBlank()) {
                accountData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            }
            
            Log.d("BankAccountRepo", "Salvando conta bancária - userId: $authenticatedUserId, accountId: ${docRef.id}, isNew: ${account.id.isBlank()}, path: users/$authenticatedUserId/bank_accounts/${docRef.id}")
            
            docRef.set(accountData, SetOptions.merge()).await()
            
            Log.d("BankAccountRepo", "✅ Conta bancária salva com sucesso: ${docRef.id} em users/$authenticatedUserId/bank_accounts")
            
            // Se esta é a conta padrão, remover padrão das outras
            if (account.isDefault) {
                val otherDefaultAccounts = collection
                    .whereEqualTo("isDefault", true)
                    .get()
                    .await()
                    .documents
                    .filter { it.id != docRef.id }
                
                if (otherDefaultAccounts.isNotEmpty()) {
                    Log.d("BankAccountRepo", "Removendo padrão de ${otherDefaultAccounts.size} outras contas")
                    otherDefaultAccounts.forEach { doc ->
                        doc.reference.update("isDefault", false).await()
                    }
                }
            }
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("BankAccountRepo", "Erro ao salvar conta bancária: ${e.message}", e)
            Log.e("BankAccountRepo", "Stack trace:", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deleta uma conta bancária
     * CRÍTICO: Dados privados - deleta de users/{userId}/bank_accounts (não de locations)
     */
    suspend fun deleteBankAccount(accountId: String): Result<Unit> {
        return try {
            // CRÍTICO: Sempre obter userId do usuário autenticado
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.e("BankAccountRepo", "Usuário não autenticado ao deletar conta")
                return Result.failure(Exception("Usuário não autenticado"))
            }
            
            val authenticatedUserId = currentUser.uid
            
            val account = getBankAccount(accountId)
            if (account == null) {
                Log.e("BankAccountRepo", "Conta bancária não encontrada: $accountId")
                return Result.failure(Exception("Conta bancária não encontrada"))
            }
            
            if (account.userId != authenticatedUserId) {
                Log.e("BankAccountRepo", "Permissão negada: conta pertence a outro usuário (${account.userId} != $authenticatedUserId)")
                return Result.failure(Exception("Sem permissão para deletar esta conta"))
            }
            
            // Dados privados: usar users/{userId}/bank_accounts (não locations)
            val collection = firestore.collection("users")
                .document(authenticatedUserId)
                .collection("bank_accounts")
            
            Log.d("BankAccountRepo", "📍 Deletando conta bancária: $accountId de users/$authenticatedUserId/bank_accounts")
            collection.document(accountId).delete().await()
            Log.d("BankAccountRepo", "✅ Conta bancária deletada com sucesso: $accountId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BankAccountRepo", "Erro ao deletar conta bancária: ${e.message}", e)
            Log.e("BankAccountRepo", "Stack trace:", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtém a conta bancária padrão do usuário
     * CRÍTICO: Dados privados - busca em users/{userId}/bank_accounts (não em locations)
     */
    suspend fun getDefaultBankAccount(): BankAccount? {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.w("BankAccountRepo", "Usuário não autenticado")
                return null
            }
            
            // Dados privados: usar users/{userId}/bank_accounts (não locations)
            val collection = firestore.collection("users")
                .document(currentUser.uid)
                .collection("bank_accounts")
            
            Log.d("BankAccountRepo", "📍 Buscando conta bancária padrão em users/${currentUser.uid}/bank_accounts")
            
            val snapshot = collection
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .await()
            
            val account = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toBankAccount(doc.id)
            }
            
            if (account != null) {
                Log.d("BankAccountRepo", "✅ Conta bancária padrão encontrada: ${account.id}")
            } else {
                Log.d("BankAccountRepo", "Nenhuma conta bancária padrão encontrada")
            }
            
            account
        } catch (e: Exception) {
            Log.e("BankAccountRepo", "Erro ao obter conta padrão: ${e.message}", e)
            null
        }
    }
}

// Extension functions para conversão
private fun com.google.firebase.firestore.DocumentSnapshot.toBankAccount(id: String): BankAccount {
    return BankAccount(
        id = id,
        userId = getString("userId") ?: "",
        bankName = getString("bankName") ?: "",
        bankCode = getString("bankCode") ?: "",
        agency = getString("agency") ?: "",
        account = getString("account") ?: "",
        accountType = getString("accountType") ?: "",
        accountHolderName = getString("accountHolderName") ?: "",
        accountHolderDocument = getString("accountHolderDocument") ?: "",
        accountHolderDocumentType = getString("accountHolderDocumentType") ?: "",
        stripeAccountId = getString("stripeAccountId"),
        isDefault = getBoolean("isDefault") ?: false,
        isVerified = getBoolean("isVerified") ?: false,
        createdAt = getDate("createdAt"),
        updatedAt = getDate("updatedAt")
    )
}

private fun BankAccount.toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "bankName" to bankName,
        "bankCode" to bankCode,
        "agency" to agency,
        "account" to account,
        "accountType" to accountType,
        "accountHolderName" to accountHolderName,
        "accountHolderDocument" to accountHolderDocument,
        "accountHolderDocumentType" to accountHolderDocumentType,
        "stripeAccountId" to stripeAccountId,
        "isDefault" to isDefault,
        "isVerified" to isVerified,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}

