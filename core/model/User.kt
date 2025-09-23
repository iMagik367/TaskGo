package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String? = null,
    val userType: UserType = UserType.CLIENT,
    val status: UserStatus = UserStatus.ACTIVE,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val rating: Float = 0f,
    val totalReviews: Int = 0,
    val documents: List<Document> = emptyList(),
    val suspensionEnd: Long? = null,
    val bannedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class UserType {
    CLIENT,     // Cliente que contrata serviços
    PROVIDER,   // Prestador de serviços
    ADMIN       // Administrador do sistema
}

enum class UserStatus {
    ACTIVE,     // Usuário ativo
    SUSPENDED,  // Suspenso temporariamente
    BANNED,     // Banido permanentemente
    DELETED     // Conta deletada
}

enum class VerificationStatus {
    PENDING,    // Aguardando verificação
    IN_REVIEW,  // Em análise
    VERIFIED,   // Verificado
    REJECTED    // Verificação rejeitada
}

@Serializable
data class Document(
    val id: String,
    val type: DocumentType,
    val url: String,
    val status: DocumentStatus,
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val rejectionReason: String? = null
)

enum class DocumentType {
    ID,                 // Documento de identidade
    CPF,               // CPF
    PROOF_OF_ADDRESS,  // Comprovante de residência
    CRIMINAL_RECORD,   // Antecedentes criminais
    SELFIE,           // Selfie com documento
    OTHER             // Outros documentos
}

enum class DocumentStatus {
    PENDING,    // Aguardando verificação
    APPROVED,   // Aprovado
    REJECTED,   // Rejeitado
    EXPIRED     // Expirado
}
