package com.taskgoapp.taskgo.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.pow

/**
 * Classe responsável por obter localização GPS de forma ROBUSTA e CONFIÁVEL
 * 
 * ⚠️ ATENÇÃO: GPS é usado APENAS para coordenadas (latitude/longitude) quando necessário
 * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
 * NUNCA usar GPS para obter ou determinar city/state - GPS apenas para coordenadas (mapa)
 * 
 * Métodos como getAddressFromLocation são usados APENAS para geocoding reverso (coordenadas → endereço)
 * NÃO devem ser usados para obter city/state do usuário
 */
@Singleton
class LocationManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder: Geocoder? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Geocoder(context, Locale.getDefault())
    } else {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.getDefault())
    }
    
    /**
     * Obtém a localização atual do usuário com RETRY ROBUSTO
     * CRÍTICO: Tenta múltiplas vezes com backoff exponencial até obter sucesso
     * 
     * @param maxAttempts Número máximo de tentativas (padrão: 5)
     * @param timeoutMs Timeout por tentativa em ms (padrão: 15 segundos)
     * @return Location válida ou null se todas as tentativas falharem
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(maxAttempts: Int = 5, timeoutMs: Long = 15000L): Location? {
        // Verificar permissões primeiro
        if (!hasLocationPermission()) {
            android.util.Log.w("LocationManager", "❌ Permissão de localização não concedida")
            return getLastKnownLocationFromCache()
        }
        
        // Verificar se GPS está habilitado
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
            )
            .setAlwaysShow(true)
            .build()
        
        val settingsClient = LocationServices.getSettingsClient(context)
        val settingsTask = settingsClient.checkLocationSettings(locationSettingsRequest)
        
        try {
            Tasks.await(settingsTask)
        } catch (e: Exception) {
            android.util.Log.w("LocationManager", "⚠️ GPS pode estar desligado: ${e.message}")
            // Continuar mesmo assim - pode funcionar
        }
        
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt++
            android.util.Log.d("LocationManager", "📍 Tentativa $attempt/$maxAttempts de obter GPS...")
            
            val location = withTimeoutOrNull(timeoutMs) {
                getCurrentLocationSingleAttempt()
            }
            
            if (location != null) {
                // Aceitar localização mesmo se a validação falhar (pode ser falsa rejeição)
                if (LocationValidator.isValidLocationQuality(location)) {
                    android.util.Log.d("LocationManager", "✅ GPS obtido com sucesso na tentativa $attempt: (${location.latitude}, ${location.longitude})")
                    saveLocationToCache(location)
                    return location
                } else {
                    // Mesmo se a validação falhar, aceitar se não for (0,0)
                    if (location.latitude != 0.0 || location.longitude != 0.0) {
                        android.util.Log.w("LocationManager", "⚠️ GPS obtido mas validação falhou, aceitando mesmo assim: (${location.latitude}, ${location.longitude})")
                        saveLocationToCache(location)
                        return location
                    } else {
                        android.util.Log.w("LocationManager", "⚠️ Tentativa $attempt falhou: GPS é (0,0) - inválido")
                    }
                }
            } else {
                android.util.Log.w("LocationManager", "⚠️ Tentativa $attempt falhou: location é null")
            }
            
            // Backoff exponencial REDUZIDO: 500ms, 1s, 2s (max)
            val delayMs = minOf(500L * 2.0.pow(attempt - 1).toLong(), 2000L)
            if (attempt < maxAttempts) {
                android.util.Log.d("LocationManager", "⏳ Aguardando ${delayMs}ms antes da próxima tentativa...")
                delay(delayMs)
            }
        }
        
        android.util.Log.e("LocationManager", "❌ Falha ao obter GPS após $maxAttempts tentativas. Usando cache...")
        return getLastKnownLocationFromCache()
    }
    
    /**
     * Obtém GPS em uma única tentativa
     * CRÍTICO: Timeouts adequados para garantir que o GPS funcione
     */
    private suspend fun getCurrentLocationSingleAttempt(): Location? = suspendCancellableCoroutine { continuation ->
        var isResumed = false
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Intervalo de 5 segundos
        ).apply {
            setMaxUpdateDelayMillis(10000L) // Máximo 10 segundos de atraso
            setWaitForAccurateLocation(true) // Esperar por localização precisa
            setMinUpdateIntervalMillis(1000L) // Mínimo 1 segundo entre atualizações
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isResumed) {
                    isResumed = true
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    continuation.resume(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
            
            // Timeout de segurança - dar tempo suficiente para o GPS funcionar
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                delay(30000L) // 30 segundos - tempo adequado para GPS obter localização
                if (!isResumed) {
                    isResumed = true
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    // Tentar obter última localização conhecida
                    try {
                        val lastLocation = Tasks.await(fusedLocationClient.lastLocation)
                        if (lastLocation != null && (lastLocation.latitude != 0.0 || lastLocation.longitude != 0.0)) {
                            android.util.Log.d("LocationManager", "📍 Usando última localização conhecida após timeout: (${lastLocation.latitude}, ${lastLocation.longitude})")
                            continuation.resume(lastLocation)
                        } else {
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("LocationManager", "⚠️ Erro ao obter última localização: ${e.message}")
                        continuation.resume(null)
                    }
                }
            }
            
            continuation.invokeOnCancellation {
                if (!isResumed) {
                    isResumed = true
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        } catch (e: SecurityException) {
            if (!isResumed) {
                isResumed = true
                continuation.resume(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Erro ao solicitar atualizações de localização: ${e.message}", e)
            if (!isResumed) {
                isResumed = true
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Verifica se o app tem permissão de localização
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Obtém última localização conhecida do cache persistente
     */
    private suspend fun getLastKnownLocationFromCache(): Location? {
        return try {
            val prefs = preferencesManager.dataStore.data.first()
            val lat = prefs[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LATITUDE]
            val lng = prefs[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LONGITUDE]
            val timestamp = prefs[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LOCATION_TIMESTAMP] ?: 0L
            
            if (lat != null && lng != null) {
                // Verificar se cache não está muito antigo (máximo 7 dias)
                val cacheAge = System.currentTimeMillis() - timestamp
                if (cacheAge < 7 * 24 * 60 * 60 * 1000L) {
                    android.util.Log.d("LocationManager", "📍 Usando última localização do cache: ($lat, $lng), idade: ${cacheAge / 1000 / 60} minutos")
                    Location("cache").apply {
                        latitude = lat
                        longitude = lng
                        time = timestamp
                    }
                } else {
                    android.util.Log.w("LocationManager", "⚠️ Cache de localização muito antigo (${cacheAge / 1000 / 60 / 60} horas)")
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Erro ao ler cache de localização: ${e.message}", e)
            null
        }
    }
    
    /**
     * Salva localização válida no cache persistente
     */
    private suspend fun saveLocationToCache(location: Location) {
        try {
            preferencesManager.dataStore.edit { preferences ->
                preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LATITUDE] = location.latitude
                preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LONGITUDE] = location.longitude
                preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LOCATION_TIMESTAMP] = location.time
            }
            android.util.Log.d("LocationManager", "✅ Localização salva no cache: (${location.latitude}, ${location.longitude})")
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Erro ao salvar cache de localização: ${e.message}", e)
        }
    }
    
    /**
     * Obtém o endereço a partir das coordenadas
     * CRÍTICO: Tenta múltiplas vezes com retry robusto e fallback para cache
     * 
     * @param maxAttempts Número máximo de tentativas (padrão: 10)
     * @return Address válido ou null se todas as tentativas falharem
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double, maxAttempts: Int = 10): Address? {
        if (geocoder == null) {
            android.util.Log.w("LocationManager", "📍 Geocoder não está disponível, tentando cache...")
            return getLastKnownAddressFromCache()
        }
        
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt++
            try {
                android.util.Log.d("LocationManager", "📍 Tentativa $attempt/$maxAttempts de geocoding para ($latitude, $longitude)")
                
                val addresses = withTimeoutOrNull(10000L) { // Timeout de 10s por tentativa
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1)
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                    }
                }
                
                val address = addresses?.firstOrNull()
                
                if (address != null && address.locality != null && address.adminArea != null) {
                    android.util.Log.d("LocationManager", """
                        ✅ Geocoding bem-sucedido na tentativa $attempt:
                        Locality: ${address.locality}
                        AdminArea: ${address.adminArea}
                        CountryCode: ${address.countryCode}
                    """.trimIndent())
                    // Salvar no cache
                    saveAddressToCache(latitude, longitude, address)
                    return address
                } else {
                    android.util.Log.w("LocationManager", "📍 Geocoder retornou endereço incompleto na tentativa $attempt")
                }
            } catch (e: java.io.IOException) {
                android.util.Log.w("LocationManager", "📍 Erro de IO no geocoding (tentativa $attempt): ${e.message}")
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("LocationManager", "📍 Coordenadas inválidas para geocoding: ($latitude, $longitude)")
                return getLastKnownAddressFromCache() // Não tentar novamente se as coordenadas são inválidas
            } catch (e: Exception) {
                android.util.Log.e("LocationManager", "📍 Erro inesperado no geocoding (tentativa $attempt): ${e.message}", e)
            }
            
            // Backoff exponencial: 1s, 2s, 4s, 8s, 16s, 30s (max)
            val delayMs = minOf(1000L * 2.0.pow(attempt - 1).toLong(), 30000L)
            if (attempt < maxAttempts) {
                delay(delayMs)
            }
        }
        
        android.util.Log.e("LocationManager", "❌ Falha ao obter endereço após $maxAttempts tentativas. Usando cache...")
        return getLastKnownAddressFromCache()
    }
    
    /**
     * Obtém último endereço conhecido do cache
     * 
     * ⚠️ ATENÇÃO: Este cache é usado APENAS para coordenadas (mapa) e fallback de geocoding
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar este cache para obter city/state - apenas para coordenadas quando necessário
     */
    private suspend fun getLastKnownAddressFromCache(): Address? {
        return try {
            val prefs = preferencesManager.dataStore.data.first()
            val city = prefs[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_CITY]
            val state = prefs[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_STATE]
            
            // ⚠️ Cache usado apenas para geocoding reverso (coordenadas → endereço)
            // NÃO usar para determinar city/state do usuário - isso vem do perfil
            if (city != null && state != null) {
                android.util.Log.d("LocationManager", "📍 Usando último endereço do cache (apenas para coordenadas): $city, $state")
                // Criar Address sintético do cache
                Address(Locale.getDefault()).apply {
                    locality = city
                    adminArea = state
                    countryCode = "BR"
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Erro ao ler cache de endereço: ${e.message}", e)
            null
        }
    }
    
    /**
     * Salva endereço válido no cache persistente
     * 
     * ⚠️ ATENÇÃO: Este cache é usado APENAS para coordenadas (mapa) e fallback de geocoding
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar este cache para obter city/state - apenas para coordenadas quando necessário
     */
    private suspend fun saveAddressToCache(latitude: Double, longitude: Double, address: Address) {
        try {
            val city = address.locality ?: address.subLocality ?: address.featureName
            val state = address.adminArea ?: address.subAdminArea
            
            // ⚠️ ATENÇÃO: Salvar no cache apenas para coordenadas/geocoding, NÃO para city/state
            // City/state devem vir APENAS do perfil do usuário no Firestore
            if (city != null && state != null) {
                preferencesManager.dataStore.edit { preferences ->
                    preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LATITUDE] = latitude
                    preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LONGITUDE] = longitude
                    // ⚠️ Cache de city/state apenas para geocoding reverso (coordenadas → endereço)
                    // NÃO usar para determinar city/state do usuário - isso vem do perfil
                    preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_CITY] = city
                    preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_STATE] = state
                    preferences[com.taskgoapp.taskgo.data.local.datastore.PrefsKeys.LAST_VALID_LOCATION_TIMESTAMP] = System.currentTimeMillis()
                }
                android.util.Log.d("LocationManager", "✅ Endereço salvo no cache (apenas para coordenadas): $city, $state")
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Erro ao salvar cache de endereço: ${e.message}", e)
        }
    }
    
    /**
     * Obtém GPS com GARANTIA - NUNCA retorna null
     * CRÍTICO: Esta é a função mais importante do app
     * PRIORIZA CACHE/ÚLTIMA LOCALIZAÇÃO (RÁPIDO) e tenta GPS atual em background
     * 
     * @return Location válida (NUNCA null)
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocationGuaranteed(): Location {
        android.util.Log.d("LocationManager", "🚀 getCurrentLocationGuaranteed: Obtendo GPS com garantia...")
        
        // PRIMEIRO: Tentar cache persistente (RÁPIDO - instantâneo)
        val cachedLocation = getLastKnownLocationFromCache()
        if (cachedLocation != null && LocationValidator.isValidLocationQuality(cachedLocation)) {
            android.util.Log.d("LocationManager", "✅ Usando GPS do cache persistente (rápido)")
            // Tentar obter GPS atual em background (não bloqueia)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val currentLocation = getCurrentLocation(maxAttempts = 3, timeoutMs = 5000L) // Reduzido: 3 tentativas de 5s
                    if (currentLocation != null && LocationValidator.isValidLocationQuality(currentLocation)) {
                        saveLocationToCache(currentLocation)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("LocationManager", "Erro ao atualizar GPS em background: ${e.message}")
                }
            }
            return cachedLocation
        }
        
        // Fallback 2: Última localização conhecida do sistema Android (RÁPIDO)
        try {
            val lastLocation = Tasks.await(fusedLocationClient.lastLocation)
            if (lastLocation != null && LocationValidator.isValidLocationQuality(lastLocation)) {
                android.util.Log.d("LocationManager", "✅ Usando última localização conhecida do sistema (rápido)")
                saveLocationToCache(lastLocation)
                // Tentar obter GPS atual em background
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val currentLocation = getCurrentLocation(maxAttempts = 3, timeoutMs = 5000L)
                        if (currentLocation != null && LocationValidator.isValidLocationQuality(currentLocation)) {
                            saveLocationToCache(currentLocation)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("LocationManager", "Erro ao atualizar GPS em background: ${e.message}")
                    }
                }
                return lastLocation
            }
        } catch (e: Exception) {
            android.util.Log.w("LocationManager", "⚠️ Erro ao obter última localização do sistema: ${e.message}")
        }
        
        // ÚLTIMO RECURSO: Tentar obter GPS atual (com timeout adequado)
        val currentLocation = withTimeoutOrNull(60000L) { // Timeout total de 60s
            getCurrentLocation(maxAttempts = 5, timeoutMs = 15000L) // 5 tentativas de 15s
        }
        if (currentLocation != null && LocationValidator.isValidLocationQuality(currentLocation)) {
            android.util.Log.d("LocationManager", "✅ GPS atual obtido com sucesso")
            saveLocationToCache(currentLocation)
            return currentLocation
        }
        
        // CRÍTICO: NUNCA usar fallback para Brasília/DF
        // Se não conseguir GPS, lançar exceção explícita
        val errorMsg = "ERRO CRÍTICO: Não foi possível obter localização GPS após todas as tentativas. " +
                "GPS é necessário para coordenadas do mapa. " +
                "Verifique se as permissões de localização estão habilitadas."
        android.util.Log.e("LocationManager", "❌ $errorMsg")
        throw Exception(errorMsg)
    }
    
    /**
     * Obtém endereço com GARANTIA - NUNCA retorna null
     * 
     * ⚠️ ATENÇÃO: Esta função é usada APENAS para coordenadas (mapa) e geocoding reverso
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar o city/state retornado por esta função para determinar localização do usuário
     * 
     * @return Address válido (NUNCA null) - usado apenas para coordenadas/geocoding
     */
    suspend fun getAddressGuaranteed(latitude: Double, longitude: Double): Address {
        android.util.Log.d("LocationManager", "🚀 getAddressGuaranteed: Obtendo endereço com garantia (apenas para coordenadas)...")
        
        // Tentar geocoding com retry robusto
        val address = getAddressFromLocation(latitude, longitude, maxAttempts = 10)
        if (address != null && address.locality != null && address.adminArea != null) {
            android.util.Log.d("LocationManager", "✅ Endereço obtido com sucesso (apenas para coordenadas)")
            return address
        }
        
        // Fallback: Cache persistente
        val cachedAddress = getLastKnownAddressFromCache()
        if (cachedAddress != null && cachedAddress.locality != null && cachedAddress.adminArea != null) {
            android.util.Log.d("LocationManager", "✅ Usando endereço do cache persistente (apenas para coordenadas)")
            return cachedAddress
        }
        
        // CRÍTICO: NUNCA usar fallback para Brasília/DF
        // Se não conseguir endereço, lançar exceção explícita
        val errorMsg = "ERRO CRÍTICO: Não foi possível obter endereço via geocoding após todas as tentativas. " +
                "Geocoding é necessário para coordenadas do mapa. " +
                "Verifique se o serviço de geocoding está disponível."
        android.util.Log.e("LocationManager", "❌ $errorMsg")
        throw Exception(errorMsg)
    }
    
    /**
     * Obtém o endereço completo a partir da localização atual
     * 
     * ⚠️ ATENÇÃO: Esta função é usada APENAS para coordenadas (mapa) e geocoding reverso
     * LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
     * NUNCA usar o city/state retornado por esta função para determinar localização do usuário
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentAddress(): Address? {
        val location = getCurrentLocation() ?: return null
        return getAddressFromLocation(location.latitude, location.longitude)
    }
    
    /**
     * Observa mudanças de localização
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun observeLocation(): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            close(e)
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

