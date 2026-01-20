package com.taskgoapp.taskgo.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.taskgoapp.taskgo.data.local.dao.*
import com.taskgoapp.taskgo.data.local.entity.*
import com.taskgoapp.taskgo.data.local.converter.Converters

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
        CardEntity::class,
        SyncQueueEntity::class
    ],
    version = 8,
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
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: TaskGoDatabase? = null

        // Migration de versão 7 para 8: adiciona coluna 'state' à tabela user_profile
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN state TEXT")
            }
        }

        fun getDatabase(context: Context): TaskGoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskGoDatabase::class.java,
                    "taskgo_database"
                )
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
