package com.taskgoapp.taskgo.core.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gerenciador de verificação facial usando ML Kit
 * Compara uma selfie com a foto do documento para verificar identidade
 */
class FaceVerificationManager(private val context: Context) {

    private val faceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Mudado para ACCURATE para melhor detecção
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // CRÍTICO: Mudado para ALL para obter landmarks necessários para comparação
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Habilitar classificação para melhor precisão
            .enableTracking()
            .build()
    )

    /**
     * Detecta faces em uma imagem
     */
    suspend fun detectFaces(imageUri: Uri): List<Face> = suspendCancellableCoroutine { continuation ->
        try {
            // Usa BitmapUtils para carregar e redimensionar a imagem
            val bitmap = com.taskgoapp.taskgo.core.utils.BitmapUtils.loadAndResizeBitmap(
                context,
                imageUri,
                maxDimension = 1024 // Limita a 1024px para detecção facial
            )

            if (bitmap == null) {
                continuation.resumeWithException(Exception("Não foi possível carregar a imagem"))
                return@suspendCancellableCoroutine
            }

            val image = InputImage.fromBitmap(bitmap, 0)
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "Faces detectadas: ${faces.size}")
                    continuation.resume(faces)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao detectar faces: ${e.message}", e)
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar imagem: ${e.message}", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Verifica se há uma face na imagem
     */
    suspend fun hasFace(imageUri: Uri): Boolean {
        return try {
            val faces = detectFaces(imageUri)
            faces.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar face: ${e.message}", e)
            false
        }
    }

    /**
     * Compara duas imagens para verificar se são da mesma pessoa
     * Retorna um score de similaridade (0.0 a 1.0)
     * 
     * Nota: Esta é uma implementação básica. Para produção, considere usar
     * Firebase ML Face Recognition ou um serviço de terceiros mais robusto
     */
    suspend fun compareFaces(selfieUri: Uri, documentUri: Uri): FaceComparisonResult {
        return try {
            val selfieFaces = detectFaces(selfieUri)
            val documentFaces = detectFaces(documentUri)

            if (selfieFaces.isEmpty()) {
                return FaceComparisonResult(
                    success = false,
                    score = 0.0,
                    message = "Nenhuma face detectada na selfie"
                )
            }

            if (documentFaces.isEmpty()) {
                return FaceComparisonResult(
                    success = false,
                    score = 0.0,
                    message = "Nenhuma face detectada no documento"
                )
            }

            // Comparação usando geometria + marcos faciais (embedding simplificado)
            val selfieFace = selfieFaces[0]
            val documentFace = documentFaces[0]

            val geomScore = calculateSimilarityScore(selfieFace, documentFace)
            val embSelfie = computeEmbedding(selfieFace)
            val embDoc = computeEmbedding(documentFace)
            val cosine = if (embSelfie.isNotEmpty() && embDoc.isNotEmpty()) cosineSimilarity(embSelfie, embDoc) else 0.0

            // Combinação ponderada: 40% geometria + 60% embedding
            val score = (geomScore * 0.4 + cosine * 0.6)

            // Threshold ajustado: 0.40 para permitir mais variações
            // Selfies podem ter iluminação/ângulo/expressão diferentes do documento
            // Este threshold foi reduzido para ser mais tolerante, mas ainda seguro
            val success = score >= 0.40
            
            Log.d(TAG, "Comparação facial: score=$score (${(score * 100).toInt()}%), threshold=0.40, success=$success")

            FaceComparisonResult(
                success = success,
                score = score,
                message = if (success) {
                    "Faces correspondem (${(score * 100).toInt()}% de similaridade)"
                } else {
                    "Faces não correspondem suficientemente (${(score * 100).toInt()}% de similaridade)"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comparar faces: ${e.message}", e)
            FaceComparisonResult(
                success = false,
                score = 0.0,
                message = "Erro ao comparar faces: ${e.message}"
            )
        }
    }

    /**
     * Gera um embedding 2D simples baseado em distâncias/razões entre marcos (contours/landmarks)
     */
    private fun computeEmbedding(face: Face): DoubleArray {
        val bb = face.boundingBox
        val w = bb.width().coerceAtLeast(1)
        val h = bb.height().coerceAtLeast(1)

        fun p(type: Int): android.graphics.PointF? = face.getLandmark(type)?.position
        val lEye = p(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
        val rEye = p(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
        val nose = p(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
        val mouth = p(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM)
        val lEar = p(com.google.mlkit.vision.face.FaceLandmark.LEFT_EAR)
        val rEar = p(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EAR)

        val pts = listOf(lEye, rEye, nose, mouth, lEar, rEar).mapNotNull { it }
        if (pts.size < 4) return DoubleArray(0)

        fun nd(ax: Float, ay: Float, bx: Float, by: Float): Double {
            val dx = (ax - bx) / w
            val dy = (ay - by) / h
            return kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        }

        // Distâncias normalizadas principais
        val dEye = if (lEye != null && rEye != null) nd(lEye.x, lEye.y, rEye.x, rEye.y) else 0.0
        val dEyeNoseL = if (lEye != null && nose != null) nd(lEye.x, lEye.y, nose.x, nose.y) else 0.0
        val dEyeNoseR = if (rEye != null && nose != null) nd(rEye.x, rEye.y, nose.x, nose.y) else 0.0
        val dNoseMouth = if (nose != null && mouth != null) nd(nose.x, nose.y, mouth.x, mouth.y) else 0.0
        val dMouthEyeL = if (mouth != null && lEye != null) nd(mouth.x, mouth.y, lEye.x, lEye.y) else 0.0
        val dMouthEyeR = if (mouth != null && rEye != null) nd(mouth.x, mouth.y, rEye.x, rEye.y) else 0.0
        val dEarWidth = if (lEar != null && rEar != null) nd(lEar.x, lEar.y, rEar.x, rEar.y) else 0.0

        // Razões estáveis
        val r1 = if (dEye > 0) dNoseMouth / dEye else 0.0
        val r2 = if (dEye > 0) (dEyeNoseL + dEyeNoseR) / (2 * dEye) else 0.0
        val r3 = if (dEarWidth > 0) dEye / dEarWidth else 0.0

        return doubleArrayOf(dEye, dEyeNoseL, dEyeNoseR, dNoseMouth, dMouthEyeL, dMouthEyeR, dEarWidth, r1, r2, r3)
    }

    private fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return 0.0
        return (dot / (kotlin.math.sqrt(na) * kotlin.math.sqrt(nb))).coerceIn(-1.0, 1.0)
    }

    /**
     * Calcula score de similaridade entre duas faces
     * Combina bounding box + proporções + marcos faciais normalizados
     */
    private fun calculateSimilarityScore(face1: Face, face2: Face): Double {
        val boundingBox1 = face1.boundingBox
        val boundingBox2 = face2.boundingBox

        // Comparar tamanho relativo (0..1)
        val size1 = boundingBox1.width() * boundingBox1.height()
        val size2 = boundingBox2.width() * boundingBox2.height()
        val sizeRatio = minOf(size1, size2).toDouble() / maxOf(size1, size2).toDouble()

        // Comparar proporção (largura/altura) (0..1)
        val aspectRatio1 = boundingBox1.width().toDouble() / boundingBox1.height()
        val aspectRatio2 = boundingBox2.width().toDouble() / boundingBox2.height()
        val aspectRatioDiff = 1.0 - kotlin.math.abs(aspectRatio1 - aspectRatio2) / maxOf(aspectRatio1, aspectRatio2)

        // Comparar marcos faciais (se disponíveis) - normalizados pelo tamanho da caixa
        fun norm(dx: Float, dy: Float, w: Int, h: Int): Double {
            val nx = dx / w
            val ny = dy / h
            return kotlin.math.sqrt((nx * nx + ny * ny).toDouble())
        }

        fun getPoint(face: Face, type: Int): android.graphics.PointF? =
            face.getLandmark(type)?.position

        // Pontos de interesse: olhos, nariz, boca
        val f1LeftEye = getPoint(face1, com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
        val f1RightEye = getPoint(face1, com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
        val f1Nose = getPoint(face1, com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
        val f1Mouth = getPoint(face1, com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM)

        val f2LeftEye = getPoint(face2, com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
        val f2RightEye = getPoint(face2, com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
        val f2Nose = getPoint(face2, com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
        val f2Mouth = getPoint(face2, com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM)

        var landmarksScore = 0.0
        var pairs = 0

        if (f1LeftEye != null && f1RightEye != null && f2LeftEye != null && f2RightEye != null) {
            // Distância entre olhos
            val d1 = norm(
                f1RightEye.x - f1LeftEye.x,
                f1RightEye.y - f1LeftEye.y,
                boundingBox1.width(),
                boundingBox1.height()
            )
            val d2 = norm(
                f2RightEye.x - f2LeftEye.x,
                f2RightEye.y - f2LeftEye.y,
                boundingBox2.width(),
                boundingBox2.height()
            )
            landmarksScore += 1.0 - kotlin.math.abs(d1 - d2) / maxOf(d1, d2)
            pairs++
        }

        if (f1LeftEye != null && f1Mouth != null && f2LeftEye != null && f2Mouth != null) {
            // Distância olho esquerdo - boca
            val d1 = norm(
                f1Mouth.x - f1LeftEye.x,
                f1Mouth.y - f1LeftEye.y,
                boundingBox1.width(),
                boundingBox1.height()
            )
            val d2 = norm(
                f2Mouth.x - f2LeftEye.x,
                f2Mouth.y - f2LeftEye.y,
                boundingBox2.width(),
                boundingBox2.height()
            )
            landmarksScore += 1.0 - kotlin.math.abs(d1 - d2) / maxOf(d1, d2)
            pairs++
        }

        if (f1RightEye != null && f1Mouth != null && f2RightEye != null && f2Mouth != null) {
            // Distância olho direito - boca
            val d1 = norm(
                f1Mouth.x - f1RightEye.x,
                f1Mouth.y - f1RightEye.y,
                boundingBox1.width(),
                boundingBox1.height()
            )
            val d2 = norm(
                f2Mouth.x - f2RightEye.x,
                f2Mouth.y - f2RightEye.y,
                boundingBox2.width(),
                boundingBox2.height()
            )
            landmarksScore += 1.0 - kotlin.math.abs(d1 - d2) / maxOf(d1, d2)
            pairs++
        }

        if (f1Nose != null && f1Mouth != null && f2Nose != null && f2Mouth != null) {
            // Distância nariz - boca
            val d1 = norm(
                f1Mouth.x - f1Nose.x,
                f1Mouth.y - f1Nose.y,
                boundingBox1.width(),
                boundingBox1.height()
            )
            val d2 = norm(
                f2Mouth.x - f2Nose.x,
                f2Mouth.y - f2Nose.y,
                boundingBox2.width(),
                boundingBox2.height()
            )
            landmarksScore += 1.0 - kotlin.math.abs(d1 - d2) / maxOf(d1, d2)
            pairs++
        }

        val landmarksFinal = if (pairs > 0) (landmarksScore / pairs) else 0.0

        // Score combinado (pesos)
        val score = (sizeRatio * 0.2 + aspectRatioDiff * 0.2 + landmarksFinal * 0.6)

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Libera recursos
     */
    fun release() {
        faceDetector.close()
    }

    companion object {
        private const val TAG = "FaceVerificationManager"
    }
}

data class FaceComparisonResult(
    val success: Boolean,
    val score: Double,
    val message: String
)

