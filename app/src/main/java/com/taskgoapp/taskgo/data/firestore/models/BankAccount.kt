package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * Conta bancária do vendedor para receber pagamentos
 */
data class BankAccount(
    val id: String = "",
    val userId: String = "", // ID do vendedor
    val bankName: String = "", // Nome do banco
    val bankCode: String = "", // Código do banco (ex: 001 para Banco do Brasil)
    val agency: String = "", // Agência
    val account: String = "", // Conta
    val accountType: String = "", // "CHECKING" ou "SAVINGS"
    val accountHolderName: String = "", // Nome do titular
    val accountHolderDocument: String = "", // CPF ou CNPJ do titular
    val accountHolderDocumentType: String = "", // "CPF" ou "CNPJ"
    val stripeAccountId: String? = null, // ID da conta Stripe Connect (se configurada)
    val isDefault: Boolean = false, // Conta padrão para recebimentos
    val isVerified: Boolean = false, // Conta verificada pelo Stripe
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

