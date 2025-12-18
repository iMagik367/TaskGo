package com.taskgoapp.taskgo.core.security

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Analisador de face em tempo real usando ML Kit e CameraX ImageAnalysis
 * Processa frames da câmera em tempo real para detectar e validar faces
 */
class RealTimeFaceAnalyzer(
    private val onFaceDetected: (FaceDetectionResult) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val faceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .enableTracking()
            .setMinFaceSize(0.15f) // Face deve ocupar pelo menos 15% da imagem
            .build()
    )
    
    private var isProcessing = false
    private var frameSkipCount = 0
    private val FRAME_SKIP = 2 // Processa 1 frame a cada 3 (reduz carga)
    
    // Estados de vivacidade
    private var lastEyesOpen: Pair<Float?, Float?> = 1.0f to 1.0f
    private var blinkInProgress = false
    private var blinkDetectedInternal = false
    private var lastBlinkTimeMs = 0L
    private val blinkMinCloseProb = 0.3f
    private val blinkOpenProb = 0.6f
    private val blinkTimeoutMs = 2500L
    private var lookLeftDetectedInternal = false
    private var lookRightDetectedInternal = false
    private var lookLeftStartMs = 0L
    private var lookRightStartMs = 0L
    private val lookHoldMs = 400L
    private val lookYawLeft = -18f
    private val lookYawRight = 18f
    
    override fun analyze(imageProxy: ImageProxy) {
        // Pular frames para reduzir carga de processamento
        if (frameSkipCount < FRAME_SKIP) {
            frameSkipCount++
            imageProxy.close()
            return
        }
        frameSkipCount = 0
        
        // Evitar processar múltiplos frames simultaneamente
        if (isProcessing) {
            imageProxy.close()
            return
        }
        
        isProcessing = true
        
        try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                isProcessing = false
                return
            }
            
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    val result = analyzeFaces(faces, imageProxy.width, imageProxy.height)
                    onFaceDetected(result)
                    imageProxy.close()
                    isProcessing = false
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao processar face: ${e.message}", e)
                    onFaceDetected(FaceDetectionResult.noFace())
                    imageProxy.close()
                    isProcessing = false
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar frame: ${e.message}", e)
            imageProxy.close()
            isProcessing = false
        }
    }
    
    /**
     * Analisa as faces detectadas e retorna resultado detalhado
     */
    private fun analyzeFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int): FaceDetectionResult {
        if (faces.isEmpty()) {
            return FaceDetectionResult.noFace()
        }
        
        // Usar apenas a primeira face detectada (mais próxima/central)
        val face = faces[0]
        val boundingBox = face.boundingBox
        
        // Calcular posição relativa da face na imagem
        val faceCenterX = boundingBox.centerX().toFloat() / imageWidth
        val faceCenterY = boundingBox.centerY().toFloat() / imageHeight
        
        // Calcular tamanho relativo da face
        val faceWidth = boundingBox.width().toFloat() / imageWidth
        val faceHeight = boundingBox.height().toFloat() / imageHeight
        val faceSize = (faceWidth + faceHeight) / 2f
        
        // Verificar se a face está centralizada (dentro de 40% do centro)
        val isCentered = faceCenterX in 0.3f..0.7f && faceCenterY in 0.3f..0.7f
        
        // Verificar tamanho da face (ideal: 30-50% da imagem)
        val isGoodSize = faceSize in 0.25f..0.55f
        
        // Verificar se olhos estão abertos (se disponível)
        val leftEyeOpen = face.leftEyeOpenProbability
        val rightEyeOpen = face.rightEyeOpenProbability
        val eyesOpen = if (leftEyeOpen != null && rightEyeOpen != null) {
            leftEyeOpen > 0.5f && rightEyeOpen > 0.5f
        } else {
            true // Se não disponível, assumir OK
        }
        
        // Verificar se está sorrindo (opcional - pode indicar pose natural)
        val smiling = face.smilingProbability
        val isNaturalPose = smiling == null || smiling in 0.2f..0.8f
        
        // Verificar se está olhando para a câmera (head euler angles)
        val headEulerY = face.headEulerAngleY // Rotação horizontal (-30 a +30 é bom)
        val headEulerZ = face.headEulerAngleZ // Inclinação (-15 a +15 é bom)
        val isLookingAtCamera = Math.abs(headEulerY) < 25f && Math.abs(headEulerZ) < 15f
        
        // Vivacidade: detecção de piscar e olhar lateral sustentado
        val now = System.currentTimeMillis()
        if (leftEyeOpen != null && rightEyeOpen != null) {
            val wasOpen = (lastEyesOpen.first ?: 1.0f) > blinkOpenProb && (lastEyesOpen.second ?: 1.0f) > blinkOpenProb
            val nowClosed = leftEyeOpen < blinkMinCloseProb && rightEyeOpen < blinkMinCloseProb
            val nowOpen = leftEyeOpen > blinkOpenProb && rightEyeOpen > blinkOpenProb
            if (!blinkInProgress && wasOpen && nowClosed) {
                blinkInProgress = true
            } else if (blinkInProgress && nowOpen) {
                blinkDetectedInternal = true
                lastBlinkTimeMs = now
                blinkInProgress = false
            } else if (blinkInProgress && (now - lastBlinkTimeMs) > blinkTimeoutMs) {
                blinkInProgress = false
            }
            lastEyesOpen = leftEyeOpen to rightEyeOpen
        }
        if (headEulerY <= lookYawLeft) {
            if (lookLeftStartMs == 0L) lookLeftStartMs = now
            if (!lookLeftDetectedInternal && (now - lookLeftStartMs) >= lookHoldMs) {
                lookLeftDetectedInternal = true
            }
        } else {
            lookLeftStartMs = 0L
        }
        if (headEulerY >= lookYawRight) {
            if (lookRightStartMs == 0L) lookRightStartMs = now
            if (!lookRightDetectedInternal && (now - lookRightStartMs) >= lookHoldMs) {
                lookRightDetectedInternal = true
            }
        } else {
            lookRightStartMs = 0L
        }
        
        // Calcular score geral de qualidade
        var qualityScore = 0.0f
        
        if (isCentered) qualityScore += 0.3f
        if (isGoodSize) qualityScore += 0.3f
        if (eyesOpen) qualityScore += 0.2f
        if (isLookingAtCamera) qualityScore += 0.2f
        
        val isGoodQuality = qualityScore >= 0.7f && isCentered && isGoodSize && eyesOpen && isLookingAtCamera
        
        return FaceDetectionResult(
            hasFace = true,
            isCentered = isCentered,
            isGoodSize = isGoodSize,
            eyesOpen = eyesOpen,
            isLookingAtCamera = isLookingAtCamera,
            qualityScore = qualityScore,
            isGoodQuality = isGoodQuality,
            faceSize = faceSize,
            faceCenterX = faceCenterX,
            faceCenterY = faceCenterY,
            headEulerY = headEulerY,
            headEulerZ = headEulerZ,
            leftEyeOpen = leftEyeOpen,
            rightEyeOpen = rightEyeOpen,
            smiling = smiling,
            blinkDetected = blinkDetectedInternal,
            lookLeft = lookLeftDetectedInternal,
            lookRight = lookRightDetectedInternal
        )
    }
    
    fun release() {
        faceDetector.close()
    }
    
    companion object {
        private const val TAG = "RealTimeFaceAnalyzer"
    }
}

/**
 * Resultado da detecção facial em tempo real
 */
data class FaceDetectionResult(
    val hasFace: Boolean,
    val isCentered: Boolean = false,
    val isGoodSize: Boolean = false,
    val eyesOpen: Boolean = false,
    val isLookingAtCamera: Boolean = false,
    val qualityScore: Float = 0f,
    val isGoodQuality: Boolean = false,
    val faceSize: Float = 0f,
    val faceCenterX: Float = 0.5f,
    val faceCenterY: Float = 0.5f,
    val headEulerY: Float = 0f,
    val headEulerZ: Float = 0f,
    val leftEyeOpen: Float? = null,
    val rightEyeOpen: Float? = null,
    val smiling: Float? = null,
    val blinkDetected: Boolean = false,
    val lookLeft: Boolean = false,
    val lookRight: Boolean = false
) {
    companion object {
        fun noFace() = FaceDetectionResult(hasFace = false)
    }
}

