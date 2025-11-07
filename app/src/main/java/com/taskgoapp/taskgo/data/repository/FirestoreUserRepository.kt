package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getUser(uid: String): UserFirestore? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserFirestore::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(user: UserFirestore): Result<Unit> {
        return try {
            val data = user.copy(updatedAt = com.google.firebase.Timestamp.now().toDate())
            usersCollection.document(user.uid).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateField(uid: String, field: String, value: Any): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update(field, value, "updatedAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun promoteToProvider(uid: String): Result<Unit> {
        return updateField(uid, "role", "provider")
    }

    suspend fun approveDocuments(uid: String, documents: List<String>, approvedBy: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                "documents", documents,
                "documentsApproved", true,
                "documentsApprovedAt", FieldValue.serverTimestamp(),
                "documentsApprovedBy", approvedBy,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStripeAccount(uid: String, accountId: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                "stripeAccountId", accountId,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





