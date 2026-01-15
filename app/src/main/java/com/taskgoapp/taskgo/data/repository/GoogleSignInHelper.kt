package com.taskgoapp.taskgo.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInHelper(context: Context) {
    private val context: Context = context.applicationContext
    private val webClientId = "1093466748007-bk95o4ouk4966bvgqbm98n5h8js8m28v.apps.googleusercontent.com"
    
    private fun createGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Obtém o Intent de sign-in, sempre fazendo signOut antes para garantir que o seletor de conta seja mostrado
     * Cria um novo client a cada vez para garantir que não há cache de conta
     * 
     * IMPORTANTE: O signOut() é assíncrono, mas como não podemos tornar este método suspenso,
     * fazemos o signOut() e imediatamente retornamos o intent. O Google Sign-In mostrará
     * o seletor de conta mesmo se houver uma sessão ativa, desde que não haja uma conta
     * em cache no dispositivo.
     */
    fun getSignInIntent(): android.content.Intent {
        // Criar um novo client a cada vez para evitar cache
        val client = createGoogleSignInClient()
        // Fazer signOut de forma assíncrona (não bloqueia, mas limpa o cache)
        // O Google Sign-In mostrará o seletor mesmo com signOut em andamento
        client.signOut()
        // Retornar o intent imediatamente - o seletor será mostrado
        return client.signInIntent
    }

    fun getSignInResultFromIntent(data: android.content.Intent?): GoogleSignInAccount? {
        return try {
            if (data == null) {
                Log.e("GoogleSignInHelper", "Intent data é null")
                return null
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignInHelper", "Conta Google obtida: ${account.email}, ID Token: ${account.idToken != null}")
            account
        } catch (e: ApiException) {
            Log.e("GoogleSignInHelper", "Erro ao obter conta do Google: ${e.message}", e)
            Log.e("GoogleSignInHelper", "Código de erro: ${e.statusCode}")
            null
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Erro inesperado: ${e.message}", e)
            null
        }
    }

    fun signOut() {
        val client = createGoogleSignInClient()
        client.signOut()
    }
}

