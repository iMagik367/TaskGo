package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.BuildConfig
import com.taskgoapp.taskgo.data.repository.*
import com.taskgoapp.taskgo.core.data.PreferencesManager
import com.taskgoapp.taskgo.data.local.TaskGoDatabase
import com.taskgoapp.taskgo.data.local.dao.*
import com.taskgoapp.taskgo.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskGoDatabase(@ApplicationContext context: Context): TaskGoDatabase {
        return TaskGoDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideProductDao(database: TaskGoDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideCartDao(database: TaskGoDatabase): CartDao {
        return database.cartDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: TaskGoDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun providePurchaseOrderDao(database: TaskGoDatabase): PurchaseOrderDao {
        return database.purchaseOrderDao()
    }

    @Provides
    @Singleton
    fun provideServiceOrderDao(database: TaskGoDatabase): ServiceOrderDao {
        return database.serviceOrderDao()
    }

    @Provides
    @Singleton
    fun provideProposalDao(database: TaskGoDatabase): ProposalDao {
        return database.proposalDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: TaskGoDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideAddressDao(database: TaskGoDatabase): AddressDao {
        return database.addressDao()
    }

    @Provides
    @Singleton
    fun provideTrackingDao(database: TaskGoDatabase): TrackingDao {
        return database.trackingDao()
    }

    @Provides
    @Singleton
    fun provideProductsRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        productDao: ProductDao,
        cartDao: CartDao
    ): ProductsRepository {
        return if (BuildConfig.USE_FIREBASE) {
            FirestoreProductsRepositoryImpl(firestore, firebaseAuth, cartDao)
        } else {
            ProductsRepositoryImpl(productDao, cartDao)
        }
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userProfileDao: UserProfileDao
    ): UserRepository {
        return UserRepositoryImpl(userProfileDao)
    }

    @Provides
    @Singleton
    fun provideOrdersRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        purchaseOrderDao: PurchaseOrderDao,
        cartDao: CartDao
    ): OrdersRepository {
        return if (BuildConfig.USE_FIREBASE) {
            FirestoreOrdersRepositoryImpl(firestore, firebaseAuth, cartDao)
        } else {
            OrdersRepositoryImpl(purchaseOrderDao, cartDao)
        }
    }

    @Provides
    @Singleton
    fun provideServiceRepository(
        serviceOrderDao: ServiceOrderDao,
        proposalDao: ProposalDao
    ): ServiceRepository {
        return ServiceRepositoryImpl(serviceOrderDao, proposalDao)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: MessageDao
    ): MessageRepository {
        return MessageRepositoryImpl(messageDao)
    }

    @Provides
    @Singleton
    fun provideAddressRepository(
        addressDao: AddressDao
    ): AddressRepository {
        return AddressRepositoryImpl(addressDao)
    }

    @Provides
    @Singleton
    fun provideCardDao(database: TaskGoDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    @Singleton
    fun provideCardRepository(
        cardDao: CardDao
    ): CardRepository {
        return CardRepositoryImpl(cardDao)
    }

    @Provides
    @Singleton
    fun provideTrackingRepository(
        trackingDao: TrackingDao
    ): TrackingRepository {
        return TrackingRepositoryImpl(trackingDao)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesManager: PreferencesManager
    ): PreferencesRepository {
        return object : PreferencesRepository {
            override fun observePromosEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(true) // Default value
            override fun observeSoundEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(preferencesManager.getSoundEnabled())
            override fun observePushEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(preferencesManager.getNotificationEnabled())
            override fun observeLockscreenEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(true) // Default value
            override fun observeLanguage(): kotlinx.coroutines.flow.Flow<String> = 
                kotlinx.coroutines.flow.flowOf(preferencesManager.getLanguage())
            override fun observeTheme(): kotlinx.coroutines.flow.Flow<String> = 
                kotlinx.coroutines.flow.flowOf("light")
            override fun observeCategories(): kotlinx.coroutines.flow.Flow<String> = 
                kotlinx.coroutines.flow.flowOf("")
            
            override suspend fun updatePromosEnabled(enabled: Boolean) {
                // Not implemented in PreferencesManager
            }
            override suspend fun updateSoundEnabled(enabled: Boolean) {
                preferencesManager.saveSoundEnabled(enabled)
            }
            override suspend fun updatePushEnabled(enabled: Boolean) {
                preferencesManager.saveNotificationEnabled(enabled)
            }
            override suspend fun updateLockscreenEnabled(enabled: Boolean) {
                // Not implemented in PreferencesManager
            }
            override suspend fun updateLanguage(language: String) {
                preferencesManager.saveLanguage(language)
            }
            override suspend fun updateTheme(theme: String) {
                // Not implemented in PreferencesManager
            }
            override suspend fun updateCategories(categories: String) {
                // Not implemented in PreferencesManager
            }
        }
    }

}
