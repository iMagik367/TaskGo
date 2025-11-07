package com.taskgoapp.taskgo.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInHelper(context: Context) {
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("154959127714-mvime4hhraia9s2eldrtifsv0a2hb51d.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

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
        googleSignInClient.signOut()
    }
}

