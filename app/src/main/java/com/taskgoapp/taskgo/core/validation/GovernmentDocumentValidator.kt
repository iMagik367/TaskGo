package com.taskgoapp.taskgo.core.validation

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serviço avançado de validação de documentos brasileiros
 * Utiliza APIs públicas do governo quando disponíveis
 * Implementa validações robustas seguindo padrões bancários e governamentais
 */
@Singleton
class GovernmentDocumentValidator @Inject constructor() {
    
    companion object {
        private const val TAG = "GovDocumentValidator"
        // API pública para consulta de CPF (quando disponível)
        // Nota: APIs públicas de validação de CPF são limitadas por questões de privacidade
        // Por isso, usamos validação algorítmica robusta + verificação de padrões conhecidos
        private const val RECEITA_WS_API = "https://www.receitaws.com.br/v1/cnpj/%s"
    }
    
    /**
     * Valida CPF com verificação avançada
     * Inclui verificação de CPFs bloqueados e padrões conhecidos
     */
    suspend fun validateCpfAdvanced(cpf: String): DocumentValidationResult = withContext(Dispatchers.IO) {
        try {
            val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
            
            // Validação básica
            if (cleanCpf.length != 11) {
                return@withContext DocumentValidationResult.Invalid("CPF deve conter exatamente 11 dígitos")
            }
            
            // Verifica CPFs conhecidos como inválidos (todos os dígitos iguais)
            if (cleanCpf.all { it == cleanCpf[0] }) {
                return@withContext DocumentValidationResult.Invalid("CPF inválido: todos os dígitos são iguais")
            }
            
            // Validação algorítmica dos dígitos verificadores
            val validator = DocumentValidator()
            val basicValidation = validator.validateCpf(cpf)
            
            if (basicValidation is ValidationResult.Invalid) {
                return@withContext DocumentValidationResult.Invalid(basicValidation.message)
            }
            
            // Verificação adicional: CPFs conhecidos como bloqueados pela Receita Federal
            // (Lista de CPFs bloqueados por questões fiscais - exemplo)
            val blockedCpfs = listOf(
                "00000000000", "11111111111", "22222222222", "33333333333",
                "44444444444", "55555555555", "66666666666", "77777777777",
                "88888888888", "99999999999"
            )
            
            if (blockedCpfs.contains(cleanCpf)) {
                return@withContext DocumentValidationResult.Invalid("CPF bloqueado pela Receita Federal")
            }
            
            // Verificação de CPF suspeito (padrões que indicam possível fraude)
            // Exemplo: sequências muito regulares
            if (isSuspiciousPattern(cleanCpf)) {
                return@withContext DocumentValidationResult.Suspicious("CPF com padrão suspeito. Verifique os dados.")
            }
            
            DocumentValidationResult.Valid(cleanCpf)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar CPF: ${e.message}", e)
            DocumentValidationResult.Error("Erro ao validar CPF: ${e.message}")
        }
    }
    
    /**
     * Valida CNPJ com verificação avançada e consulta à API da ReceitaWS
     */
    suspend fun validateCnpjAdvanced(cnpj: String): DocumentValidationResult = withContext(Dispatchers.IO) {
        try {
            val cleanCnpj = cnpj.replace(Regex("[^0-9]"), "")
            
            // Validação básica
            if (cleanCnpj.length != 14) {
                return@withContext DocumentValidationResult.Invalid("CNPJ deve conter exatamente 14 dígitos")
            }
            
            // Verifica CNPJs conhecidos como inválidos
            if (cleanCnpj.all { it == cleanCnpj[0] }) {
                return@withContext DocumentValidationResult.Invalid("CNPJ inválido: todos os dígitos são iguais")
            }
            
            // Validação algorítmica dos dígitos verificadores
            val validator = DocumentValidator()
            val basicValidation = validator.validateCnpj(cnpj)
            
            if (basicValidation is ValidationResult.Invalid) {
                return@withContext DocumentValidationResult.Invalid(basicValidation.message)
            }
            
            // Consulta à API da ReceitaWS para verificar se CNPJ existe e está ativo
            try {
                val url = URL(RECEITA_WS_API.format(cleanCnpj))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/json")
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    
                    // Verifica se CNPJ está ativo
                    val status = json.optString("status", "")
                    if (status == "ERROR" || json.has("erro")) {
                        return@withContext DocumentValidationResult.Invalid("CNPJ não encontrado na Receita Federal")
                    }
                    
                    // Verifica situação cadastral
                    val situacao = json.optString("situacao", "")
                    if (situacao.isNotEmpty() && situacao != "ATIVA") {
                        return@withContext DocumentValidationResult.Invalid("CNPJ com situação cadastral: $situacao")
                    }
                    
                    // Retorna CNPJ válido com dados adicionais
                    val companyName = json.optString("nome", "")
                    val companyData = CnpjCompanyData(
                        cnpj = cleanCnpj,
                        companyName = companyName,
                        situation = situacao,
                        openingDate = json.optString("abertura", ""),
                        legalNature = json.optString("natureza_juridica", ""),
                        mainActivity = json.optString("atividade_principal", "")
                    )
                    
                    DocumentValidationResult.ValidWithData(cleanCnpj, companyData)
                } else {
                    // Se API não responder, retorna validação algorítmica
                    Log.w(TAG, "API ReceitaWS não disponível, usando validação algorítmica")
                    DocumentValidationResult.Valid(cleanCnpj)
                }
            } catch (e: Exception) {
                // Se falhar a consulta à API, usa apenas validação algorítmica
                Log.w(TAG, "Erro ao consultar API ReceitaWS: ${e.message}")
                DocumentValidationResult.Valid(cleanCnpj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar CNPJ: ${e.message}", e)
            DocumentValidationResult.Error("Erro ao validar CNPJ: ${e.message}")
        }
    }
    
    /**
     * Valida RG com verificação avançada baseada no estado
     */
    suspend fun validateRgAdvanced(rg: String, state: String?): DocumentValidationResult = withContext(Dispatchers.IO) {
        try {
            val validator = DocumentValidator()
            val basicValidation = validator.validateRg(rg, state)
            
            if (basicValidation is ValidationResult.Invalid) {
                return@withContext DocumentValidationResult.Invalid(basicValidation.message)
            }
            
            // Verificações adicionais de padrões suspeitos
            val cleanRg = rg.replace(Regex("[^0-9A-Za-z]"), "").uppercase()
            
            if (cleanRg.length < 6 || cleanRg.length > 12) {
                return@withContext DocumentValidationResult.Invalid("RG deve ter entre 6 e 12 caracteres")
            }
            
            // Verifica padrões suspeitos
            if (isSuspiciousPattern(cleanRg)) {
                return@withContext DocumentValidationResult.Suspicious("RG com padrão suspeito. Verifique os dados.")
            }
            
            DocumentValidationResult.Valid(cleanRg)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar RG: ${e.message}", e)
            DocumentValidationResult.Error("Erro ao validar RG: ${e.message}")
        }
    }
    
    /**
     * Verifica se um documento tem padrão suspeito
     */
    private fun isSuspiciousPattern(document: String): Boolean {
        // Verifica sequências muito regulares (ex: 123456789, 111111111)
        if (document.length >= 6) {
            // Verifica se todos os dígitos são iguais
            if (document.all { it == document[0] }) {
                return true
            }
            
            // Verifica sequências incrementais ou decrementais
            var isSequential = true
            var isReverseSequential = true
            
            for (i in 1 until document.length) {
                val current = document[i].code
                val previous = document[i - 1].code
                
                // Sequência incremental
                if (current != previous + 1) {
                    isSequential = false
                }
                
                // Sequência decremental
                if (current != previous - 1) {
                    isReverseSequential = false
                }
            }
            
            if (isSequential || isReverseSequential) {
                return true
            }
        }
        
        return false
    }
}

/**
 * Resultado avançado de validação de documentos
 */
sealed class DocumentValidationResult {
    data class Valid(val document: String) : DocumentValidationResult()
    data class ValidWithData(val document: String, val companyData: CnpjCompanyData) : DocumentValidationResult()
    data class Invalid(val message: String) : DocumentValidationResult()
    data class Suspicious(val message: String) : DocumentValidationResult()
    data class Error(val message: String) : DocumentValidationResult()
}

/**
 * Dados da empresa obtidos da ReceitaWS
 */
data class CnpjCompanyData(
    val cnpj: String,
    val companyName: String,
    val situation: String,
    val openingDate: String,
    val legalNature: String,
    val mainActivity: String
)

