package br.com.taskgo.taskgo.core.data.remote

import retrofit2.http.*

data class SocialLoginRequest(val provider: String, val idToken: String)
data class SocialLoginResponse(val token: String)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class ChangePasswordResponse(val message: String)

data class SignupRequest(val email: String, val password: String, val role: String? = null)
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

data class ProductsResponse(
    val items: List<ProductDto>,
    val page: Int,
    val size: Int
)

data class AddCartItemRequest(val productId: Long, val quantity: Int)
data class CartItemDto(val productId: Long, val quantity: Int)
data class CartDto(val userEmail: String, val items: List<CartItemDto>)

data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val message: String)

interface AuthApi {
    @POST("auth/signup") suspend fun signup(@Body body: SignupRequest): AuthResponse
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ForgotPasswordResponse
    @POST("auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): ChangePasswordResponse
    @POST("oauth/login") suspend fun socialLogin(@Body body: SocialLoginRequest): SocialLoginResponse
}

interface ProductsApi {
    @GET("products") suspend fun list(@Query("search") search: String? = null, @Query("category") category: String? = null, @Query("page") page: Int = 1, @Query("size") size: Int = 20): ProductsResponse
    @GET("products/{id}") suspend fun getById(@Path("id") id: Long): ProductDto
}

interface CartApi {
    @GET("cart") suspend fun getCart(): CartDto
    @POST("cart") suspend fun add(@Body body: AddCartItemRequest): CartDto
    @DELETE("cart/{productId}") suspend fun remove(@Path("productId") productId: Long): CartDto
    @POST("cart/checkout") suspend fun checkout(): Any
}

data class OrderItemDto(val productId: Long, val quantity: Int, val price: Double)
data class OrderDto(
    val id: Long,
    val userEmail: String,
    val items: List<OrderItemDto>,
    val total: Double,
    val status: String,
    val createdAt: String
)
data class OrdersResponse(val items: List<OrderDto>)

interface OrdersApi {
    @GET("orders") suspend fun list(): OrdersResponse
}


