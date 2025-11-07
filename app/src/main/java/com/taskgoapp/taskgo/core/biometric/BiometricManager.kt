package com.taskgoapp.taskgo.core.biometric

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricManager(private val context: Context) {

    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = AndroidBiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: Activity,
        title: String = "Autenticação Biométrica",
        subtitle: String = "Use sua biometria para continuar",
        negativeButtonText: String = "Cancelar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val status = isBiometricAvailable()
        if (status != BiometricStatus.AVAILABLE) {
            onError(getStatusMessage(status))
            return
        }

        // Converter Activity para FragmentActivity se necessário
        val fragmentActivity = activity as? FragmentActivity
            ?: run {
                // Se não for FragmentActivity, não podemos usar biometria
                onError("Biometria requer FragmentActivity")
                return
            }

        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autenticação falhou. Tente novamente.")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private fun getStatusMessage(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.NO_HARDWARE -> "Este dispositivo não possui hardware biométrico."
            BiometricStatus.HARDWARE_UNAVAILABLE -> "Hardware biométrico não está disponível."
            BiometricStatus.NONE_ENROLLED -> "Nenhuma biometria cadastrada. Configure no dispositivo."
            BiometricStatus.UNAVAILABLE -> "Autenticação biométrica não disponível."
            BiometricStatus.AVAILABLE -> "Disponível"
        }
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNAVAILABLE
}

