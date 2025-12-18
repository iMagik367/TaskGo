package com.taskgoapp.taskgo.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {
    private const val MAX_IMAGE_DIMENSION = 2048 // Máximo de 2048px em qualquer dimensão
    private const val MAX_BITMAP_SIZE = 10 * 1024 * 1024 // 10MB máximo em memória
    
    /**
     * Redimensiona um bitmap para um tamanho máximo, mantendo a proporção
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = MAX_IMAGE_DIMENSION): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Se já está dentro do limite, retorna o bitmap original
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = min(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Carrega e redimensiona um bitmap de uma URI, considerando a orientação EXIF
     */
    fun loadAndResizeBitmap(context: Context, uri: Uri, maxDimension: Int = MAX_IMAGE_DIMENSION): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            
            // Primeiro, decodifica apenas as dimensões
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calcula o sample size para reduzir memória
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            
            // Decodifica o bitmap com o sample size
            val newInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
            newInputStream?.close()
            
            if (bitmap == null) return null
            
            // Corrige orientação EXIF se necessário
            val orientedBitmap = fixOrientation(context, uri, bitmap)
            
            // Redimensiona se ainda estiver muito grande
            if (orientedBitmap.width > maxDimension || orientedBitmap.height > maxDimension) {
                resizeBitmap(orientedBitmap, maxDimension)
            } else {
                orientedBitmap
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calcula o inSampleSize para reduzir o uso de memória
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Corrige a orientação da imagem baseado nos dados EXIF
     */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                return bitmap
            }
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }
            
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            // Se houver erro ao ler EXIF, retorna o bitmap original
            bitmap
        }
    }
    
    /**
     * Verifica se um bitmap excede o tamanho máximo permitido
     */
    fun isBitmapTooLarge(bitmap: Bitmap): Boolean {
        val byteCount = bitmap.allocationByteCount
        return byteCount > MAX_BITMAP_SIZE
    }
}

