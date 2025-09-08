package com.example.taskgoapp.core.data

import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant

// User Models
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val city: String = "",
    val accountType: AccountType = AccountType.CLIENT,
    val timeOnAppYears: Int = 0,
    val rating: Float = 0f,
    val avatarUrl: String? = null,
    val avatar: String? = null,
    val isProvider: Boolean = false
)

enum class AccountType {
    CLIENT, PROVIDER, SELLER
}

// Service Models
data class ServiceCategory(
    val id: Long,
    val name: String,
    val icon: ImageVector? = null
)

data class Provider(
    val id: Long,
    val name: String,
    val role: String,
    val rating: Float,
    val reviewsCount: Int,
    val services: List<String> = emptyList(),
    val city: String = "",
    val avatarUrl: String? = null
)

data class Service(
    val id: Long,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val provider: Provider,
    val price: Double,
    val priceType: PriceType,
    val rating: Float = 0f,
    val reviewsCount: Int = 0
)

enum class PriceType {
    FIXED, HOURLY, NEGOTIABLE
}

// Product Models
data class Product(
    val id: Long,
    val title: String,
    val price: Double,
    val description: String,
    val seller: Provider,
    val category: String,
    val inStock: Boolean = true,
    val imageUrl: String? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

// Order Models
data class Order(
    val id: Long,
    val items: List<CartItem>,
    val total: Double,
    val status: OrderStatus,
    val tracking: List<TrackingEvent> = emptyList(),
    val purchaseDate: Long = System.currentTimeMillis(),
    val deliveryAddress: Address? = null,
    val paymentMethod: PaymentMethod? = null
)

enum class OrderStatus {
    PENDING, CONFIRMED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
}

data class TrackingEvent(
    val status: String,
    val dateTime: Long,
    val note: String? = null
)

// Proposal Models
data class Proposal(
    val id: Long,
    val requesterName: String,
    val title: String,
    val date: Long,
    val location: String,
    val budget: Double,
    val provider: Provider,
    val rating: Float,
    val description: String,
    val status: ProposalStatus
)

enum class ProposalStatus {
    PENDING, ACCEPTED, REJECTED, COMPLETED
}

// Payment Models
data class PaymentMethod(
    val type: PaymentType,
    val masked: String,
    val holder: String
)

enum class PaymentType {
    PIX, CREDIT_CARD, DEBIT_CARD
}

// Address Models
data class Address(
    val name: String,
    val phone: String,
    val cep: String,
    val street: String,
    val district: String,
    val city: String,
    val state: String
)

// Notification Models
data class NotificationItem(
    val id: Long,
    val title: String,
    val snippet: String,
    val dateLabel: String,
    val type: NotificationType
)

// Notification types
enum class NotificationType {
    ORDER_UPDATE,
    PROPOSAL_UPDATE,
    NEW_MESSAGE,
    SYSTEM_UPDATE
}

// Chat message
data class ChatMessage(
    val id: Long,
    val threadId: Long,
    val author: String,
    val text: String,
    val time: String,
    val content: String,
    val timestamp: Long
)

// Message Models
data class MessageThread(
    val id: Long,
    val title: String,
    val lastPreview: String,
    val lastTime: Long,
    val unreadCount: Int = 0
)

// Conversation Models
data class Conversation(
    val id: Long,
    val participant: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

// Service Provider Models
data class ServiceProvider(
    val id: Long,
    val name: String,
    val role: String,
    val rating: Float,
    val reviewsCount: Int,
    val hourlyRate: Double,
    val description: String,
    val categories: List<String>
)

// Plan Models
data class Plan(
    val name: String,
    val pricePerMonth: Double
)

// Banner Models
data class Banner(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val type: BannerType
)

enum class BannerType {
    SMALL, LARGE
}

// Review Models
data class Review(
    val id: Long,
    val rating: Float,
    val comment: String,
    val author: User,
    val target: User,
    val date: Long = System.currentTimeMillis()
)

// User type
enum class UserType {
    BUYER,
    SELLER,
    BOTH
}
