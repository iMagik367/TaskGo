package com.taskgoapp.taskgo.feature.checkout.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

data class PixPaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pixKey: String? = null,
    val qrCodeData: String? = null,
    val qrCodeBitmap: Bitmap? = null,
    val amount: Double = 0.0,
    val expiresAt: Long? = null,
    val paymentId: String? = null,
    val isPaymentVerified: Boolean = false,
    val isCheckingPayment: Boolean = false
)

@HiltViewModel
class PixPaymentViewModel @Inject constructor(
    private val functionsService: FirebaseFunctionsService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PixPaymentUiState())
    val uiState: StateFlow<PixPaymentUiState> = _uiState.asStateFlow()

    fun createPixPayment(orderId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = functionsService.createPixPayment(orderId)
                result.onSuccess { data ->
                    val pixKey = data["pixKey"] as? String
                    val qrCodeData = data["qrCodeData"] as? String
                    val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
                    val expiresAt = (data["expiresAt"] as? Number)?.toLong()
                    val paymentId = data["paymentId"] as? String

                    // Generate QR code bitmap
                    val qrBitmap = qrCodeData?.let { generateQRCode(it, 512) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pixKey = pixKey,
                        qrCodeData = qrCodeData,
                        qrCodeBitmap = qrBitmap,
                        amount = amount,
                        expiresAt = expiresAt,
                        paymentId = paymentId,
                        error = null
                    )
                    
                    // Start polling for payment verification
                    paymentId?.let { startPaymentPolling(it) }
                }.onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Erro ao criar pagamento PIX"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao criar pagamento PIX"
                )
            }
        }
    }

    fun copyPixKey() {
        val pixKey = _uiState.value.pixKey ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chave PIX", pixKey)
        clipboard.setPrimaryClip(clip)
    }

    private fun generateQRCode(data: String, size: Int): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("PixPaymentViewModel", "Erro ao gerar QR code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Start polling to verify PIX payment status
     */
    private fun startPaymentPolling(paymentId: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // 5 minutes (60 * 5 seconds)
            
            while (attempts < maxAttempts && !_uiState.value.isPaymentVerified) {
                delay(5000) // Check every 5 seconds
                attempts++
                
                _uiState.value = _uiState.value.copy(isCheckingPayment = true)
                
                val result = functionsService.verifyPixPayment(paymentId)
                result.onSuccess { data ->
                    val status = data["status"] as? String
                    val paid = data["paid"] as? Boolean ?: false
                    
                    if (paid && status == "succeeded") {
                        _uiState.value = _uiState.value.copy(
                            isPaymentVerified = true,
                            isCheckingPayment = false
                        )
                    } else if (status == "expired") {
                        _uiState.value = _uiState.value.copy(
                            error = "Pagamento PIX expirado. Por favor, gere um novo pagamento.",
                            isCheckingPayment = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isCheckingPayment = false)
                    }
                }.onFailure {
                    // Continue polling even if verification fails temporarily
                    _uiState.value = _uiState.value.copy(isCheckingPayment = false)
                }
            }
            
            if (!_uiState.value.isPaymentVerified && attempts >= maxAttempts) {
                _uiState.value = _uiState.value.copy(
                    error = "Tempo de verificação do pagamento expirado. Verifique manualmente.",
                    isCheckingPayment = false
                )
            }
        }
    }
    
    /**
     * Manually verify payment (for testing/admin)
     */
    fun verifyPayment() {
        val paymentId = _uiState.value.paymentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingPayment = true)
            val result = functionsService.verifyPixPayment(paymentId)
            result.onSuccess { data ->
                val status = data["status"] as? String
                val paid = data["paid"] as? Boolean ?: false
                
                if (paid && status == "succeeded") {
                    _uiState.value = _uiState.value.copy(
                        isPaymentVerified = true,
                        isCheckingPayment = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCheckingPayment = false,
                        error = "Pagamento ainda não confirmado. Status: $status"
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPayment = false,
                    error = throwable.message ?: "Erro ao verificar pagamento"
                )
            }
        }
    }
}

