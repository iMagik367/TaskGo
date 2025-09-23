package br.com.taskgo.taskgo.di

import retrofit2.Retrofit
import com.example.taskgoapp.core.data.remote.AuthApi
import com.example.taskgoapp.core.data.remote.ProductsApi
import com.example.taskgoapp.core.data.remote.CartApi
import com.example.taskgoapp.core.data.repositories.MarketplaceRepository
import com.example.taskgoapp.core.data.repositories.MarketplaceRepositoryImpl
import com.example.taskgoapp.data.repository.*
import com.example.taskgoapp.BuildConfig
import com.example.taskgoapp.domain.repository.*
import com.example.taskgoapp.data.local.TaskGoDatabase
import com.example.taskgoapp.data.local.dao.*
import com.example.taskgoapp.core.data.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
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
    fun provideMarketplaceRepository(productsApi: ProductsApi): MarketplaceRepository {
        return MarketplaceRepositoryImpl(productsApi)
    }

    @Provides
    @Singleton
    fun provideProductsRepository(
        productDao: ProductDao,
        cartDao: CartDao,
        remoteAdapter: RemoteProductsAdapter
    ): ProductsRepository {
        return if (BuildConfig.USE_REMOTE_API) remoteAdapter else ProductsRepositoryImpl(productDao, cartDao)
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
        purchaseOrderDao: PurchaseOrderDao,
        cartDao: CartDao
    ): OrdersRepository {
        return OrdersRepositoryImpl(purchaseOrderDao, cartDao)
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

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProductsApi(retrofit: Retrofit): ProductsApi = retrofit.create(ProductsApi::class.java)

    @Provides
    @Singleton
    fun provideCartApi(retrofit: Retrofit): CartApi = retrofit.create(CartApi::class.java)

    @Provides
    @Singleton
    fun provideOrdersApi(retrofit: Retrofit): com.example.taskgoapp.core.data.remote.OrdersApi = retrofit.create(com.example.taskgoapp.core.data.remote.OrdersApi::class.java)

    @Provides
    @Singleton
    fun provideOrderRepository(ordersApi: com.example.taskgoapp.core.data.remote.OrdersApi): com.example.taskgoapp.domain.repository.OrderRepository = 
        com.example.taskgoapp.data.repository.OrderRepositoryImpl(ordersApi)
}
