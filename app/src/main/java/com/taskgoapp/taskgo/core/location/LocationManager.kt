package com.taskgoapp.taskgo.core.location

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationManager @Inject constructor(
    private val context: Context
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
     * Obtém a localização atual do usuário
     * Requer permissões ACCESS_FINE_LOCATION ou ACCESS_COARSE_LOCATION
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        var isResumed = false // Flag para garantir que só resuma uma vez
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 segundos
        ).apply {
            setMaxUpdateDelayMillis(5000L)
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
        }
    }
    
    /**
     * Obtém o endereço a partir das coordenadas
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Address? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder?.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                geocoder?.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtém o endereço completo a partir da localização atual
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

