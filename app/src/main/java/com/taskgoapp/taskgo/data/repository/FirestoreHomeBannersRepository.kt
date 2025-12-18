package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.taskgoapp.taskgo.core.model.HomeBanner
import com.taskgoapp.taskgo.domain.repository.HomeBannersRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreHomeBannersRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : HomeBannersRepository {

    override fun observeHomeBanners(): Flow<List<HomeBanner>> = callbackFlow {
        val registration = firestore.collection("homeBanners")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                val banners = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(HomeBannerDocument::class.java)?.toModel(document.id)
                }.orEmpty()

                trySend(banners.sortedBy { it.priority }).isSuccess
            }

        awaitClose { registration.remove() }
    }

    private data class HomeBannerDocument(
        val title: String? = null,
        val subtitle: String? = null,
        val actionLabel: String? = null,
        val imageUrl: String? = null,
        val audience: String? = null,
        val actionRoute: String? = null,
        val priority: Long? = null,
        val active: Boolean? = null
    ) {
        fun toModel(id: String): HomeBanner? {
            if (active == false || title.isNullOrBlank() || subtitle.isNullOrBlank() || actionLabel.isNullOrBlank()) {
                return null
            }

            val audienceEnum = when (audience?.uppercase()) {
                "CLIENTE", "CLIENT", "CLIENTES" -> HomeBanner.Audience.CLIENTE
                "PRESTADOR", "PROVIDER", "PRESTADORES" -> HomeBanner.Audience.PRESTADOR
                else -> HomeBanner.Audience.TODOS
            }

            return HomeBanner(
                id = id,
                title = title,
                subtitle = subtitle,
                actionLabel = actionLabel,
                imageUrl = imageUrl,
                audience = audienceEnum,
                actionRoute = actionRoute,
                priority = priority?.toInt() ?: 0
            )
        }
    }
}

