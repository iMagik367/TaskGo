package com.taskgoapp.taskgo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.data.firestore.models.NotificationFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val notificationsCollection = firestore.collection("notifications")

    fun observeNotifications(): Flow<List<NotificationFirestore>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }

            notificationsCollection
                .document(notificationId)
                .update("read", true, "readAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        type: String,
        title: String,
        message: String,
        orderId: String? = null,
        data: Map<String, Any>? = null
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }

            val notification = NotificationFirestore(
                userId = currentUser.uid,
                orderId = orderId,
                type = type,
                title = title,
                message = message,
                data = data,
                read = false,
                createdAt = java.util.Date()
            )

            val docRef = notificationsCollection.add(notification).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

