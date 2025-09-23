package br.com.taskgo.core.data.remote.service

import br.com.taskgo.core.data.remote.model.LoginRequest
import br.com.taskgo.core.data.remote.model.LoginResponse
import br.com.taskgo.core.data.remote.model.RefreshTokenRequest
import br.com.taskgo.core.data.remote.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): LoginResponse
}