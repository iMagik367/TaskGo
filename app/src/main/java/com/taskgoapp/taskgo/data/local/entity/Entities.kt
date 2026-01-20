package com.taskgoapp.taskgo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val city: String?,
    val state: String? = null, // Estado (UF) - adicionado para suporte a arquitetura regional
    val profession: String?,
    val accountType: String,
    val rating: Double?,
    val avatarUri: String?,
    val profileImages: String? // JSON string com lista de URIs
)

@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    val description: String?,
    val sellerId: String? = null,
    val sellerName: String?,
    val rating: Double? = null, // Avaliação média do produto
    val latitude: Double? = null, // Latitude da localização do produto
    val longitude: Double? = null, // Longitude da localização do produto
    val featured: Boolean = false, // Produto em destaque
    val discountPercentage: Double? = null, // Percentual de desconto para destaque/promoções
    val active: Boolean = true // Controle de soft-delete
)

@Entity(tableName = "product_image")
data class ProductImageEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val productId: String,
    val uri: String
)

@Entity(tableName = "marketplace_product")
data class MarketplaceProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val sellerId: String,
    val sellerName: String,
    val sellerEmail: String,
    val sellerPhone: String,
    val sellerCity: String,
    val sellerRating: Double,
    val sellerReviewCount: Int,
    val category: String,
    val imageUrl: String? = null,
    val inStock: Boolean = true
)

@Entity(tableName = "service_order")
data class ServiceOrderEntity(
    @PrimaryKey val id: String,
    val category: String,
    val description: String,
    val date: Long,
    val addressLine: String,
    val city: String,
    val state: String
)

@Entity(tableName = "proposal")
data class ProposalEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val providerName: String,
    val rating: Double,
    val amount: Double,
    val message: String,
    val scheduledDate: Long,
    val address: String,
    val accepted: Boolean = false
)

@Entity(tableName = "cart_item", primaryKeys = ["productId"])
data class CartItemEntity(
    val productId: String,
    val qty: Int
)

@Entity(tableName = "purchase_order")
data class PurchaseOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String?,
    val createdAt: Long,
    val total: Double,
    val subtotal: Double?,
    val deliveryFee: Double?,
    val status: String,
    val paymentMethod: String,
    val trackingCode: String?,
    val deliveryAddress: String?
)

@Entity(tableName = "purchase_order_item", primaryKeys = ["orderId","productId"])
data class PurchaseOrderItemEntity(
    val orderId: String,
    val productId: String,
    val productName: String?,
    val productImage: String?,
    val price: Double,
    val qty: Int,
    val deliveryDate: String?
)

@Entity(tableName = "tracking_event")
data class TrackingEventEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val label: String,
    val date: Long,
    val done: Boolean
)

@Entity(tableName = "message_thread")
data class MessageThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val lastTime: Long
)

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val senderMe: Boolean,
    val text: String,
    val time: Long
)

@Entity(tableName = "address")
data class AddressEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val cep: String,
    val street: String,
    val district: String,
    val city: String,
    val state: String
)

@Entity(tableName = "card")
data class CardEntity(
    @PrimaryKey val id: String,
    val holder: String,
    val numberMasked: String,
    val brand: String,
    val expMonth: Int,
    val expYear: Int,
    val type: String
)
