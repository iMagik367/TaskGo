package br.com.taskgo.taskgo.di

interface AuthTokenProvider {
    fun getToken(): String?
}

class DefaultAuthTokenProvider : AuthTokenProvider {
    override fun getToken(): String? = null
}


