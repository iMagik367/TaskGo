package com.taskgoapp.taskgo.core.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.operationalLocationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "taskgo_operational_location"
)

object OperationalLocationKeys {
    val CITY = stringPreferencesKey("operational_city")
    val STATE = stringPreferencesKey("operational_state")
    val LOCATION_ID = stringPreferencesKey("operational_location_id")
    val SOURCE = stringPreferencesKey("operational_source") // "GPS" ou "PROFILE"
    val UPDATED_AT = longPreferencesKey("operational_updated_at")
}

/**
 * Store para persistir localização operacional
 * 
 * Esta é a fonte primária de localização do app.
 * O app sempre tenta usar este cache primeiro antes de buscar GPS.
 */
@Singleton
class OperationalLocationStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.operationalLocationDataStore
    
    /**
     * Obtém a localização operacional persistida
     * 
     * @return OperationalLocation se existir, null caso contrário
     */
    suspend fun get(): OperationalLocation? {
        return dataStore.data.map { preferences ->
            val city = preferences[OperationalLocationKeys.CITY]
            val state = preferences[OperationalLocationKeys.STATE]
            val locationId = preferences[OperationalLocationKeys.LOCATION_ID]
            val sourceStr = preferences[OperationalLocationKeys.SOURCE] ?: "PROFILE"
            val updatedAt = preferences[OperationalLocationKeys.UPDATED_AT] ?: 0L
            
            if (city != null && state != null && locationId != null) {
                try {
                    val source = LocationSource.valueOf(sourceStr)
                    OperationalLocation(
                        city = city,
                        state = state,
                        locationId = locationId,
                        source = source,
                        updatedAt = updatedAt
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.first()
    }
    
    /**
     * Salva a localização operacional
     * 
     * @param location Localização a ser persistida
     */
    suspend fun save(location: OperationalLocation) {
        dataStore.edit { preferences ->
            preferences[OperationalLocationKeys.CITY] = location.city
            preferences[OperationalLocationKeys.STATE] = location.state
            preferences[OperationalLocationKeys.LOCATION_ID] = location.locationId
            preferences[OperationalLocationKeys.SOURCE] = location.source.name
            preferences[OperationalLocationKeys.UPDATED_AT] = location.updatedAt
        }
    }
    
    /**
     * Limpa a localização operacional persistida
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(OperationalLocationKeys.CITY)
            preferences.remove(OperationalLocationKeys.STATE)
            preferences.remove(OperationalLocationKeys.LOCATION_ID)
            preferences.remove(OperationalLocationKeys.SOURCE)
            preferences.remove(OperationalLocationKeys.UPDATED_AT)
        }
    }
}
