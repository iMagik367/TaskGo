package com.taskgoapp.taskgo.core.location

import android.util.Log
import com.taskgoapp.taskgo.core.model.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serviço de geocoding para converter endereços em coordenadas
 * Utiliza Google Maps Geocoding API
 */
@Singleton
class GeocodingService @Inject constructor() {
    
    companion object {
        private const val TAG = "GeocodingService"
        private const val GEOCODING_API = "https://maps.googleapis.com/maps/api/geocode/json"
        private const val API_KEY = "AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"
    }
    
    /**
     * Converte um endereço em coordenadas (latitude, longitude)
     */
    suspend fun geocodeAddress(address: Address): GeocodingResult = withContext(Dispatchers.IO) {
        try {
            // Construir endereço completo para geocoding
            val fullAddress = buildString {
                if (address.street.isNotEmpty()) append(address.street)
                if (address.number.isNotEmpty()) append(", ${address.number}")
                if (address.neighborhood.isNotEmpty()) append(", ${address.neighborhood}")
                if (address.city.isNotEmpty()) append(", ${address.city}")
                if (address.state.isNotEmpty()) append(", ${address.state}")
                if (address.zipCode.isNotEmpty()) append(" ${address.zipCode}")
                append(", Brasil")
            }
            
            // Se tiver CEP, usar CEP para geocoding (mais preciso)
            val query = if (address.zipCode.isNotEmpty() || address.cep.isNotEmpty()) {
                val cep = address.zipCode.ifEmpty { address.cep }
                "$cep, Brasil"
            } else {
                fullAddress
            }
            
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = URL("$GEOCODING_API?address=$encodedQuery&key=$API_KEY")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val status = json.getString("status")
                
                if (status == "OK") {
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val geometry = firstResult.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")
                        
                        val latitude = location.getDouble("lat")
                        val longitude = location.getDouble("lng")
                        
                        Log.d(TAG, "Geocoding bem-sucedido: $query -> ($latitude, $longitude)")
                        return@withContext GeocodingResult.Success(
                            latitude = latitude,
                            longitude = longitude,
                            formattedAddress = firstResult.getString("formatted_address")
                        )
                    }
                } else {
                    Log.w(TAG, "Geocoding falhou: status=$status para endereço=$query")
                    return@withContext GeocodingResult.Error("Status: $status")
                }
            } else {
                Log.e(TAG, "Erro HTTP ao fazer geocoding: $responseCode")
                return@withContext GeocodingResult.Error("Erro HTTP: $responseCode")
            }
            
            GeocodingResult.Error("Nenhum resultado encontrado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer geocoding: ${e.message}", e)
            GeocodingResult.Error("Erro: ${e.message}")
        }
    }
    
    /**
     * Converte CEP em coordenadas
     */
    suspend fun geocodeCep(cep: String): GeocodingResult = withContext(Dispatchers.IO) {
        try {
            val cleanCep = cep.replace(Regex("[^0-9]"), "")
            if (cleanCep.length != 8) {
                return@withContext GeocodingResult.Error("CEP inválido")
            }
            
            val encodedCep = java.net.URLEncoder.encode("$cleanCep, Brasil", "UTF-8")
            val url = URL("$GEOCODING_API?address=$encodedCep&key=$API_KEY")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val status = json.getString("status")
                
                if (status == "OK") {
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val geometry = firstResult.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")
                        
                        val latitude = location.getDouble("lat")
                        val longitude = location.getDouble("lng")
                        
                        return@withContext GeocodingResult.Success(
                            latitude = latitude,
                            longitude = longitude,
                            formattedAddress = firstResult.getString("formatted_address")
                        )
                    }
                }
            }
            
            GeocodingResult.Error("CEP não encontrado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer geocoding do CEP: ${e.message}", e)
            GeocodingResult.Error("Erro: ${e.message}")
        }
    }
}

/**
 * Resultado do geocoding
 */
sealed class GeocodingResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val formattedAddress: String
    ) : GeocodingResult()
    
    data class Error(val message: String) : GeocodingResult()
}

