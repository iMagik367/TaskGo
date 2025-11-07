package com.taskgoapp.taskgo.core.photo

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoPickerManager @Inject constructor(
    private val context: Context
) {
    
    fun takePersistableUriPermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
}
