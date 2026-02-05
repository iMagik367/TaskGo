package com.taskgoapp.taskgo.data.firestore.models

import com.taskgoapp.taskgo.core.model.Address
import java.util.Date

data class UserFirestore(
    val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoURL: String? = null,
    val phone: String? = null,
    val role: String, // SEMPRE será "partner" ou "client" - garantido pelo sistema
    val pendingAccountType: Boolean = false, // Flag para indicar que o app precisa mostrar dialog de seleção de tipo de conta
    val profileComplete: Boolean = false,
    val verified: Boolean = false,
    
    // Identity Verification Fields
    val cpf: String? = null,
    val rg: String? = null,
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
    
    // Provider Preferences
    val preferredCategories: List<String>? = null, // Categories the provider prefers to receive orders for
    
    // Location Fields (Lei 1: Fonte única de verdade - na raiz do documento)
    val city: String? = null, // Lei 1: Localização vem EXCLUSIVAMENTE de users/{userId}.city na raiz
    val state: String? = null, // Lei 1: Localização vem EXCLUSIVAMENTE de users/{userId}.state na raiz
    
    // User Identifier (calculado automaticamente)
    val userIdentifier: String? = null, // ID único baseado em role, localização e categorias
    
    // App Preferences
    val notificationSettings: NotificationSettingsFirestore? = null,
    val privacySettings: PrivacySettingsFirestore? = null,
    val language: String? = "pt",
    
    // Rating (calculado a partir de avaliações)
    val rating: Double? = null, // Média de avaliações recebidas
    
    // Timestamps
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class NotificationSettingsFirestore(
    val push: Boolean = true,
    val promos: Boolean = true,
    val sound: Boolean = true,
    val lockscreen: Boolean = true,
    val email: Boolean = false,
    val sms: Boolean = false
)

data class PrivacySettingsFirestore(
    val locationSharing: Boolean = true,
    val profileVisible: Boolean = true,
    val contactInfoSharing: Boolean = false,
    val analytics: Boolean = true,
    val personalizedAds: Boolean = false,
    val dataCollection: Boolean = true,
    val thirdPartySharing: Boolean = false
)





