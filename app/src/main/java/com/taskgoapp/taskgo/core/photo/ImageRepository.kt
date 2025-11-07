package com.taskgoapp.taskgo.core.photo

import android.content.ContentResolver
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    private val photoPickerManager: PhotoPickerManager
) {
    
    suspend fun saveImageUri(contentResolver: ContentResolver, uri: Uri): String {
        // Take persistable URI permission
        photoPickerManager.takePersistableUriPermission(contentResolver, uri)
        
        // Return the URI string for storage in Room
        return uri.toString()
    }
    
    fun getImageUri(uriString: String): Uri? {
        return try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            null
        }
    }
}
