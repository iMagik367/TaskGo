package com.taskgoapp.taskgo.core.model

// User Models
enum class UserType {
    CLIENT,     // Cliente que contrata serviços
    PARTNER     // Parceiro - oferece serviços e produtos
}

enum class AccountType { 
    PARCEIRO,   // Parceiro - oferece serviços e produtos
    CLIENTE     // Cliente - contrata serviços e compra produtos
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val city: String?,
    val state: String? = null, // Estado (UF) - adicionado para suporte a arquitetura regional
    val profession: String?, // ex: "Montador de Móveis"
    val accountType: AccountType,
    val rating: Double?, // mostra em "Meus dados"
    val avatarUri: String?, // persistir URI (MediaStore) local
    val profileImages: List<String>? = null // múltiplas imagens de perfil
)

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val description: String?,
    val sellerId: String? = null, // ID do vendedor
    val sellerName: String?,
    val imageUris: List<String>, // 1..n imagens
    val category: String? = null, // Categoria do produto
    val rating: Double? = null, // Avaliação média do produto/vendedor
    val latitude: Double? = null, // Latitude da localização do produto
    val longitude: Double? = null, // Longitude da localização do produto
    val featured: Boolean? = false, // Produto em destaque/promocional
    val discountPercentage: Double? = null, // Percentual de desconto para destaque/promoções
    val active: Boolean = true // Controle de soft-delete
)

data class ServiceOrder(
    val id: String,
    val category: String,
    val description: String,
    val date: Long, // epoch millis
    val addressLine: String,
    val city: String,
    val state: String,
    val rating: Double? = null, // Avaliação média do prestador de serviço
    val latitude: Double? = null, // Latitude da localização do serviço
    val longitude: Double? = null, // Longitude da localização do serviço
    val featured: Boolean? = false // Serviço em destaque
)

data class Proposal(
    val id: String,
    val orderId: String,
    val providerName: String,
    val rating: Double,
    val amount: Double,
    val message: String,
    val scheduledDate: Long,
    val address: String,
    val accepted: Boolean = false
)

data class CartItem(
    val productId: String, 
    val qty: Int
)

data class OrderItem(
    val productId: String,
    val productTitle: String? = null,
    val quantity: Int,
    val price: Double
)

enum class OrderStatus { 
    EM_ANDAMENTO, 
    CONCLUIDO, 
    CANCELADO 
}


data class PurchaseOrder(
    val id: String,
    val orderNumber: String,
    val createdAt: Long,
    val total: Double,
    val subtotal: Double,
    val deliveryFee: Double,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val paymentMethod: String, // "Pix" | "Crédito" | "Débito"
    val trackingCode: String? = null,
    val deliveryAddress: String? = null
)

data class TrackingEvent(
    val label: String, 
    val date: Long, 
    val done: Boolean
)

data class MessageThread(
    val id: String,
    val title: String, // ex: "Pedro Amaral"
    val lastMessage: String,
    val lastTime: Long
)

data class ChatMessage(
    val id: String,
    val threadId: String,
    val senderMe: Boolean,
    val text: String,
    val time: Long
)

data class Address(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val cep: String = "",
    val street: String = "",
    val district: String = "",
    val city: String = "",
    val state: String = "",
    val number: String = "",
    val complement: String? = null,
    val neighborhood: String = "",
    val zipCode: String = "",
    val country: String = "Brasil"
) {
    fun getFullAddress(): String {
        return buildString {
            append(street)
            if (number.isNotEmpty()) append(", $number")
            complement?.let { append(" - $it") }
            append("\n${neighborhood.ifEmpty { district }}")
            append("\n$city - $state")
            append("\n${zipCode.ifEmpty { cep }}")
            if (country.isNotEmpty()) append("\n$country")
        }
    }
    
    fun isValid(): Boolean {
        return street.isNotEmpty() &&
               (number.isNotEmpty() || id.isNotEmpty()) &&
               (neighborhood.isNotEmpty() || district.isNotEmpty()) &&
               city.isNotEmpty() &&
               state.isNotEmpty() &&
               (zipCode.isNotEmpty() || cep.isNotEmpty())
    }
}

data class Card(
    val id: String,
    val holder: String,
    val numberMasked: String, // "**** **** **** 1234"
    val brand: String, // heurística
    val expMonth: Int,
    val expYear: Int,
    val type: String // "Crédito" | "Débito"
)

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

// Notification Models
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val date: Long,
    val isRead: Boolean = false,
    val actionRoute: String? = null
)

enum class NotificationType {
    ORDER_SHIPPED, PROPOSAL_APPROVED, NEW_MESSAGE, UPDATE_AVAILABLE, ORDER_PUBLISHED
}

// Review/Rating Models
enum class ReviewType {
    PRODUCT,      // Avaliação de produto
    SERVICE,      // Avaliação de serviço
    PARTNER       // Avaliação de parceiro (serviço ou produto)
}

data class Review(
    val id: String,
    val type: ReviewType,
    val targetId: String, // ID do produto, serviço ou prestador
    val reviewerId: String,
    val reviewerName: String,
    val reviewerAvatarUri: String? = null,
    val rating: Int, // 1-5 estrelas
    val comment: String? = null,
    val photoUrls: List<String> = emptyList(),
    val createdAt: Long, // epoch millis
    val updatedAt: Long? = null,
    val orderId: String? = null, // ID do pedido relacionado (se aplicável)
    val helpfulCount: Int = 0, // Quantidade de "útil" recebidos
    val verifiedPurchase: Boolean = false // Se foi uma compra verificada
)

data class ReviewSummary(
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int> = emptyMap() // 1->count, 2->count, etc.
)

// Result wrapper for error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
