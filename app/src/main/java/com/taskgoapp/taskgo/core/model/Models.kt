package com.taskgoapp.taskgo.core.model

// User Models
enum class UserType {
    CLIENT,     // Cliente que contrata serviços
    PROVIDER    // Prestador de serviços
}

enum class AccountType { 
    PRESTADOR, 
    VENDEDOR, 
    CLIENTE 
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val city: String?,
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
    val sellerName: String?,
    val imageUris: List<String> // 1..n imagens
)

data class ServiceOrder(
    val id: String,
    val category: String,
    val description: String,
    val date: Long, // epoch millis
    val addressLine: String,
    val city: String,
    val state: String
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

// Result wrapper for error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
