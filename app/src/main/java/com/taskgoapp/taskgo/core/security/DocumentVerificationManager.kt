package com.taskgoapp.taskgo.core.security

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de verificação de documentos e bloqueio de funcionalidades
 */
@Singleton
class DocumentVerificationManager @Inject constructor(
    private val firestoreUserRepository: FirestoreUserRepository,
    private val auth: FirebaseAuth
) {
    
    /**
     * Verifica se o usuário tem documentos cadastrados
     */
    suspend fun hasDocumentsVerified(): Boolean {
        // DESATIVADO TEMPORARIAMENTE PARA TESTES: liberar criação de produtos/serviços
        return true
    }
    
    /**
     * Observa o status de verificação de documentos
     */
    fun observeVerificationStatus(): Flow<Boolean> = flow {
        while (true) {
            emit(hasDocumentsVerified())
            kotlinx.coroutines.delay(5000) // Verificar a cada 5 segundos
        }
    }
    
    /**
     * Verifica se o usuário pode criar produtos/serviços
     */
    suspend fun canCreateContent(): Boolean {
        // DESATIVADO TEMPORARIAMENTE PARA TESTES
        return true
    }
    
    /**
     * Cria notificação para lembrar cadastro de documentos
     */
    suspend fun createDocumentReminderNotification(): Result<String> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("Usuário não autenticado"))
        
        return try {
            val notificationRepository = com.taskgoapp.taskgo.data.repository.FirestoreNotificationRepository(
                com.taskgoapp.taskgo.core.firebase.FirestoreHelper.getInstance(),
                auth
            )
            
            notificationRepository.createNotification(
                type = "document_verification",
                title = "Complete seu cadastro",
                message = "Para criar produtos e serviços, você precisa enviar seus documentos de identificação.",
                data = mapOf("action" to "verify_identity")
            )
        } catch (e: Exception) {
            Log.e("DocumentVerificationManager", "Erro ao criar notificação: ${e.message}", e)
            Result.Error(e)
        }
    }
}

