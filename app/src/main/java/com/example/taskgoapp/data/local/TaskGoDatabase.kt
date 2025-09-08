package com.example.taskgoapp.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.taskgoapp.data.local.dao.*
import com.example.taskgoapp.data.local.entity.*
import com.example.taskgoapp.data.local.converter.Converters

@Database(
    entities = [
        UserProfileEntity::class,
        ProductEntity::class,
        ProductImageEntity::class,
        MarketplaceProductEntity::class,
        ServiceOrderEntity::class,
        ProposalEntity::class,
        CartItemEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        TrackingEventEntity::class,
        MessageThreadEntity::class,
        ChatMessageEntity::class,
        AddressEntity::class,
        CardEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskGoDatabase : RoomDatabase() {
    
    abstract fun userProfileDao(): UserProfileDao
    abstract fun productDao(): ProductDao
    abstract fun marketplaceProductDao(): MarketplaceProductDao
    abstract fun serviceOrderDao(): ServiceOrderDao
    abstract fun proposalDao(): ProposalDao
    abstract fun cartDao(): CartDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun trackingDao(): TrackingDao
    abstract fun messageDao(): MessageDao
    abstract fun addressDao(): AddressDao
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: TaskGoDatabase? = null

        fun getDatabase(context: Context): TaskGoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskGoDatabase::class.java,
                    "taskgo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
