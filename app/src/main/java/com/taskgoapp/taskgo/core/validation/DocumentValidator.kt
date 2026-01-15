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
        
        // Remove formatação (pontos, hífens) para contar dígitos
        val digitsOnly = cleanRg.replace(Regex("[^0-9A-Za-z]"), "").uppercase()
        
        // Validação genérica: 6 a 12 dígitos alfanuméricos
        if (digitsOnly.length < 6 || digitsOnly.length > 12) {
            return ValidationResult.Invalid("RG inválido. Formato incorreto.")
        }
        
        // Valida formato brasileiro padrão: XX.XXX.XXX-X (ex: 13.262.015-6)
        // Aceita formato com pontos e hífen
        val formattedPattern = Regex("^\\d{2}\\.\\d{3}\\.\\d{3}-\\d{1}$") // Formato padrão: XX.XXX.XXX-X
        
        // Se tem pontos, valida formato formatado primeiro (mais comum)
        if (cleanRg.contains(".")) {
            // Verifica se segue o formato XX.XXX.XXX-X exatamente
            if (formattedPattern.matches(cleanRg)) {
                // Formato está correto! Valida por estado se especificado
                val mainDigitsOnly = cleanRg.split("-")[0].replace(Regex("[^0-9A-Za-z]"), "").uppercase()
                val mainDigitsCount = mainDigitsOnly.length
                
                // Valida por estado se especificado
                if (state != null && state.isNotEmpty()) {
                    val stateUpper = state.uppercase()
                    // PR aceita 8-10 dígitos principais (formato padrão tem 8)
                    if (stateUpper == "PR" && mainDigitsCount >= 8 && mainDigitsCount <= 10) {
                        return ValidationResult.Valid
                    } else if (stateUpper != "PR") {
                        // Para outros estados, verifica se a quantidade está dentro do esperado
                        val format = RG_FORMATS[stateUpper]
                        if (format != null && format.matches(mainDigitsOnly)) {
                            return ValidationResult.Valid
                        }
                        return ValidationResult.Invalid("RG inválido para o estado $stateUpper. Formato esperado: 00.000.000-0")
                    }
                } else {
                    // Se não especificou estado, aceita formato padrão brasileiro (8-9 dígitos principais + 1 verificador)
                    if (mainDigitsCount >= 8 && mainDigitsCount <= 9) {
                        return ValidationResult.Valid
                    }
                }
            } else {
                // Se não passou na regex exata, verifica manualmente a estrutura (formato XX.XXX.XXX-X)
                val parts = cleanRg.split(".")
                if (parts.size == 3) {
                    val firstPart = parts[0]
                    val secondPart = parts[1]
                    val thirdWithDash = parts[2]
                    
                    // Verifica se cada parte tem a quantidade correta de dígitos
                    if (firstPart.length == 2 && firstPart.all { it.isDigit() } &&
                        secondPart.length == 3 && secondPart.all { it.isDigit() } &&
                        thirdWithDash.contains("-")) {
                        
                        val lastParts = thirdWithDash.split("-")
                        if (lastParts.size == 2 &&
                            lastParts[0].length == 3 && lastParts[0].all { it.isDigit() } &&
                            lastParts[1].length >= 1 && lastParts[1].length <= 2 && lastParts[1].all { it.isDigit() }) {
                            // Formato válido: XX.XXX.XXX-X ou XX.XXX.XXX-XX
                            // Valida por estado se especificado
                            val mainDigitsOnly = cleanRg.split("-")[0].replace(Regex("[^0-9A-Za-z]"), "").uppercase()
                            val mainDigitsCount = mainDigitsOnly.length
                            
                            if (state != null && state.isNotEmpty()) {
                                val stateUpper = state.uppercase()
                                if (stateUpper == "PR" && mainDigitsCount >= 8 && mainDigitsCount <= 10) {
                                    return ValidationResult.Valid
                                } else if (stateUpper != "PR") {
                                    val format = RG_FORMATS[stateUpper]
                                    if (format != null && format.matches(mainDigitsOnly)) {
                                        return ValidationResult.Valid
                                    }
                                    return ValidationResult.Invalid("RG inválido para o estado $stateUpper. Formato esperado: 00.000.000-0")
                                }
                            }
                            // Se não especificou estado ou passou na validação, aceita
                            return ValidationResult.Valid
                        }
                    }
                }
                return ValidationResult.Invalid("RG inválido. Formato incorreto. Use o formato: 00.000.000-0")
            }
        }
        
        // Se especificar estado e não tem pontos, valida formato específico após remover formatação
        if (state != null && state.isNotEmpty()) {
            val stateUpper = state.uppercase()
            val format = RG_FORMATS[stateUpper]
            
            if (format != null) {
                // Para validação por estado sem formatação, separa dígitos principais do verificador
                val mainDigitsOnly = if (cleanRg.contains("-")) {
                    cleanRg.split("-")[0].replace(Regex("[^0-9A-Za-z]"), "").uppercase()
                } else {
                    // Se não tem hífen, assume que o último dígito pode ser verificador
                    if (stateUpper == "PR" && digitsOnly.length >= 9) {
                        digitsOnly.substring(0, digitsOnly.length - 1)
                    } else if (stateUpper != "PR" && digitsOnly.length >= 8) {
                        digitsOnly.substring(0, digitsOnly.length - 1)
                    } else {
                        digitsOnly
                    }
                }
                
                // Valida formato específico do estado usando apenas dígitos principais
                if (stateUpper == "PR") {
                    val mainDigitsCount = mainDigitsOnly.length
                    if (mainDigitsCount >= 8 && mainDigitsCount <= 10) {
                        return ValidationResult.Valid
                    }
                } else {
                    if (format.matches(mainDigitsOnly) || format.matches(digitsOnly)) {
                        return ValidationResult.Valid
                    }
                }
                
                return ValidationResult.Invalid("RG inválido para o estado $stateUpper. Formato esperado: 00.000.000-0")
            }
        }
        
        // Se não tem pontos, valida formato simples (apenas dígitos e hífen opcional)
        val simplePattern = Regex("^\\d{6,12}(-\\d{1,2})?$")
        if (simplePattern.matches(cleanRg)) {
            return ValidationResult.Valid
        }
        
        // Valida apenas dígitos alfanuméricos (6-12 caracteres)
        if (digitsOnly.all { it.isDigit() || it.isLetter() }) {
            return ValidationResult.Valid
        }
        
        return ValidationResult.Invalid("RG inválido. Formato incorreto.")
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
    fun formatRg(rg: String, @Suppress("UNUSED_PARAMETER") state: String? = null): String {
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

