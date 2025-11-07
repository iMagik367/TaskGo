package com.taskgoapp.taskgo.core.data.models

import java.time.LocalDateTime

// User Models
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val accountType: AccountType,
    val timeOnTaskGo: String, // ex: "2 anos"
    val rating: Double,
    val reviewCount: Int,
    val city: String,
    val avatarUrl: String? = null
)

enum class AccountType {
    PROVIDER, SELLER, CLIENT
}

// Service Models
data class ServiceCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val description: String
)

data class Provider(
    val id: Long,
    val name: String,
    val profession: String,
    val rating: Double,
    val reviewCount: Int,
    val serviceCount: String, // ex: "100+ serviços"
    val avatarUrl: String? = null,
    val city: String
)

data class Service(
    val id: Long,
    val title: String,
    val description: String,
    val category: ServiceCategory,
    val provider: Provider,
    val price: Double,
    val location: String,
    val createdAt: LocalDateTime
)

data class WorkOrder(
    val id: Long,
    val category: ServiceCategory,
    val description: String,
    val date: LocalDateTime,
    val address: String,
    val city: String,
    val state: String,
    val status: WorkOrderStatus,
    val createdAt: LocalDateTime
)

enum class WorkOrderStatus {
    OPEN, IN_PROGRESS, COMPLETED, CANCELLED
}

data class Proposal(
    val id: Long,
    val workOrderId: Long,
    val provider: Provider,
    val title: String,
    val description: String,
    val price: Double,
    val date: LocalDateTime,
    val location: String,
    val status: ProposalStatus
)

enum class ProposalStatus {
    PENDING, ACCEPTED, REJECTED
}

data class Review(
    val id: Long,
    val rating: Double,
    val comment: String,
    val date: LocalDateTime,
    val reviewer: User,
    val provider: Provider
)

// Product Models
data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val seller: User? = null,
    val imageUrl: String? = null,
    val category: String,
    val inStock: Boolean = true
)

data class CartItem(
    val id: Long,
    val product: Product,
    val quantity: Int
)

data class Order(
    val id: Long,
    val items: List<CartItem>,
    val total: Double,
    val shipping: Double,
    val status: OrderStatus,
    val trackingCode: String? = null,
    val createdAt: LocalDateTime,
    val estimatedDelivery: LocalDateTime? = null
)

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
}

data class TrackingEvent(
    val id: Long,
    val orderId: Long,
    val status: OrderStatus,
    val description: String,
    val date: LocalDateTime,
    val location: String? = null
)

// Payment Models
data class PaymentMethod(
    val id: Long,
    val type: PaymentType,
    val lastFourDigits: String,
    val cardholderName: String,
    val expiryDate: String,
    val isDefault: Boolean = false
)

enum class PaymentType {
    CREDIT_CARD, DEBIT_CARD, PIX
}

data class Address(
    val id: Long,
    val name: String,
    val phone: String,
    val cep: String,
    val street: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val isDefault: Boolean = false
)

// Message Models
data class MessageThread(
    val id: Long,
    val title: String,
    val preview: String,
    val lastMessageTime: LocalDateTime,
    val unreadCount: Int = 0,
    val participants: List<User>
)

data class ChatMessage(
    val id: Long,
    val threadId: Long,
    val sender: User,
    val content: String,
    val timestamp: LocalDateTime,
    val isMine: Boolean
)

// Notification Models
data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val date: LocalDateTime,
    val isRead: Boolean = false,
    val actionRoute: String? = null
)

enum class NotificationType {
    ORDER_SHIPPED, PROPOSAL_APPROVED, NEW_MESSAGE, UPDATE_AVAILABLE, ORDER_PUBLISHED
}

// Plan Models
data class Plan(
    val id: Long,
    val name: String,
    val price: Double,
    val description: String,
    val features: List<String>
)

// Banner/Ads Models
data class Banner(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val duration: String, // ex: "R$50/dia"
    val type: BannerType
)

enum class BannerType {
    SMALL, LARGE
}
