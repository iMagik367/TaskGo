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
            android.util.Log.d("FirebaseAuthRepository", "Criando usuário com email: $email")
            android.util.Log.d("FirebaseAuthRepository", "FirebaseAuth instance: $firebaseAuth")
            android.util.Log.d("FirebaseAuthRepository", "FirebaseApp: ${com.google.firebase.FirebaseApp.getInstance().name}")
            
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            
            android.util.Log.d("FirebaseAuthRepository", "Usuário criado com sucesso: ${user.uid}, email: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "Erro ao criar usuário: ${e.message}", e)
            android.util.Log.e("FirebaseAuthRepository", "Tipo de erro: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            // Verificar se Firebase Auth está inicializado
            if (firebaseAuth.app == null) {
                android.util.Log.e("FirebaseAuthRepository", "❌ Firebase Auth não inicializado")
                return Result.failure(Exception("Firebase Auth não inicializado. Reinicie o app."))
            }
            
            android.util.Log.d("FirebaseAuthRepository", "=== INÍCIO LOGIN ===")
            android.util.Log.d("FirebaseAuthRepository", "Email: $email")
            android.util.Log.d("FirebaseAuthRepository", "BuildConfig.DEBUG: ${com.taskgoapp.taskgo.BuildConfig.DEBUG}")
            android.util.Log.d("FirebaseAuthRepository", "App Check Enabled: ${com.taskgoapp.taskgo.BuildConfig.FIREBASE_APP_CHECK_ENABLED}")
            
            // Diagnosticar conectividade em background (não bloqueia)
            val appContext = firebaseAuth.app.applicationContext
            try {
                val diagnostic = com.taskgoapp.taskgo.core.network.NetworkDiagnostic.diagnose(appContext)
                android.util.Log.d("FirebaseAuthRepository", "Diagnóstico de rede: Internet=${diagnostic.hasInternet}, Firebase=${diagnostic.canReachFirebase}")
            } catch (e: Exception) {
                android.util.Log.w("FirebaseAuthRepository", "Erro ao diagnosticar rede (continuando): ${e.message}")
            }
            
            android.util.Log.d("FirebaseAuthRepository", "Chamando signInWithEmailAndPassword...")
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            android.util.Log.d("FirebaseAuthRepository", "=== LOGIN BEM-SUCEDIDO ===")
            android.util.Log.d("FirebaseAuthRepository", "UID: ${result.user?.uid}")
            android.util.Log.d("FirebaseAuthRepository", "Email: ${result.user?.email}")
            android.util.Log.d("FirebaseAuthRepository", "Email Verified: ${result.user?.isEmailVerified}")
            Result.success(result.user ?: throw Exception("User is null"))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "=== ERRO NO LOGIN ===")
            android.util.Log.e("FirebaseAuthRepository", "Tipo: ${e.javaClass.name}")
            android.util.Log.e("FirebaseAuthRepository", "Mensagem: ${e.message}")
            android.util.Log.e("FirebaseAuthRepository", "Stack trace completo:", e)
            
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
                    android.util.Log.e("FirebaseAuthRepository", "=== FIREBASE AUTH EXCEPTION ===")
                    android.util.Log.e("FirebaseAuthRepository", "Error Code: ${e.errorCode}")
                    android.util.Log.e("FirebaseAuthRepository", "Error Message: ${e.message}")
                    android.util.Log.e("FirebaseAuthRepository", "Full Stack Trace:")
                    android.util.Log.e("FirebaseAuthRepository", fullStackTrace)
                    
                    // Verificar se é erro relacionado ao App Check
                    val isAppCheckError = errorMsg.contains("app-check", ignoreCase = true) || 
                        errorMsg.contains("App Check token is invalid", ignoreCase = true) ||
                        errorMsg.contains("app check", ignoreCase = true) ||
                        fullStackTrace.contains("appcheck", ignoreCase = true) ||
                        fullStackTrace.contains("AppCheck", ignoreCase = true)
                    
                    if (isAppCheckError) {
                        android.util.Log.e("FirebaseAuthRepository", "🔴 ERRO CRÍTICO: APP CHECK FALHANDO")
                        android.util.Log.e("FirebaseAuthRepository", "Causa: Token do App Check inválido ou não gerado")
                        android.util.Log.e("FirebaseAuthRepository", "Soluções:")
                        android.util.Log.e("FirebaseAuthRepository", "  1. Verificar SHA-256 cadastrado no Firebase Console")
                        android.util.Log.e("FirebaseAuthRepository", "  2. Verificar Play Integrity API habilitada no Google Cloud")
                        android.util.Log.e("FirebaseAuthRepository", "  3. Verificar App Check configurado no Firebase Console")
                        android.util.Log.e("FirebaseAuthRepository", "  4. Se app não está na Play Store, Play Integrity não funciona")
                    }
                    
                    // Verificar se é erro de API bloqueada
                    if (errorMsg.contains("403", ignoreCase = true) ||
                        errorMsg.contains("blocked", ignoreCase = true) ||
                        fullStackTrace.contains("API_KEY_SERVICE_BLOCKED", ignoreCase = true)) {
                        android.util.Log.e("FirebaseAuthRepository", "⚠️ ERRO: API KEY BLOQUEADA")
                        android.util.Log.e("FirebaseAuthRepository", "API Key: ${com.google.firebase.FirebaseApp.getInstance().options.apiKey}")
                    }
                }
                is java.net.UnknownHostException -> {
                    android.util.Log.e("FirebaseAuthRepository", "❌ Host desconhecido - problema de DNS ou conexão")
                    android.util.Log.e("FirebaseAuthRepository", "SOLUÇÃO:")
                    android.util.Log.e("FirebaseAuthRepository", "   1. Verifique sua conexão com a internet")
                    android.util.Log.e("FirebaseAuthRepository", "   2. Verifique as configurações de DNS do dispositivo")
                    android.util.Log.e("FirebaseAuthRepository", "   3. Verifique se há firewall ou proxy bloqueando")
                    android.util.Log.e("FirebaseAuthRepository", "   4. Tente reiniciar o dispositivo")
                }
                is java.net.ConnectException -> {
                    android.util.Log.e("FirebaseAuthRepository", "❌ Erro de conexão - não foi possível conectar ao servidor")
                    android.util.Log.e("FirebaseAuthRepository", "SOLUÇÃO:")
                    android.util.Log.e("FirebaseAuthRepository", "   1. Verifique sua conexão com a internet")
                    android.util.Log.e("FirebaseAuthRepository", "   2. Verifique se há firewall bloqueando")
                    android.util.Log.e("FirebaseAuthRepository", "   3. Verifique se o dispositivo está em uma rede corporativa/VPN")
                    android.util.Log.e("FirebaseAuthRepository", "   4. Consulte DIAGNOSTICO_CONECTIVIDADE.md para mais informações")
                }
                is java.net.SocketTimeoutException -> {
                    android.util.Log.e("FirebaseAuthRepository", "❌ Timeout de conexão")
                    android.util.Log.e("FirebaseAuthRepository", "SOLUÇÃO:")
                    android.util.Log.e("FirebaseAuthRepository", "   1. Verifique sua conexão com a internet")
                    android.util.Log.e("FirebaseAuthRepository", "   2. Verifique se há firewall bloqueando")
                    android.util.Log.e("FirebaseAuthRepository", "   3. Tente novamente em alguns instantes")
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
        // CRÍTICO: Limpar dados locais antes de fazer logout para evitar mistura de dados
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
            // Verificar se Firebase Auth está inicializado
            if (firebaseAuth.app == null) {
                android.util.Log.e("FirebaseAuthRepository", "❌ Firebase Auth não inicializado")
                return Result.failure(Exception("Firebase Auth não inicializado. Reinicie o app."))
            }
            
            android.util.Log.d("FirebaseAuthRepository", "Iniciando login com Google")
            
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null")
            
            android.util.Log.d("FirebaseAuthRepository", "✅ Login com Google bem-sucedido: ${user.uid}, email: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "❌ Erro ao fazer login com Google: ${e.message}", e)
            if (e is com.google.firebase.auth.FirebaseAuthException) {
                android.util.Log.e("FirebaseAuthRepository", "Código de erro Firebase: ${e.errorCode}")
            }
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





