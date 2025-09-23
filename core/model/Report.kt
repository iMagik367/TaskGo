package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String = "",
    val type: ReportType,
    val status: ReportStatus = ReportStatus.PENDING,
    val priority: ReportPriority = ReportPriority.MEDIUM,
    val description: String,
    val reporterId: String,
    val reportedContent: ReportedContent,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolution: Resolution? = null
)

enum class ReportType {
    INAPPROPRIATE_CONTENT,  // Conteúdo impróprio
    FAKE_PROFILE,          // Perfil falso
    SPAM,                  // Spam
    HARASSMENT,            // Assédio
    SCAM,                  // Golpe
    OTHER                  // Outros
}

enum class ReportStatus {
    PENDING,    // Aguardando análise
    IN_REVIEW,  // Em análise
    RESOLVED,   // Resolvido
    DISMISSED   // Descartado
}

enum class ReportPriority {
    LOW,    // Baixa prioridade
    MEDIUM, // Média prioridade
    HIGH,   // Alta prioridade
    URGENT  // Urgente
}

@Serializable
sealed class ReportedContent {
    abstract val id: String
    abstract val type: String
}

@Serializable
data class UserProfileContent(
    override val id: String,
    override val type: String = "USER_PROFILE",
    val name: String,
    val email: String,
    val accountType: UserType,
    val verificationStatus: VerificationStatus
) : ReportedContent()

@Serializable
data class ServiceContent(
    override val id: String,
    override val type: String = "SERVICE",
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val status: String
) : ReportedContent()

@Serializable
data class ReviewContent(
    override val id: String,
    override val type: String = "REVIEW",
    val rating: Int,
    val comment: String,
    val authorId: String,
    val authorName: String,
    val createdAt: Long
) : ReportedContent()

@Serializable
data class ChatMessageContent(
    override val id: String,
    override val type: String = "CHAT_MESSAGE",
    val content: String,
    val senderId: String,
    val senderName: String,
    val sentAt: Long
) : ReportedContent()

@Serializable
data class Resolution(
    val action: ModerationAction,
    val notes: String,
    val adminId: String,
    val adminName: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ModerationAction {
    WARNING,         // Advertência
    CONTENT_REMOVAL, // Remoção de conteúdo
    TEMPORARY_BAN,   // Suspensão temporária
    PERMANENT_BAN    // Banimento permanente
}