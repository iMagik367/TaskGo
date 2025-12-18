package com.taskgoapp.taskgo.core.validation

/**
 * Serviço para validação de documentos brasileiros (CPF e RG)
 */
class DocumentValidator {
    
    companion object {
        // Estados brasileiros e seus formatos de RG
        private val RG_FORMATS = mapOf(
            "SP" to Regex("^\\d{9}(-\\d{1})?$"), // SP: 123456789 ou 123456789-0
            "RJ" to Regex("^\\d{7,8}(-\\d{1})?$"), // RJ: 1234567 ou 12345678-0
            "MG" to Regex("^[A-Z]{0,2}\\d{6,9}(-\\d{1})?$"), // MG: pode ter prefixo
            "RS" to Regex("^\\d{7,10}$"), // RS: 7 a 10 dígitos
            "PR" to Regex("^\\d{8,10}(-\\d{1})?$"), // PR: 8 a 10 dígitos
            "SC" to Regex("^\\d{6,9}$"), // SC: 6 a 9 dígitos
            "BA" to Regex("^\\d{6,9}(-\\d{1})?$"), // BA: 6 a 9 dígitos
            "GO" to Regex("^\\d{7,9}(-\\d{1})?$"), // GO: 7 a 9 dígitos
            "PE" to Regex("^\\d{7,9}$"), // PE: 7 a 9 dígitos
            "CE" to Regex("^\\d{7,9}(-\\d{1})?$"), // CE: 7 a 9 dígitos
            "DF" to Regex("^\\d{6,9}(-\\d{1})?$"), // DF: 6 a 9 dígitos
            "ES" to Regex("^\\d{7,9}(-\\d{1})?$"), // ES: 7 a 9 dígitos
            "MT" to Regex("^\\d{7,9}(-\\d{1})?$"), // MT: 7 a 9 dígitos
            "MS" to Regex("^\\d{7,9}(-\\d{1})?$"), // MS: 7 a 9 dígitos
            "PA" to Regex("^\\d{7,9}(-\\d{1})?$"), // PA: 7 a 9 dígitos
            "PB" to Regex("^\\d{7,9}(-\\d{1})?$"), // PB: 7 a 9 dígitos
            "AM" to Regex("^\\d{7,9}(-\\d{1})?$"), // AM: 7 a 9 dígitos
            "RN" to Regex("^\\d{7,9}(-\\d{1})?$"), // RN: 7 a 9 dígitos
            "AL" to Regex("^\\d{7,9}(-\\d{1})?$"), // AL: 7 a 9 dígitos
            "SE" to Regex("^\\d{7,9}(-\\d{1})?$"), // SE: 7 a 9 dígitos
            "TO" to Regex("^\\d{7,9}(-\\d{1})?$"), // TO: 7 a 9 dígitos
            "AC" to Regex("^\\d{7,9}(-\\d{1})?$"), // AC: 7 a 9 dígitos
            "AP" to Regex("^\\d{7,9}(-\\d{1})?$"), // AP: 7 a 9 dígitos
            "RO" to Regex("^\\d{7,9}(-\\d{1})?$"), // RO: 7 a 9 dígitos
            "RR" to Regex("^\\d{7,9}(-\\d{1})?$"), // RR: 7 a 9 dígitos
            "PI" to Regex("^\\d{7,9}(-\\d{1})?$"), // PI: 7 a 9 dígitos
            "MA" to Regex("^\\d{7,9}(-\\d{1})?$")  // MA: 7 a 9 dígitos
        )
    }
    
    /**
     * Valida CPF usando o algoritmo oficial brasileiro
     * @param cpf CPF no formato 00000000000 ou 000.000.000-00
     * @return ValidationResult com resultado da validação
     */
    fun validateCpf(cpf: String): ValidationResult {
        // Remove formatação
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
        
        // Verifica tamanho
        if (cleanCpf.length != 11) {
            return ValidationResult.Invalid("CPF deve conter 11 dígitos")
        }
        
        // Verifica se todos os dígitos são iguais (CPFs inválidos conhecidos)
        if (cleanCpf.all { it == cleanCpf[0] }) {
            return ValidationResult.Invalid("CPF inválido")
        }
        
        // Valida primeiro dígito verificador
        var sum = 0
        for (i in 0..8) {
            sum += Character.getNumericValue(cleanCpf[i]) * (10 - i)
        }
        var digit1 = 11 - (sum % 11)
        if (digit1 >= 10) digit1 = 0
        
        if (digit1 != Character.getNumericValue(cleanCpf[9])) {
            return ValidationResult.Invalid("CPF inválido")
        }
        
        // Valida segundo dígito verificador
        sum = 0
        for (i in 0..9) {
            sum += Character.getNumericValue(cleanCpf[i]) * (11 - i)
        }
        var digit2 = 11 - (sum % 11)
        if (digit2 >= 10) digit2 = 0
        
        if (digit2 != Character.getNumericValue(cleanCpf[10])) {
            return ValidationResult.Invalid("CPF inválido")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Valida RG baseado no estado
     * @param rg RG no formato do estado
     * @param state Estado brasileiro (UF) para validação específica
     * @return ValidationResult com resultado da validação
     */
    fun validateRg(rg: String, state: String? = null): ValidationResult {
        // Remove espaços extras
        val cleanRg = rg.trim()
        
        if (cleanRg.isEmpty()) {
            return ValidationResult.Valid // RG é opcional
        }
        
        // Se não especificar estado, faz validação genérica
        if (state == null) {
            // Validação genérica: 6 a 10 dígitos, pode ter hífen e dígito verificador
            val genericPattern = Regex("^[A-Z0-9]{6,12}(-[0-9A-Z])?$")
            if (!genericPattern.matches(cleanRg.uppercase())) {
                return ValidationResult.Invalid("RG inválido. Formato incorreto.")
            }
            return ValidationResult.Valid
        }
        
        // Validação específica por estado
        val stateUpper = state.uppercase()
        val format = RG_FORMATS[stateUpper]
        
        if (format == null) {
            // Se não tiver formato específico, usa validação genérica
            val genericPattern = Regex("^[A-Z0-9]{6,12}(-[0-9A-Z])?$")
            if (!genericPattern.matches(cleanRg.uppercase())) {
                return ValidationResult.Invalid("RG inválido. Formato incorreto.")
            }
            return ValidationResult.Valid
        }
        
        if (!format.matches(cleanRg.uppercase())) {
            return ValidationResult.Invalid("RG inválido para o estado $stateUpper. Verifique o formato.")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Formata CPF para exibição (000.000.000-00)
     */
    fun formatCpf(cpf: String): String {
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
        if (cleanCpf.length != 11) return cpf
        
        return "${cleanCpf.substring(0, 3)}.${cleanCpf.substring(3, 6)}.${cleanCpf.substring(6, 9)}-${cleanCpf.substring(9)}"
    }
    
    /**
     * Formata CEP para exibição (00000-000)
     */
    fun formatCep(cep: String): String {
        val cleanCep = cep.replace(Regex("[^0-9]"), "")
        if (cleanCep.length != 8) return cep
        
        return "${cleanCep.substring(0, 5)}-${cleanCep.substring(5)}"
    }
    
    /**
     * Formata RG baseado no estado
     */
    fun formatRg(rg: String, state: String? = null): String {
        val cleanRg = rg.replace(Regex("[^0-9A-Za-z]"), "").uppercase()
        
        // Formatação básica: adiciona hífen antes do último dígito se não tiver
        if (cleanRg.length >= 7 && !cleanRg.contains("-")) {
            return "${cleanRg.substring(0, cleanRg.length - 1)}-${cleanRg.last()}"
        }
        
        return cleanRg
    }
    
    /**
     * Valida CNPJ usando o algoritmo oficial brasileiro
     * @param cnpj CNPJ no formato 00000000000000 ou 00.000.000/0000-00
     * @return ValidationResult com resultado da validação
     */
    fun validateCnpj(cnpj: String): ValidationResult {
        // Remove formatação
        val cleanCnpj = cnpj.replace(Regex("[^0-9]"), "")
        
        // Verifica tamanho
        if (cleanCnpj.length != 14) {
            return ValidationResult.Invalid("CNPJ deve conter 14 dígitos")
        }
        
        // Verifica se todos os dígitos são iguais (CNPJs inválidos conhecidos)
        if (cleanCnpj.all { it == cleanCnpj[0] }) {
            return ValidationResult.Invalid("CNPJ inválido")
        }
        
        // Valida primeiro dígito verificador
        val weights1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        for (i in 0..11) {
            sum += Character.getNumericValue(cleanCnpj[i]) * weights1[i]
        }
        var digit1 = sum % 11
        digit1 = if (digit1 < 2) 0 else 11 - digit1
        
        if (digit1 != Character.getNumericValue(cleanCnpj[12])) {
            return ValidationResult.Invalid("CNPJ inválido")
        }
        
        // Valida segundo dígito verificador
        val weights2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        sum = 0
        for (i in 0..12) {
            sum += Character.getNumericValue(cleanCnpj[i]) * weights2[i]
        }
        var digit2 = sum % 11
        digit2 = if (digit2 < 2) 0 else 11 - digit2
        
        if (digit2 != Character.getNumericValue(cleanCnpj[13])) {
            return ValidationResult.Invalid("CNPJ inválido")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Formata CNPJ para exibição (00.000.000/0000-00)
     */
    fun formatCnpj(cnpj: String): String {
        val cleanCnpj = cnpj.replace(Regex("[^0-9]"), "")
        if (cleanCnpj.length != 14) return cnpj
        
        return "${cleanCnpj.substring(0, 2)}.${cleanCnpj.substring(2, 5)}.${cleanCnpj.substring(5, 8)}/${cleanCnpj.substring(8, 12)}-${cleanCnpj.substring(12)}"
    }
    
    /**
     * Detecta se o documento é CPF ou CNPJ e valida
     */
    fun validateCpfOrCnpj(document: String): ValidationResult {
        val cleanDoc = document.replace(Regex("[^0-9]"), "")
        return when (cleanDoc.length) {
            11 -> validateCpf(document)
            14 -> validateCnpj(document)
            else -> ValidationResult.Invalid("Documento deve ter 11 dígitos (CPF) ou 14 dígitos (CNPJ)")
        }
    }
}

