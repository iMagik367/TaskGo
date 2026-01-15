package com.taskgoapp.taskgo.core.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Utilitários para formatação automática de campos de texto em formulários
 * Com preservação de posição do cursor
 */
object TextFormatters {
    
    /**
     * Calcula a nova posição do cursor após formatação
     */
    private fun calculateNewCursorPosition(
        oldText: String,
        newText: String,
        oldCursorPosition: Int
    ): Int {
        // Conta quantos caracteres não numéricos existem antes da posição do cursor no texto antigo
        val charsBeforeCursor = oldText.take(oldCursorPosition)
        val digitsBeforeCursor = charsBeforeCursor.count { it.isDigit() }
        
        // Encontra a posição no novo texto onde esse mesmo número de dígitos está
        var digitCount = 0
        for (i in newText.indices) {
            if (newText[i].isDigit()) {
                digitCount++
                if (digitCount >= digitsBeforeCursor) {
                    // Coloca o cursor após o dígito encontrado
                    return i + 1
                }
            }
        }
        
        // Se chegou aqui, cursor deve estar no final
        return newText.length
    }
    
    /**
     * Formata preço para exibição brasileira (R$ 1.234,56)
     * Aceita entrada como "1234.56" ou "1234,56" ou "123456"
     */
    fun formatPrice(value: String): String {
        // Se vazio, retorna vazio
        if (value.isEmpty()) return ""
        
        // Detecta se há vírgula (separador decimal brasileiro)
        val hasComma = value.contains(",")
        
        if (hasComma) {
            // Há vírgula: tudo antes da vírgula é parte inteira (pontos são separadores de milhar), tudo após são centavos
            val parts = value.split(",")
            val integerPart = parts[0].replace(Regex("[^0-9]"), "") // Remove pontos (separadores de milhar)
            val decimalPart = parts.getOrNull(1)?.replace(Regex("[^0-9]"), "")?.take(2)?.padEnd(2, '0') ?: "00"
            
            // Formata parte inteira com separador de milhar (ponto)
            val formattedInteger = if (integerPart.isNotEmpty() && integerPart != "0") {
                integerPart.reversed().chunked(3).joinToString(".").reversed()
            } else {
                "0"
            }
            
            return "$formattedInteger,$decimalPart"
        } else {
            // Não há vírgula: remover pontos (tratá-los como separadores de milhar sendo digitados)
            // e tratar tudo como parte inteira, adicionando ",00" ao final
            val clean = value.replace(Regex("[^0-9]"), "")
            
            if (clean.isEmpty()) return ""
            
            // Sem vírgula, trata tudo como parte inteira e adiciona ",00"
            val formattedInteger = if (clean.isNotEmpty() && clean != "0") {
                clean.reversed().chunked(3).joinToString(".").reversed()
            } else {
                "0"
            }
            
            return "$formattedInteger,00"
        }
    }
    
    /**
     * Remove formatação de preço para obter valor numérico
     */
    fun unformatPrice(value: String): String {
        return value.replace(Regex("[^0-9]"), "")
    }
    
    /**
     * Formata telefone brasileiro (XX) XXXXX-XXXX ou (XX) XXXX-XXXX
     */
    fun formatPhone(value: String): String {
        val clean = value.replace(Regex("[^0-9]"), "")
        
        return when {
            clean.isEmpty() -> ""
            clean.length <= 2 -> "($clean"
            clean.length <= 6 -> {
                val ddd = clean.substring(0, 2)
                val rest = clean.substring(2)
                "($ddd) $rest"
            }
            clean.length <= 10 -> {
                // Telefone fixo (XX) XXXX-XXXX
                val ddd = clean.substring(0, 2)
                val part1 = clean.substring(2, 6)
                val part2 = clean.substring(6)
                "($ddd) $part1-$part2"
            }
            else -> {
                // Celular (XX) XXXXX-XXXX
                val ddd = clean.substring(0, 2)
                val part1 = clean.substring(2, 7)
                val part2 = clean.substring(7, 11)
                "($ddd) $part1-$part2"
            }
        }
    }
    
    /**
     * Formata telefone preservando posição do cursor
     */
    fun formatPhoneWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatPhone(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Formata CPF (000.000.000-00) progressivamente
     */
    fun formatCpf(cpf: String): String {
        val clean = cpf.replace(Regex("[^0-9]"), "")
        if (clean.isEmpty()) return ""
        
        return when {
            clean.length <= 3 -> clean
            clean.length <= 6 -> "${clean.substring(0, 3)}.${clean.substring(3)}"
            clean.length <= 9 -> "${clean.substring(0, 3)}.${clean.substring(3, 6)}.${clean.substring(6)}"
            clean.length <= 11 -> "${clean.substring(0, 3)}.${clean.substring(3, 6)}.${clean.substring(6, 9)}-${clean.substring(9)}"
            else -> {
                // Se tiver mais de 11 dígitos, limita a 11
                val limited = clean.substring(0, 11)
                "${limited.substring(0, 3)}.${limited.substring(3, 6)}.${limited.substring(6, 9)}-${limited.substring(9)}"
            }
        }
    }
    
    /**
     * Formata CPF preservando posição do cursor
     */
    fun formatCpfWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatCpf(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Formata CNPJ (00.000.000/0000-00) progressivamente
     */
    fun formatCnpj(cnpj: String): String {
        val clean = cnpj.replace(Regex("[^0-9]"), "")
        if (clean.isEmpty()) return ""
        
        return when {
            clean.length <= 2 -> clean
            clean.length <= 5 -> "${clean.substring(0, 2)}.${clean.substring(2)}"
            clean.length <= 8 -> "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5)}"
            clean.length <= 12 -> "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5, 8)}/${clean.substring(8)}"
            clean.length <= 14 -> "${clean.substring(0, 2)}.${clean.substring(2, 5)}.${clean.substring(5, 8)}/${clean.substring(8, 12)}-${clean.substring(12)}"
            else -> {
                // Se tiver mais de 14 dígitos, limita a 14
                val limited = clean.substring(0, 14)
                "${limited.substring(0, 2)}.${limited.substring(2, 5)}.${limited.substring(5, 8)}/${limited.substring(8, 12)}-${limited.substring(12)}"
            }
        }
    }
    
    /**
     * Formata CNPJ preservando posição do cursor
     */
    fun formatCnpjWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatCnpj(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Formata data brasileira (DD/MM/AAAA)
     */
    fun formatDate(value: String): String {
        val clean = value.replace(Regex("[^0-9]"), "")
        
        return when {
            clean.isEmpty() -> ""
            clean.length <= 2 -> clean
            clean.length <= 4 -> {
                val day = clean.substring(0, 2)
                val month = clean.substring(2)
                "$day/$month"
            }
            else -> {
                val day = clean.substring(0, 2)
                val month = clean.substring(2, 4)
                val year = clean.substring(4, 8)
                "$day/$month/$year"
            }
        }
    }
    
    /**
     * Formata data preservando posição do cursor
     */
    fun formatDateWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatDate(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Valida formato de data durante digitação
     * Corrigido para evitar crash ao converter para int
     */
    fun isValidDateInput(value: String): Boolean {
        return try {
            val clean = value.replace(Regex("[^0-9]"), "")
            if (clean.isEmpty()) return true
            
            // Validar dia (01-31)
            if (clean.length >= 2) {
                val dayStr = clean.substring(0, 2)
                val day = dayStr.toIntOrNull()
                if (day == null || day < 1 || day > 31) return false
            }
            
            // Validar mês (01-12)
            if (clean.length >= 4) {
                val monthStr = clean.substring(2, 4)
                val month = monthStr.toIntOrNull()
                if (month == null || month < 1 || month > 12) return false
            }
            
            // Validar ano (1900-2099)
            if (clean.length >= 8) {
                val yearStr = clean.substring(4, 8)
                val yearValue = yearStr.toIntOrNull()
                if (yearValue == null || yearValue < 1900 || yearValue > 2099) return false
                
                // Validação adicional: verificar se a data é válida (ex: 31/02 não existe)
                if (clean.length == 8) {
                    val dayValue = clean.substring(0, 2).toIntOrNull() ?: return false
                    val monthValue = clean.substring(2, 4).toIntOrNull() ?: return false
                    
                    // Validar se a data existe
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.YEAR, yearValue)
                    calendar.set(java.util.Calendar.MONTH, monthValue - 1) // Calendar month is 0-based
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    if (dayValue > maxDay) return false
                }
            }
            
            true
        } catch (e: Exception) {
            // Qualquer exceção durante validação retorna false
            false
        }
    }
    
    /**
     * Formata CEP (00000-000)
     * Reutiliza função do DocumentValidator
     */
    fun formatCep(cep: String): String {
        val clean = cep.replace(Regex("[^0-9]"), "")
        if (clean.isEmpty()) return ""
        if (clean.length <= 5) return clean
        if (clean.length <= 8) return "${clean.substring(0, 5)}-${clean.substring(5)}"
        return "${clean.substring(0, 5)}-${clean.substring(5, 8)}"
    }
    
    /**
     * Formata CEP preservando posição do cursor
     */
    fun formatCepWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatCep(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Formata RG brasileiro (00.000.000-0 ou formato similar)
     * Formato padrão: XX.XXX.XXX-X (9 dígitos)
     * Suporta até 12 dígitos alfanuméricos para diferentes estados
     */
    fun formatRg(rg: String): String {
        val clean = rg.replace(Regex("[^0-9A-Za-z]"), "").uppercase()
        if (clean.isEmpty()) return ""
        
        // Limita a 12 dígitos para compatibilidade com diferentes estados
        val limited = if (clean.length > 12) clean.substring(0, 12) else clean
        
        return when {
            limited.length <= 2 -> limited
            limited.length <= 5 -> "${limited.substring(0, 2)}.${limited.substring(2)}"
            limited.length <= 8 -> "${limited.substring(0, 2)}.${limited.substring(2, 5)}.${limited.substring(5)}"
            limited.length <= 9 -> {
                // Formato padrão: XX.XXX.XXX-X (9 dígitos)
                "${limited.substring(0, 2)}.${limited.substring(2, 5)}.${limited.substring(5, 8)}-${limited.substring(8)}"
            }
            else -> {
                // Para RGs com mais de 9 dígitos (até 12), formata como XX.XXX.XXX-XXX
                // Mantém os primeiros 8 dígitos no padrão e adiciona o resto após hífen
                "${limited.substring(0, 2)}.${limited.substring(2, 5)}.${limited.substring(5, 8)}-${limited.substring(8)}"
            }
        }
    }
    
    /**
     * Formata RG preservando posição do cursor
     */
    fun formatRgWithCursor(textFieldValue: TextFieldValue): TextFieldValue {
        val oldText = textFieldValue.text
        val oldCursor = textFieldValue.selection.start
        val newText = formatRg(oldText)
        val newCursor = calculateNewCursorPosition(oldText, newText, oldCursor)
        return TextFieldValue(newText, TextRange(newCursor))
    }
    
    /**
     * Remove formatação de qualquer campo
     */
    fun removeFormatting(value: String): String {
        return value.replace(Regex("[^0-9]"), "")
    }
}

