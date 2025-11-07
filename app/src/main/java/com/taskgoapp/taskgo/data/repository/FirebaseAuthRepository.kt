package com.taskgoapp.taskgo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user ?: throw Exception("User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            android.util.Log.d("FirebaseAuthRepository", "Tentando fazer login para: $email")
            android.util.Log.d("FirebaseAuthRepository", "FirebaseAuth instance: $firebaseAuth")
            android.util.Log.d("FirebaseAuthRepository", "FirebaseApp: ${com.google.firebase.FirebaseApp.getInstance().name}")
            
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            android.util.Log.d("FirebaseAuthRepository", "Login bem-sucedido: ${result.user?.uid}")
            Result.success(result.user ?: throw Exception("User is null"))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "Erro ao fazer login: ${e.message}", e)
            android.util.Log.e("FirebaseAuthRepository", "Tipo de erro: ${e.javaClass.simpleName}")
            
            // Log mais detalhado do erro
            val errorMsg = e.message ?: ""
            val fullStackTrace = e.stackTraceToString()
            
            when (e) {
                is com.google.firebase.FirebaseNetworkException -> {
                    android.util.Log.e("FirebaseAuthRepository", "Erro de rede do Firebase")
                    
                    // Verificar se o erro está relacionado ao App Check ou API bloqueada
                    if (errorMsg.contains("app-check", ignoreCase = true) || 
                        errorMsg.contains("403", ignoreCase = true) ||
                        errorMsg.contains("API has not been used", ignoreCase = true) ||
                        errorMsg.contains("blocked", ignoreCase = true) ||
                        fullStackTrace.contains("API_KEY_SERVICE_BLOCKED", ignoreCase = true)) {
                        android.util.Log.e("FirebaseAuthRepository", "⚠️ ERRO RELACIONADO AO APP CHECK OU API KEY BLOQUEADA")
                        android.util.Log.e("FirebaseAuthRepository", "Possíveis causas:")
                        android.util.Log.e("FirebaseAuthRepository", "   1. APIs do Firebase não habilitadas no Google Cloud Console")
                        android.util.Log.e("FirebaseAuthRepository", "   2. API Key com restrições bloqueando as APIs necessárias")
                        android.util.Log.e("FirebaseAuthRepository", "Consulte CORRECAO_API_KEY_BLOQUEADA.md para resolver")
                        android.util.Log.e("FirebaseAuthRepository", "API Key: ${com.google.firebase.FirebaseApp.getInstance().options.apiKey}")
                    }
                }
                is com.google.firebase.auth.FirebaseAuthException -> {
                    android.util.Log.e("FirebaseAuthRepository", "Código de erro: ${e.errorCode}")
                    android.util.Log.e("FirebaseAuthRepository", "Mensagem de erro: ${e.message}")
                    
                    // Verificar se é erro relacionado ao App Check ou API bloqueada
                    if (errorMsg.contains("app-check", ignoreCase = true) || 
                        errorMsg.contains("403", ignoreCase = true) ||
                        errorMsg.contains("blocked", ignoreCase = true) ||
                        fullStackTrace.contains("API_KEY_SERVICE_BLOCKED", ignoreCase = true)) {
                        android.util.Log.e("FirebaseAuthRepository", "⚠️ ERRO RELACIONADO AO APP CHECK OU API KEY BLOQUEADA")
                        android.util.Log.e("FirebaseAuthRepository", "Consulte CORRECAO_API_KEY_BLOQUEADA.md para resolver")
                    }
                }
                is java.net.UnknownHostException -> {
                    android.util.Log.e("FirebaseAuthRepository", "Host desconhecido - problema de DNS ou conexão")
                }
                is java.net.ConnectException -> {
                    android.util.Log.e("FirebaseAuthRepository", "Erro de conexão - não foi possível conectar ao servidor")
                }
                is java.net.SocketTimeoutException -> {
                    android.util.Log.e("FirebaseAuthRepository", "Timeout de conexão")
                }
            }
            
            // Verificar se o erro pode estar relacionado ao App Check mesmo que não seja uma exceção específica
            val errorMessage = e.message ?: ""
            if (errorMessage.contains("network error", ignoreCase = true) && 
                errorMessage.contains("timeout", ignoreCase = true)) {
                android.util.Log.w("FirebaseAuthRepository", "⚠️ Este erro de rede pode estar relacionado ao App Check não configurado")
                android.util.Log.w("FirebaseAuthRepository", "Verifique se as APIs do Firebase estão habilitadas no Google Cloud Console")
            }
            
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(displayName: String, photoUrl: String? = null): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply {
                    if (photoUrl != null) {
                        setPhotoUri(android.net.Uri.parse(photoUrl))
                    }
                }
                .build()
            
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            user.updateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reauthenticate(password: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            
            val email = user.email ?: return Result.failure(Exception("User email is null"))
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            Result.success(result.user ?: throw Exception("User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}





