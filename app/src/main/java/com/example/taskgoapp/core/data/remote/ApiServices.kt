package com.example.taskgoapp.core.data.remote

import retrofit2.http.*

data class SignupRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String)

data class ProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val bannerUrl: String?
)

data class AddCartItemRequest(val productId: Long, val quantity: Int)
data class CartItemDto(val productId: Long, val quantity: Int)
data class CartDto(val userEmail: String, val items: List<CartItemDto>)

interface AuthApi {
    @POST("auth/signup") suspend fun signup(@Body body: SignupRequest): AuthResponse
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
}

interface ProductsApi {
    @GET("products") suspend fun list(@Query("search") search: String? = null, @Query("category") category: String? = null, @Query("page") page: Int = 1, @Query("size") size: Int = 20): List<ProductDto>
    @GET("products/{id}") suspend fun getById(@Path("id") id: Long): ProductDto
}

interface CartApi {
    @GET("cart") suspend fun getCart(): CartDto
    @POST("cart") suspend fun add(@Body body: AddCartItemRequest): CartDto
    @DELETE("cart/{productId}") suspend fun remove(@Path("productId") productId: Long): CartDto
    @POST("cart/checkout") suspend fun checkout(): Any
}


