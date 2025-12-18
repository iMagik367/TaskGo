package com.taskgoapp.taskgo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.data.firestore.models.BankAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBankAccountRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    
    private val collection = firestore.collection("bank_accounts")
    
    /**
     * Observa todas as contas bancárias do usuário atual
     */
    fun observeUserBankAccounts(): Flow<List<BankAccount>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BankAccountRepo", "Erro ao observar contas bancárias: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val accounts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toBankAccount(doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("BankAccountRepo", "Erro ao converter conta bancária: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(accounts)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Obtém uma conta bancária por ID
     */
    suspend fun getBankAccount(accountId: String): BankAccount? {
        return try {
            val doc = collection.document(accountId).get().await()
            if (doc.exists()) {
                doc.toBankAccount(doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("BankAccountRepo", "Erro ao obter conta bancária: ${e.message}", e)
            null
        }
    }
    
    /**
     * Cria ou atualiza uma conta bancária
     */
    suspend fun saveBankAccount(account: BankAccount): Result<String> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }
            
            val accountToSave = account.copy(
                userId = currentUser.uid,
                updatedAt = Date()
            )
            val accountData = accountToSave.toMap().toMutableMap()
            
            val docRef = if (account.id.isBlank()) {
                collection.document()
            } else {
                collection.document(account.id)
            }
            
            if (account.id.isBlank()) {
                accountData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            }
            
            docRef.set(accountData).await()
            
            // Se esta é a conta padrão, remover padrão das outras
            if (account.isDefault) {
                collection
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("isDefault", true)
                    .get()
                    .await()
                    .documents
                    .filter { it.id != docRef.id }
                    .forEach { it.reference.update("isDefault", false).await() }
            }
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("BankAccountRepo", "Erro ao salvar conta bancária: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deleta uma conta bancária
     */
    suspend fun deleteBankAccount(accountId: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }
            
            val account = getBankAccount(accountId)
            if (account?.userId != currentUser.uid) {
                return Result.failure(Exception("Sem permissão para deletar esta conta"))
            }
            
            collection.document(accountId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BankAccountRepo", "Erro ao deletar conta bancária: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtém a conta bancária padrão do usuário
     */
    suspend fun getDefaultBankAccount(): BankAccount? {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) return null
            
            val snapshot = collection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toBankAccount(doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("BankAccountRepo", "Erro ao obter conta padrão: ${e.message}", e)
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

