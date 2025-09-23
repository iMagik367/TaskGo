package br.com.taskgo.core.data.repository.auth

import android.content.Intent
import android.content.IntentSender
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

class AuthRepository @Inject constructor(
    private val oneTapClient: SignInClient,
    private val loginManager: LoginManager,
    private val callbackManager: CallbackManager,
    private val authService: AuthService
) {
    // Google Sign In
    suspend fun signInWithGoogle(): IntentSender? {
        return try {
            val result = oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
            result.pendingIntent.intentSender
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Facebook Sign In
    suspend fun signInWithFacebook(): Flow<SignInResult> = callbackFlow {
        loginManager.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // Fazer login na API com o token do Facebook
                    viewModelScope.launch {
                        try {
                            val request = LoginRequest(
                                email = "",  // O email será obtido do token no backend
                                token = result.accessToken.token,
                                provider = "facebook"
                            )
                            
                            val response = authService.login(request)
                            
                            trySend(
                                SignInResult(
                                    data = UserData(
                                        userId = response.user.id,
                                        email = response.user.email,
                                        name = response.user.name,
                                        profilePictureUrl = response.user.profilePicture,
                                        provider = AuthProvider.FACEBOOK
                                    ),
                                    errorMessage = null
                                )
                            )
                        } catch (e: Exception) {
                            trySend(SignInResult(null, e.message ?: "Erro ao fazer login com Facebook"))
                        }
                    }
                }

                override fun onCancel() {
                    trySend(SignInResult(null, "Login cancelado"))
                }

                override fun onError(error: FacebookException) {
                    trySend(SignInResult(null, error.message))
                }
            })

        loginManager.logInWithReadPermissions(null, listOf("public_profile", "email"))
        
        awaitClose {
            loginManager.unregisterCallback(callbackManager)
        }
    }

    // Email/Password Sign In
    suspend fun signInWithEmailPassword(email: String, password: String): SignInResult {
        return try {
            val request = LoginRequest(email = email, password = password)
            val response = authService.login(request)
            
            SignInResult(
                data = UserData(
                    userId = response.user.id,
                    email = response.user.email,
                    name = response.user.name,
                    profilePictureUrl = response.user.profilePicture,
                    provider = AuthProvider.EMAIL
                ),
                errorMessage = null
            )
        } catch (e: Exception) {
            SignInResult(
                data = null,
                errorMessage = e.message ?: "Erro ao fazer login"
            )
        }
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            loginManager.logOut()
            // TODO: Implementar logout da sua API se necessário
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getGoogleSignInResultFromIntent(intent: Intent): SignInResult {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleEmail = credential.id
            
            // Fazer login na API com o token do Google
            val request = LoginRequest(
                email = googleEmail ?: "",
                token = googleIdToken,
                provider = "google"
            )
            
            val response = authService.login(request)
            
            return SignInResult(
                data = UserData(
                    userId = response.user.id,
                    email = response.user.email,
                    name = response.user.name,
                    profilePictureUrl = response.user.profilePicture,
                    provider = AuthProvider.GOOGLE
                ),
                errorMessage = null
            )
        } catch (e: Exception) {
            return SignInResult(
                data = null,
                errorMessage = e.message ?: "Erro ao fazer login com Google"
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(CLIENT_ID)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }

    companion object {
        // Substitua pelo seu Client ID gerado no Google Cloud Console
        private const val CLIENT_ID = "154959127714-mvime4hhraia9s2eldrtifsv0a2hb51d.apps.googleusercontent.com"
    }
}

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)

data class UserData(
    val userId: String,
    val email: String,
    val name: String,
    val profilePictureUrl: String?,
    val provider: AuthProvider
)

enum class AuthProvider {
    GOOGLE,
    FACEBOOK,
    EMAIL
}