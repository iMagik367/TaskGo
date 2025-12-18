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
    
    /**
     * Upload de imagem para serviço
     * Path: {providerId}/services/{serviceId}/images/{filename}
     */
    suspend fun uploadServiceImage(
        providerId: String,
        serviceId: String,
        uri: Uri,
        imageIndex: Int = 0
    ): Result<String> {
        return try {
            val extension = uri.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
            val fileName = "${System.currentTimeMillis()}_img_$imageIndex.$extension"
            val storageRef = storage.reference
                .child("$providerId/services/$serviceId/images/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload de vídeo MP4 para serviço
     * Path: {providerId}/services/{serviceId}/videos/{filename}
     */
    suspend fun uploadServiceVideo(
        providerId: String,
        serviceId: String,
        uri: Uri,
        videoIndex: Int = 0
    ): Result<String> {
        return try {
            val fileName = "${System.currentTimeMillis()}_video_$videoIndex.mp4"
            val storageRef = storage.reference
                .child("$providerId/services/$serviceId/videos/$fileName")
            
            // Metadata para vídeo
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("video/mp4")
                .build()
            
            val uploadTask = storageRef.putFile(uri, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload de foto de perfil do usuário
     * Path: {userId}/profile/{filename}
     */
    suspend fun uploadProfileImage(
        userId: String,
        uri: Uri
    ): Result<String> {
        return try {
            val extension = uri.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
            val fileName = "${System.currentTimeMillis()}_profile.$extension"
            val storageRef = storage.reference
                .child("$userId/profile/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload de imagem para produto
     * Path: {sellerId}/products/{productId}/images/{filename}
     */
    suspend fun uploadProductImage(
        userId: String,
        productId: String,
        uri: Uri,
        imageIndex: Int = 0
    ): Result<String> {
        return try {
            val extension = uri.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
            val fileName = "${System.currentTimeMillis()}_img_$imageIndex.$extension"
            val storageRef = storage.reference
                .child("$userId/products/$productId/images/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deleta arquivo do Storage
     */
    suspend fun deleteFile(downloadUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(downloadUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


