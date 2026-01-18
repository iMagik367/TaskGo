package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.BuildConfig
import com.taskgoapp.taskgo.data.repository.*
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.taskgoapp.taskgo.core.sync.SyncManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.taskgoapp.taskgo.core.sync.InitialDataSyncManager
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
        cartDao: CartDao,
        syncManager: SyncManager,
        realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository
    ): ProductsRepository {
        return if (BuildConfig.USE_FIREBASE) {
            FirestoreProductsRepositoryImpl(
                firestore = firestore,
                firebaseAuth = firebaseAuth,
                cartDao = cartDao
            )
        } else {
            ProductsRepositoryImpl(productDao, cartDao)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthRepository(
        firebaseAuth: FirebaseAuth
    ): FirebaseAuthRepository {
        return FirebaseAuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userProfileDao: UserProfileDao,
        firestoreUserRepository: FirestoreUserRepository,
        syncManager: SyncManager,
        authRepository: FirebaseAuthRepository
    ): UserRepository {
        return UserRepositoryImpl(userProfileDao, firestoreUserRepository, syncManager, authRepository)
    }

    @Provides
    @Singleton
    fun provideOrdersRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        purchaseOrderDao: PurchaseOrderDao,
        cartDao: CartDao,
        syncManager: SyncManager,
        realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository
    ): OrdersRepository {
        return if (BuildConfig.USE_FIREBASE) {
            FirestoreOrdersRepositoryImpl(firestore, firebaseAuth, cartDao, purchaseOrderDao, syncManager, realtimeRepository)
        } else {
            OrdersRepositoryImpl(purchaseOrderDao, cartDao)
        }
    }

    @Provides
    @Singleton
    fun provideFirestoreOrderRepository(
        firestore: FirebaseFirestore,
        authRepository: FirebaseAuthRepository
    ): com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository(firestore, authRepository)
    }
    
    @Provides
    @Singleton
    fun provideServiceRepository(
        serviceOrderDao: ServiceOrderDao,
        proposalDao: ProposalDao,
        functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
        orderRepository: com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
    ): ServiceRepository {
        return ServiceRepositoryImpl(serviceOrderDao, proposalDao, functionsService, orderRepository)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: MessageDao,
        database: FirebaseDatabase,
        firebaseAuth: FirebaseAuth
    ): com.taskgoapp.taskgo.domain.repository.MessageRepository {
        return com.taskgoapp.taskgo.data.repository.MessageRepositoryImpl(messageDao, database, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideAddressRepository(
        addressDao: AddressDao,
        firestore: FirebaseFirestore,
        syncManager: SyncManager,
        authRepository: FirebaseAuthRepository
    ): AddressRepository {
        return AddressRepositoryImpl(addressDao, firestore, syncManager, authRepository)
    }

    @Provides
    @Singleton
    fun provideCardDao(database: TaskGoDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    @Singleton
    fun provideCardRepository(
        cardDao: CardDao,
        firestore: FirebaseFirestore,
        syncManager: SyncManager,
        authRepository: FirebaseAuthRepository
    ): CardRepository {
        return CardRepositoryImpl(cardDao, firestore, syncManager, authRepository)
    }

    @Provides
    @Singleton
    fun provideFirestoreTrackingRepository(
        firestore: FirebaseFirestore
    ): FirestoreTrackingRepository {
        return FirestoreTrackingRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideTrackingRepository(
        firestoreTrackingRepository: FirestoreTrackingRepository
    ): TrackingRepository {
        return FirestoreTrackingRepositoryAdapter(firestoreTrackingRepository)
    }

    @Provides
    @Singleton
    fun provideHomeBannersRepository(
        firestore: FirebaseFirestore
    ): HomeBannersRepository {
        return FirestoreHomeBannersRepository(firestore)
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
                preferencesManager.promosEnabled
            override fun observeSoundEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                preferencesManager.soundEnabled
            override fun observePushEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                preferencesManager.pushEnabled
            override fun observeLockscreenEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = 
                preferencesManager.lockscreenEnabled
            override fun observeEmailNotificationsEnabled(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.emailNotificationsEnabled
            override fun observeSmsNotificationsEnabled(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.smsNotificationsEnabled
            override fun observeLanguage(): kotlinx.coroutines.flow.Flow<String> = 
                preferencesManager.language
            override fun observeTheme(): kotlinx.coroutines.flow.Flow<String> = 
                preferencesManager.theme
            override fun observeCategories(): kotlinx.coroutines.flow.Flow<String> = 
                preferencesManager.categories
            override fun observePrivacyLocationSharing(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyLocationSharing
            override fun observePrivacyProfileVisible(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyProfileVisible
            override fun observePrivacyContactInfo(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyContactInfo
            override fun observePrivacyAnalytics(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyAnalytics
            override fun observePrivacyPersonalizedAds(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyPersonalizedAds
            override fun observePrivacyDataCollection(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyDataCollection
            override fun observePrivacyThirdPartySharing(): kotlinx.coroutines.flow.Flow<Boolean> =
                preferencesManager.privacyThirdPartySharing
            
            override suspend fun updatePromosEnabled(enabled: Boolean) {
                preferencesManager.updatePromosEnabled(enabled)
            }
            override suspend fun updateSoundEnabled(enabled: Boolean) {
                preferencesManager.updateSoundEnabled(enabled)
            }
            override suspend fun updatePushEnabled(enabled: Boolean) {
                preferencesManager.updatePushEnabled(enabled)
            }
            override suspend fun updateLockscreenEnabled(enabled: Boolean) {
                preferencesManager.updateLockscreenEnabled(enabled)
            }
            override suspend fun updateEmailNotificationsEnabled(enabled: Boolean) {
                preferencesManager.updateEmailNotificationsEnabled(enabled)
            }
            override suspend fun updateSmsNotificationsEnabled(enabled: Boolean) {
                preferencesManager.updateSmsNotificationsEnabled(enabled)
            }
            override suspend fun updateLanguage(language: String) {
                preferencesManager.updateLanguage(language)
            }
            override suspend fun updateTheme(theme: String) {
                preferencesManager.updateTheme(theme)
            }
            override suspend fun updateCategories(categories: String) {
                preferencesManager.updateCategories(categories)
            }
            override suspend fun updatePrivacyLocationSharing(enabled: Boolean) {
                preferencesManager.updatePrivacyLocationSharing(enabled)
            }
            override suspend fun updatePrivacyProfileVisible(enabled: Boolean) {
                preferencesManager.updatePrivacyProfileVisible(enabled)
            }
            override suspend fun updatePrivacyContactInfo(enabled: Boolean) {
                preferencesManager.updatePrivacyContactInfo(enabled)
            }
            override suspend fun updatePrivacyAnalytics(enabled: Boolean) {
                preferencesManager.updatePrivacyAnalytics(enabled)
            }
            override suspend fun updatePrivacyPersonalizedAds(enabled: Boolean) {
                preferencesManager.updatePrivacyPersonalizedAds(enabled)
            }
            override suspend fun updatePrivacyDataCollection(enabled: Boolean) {
                preferencesManager.updatePrivacyDataCollection(enabled)
            }
            override suspend fun updatePrivacyThirdPartySharing(enabled: Boolean) {
                preferencesManager.updatePrivacyThirdPartySharing(enabled)
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideCategoriesRepository(
        firestore: FirebaseFirestore
    ): CategoriesRepository {
        return FirestoreCategoriesRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideFilterPreferencesManager(
        @ApplicationContext context: Context
    ): com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager {
        return com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideReviewsRepository(
        firestore: FirebaseFirestore
    ): com.taskgoapp.taskgo.domain.repository.ReviewsRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreReviewsRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideProvidersRepository(
        firestore: FirebaseFirestore
    ): com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideMapLocationsRepository(
        firestore: FirebaseFirestore,
        geocodingService: com.taskgoapp.taskgo.core.location.GeocodingService
    ): com.taskgoapp.taskgo.data.repository.FirestoreMapLocationsRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreMapLocationsRepository(firestore, geocodingService)
    }
    
    @Provides
    @Singleton
    fun provideRealtimeDatabaseRepository(
        database: FirebaseDatabase
    ): com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository {
        return com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository(database)
    }
    
    @Provides
    @Singleton
    fun provideServicesRepository(
        firestore: FirebaseFirestore,
        realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository,
        authRepository: FirebaseAuthRepository,
        functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
    ): com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository(
            firestore = firestore,
            realtimeRepository = realtimeRepository,
            authRepository = authRepository,
            functionsService = functionsService
        )
    }
    
    @Provides
    @Singleton
    fun provideFirestoreProductsRepository(
        firestore: FirebaseFirestore,
        authRepository: FirebaseAuthRepository
    ): com.taskgoapp.taskgo.data.repository.FirestoreProductsRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreProductsRepository(
            firestore,
            authRepository
        )
    }
    
    @Provides
    @Singleton
    fun provideFeedRepository(
        firestore: FirebaseFirestore,
        authRepository: FirebaseAuthRepository
    ): com.taskgoapp.taskgo.domain.repository.FeedRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreFeedRepository(firestore, authRepository)
    }
    
    @Provides
    @Singleton
    fun provideStoriesRepository(
        firestore: FirebaseFirestore,
        authRepository: FirebaseAuthRepository,
        functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
    ): com.taskgoapp.taskgo.domain.repository.StoriesRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreStoriesRepository(firestore, authRepository, functionsService)
    }
    
    @Provides
    @Singleton
    fun provideFeedMediaRepository(
        storage: FirebaseStorage,
        authRepository: FirebaseAuthRepository,
        @ApplicationContext context: Context
    ): com.taskgoapp.taskgo.data.repository.FeedMediaRepository {
        return com.taskgoapp.taskgo.data.repository.FeedMediaRepository(storage, authRepository, context)
    }
    
    @Provides
    @Singleton
    fun provideDocumentVerificationManager(
        firestoreUserRepository: FirestoreUserRepository,
        auth: FirebaseAuth
    ): com.taskgoapp.taskgo.core.security.DocumentVerificationManager {
        return com.taskgoapp.taskgo.core.security.DocumentVerificationManager(firestoreUserRepository, auth)
    }
    
    @Provides
    @Singleton
    fun provideLGPDComplianceManager(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): com.taskgoapp.taskgo.core.security.LGPDComplianceManager {
        return com.taskgoapp.taskgo.core.security.LGPDComplianceManager(context, firestore)
    }
    
    @Provides
    @Singleton
    fun provideSyncQueueDao(database: TaskGoDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideInitialDataSyncManager(
        firebaseAuth: FirebaseAuth,
        productsRepository: ProductsRepository,
        ordersRepository: OrdersRepository,
        addressRepository: AddressRepository,
        cardRepository: CardRepository,
        userRepository: UserRepository,
        firestoreUserRepository: FirestoreUserRepository,
        productDao: ProductDao,
        purchaseOrderDao: PurchaseOrderDao,
        addressDao: AddressDao,
        cardDao: CardDao,
        userProfileDao: UserProfileDao
    ): InitialDataSyncManager {
        return InitialDataSyncManager(
            firebaseAuth,
            productsRepository,
            ordersRepository,
            addressRepository,
            cardRepository,
            userRepository,
            firestoreUserRepository,
            productDao,
            purchaseOrderDao,
            addressDao,
            cardDao,
            userProfileDao
        )
    }
    
    @Provides
    @Singleton
    fun provideSyncManager(
        syncQueueDao: SyncQueueDao,
        firestore: FirebaseFirestore,
        gson: Gson
    ): SyncManager {
        val manager = SyncManager(syncQueueDao, firestore, gson)
        manager.startSync() // Inicia sincronização automaticamente
        return manager
    }

}
