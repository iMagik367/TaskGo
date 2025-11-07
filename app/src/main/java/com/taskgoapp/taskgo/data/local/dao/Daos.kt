package com.taskgoapp.taskgo.data.local.dao

import androidx.room.*
import com.taskgoapp.taskgo.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM product ORDER BY title")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product ORDER BY title")
    suspend fun getAll(): List<ProductEntity>

    @Transaction
    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImages(images: List<ProductImageEntity>)

    @Query("SELECT * FROM product_image WHERE productId = :productId")
    suspend fun images(productId: String): List<ProductImageEntity>

    @Query("DELETE FROM product_image WHERE productId = :productId")
    suspend fun deleteImagesByProductId(productId: String)

    @Delete 
    suspend fun delete(product: ProductEntity)
}

@Dao
interface MarketplaceProductDao {
    @Query("SELECT * FROM marketplace_product ORDER BY name")
    fun observeAll(): Flow<List<MarketplaceProductEntity>>

    @Query("SELECT * FROM marketplace_product ORDER BY name")
    suspend fun getAll(): List<MarketplaceProductEntity>

    @Query("SELECT * FROM marketplace_product WHERE id = :id")
    suspend fun getById(id: String): MarketplaceProductEntity?

    @Query("SELECT * FROM marketplace_product WHERE category = :category ORDER BY name")
    suspend fun getByCategory(category: String): List<MarketplaceProductEntity>

    @Query("SELECT * FROM marketplace_product WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name")
    suspend fun search(query: String): List<MarketplaceProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: MarketplaceProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<MarketplaceProductEntity>)

    @Delete 
    suspend fun delete(product: MarketplaceProductEntity)

    @Query("DELETE FROM marketplace_product")
    suspend fun deleteAll()
}

@Dao
interface ServiceOrderDao {
    @Query("SELECT * FROM service_order ORDER BY date DESC")
    fun observeAll(): Flow<List<ServiceOrderEntity>>

    @Query("SELECT * FROM service_order WHERE id = :id")
    suspend fun getById(id: String): ServiceOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: ServiceOrderEntity)

    @Delete
    suspend fun delete(order: ServiceOrderEntity)
}

@Dao
interface ProposalDao {
    @Query("SELECT * FROM proposal WHERE orderId = :orderId ORDER BY scheduledDate")
    fun observeByOrderId(orderId: String): Flow<List<ProposalEntity>>

    @Query("SELECT * FROM proposal WHERE id = :id")
    suspend fun getById(id: String): ProposalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(proposal: ProposalEntity)

    @Query("UPDATE proposal SET accepted = :accepted WHERE id = :id")
    suspend fun updateAccepted(id: String, accepted: Boolean)

    @Delete
    suspend fun delete(proposal: ProposalEntity)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_item")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_item WHERE productId = :productId")
    suspend fun getByProductId(productId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_item WHERE productId = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM cart_item")
    suspend fun clearAll()
}

@Dao
interface PurchaseOrderDao {
    @Query("SELECT * FROM purchase_order ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_order WHERE id = :id")
    suspend fun getById(id: String): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_order WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<PurchaseOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<PurchaseOrderItemEntity>)

    @Query("SELECT * FROM purchase_order_item WHERE orderId = :orderId")
    suspend fun getItemsByOrderId(orderId: String): List<PurchaseOrderItemEntity>

    @Query("UPDATE purchase_order SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_event WHERE orderId = :orderId ORDER BY date")
    fun observeByOrderId(orderId: String): Flow<List<TrackingEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: TrackingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<TrackingEventEntity>)

    @Query("UPDATE tracking_event SET done = :done WHERE id = :id")
    suspend fun updateDone(id: String, done: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM message_thread ORDER BY lastTime DESC")
    fun observeThreads(): Flow<List<MessageThreadEntity>>

    @Query("SELECT * FROM message_thread WHERE id = :id")
    suspend fun getThreadById(id: String): MessageThreadEntity?

    @Query("SELECT * FROM chat_message WHERE threadId = :threadId ORDER BY time")
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: MessageThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM address ORDER BY id")
    fun observeAll(): Flow<List<AddressEntity>>

    @Query("SELECT * FROM address WHERE id = :id")
    suspend fun getById(id: String): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(address: AddressEntity)

    @Delete
    suspend fun delete(address: AddressEntity)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card ORDER BY id")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM card WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeCurrent(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
