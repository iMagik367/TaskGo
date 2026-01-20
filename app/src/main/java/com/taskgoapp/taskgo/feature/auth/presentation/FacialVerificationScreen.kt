package com.taskgoapp.taskgo.feature.auth.presentation

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.taskgoapp.taskgo.core.security.RealTimeFaceAnalyzer
import com.taskgoapp.taskgo.core.security.FaceDetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FacialVerificationScreen(
    onBackClick: () -> Unit,
    onVerificationComplete: () -> Unit,
    viewModel: IdentityVerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Permissões
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Estados
    var faceDetectionResult by remember { mutableStateOf<FaceDetectionResult?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf<String?>(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var imageAnalysis: ImageAnalysis? by remember { mutableStateOf(null) }
    
    // Simplificado: apenas verificação básica de rosto
    var faceDetected by remember { mutableStateOf(false) }
    // Analisador de face em tempo real
    val faceAnalyzer = remember {
        RealTimeFaceAnalyzer { result ->
            faceDetectionResult = result
        }
    }
    
    // Limpar recursos ao sair
    DisposableEffect(Unit) {
        onDispose {
            faceAnalyzer.release()
        }
    }
    
    // Detectar rosto para habilitar botão
    LaunchedEffect(faceDetectionResult) {
        val result = faceDetectionResult
        faceDetected = result?.hasFace == true && result.isCentered && result.isGoodSize
    }
    
    // Inicializar câmera
    LaunchedEffect(cameraPermission.status) {
        if (cameraPermission.status is com.google.accompanist.permissions.PermissionStatus.Granted) {
            try {
                val provider = withContext(Dispatchers.Main) {
                    ProcessCameraProvider.getInstance(context).get()
                }
                cameraProvider = provider
                
                val previewBuilder = Preview.Builder()
                    .build()
                preview = previewBuilder
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .build()
                
                // ImageAnalysis para detecção facial em tempo real
                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            faceAnalyzer
                        )
                    }
                imageAnalysis = analysisBuilder
                
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewBuilder,
                    imageCapture,
                    analysisBuilder
                )
            } catch (e: Exception) {
                android.util.Log.e("FacialVerification", "Erro ao inicializar câmera: ${e.message}", e)
            }
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Tela de loading durante verificação
    if (isVerifying || uiState.isVerifyingFace) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CircularProgressIndicator(
                    color = TaskGoGreen,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp
                )
                Text(
                    text = verificationMessage ?: "Validando sua identidade...",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Por favor, aguarde",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        return // Retornar cedo para mostrar apenas o loading
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Verificação Facial",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = com.taskgoapp.taskgo.core.design.TGIcons.Back),
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaskGoGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Preview da câmera
            if (cameraPermission.status is com.google.accompanist.permissions.PermissionStatus.Granted && cameraProvider != null && preview != null) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        preview?.setSurfaceProvider(view.surfaceProvider)
                    }
                )
            } else {
                // Placeholder enquanto câmera não está pronta
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermission.status !is com.google.accompanist.permissions.PermissionStatus.Granted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Permissão de câmera necessária",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Button(
                                onClick = { cameraPermission.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TaskGoGreen
                                )
                            ) {
                                Text("Conceder Permissão", color = Color.White)
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            
            // Overlay com oval guia (formato de rosto)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Oval guia (formato de rosto)
                Canvas(modifier = Modifier.size(width = 280.dp, height = 360.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val width = size.width * 0.85f
                    val height = size.height * 0.85f
                    
                    val faceResult = faceDetectionResult
                    val isReady = faceDetected
                    
                    // Oval externo (guia)
                    drawOval(
                        color = when {
                            isReady -> TaskGoSuccessGreen
                            faceResult?.hasFace == true -> Color.Yellow.copy(alpha = 0.7f)
                            else -> Color.White.copy(alpha = 0.5f)
                        },
                        topLeft = Offset(center.x - width / 2, center.y - height / 2),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        style = Stroke(width = if (isReady) 5.dp.toPx() else 4.dp.toPx())
                    )
                    
                    // Indicador de posição da face (se detectada)
                    faceResult?.let { result ->
                        if (result.hasFace) {
                            val faceX = center.x + (result.faceCenterX - 0.5f) * size.width
                            val faceY = center.y + (result.faceCenterY - 0.5f) * size.height
                            
                            // Desenhar ponto indicando posição da face
                            drawCircle(
                                color = if (result.isCentered) TaskGoSuccessGreen else Color.Red,
                                radius = 8.dp.toPx(),
                                center = Offset(faceX, faceY)
                            )
                        }
                    }
                    
                    // Oval interno (área de detecção)
                    val innerWidth = width * 0.85f
                    val innerHeight = height * 0.85f
                    drawOval(
                        color = Color.Transparent,
                        topLeft = Offset(center.x - innerWidth / 2, center.y - innerHeight / 2),
                        size = androidx.compose.ui.geometry.Size(innerWidth, innerHeight),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                }
            }
            
            // Instruções na parte inferior
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val faceResult = faceDetectionResult
                
                Text(
                    text = when {
                        isVerifying -> verificationMessage ?: "Processando..."
                        isCapturing -> "Capturando imagem..."
                        faceResult == null || !faceResult.hasFace -> {
                            "Posicione seu rosto dentro do oval"
                        }
                        !faceResult.isCentered -> {
                            "Centralize seu rosto no oval"
                        }
                        !faceResult.isGoodSize -> {
                            if (faceResult.faceSize < 0.25f) {
                                "Aproxime-se um pouco mais"
                            } else {
                                "Afaste-se um pouco"
                            }
                        }
                        !faceResult.eyesOpen -> {
                            "Mantenha os olhos abertos"
                        }
                        !faceResult.isLookingAtCamera -> {
                            "Olhe diretamente para a câmera"
                        }
                        else -> {
                            "Rosto detectado! Toque no botão para capturar"
                        }
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Indicadores de status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(
                        label = "Rosto",
                        isActive = faceResult?.hasFace == true
                    )
                    StatusIndicator(
                        label = "Centralizado",
                        isActive = faceResult?.isCentered == true
                    )
                    StatusIndicator(
                        label = "Tamanho",
                        isActive = faceResult?.isGoodSize == true
                    )
                    StatusIndicator(
                        label = "Olhos",
                        isActive = faceResult?.eyesOpen == true
                    )
                    StatusIndicator(
                        label = "Olhando",
                        isActive = faceResult?.isLookingAtCamera == true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botão de captura manual simplificado
                Button(
                        onClick = {
                            if (faceDetected && !isCapturing && imageCapture != null) {
                                isCapturing = true
                                
                                captureImage(
                                    imageCapture = imageCapture,
                                    context = context,
                                    onImageCaptured = { uri ->
                                        isVerifying = true
                                        verificationMessage = "Validando sua identidade..."
                                        
                                        scope.launch {
                                            viewModel.setSelfie(uri)
                                            
                                            // CRÍTICO: Fazer upload dos documentos antes da verificação facial
                                            val uploadResult = viewModel.uploadDocumentsForVerification()
                                            if (uploadResult.isFailure) {
                                                verificationMessage = "Erro ao fazer upload dos documentos. Tente novamente."
                                                kotlinx.coroutines.delay(2000)
                                                isCapturing = false
                                                isVerifying = false
                                                return@launch
                                            }
                                            
                                            // Aguardar um pouco para o usuário ver a tela de loading
                                            kotlinx.coroutines.delay(500)
                                            
                                            val matchOk = try {
                                                // Executar verificação facial
                                                val result = viewModel.verifyFaceMatch()
                                                
                                                // Aguardar atualização do estado
                                                var attempts = 0
                                                while (attempts < 50 && viewModel.uiState.value.isVerifyingFace) {
                                                    kotlinx.coroutines.delay(100)
                                                    attempts++
                                                }
                                                
                                                // Verificar resultado final do estado
                                                val finalState = viewModel.uiState.value
                                                val success = finalState.faceVerificationSuccess == true
                                                
                                                if (success) {
                                                    verificationMessage = "Identidade verificada com sucesso!"
                                                    kotlinx.coroutines.delay(1500) // Dar tempo para o usuário ver a mensagem
                                                } else {
                                                    verificationMessage = finalState.faceVerificationError ?: "Verificação falhou. Tente novamente."
                                                    kotlinx.coroutines.delay(2000) // Mostrar erro por mais tempo
                                                }
                                                
                                                success
                                            } catch (e: Exception) {
                                                android.util.Log.e("FacialVerification", "Erro na verificação facial: ${e.message}", e)
                                                verificationMessage = "Erro na verificação: ${e.message}"
                                                kotlinx.coroutines.delay(2000)
                                                false
                                            }
                                            
                                            isCapturing = false
                                            isVerifying = false
                                            
                                            // Só redirecionar após a verificação estar completa
                                            if (matchOk) {
                                                onVerificationComplete() // Volta para IdentityVerificationScreen
                                            } else {
                                                // Se falhou, voltar para a tela anterior e mostrar erro lá
                                                onVerificationComplete() // Volta para IdentityVerificationScreen (vai mostrar dialog de erro)
                                            }
                                        }
                                    },
                                    onError = {
                                        isCapturing = false
                                        isVerifying = false
                                        // Erro na captura, não precisa voltar
                                    }
                                )
                            }
                        },
                        enabled = faceDetected && !isCapturing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (faceDetected) TaskGoGreen else Color.Gray
                        )
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (faceDetected) "Confirmar e Capturar" else "Aguardando detecção do rosto...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
            }
        }
    }
    
}

@Composable
private fun StatusIndicator(
    label: String,
    isActive: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isActive) TaskGoSuccessGreen else Color.Gray,
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

private fun captureImage(
    imageCapture: ImageCapture?,
    context: Context,
    onImageCaptured: (Uri) -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) {
        onError()
        return
    }
    
    val photoFile = File(
        context.getExternalFilesDir(null),
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )
    
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageCaptured(Uri.fromFile(photoFile))
            }
            
            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("FacialVerification", "Erro ao capturar imagem: ${exception.message}", exception)
                onError()
            }
        }
    )
}

