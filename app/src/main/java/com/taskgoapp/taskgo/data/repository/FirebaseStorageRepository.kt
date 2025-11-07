package com.taskgoapp.taskgo.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    
    suspend fun uploadDocument(
        userId: String,
        documentType: String,
        uri: Uri
    ): Result<String> {
        return try {
            val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment ?: "document"}"
            val storageRef = storage.reference
                .child("$userId/documents/$documentType/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadSelfie(
        userId: String,
        uri: Uri
    ): Result<String> {
        return try {
            val fileName = "${System.currentTimeMillis()}_selfie.jpg"
            val storageRef = storage.reference
                .child("$userId/documents/selfie/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadAddressProof(
        userId: String,
        uri: Uri
    ): Result<String> {
        return try {
            val fileName = "${System.currentTimeMillis()}_address_proof.jpg"
            val storageRef = storage.reference
                .child("$userId/documents/address_proof/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


