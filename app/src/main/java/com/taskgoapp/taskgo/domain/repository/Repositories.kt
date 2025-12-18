package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.*
import kotlinx.coroutines.flow.Flow

interface ProductsRepository {
    fun observeProducts(): Flow<List<Product>>
    fun observeProductErrors(): Flow<String>
    suspend fun getProduct(id: String): Product?
    suspend fun getMyProducts(): List<Product>
    suspend fun upsertProduct(product: Product)
    suspend fun deleteProduct(id: String)
    suspend fun addToCart(productId: String, qtyDelta: Int)
    suspend fun removeFromCart(productId: String)
    fun observeCart(): Flow<List<CartItem>>
    suspend fun clearCart()
}

interface ServiceRepository {
    fun observeServiceOrders(): Flow<List<ServiceOrder>>
    suspend fun getServiceOrder(id: String): ServiceOrder?
    suspend fun upsertServiceOrder(order: ServiceOrder)
    suspend fun deleteServiceOrder(id: String)
    fun observeProposals(orderId: String): Flow<List<Proposal>>
    suspend fun getProposal(id: String): Proposal?
    suspend fun upsertProposal(proposal: Proposal)
    suspend fun acceptProposal(proposalId: String)
    suspend fun rejectProposal(proposalId: String)
}

interface OrdersRepository {
    fun observeOrders(): Flow<List<PurchaseOrder>>
    fun observeOrdersByStatus(status: OrderStatus): Flow<List<PurchaseOrder>>
    suspend fun getOrder(id: String): PurchaseOrder?
    suspend fun createOrder(cart: List<CartItem>, total: Double, paymentMethod: String, addressId: String): String
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus)
}

interface TrackingRepository {
    fun observeTrackingEvents(orderId: String): Flow<List<TrackingEvent>>
    suspend fun seedTimeline(orderId: String)
    suspend fun updateEventDone(eventId: String, done: Boolean)
}

interface MessageRepository {
    fun observeThreads(): Flow<List<MessageThread>>
    suspend fun getThread(id: String): MessageThread?
    fun observeMessages(threadId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(threadId: String, text: String)
    suspend fun createThread(title: String): String
}

interface AddressRepository {
    fun observeAddresses(): Flow<List<Address>>
    suspend fun getAddress(id: String): Address?
    suspend fun upsertAddress(address: Address)
    suspend fun deleteAddress(id: String)
}

interface CardRepository {
    fun observeCards(): Flow<List<Card>>
    suspend fun getCard(id: String): Card?
    suspend fun upsertCard(card: Card)
    suspend fun deleteCard(id: String)
}

interface UserRepository {
    fun observeCurrentUser(): Flow<UserProfile?>
    suspend fun updateUser(user: UserProfile)
    suspend fun updateAvatar(avatarUri: String)
}

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationItem>>
    suspend fun markAsRead(notificationId: String)
    suspend fun createNotification(type: NotificationType, title: String, message: String, actionRoute: String? = null)
}

interface PreferencesRepository {
    fun observePromosEnabled(): Flow<Boolean>
    fun observeSoundEnabled(): Flow<Boolean>
    fun observePushEnabled(): Flow<Boolean>
    fun observeLockscreenEnabled(): Flow<Boolean>
    fun observeEmailNotificationsEnabled(): Flow<Boolean>
    fun observeSmsNotificationsEnabled(): Flow<Boolean>
    fun observeLanguage(): Flow<String>
    fun observeTheme(): Flow<String>
    fun observeCategories(): Flow<String>
    fun observePrivacyLocationSharing(): Flow<Boolean>
    fun observePrivacyProfileVisible(): Flow<Boolean>
    fun observePrivacyContactInfo(): Flow<Boolean>
    fun observePrivacyAnalytics(): Flow<Boolean>
    fun observePrivacyPersonalizedAds(): Flow<Boolean>
    fun observePrivacyDataCollection(): Flow<Boolean>
    fun observePrivacyThirdPartySharing(): Flow<Boolean>
    
    suspend fun updatePromosEnabled(enabled: Boolean)
    suspend fun updateSoundEnabled(enabled: Boolean)
    suspend fun updatePushEnabled(enabled: Boolean)
    suspend fun updateLockscreenEnabled(enabled: Boolean)
    suspend fun updateEmailNotificationsEnabled(enabled: Boolean)
    suspend fun updateSmsNotificationsEnabled(enabled: Boolean)
    suspend fun updateLanguage(language: String)
    suspend fun updateTheme(theme: String)
    suspend fun updateCategories(categories: String)
    suspend fun updatePrivacyLocationSharing(enabled: Boolean)
    suspend fun updatePrivacyProfileVisible(enabled: Boolean)
    suspend fun updatePrivacyContactInfo(enabled: Boolean)
    suspend fun updatePrivacyAnalytics(enabled: Boolean)
    suspend fun updatePrivacyPersonalizedAds(enabled: Boolean)
    suspend fun updatePrivacyDataCollection(enabled: Boolean)
    suspend fun updatePrivacyThirdPartySharing(enabled: Boolean)
}
