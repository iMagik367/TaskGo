package com.taskgoapp.taskgo.core.validation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resultado da busca de CEP
 */
data class CepResult(
    val cep: String,
    val logradouro: String,
    val complemento: String?,
    val bairro: String,
    val localidade: String,
    val uf: String,
    val erro: Boolean = false
) {
    fun toAddress(): AddressData {
        return AddressData(
            street = logradouro,
            neighborhood = bairro,
            city = localidade,
            state = uf,
            zipCode = cep
        )
    }
}

/**
 * Dados de endereço para preenchimento automático
 */
data class AddressData(
    val street: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val complement: String? = null
)

/**
 * Serviço para buscar endereço por CEP usando a API ViaCEP
 * API pública e gratuita dos Correios
 */
class CepService {
    
    companion object {
        private const val VIA_CEP_API = "https://viacep.com.br/ws/%s/json/"
    }
    
    /**
     * Busca endereço por CEP
     * @param cep CEP no formato 00000000 ou 00000-000
     * @return CepResult com os dados do endereço ou erro
     */
    suspend fun searchCep(cep: String): Result<CepResult> = withContext(Dispatchers.IO) {
        try {
            // Remove formatação do CEP
            val cleanCep = cep.replace(Regex("[^0-9]"), "")
            
            // Valida formato do CEP
            if (cleanCep.length != 8) {
                return@withContext Result.failure(Exception("CEP deve conter 8 dígitos"))
            }
            
            // Faz requisição à API ViaCEP
            val url = URL(VIA_CEP_API.format(cleanCep))
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                // Verifica se há erro na resposta
                if (json.has("erro") && json.getBoolean("erro")) {
                    return@withContext Result.failure(Exception("CEP não encontrado"))
                }
                
                val complementoValue = json.optString("complemento", "")
                val cepResult = CepResult(
                    cep = json.optString("cep", cleanCep),
                    logradouro = json.optString("logradouro", ""),
                    complemento = complementoValue.takeIf { it.isNotEmpty() },
                    bairro = json.optString("bairro", ""),
                    localidade = json.optString("localidade", ""),
                    uf = json.optString("uf", ""),
                    erro = false
                )
                
                Result.success(cepResult)
            } else {
                Result.failure(Exception("Erro ao buscar CEP. Código: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao buscar CEP: ${e.message}", e))
        }
    }
}

