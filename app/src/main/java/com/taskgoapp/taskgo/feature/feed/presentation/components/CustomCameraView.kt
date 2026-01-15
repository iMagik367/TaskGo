package com.taskgoapp.taskgo.feature.feed.presentation.components

import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Componente de câmera customizada usando CameraX
 * Similar ao Instagram: câmera própria do app, não a câmera nativa
 */
@Composable
fun CustomCameraView(
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // Inicializar CameraX
    LaunchedEffect(Unit) {
        cameraProvider = getCameraProvider(context)
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Preview da câmera
        cameraProvider?.let { provider ->
            CameraPreview(
                cameraProvider = provider,
                lensFacing = lensFacing,
                onImageCaptureReady = { imageCapture = it },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Overlay preto semi-transparente (estilo Instagram)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f))
        )
        
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão fechar (esquerda) - X de fechar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Botão galeria (direita) - similar ao Instagram
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Abrir galeria",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Botão trocar câmera (frente/trás) - posicionado no topo direito, abaixo do botão de galeria
        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 16.dp)
                .size(48.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Trocar câmera",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Bottom bar - Botão de captura
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botão de captura (estilo Instagram)
            Button(
                onClick = {
                    if (!isCapturing && imageCapture != null) {
                        isCapturing = true
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture!!,
                            onPhotoCaptured = { uri ->
                                isCapturing = false
                                onPhotoCaptured(uri)
                            },
                            onError = {
                                isCapturing = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.Gray
                ),
                enabled = !isCapturing && imageCapture != null
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                        .background(Color.Black, CircleShape)
                )
            }
        }
    }
}

/**
 * Preview da câmera usando CameraX PreviewView
 */
@Composable
private fun CameraPreview(
    cameraProvider: ProcessCameraProvider,
    lensFacing: Int,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                onImageCaptureReady(imageCapture)
            } catch (e: Exception) {
                android.util.Log.e("CustomCameraView", "Erro ao inicializar câmera: ${e.message}", e)
            }
            
            previewView
        },
        modifier = modifier
    )
}

/**
 * Captura uma foto usando ImageCapture
 */
private fun capturePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Uri) -> Unit,
    onError: () -> Unit
) {
    // Criar arquivo para salvar a foto
    val photoFile = File(
        context.cacheDir,
        "story_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )
    
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = android.net.Uri.fromFile(photoFile)
                android.util.Log.d("CustomCameraView", "Foto capturada: $savedUri")
                onPhotoCaptured(savedUri)
            }
            
            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("CustomCameraView", "Erro ao capturar foto: ${exception.message}", exception)
                onError()
            }
        }
    )
}

/**
 * Obtém o ProcessCameraProvider
 */
private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    continuation.resume(cameraProviderFuture.get())
                } catch (e: Exception) {
                    android.util.Log.e("CustomCameraView", "Erro ao obter camera provider: ${e.message}", e)
                    continuation.resume(cameraProviderFuture.get())
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

