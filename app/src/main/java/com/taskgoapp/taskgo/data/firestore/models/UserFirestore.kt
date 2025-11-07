package com.taskgoapp.taskgo.data.firestore.models

import com.taskgoapp.taskgo.core.model.Address
import java.util.Date

data class UserFirestore(
    val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoURL: String? = null,
    val phone: String? = null,
    val role: String = "client", // client, provider, admin
    val profileComplete: Boolean = false,
    val verified: Boolean = false,
    
    // Identity Verification Fields
    val cpf: String? = null,
    val cnpj: String? = null,
    val birthDate: Date? = null,
    val documentFront: String? = null, // URL da foto do documento (frente)
    val documentBack: String? = null, // URL da foto do documento (verso)
    val selfie: String? = null, // URL da selfie para verificação facial
    val address: Address? = null,
    val addressProof: String? = null, // URL do comprovante de endereço
    val verifiedAt: Date? = null,
    val verifiedBy: String? = null,
    
    // Biometric & 2FA
    val biometricEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val twoFactorMethod: String? = null, // "sms", "email", "authenticator"
    
    // Payment Fields
    val stripeAccountId: String? = null,
    val stripeChargesEnabled: Boolean = false,
    val stripePayoutsEnabled: Boolean = false,
    val stripeDetailsSubmitted: Boolean = false,
    
    // Document Fields (legacy)
    val documents: List<String>? = null,
    val documentsApproved: Boolean = false,
    val documentsApprovedAt: Date? = null,
    val documentsApprovedBy: String? = null,
    
    // Timestamps
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)





