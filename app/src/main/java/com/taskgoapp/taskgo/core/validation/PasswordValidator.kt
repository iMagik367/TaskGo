package com.taskgoapp.taskgo.core.validation

/**
 * Enum para representar a força da senha
 */
enum class PasswordStrength {
    MUITO_FRACA,  // Não atende requisitos mínimos
    FRACA,        // Atende apenas requisitos mínimos
    MEDIA,        // Atende requisitos + extensão
    FORTE,        // Atende requisitos + boa extensão
    MUITO_FORTE   // Atende todos requisitos + extensão excelente
}

/**
 * Classe para validação de senha
 * Requisitos:
 * - Mínimo 8 caracteres
 * - Pelo menos 1 número
 * - Pelo menos 1 caractere especial
 * - Pelo menos 1 letra maiúscula
 */
class PasswordValidator {
    
    /**
     * Valida se a senha atende todos os requisitos
     */
    fun validate(password: String): ValidationResult {
        if (password.isEmpty()) {
            return ValidationResult.Invalid("Senha é obrigatória")
        }
        
        val requirements = getPasswordRequirements(password)
        val missingRequirements = requirements.filter { !it.met }
        
        if (missingRequirements.isNotEmpty()) {
            val messages = missingRequirements.map { it.message }
            return ValidationResult.Invalid(messages.joinToString(", "))
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Calcula a força da senha
     */
    fun calculateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength.MUITO_FRACA
        }
        
        val requirements = getPasswordRequirements(password)
        val metRequirements = requirements.count { it.met }
        val totalRequirements = requirements.size
        
        // Se não atende todos os requisitos básicos
        if (metRequirements < totalRequirements) {
            return PasswordStrength.MUITO_FRACA
        }
        
        // Calcula pontuação baseada na extensão e diversidade
        var score = 0
        
        // Extensão (0-3 pontos)
        when {
            password.length >= 16 -> score += 3
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
        }
        
        // Diversidade de caracteres (0-2 pontos)
        val hasNumbers = password.any { it.isDigit() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        
        val diversityCount = listOf(hasNumbers, hasLowercase, hasUppercase, hasSpecial).count { it }
        when {
            diversityCount == 4 -> score += 2
            diversityCount == 3 -> score += 1
        }
        
        // Retorna força baseada na pontuação
        return when {
            score >= 5 -> PasswordStrength.MUITO_FORTE
            score >= 4 -> PasswordStrength.FORTE
            score >= 3 -> PasswordStrength.MEDIA
            score >= 1 -> PasswordStrength.FRACA
            else -> PasswordStrength.MUITO_FRACA
        }
    }
    
    /**
     * Obtém os requisitos da senha e se foram atendidos
     */
    private fun getPasswordRequirements(password: String): List<PasswordRequirement> {
        return listOf(
            PasswordRequirement(
                met = password.length >= 8,
                message = "Mínimo 8 caracteres"
            ),
            PasswordRequirement(
                met = password.any { it.isDigit() },
                message = "Pelo menos 1 número"
            ),
            PasswordRequirement(
                met = password.any { it.isUpperCase() },
                message = "Pelo menos 1 letra maiúscula"
            ),
            PasswordRequirement(
                met = password.any { !it.isLetterOrDigit() },
                message = "Pelo menos 1 caractere especial"
            )
        )
    }
    
    /**
     * Data class para representar um requisito de senha
     */
    private data class PasswordRequirement(
        val met: Boolean,
        val message: String
    )
}

