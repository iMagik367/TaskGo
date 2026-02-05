package com.taskgoapp.taskgo.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.taskgoapp.taskgo.data.local.TaskGoDatabase
import com.taskgoapp.taskgo.data.local.dao.*
import com.taskgoapp.taskgo.data.repository.*
import com.taskgoapp.taskgo.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
    @Provides
    @Singleton
    fun provideTaskGoDatabase(@ApplicationContext context: Context): TaskGoDatabase {
        return TaskGoDatabase.getDatabase(context)
    }

        @Provides
        @Singleton
        fun provideUserProfileDao(database: TaskGoDatabase): UserProfileDao {
            return database.userProfileDao()
        }

    @Provides
    @Singleton
    fun provideProductDao(database: TaskGoDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
        fun provideMarketplaceProductDao(database: TaskGoDatabase): MarketplaceProductDao {
            return database.marketplaceProductDao()
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
        fun provideCartDao(database: TaskGoDatabase): CartDao {
            return database.cartDao()
    }

    @Provides
    @Singleton
        fun providePurchaseOrderDao(database: TaskGoDatabase): PurchaseOrderDao {
            return database.purchaseOrderDao()
    }

    @Provides
    @Singleton
    fun provideTrackingDao(database: TaskGoDatabase): TrackingDao {
        return database.trackingDao()
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
    fun provideCardDao(database: TaskGoDatabase): CardDao {
        return database.cardDao()
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
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create()
        }
    }

    @Binds
    @Singleton
    abstract fun bindProductsRepository(
        firestoreProductsRepositoryImpl: FirestoreProductsRepositoryImpl
    ): ProductsRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        firestoreOrdersRepositoryImpl: FirestoreOrdersRepositoryImpl
    ): OrdersRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        trackingRepositoryImpl: TrackingRepositoryImpl
    ): TrackingRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindAddressRepository(
        addressRepositoryImpl: AddressRepositoryImpl
    ): AddressRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        cardRepositoryImpl: CardRepositoryImpl
    ): CardRepository

    @Binds
    @Singleton
    abstract fun bindCategoriesRepository(
        firestoreCategoriesRepository: FirestoreCategoriesRepository
    ): CategoriesRepository

    @Binds
    @Singleton
    abstract fun bindFeedRepository(
        firestoreFeedRepository: FirestoreFeedRepository
    ): FeedRepository

    @Binds
    @Singleton
    abstract fun bindStoriesRepository(
        firestoreStoriesRepository: FirestoreStoriesRepository
    ): StoriesRepository

    @Binds
    @Singleton
    abstract fun bindHomeBannersRepository(
        firestoreHomeBannersRepository: FirestoreHomeBannersRepository
    ): HomeBannersRepository

    @Binds
    @Singleton
    abstract fun bindReviewsRepository(
        firestoreReviewsRepository: FirestoreReviewsRepository
    ): ReviewsRepository

}

@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    @Provides
    @Singleton
    fun provideFirestoreProvidersRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
    ): com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository {
        return com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository(
            firestore,
            userRepository
        )
    }
    
    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
    ): PreferencesRepository {
        return object : PreferencesRepository {
            override fun observePromosEnabled() = preferencesManager.promosEnabled
            override fun observeSoundEnabled() = preferencesManager.soundEnabled
            override fun observePushEnabled() = preferencesManager.pushEnabled
            override fun observeLockscreenEnabled() = preferencesManager.lockscreenEnabled
            override fun observeEmailNotificationsEnabled() = preferencesManager.emailNotificationsEnabled
            override fun observeSmsNotificationsEnabled() = preferencesManager.smsNotificationsEnabled
            override fun observeLanguage() = preferencesManager.language
            override fun observeTheme() = preferencesManager.theme
            override fun observeCategories() = preferencesManager.categories
            override fun observePrivacyLocationSharing() = preferencesManager.privacyLocationSharing
            override fun observePrivacyProfileVisible() = preferencesManager.privacyProfileVisible
            override fun observePrivacyContactInfo() = preferencesManager.privacyContactInfo
            override fun observePrivacyAnalytics() = preferencesManager.privacyAnalytics
            override fun observePrivacyPersonalizedAds() = preferencesManager.privacyPersonalizedAds
            override fun observePrivacyDataCollection() = preferencesManager.privacyDataCollection
            override fun observePrivacyThirdPartySharing() = preferencesManager.privacyThirdPartySharing
            override suspend fun updatePromosEnabled(enabled: Boolean) = preferencesManager.updatePromosEnabled(enabled)
            override suspend fun updateSoundEnabled(enabled: Boolean) = preferencesManager.updateSoundEnabled(enabled)
            override suspend fun updatePushEnabled(enabled: Boolean) = preferencesManager.updatePushEnabled(enabled)
            override suspend fun updateLockscreenEnabled(enabled: Boolean) = preferencesManager.updateLockscreenEnabled(enabled)
            override suspend fun updateEmailNotificationsEnabled(enabled: Boolean) = preferencesManager.updateEmailNotificationsEnabled(enabled)
            override suspend fun updateSmsNotificationsEnabled(enabled: Boolean) = preferencesManager.updateSmsNotificationsEnabled(enabled)
            override suspend fun updateLanguage(language: String) = preferencesManager.updateLanguage(language)
            override suspend fun updateTheme(theme: String) = preferencesManager.updateTheme(theme)
            override suspend fun updateCategories(categories: String) = preferencesManager.updateCategories(categories)
            override suspend fun updatePrivacyLocationSharing(enabled: Boolean) = preferencesManager.updatePrivacyLocationSharing(enabled)
            override suspend fun updatePrivacyProfileVisible(enabled: Boolean) = preferencesManager.updatePrivacyProfileVisible(enabled)
            override suspend fun updatePrivacyContactInfo(enabled: Boolean) = preferencesManager.updatePrivacyContactInfo(enabled)
            override suspend fun updatePrivacyAnalytics(enabled: Boolean) = preferencesManager.updatePrivacyAnalytics(enabled)
            override suspend fun updatePrivacyPersonalizedAds(enabled: Boolean) = preferencesManager.updatePrivacyPersonalizedAds(enabled)
            override suspend fun updatePrivacyDataCollection(enabled: Boolean) = preferencesManager.updatePrivacyDataCollection(enabled)
            override suspend fun updatePrivacyThirdPartySharing(enabled: Boolean) = preferencesManager.updatePrivacyThirdPartySharing(enabled)
        }
    }
}
