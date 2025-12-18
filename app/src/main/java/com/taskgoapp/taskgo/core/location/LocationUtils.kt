package com.taskgoapp.taskgo.core.location

import android.location.Location
import kotlin.math.*

/**
 * Calcula a distância entre duas coordenadas usando a fórmula de Haversine
 * @return Distância em quilômetros
 */
fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusKm = 6371.0
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return earthRadiusKm * c
}

/**
 * Verifica se uma localização está dentro do raio especificado
 */
fun isWithinRadius(
    centerLat: Double,
    centerLon: Double,
    targetLat: Double,
    targetLon: Double,
    radiusKm: Double
): Boolean {
    val distance = calculateDistance(centerLat, centerLon, targetLat, targetLon)
    return distance <= radiusKm
}

/**
 * Extensão para Location para facilitar cálculos
 */
fun Location.distanceToKm(otherLat: Double, otherLon: Double): Double {
    return calculateDistance(this.latitude, this.longitude, otherLat, otherLon)
}

