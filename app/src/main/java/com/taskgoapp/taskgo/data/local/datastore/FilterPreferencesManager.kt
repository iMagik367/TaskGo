package com.taskgoapp.taskgo.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskgoapp.taskgo.core.design.FilterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.filterPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "taskgo_filter_preferences")

object FilterPrefsKeys {
    val PRODUCT_FILTERS = stringPreferencesKey("product_filters_json")
    val SERVICE_FILTERS = stringPreferencesKey("service_filters_json")
}

@Singleton
class FilterPreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.filterPreferencesDataStore
    private val gson = Gson()
    
    /**
     * Salva filtros preferidos para produtos
     */
    suspend fun saveProductFilters(filterState: FilterState) {
        dataStore.edit { preferences ->
            val json = gson.toJson(filterState)
            preferences[FilterPrefsKeys.PRODUCT_FILTERS] = json
        }
    }
    
    /**
     * Carrega filtros preferidos para produtos
     */
    fun getProductFilters(): Flow<FilterState?> = dataStore.data.map { preferences ->
        val json = preferences[FilterPrefsKeys.PRODUCT_FILTERS]
        if (json != null) {
            try {
                gson.fromJson(json, FilterState::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Salva filtros preferidos para serviços
     */
    suspend fun saveServiceFilters(filterState: FilterState) {
        dataStore.edit { preferences ->
            val json = gson.toJson(filterState)
            preferences[FilterPrefsKeys.SERVICE_FILTERS] = json
        }
    }
    
    /**
     * Carrega filtros preferidos para serviços
     */
    fun getServiceFilters(): Flow<FilterState?> = dataStore.data.map { preferences ->
        val json = preferences[FilterPrefsKeys.SERVICE_FILTERS]
        if (json != null) {
            try {
                gson.fromJson(json, FilterState::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Limpa filtros salvos
     */
    suspend fun clearProductFilters() {
        dataStore.edit { preferences ->
            preferences.remove(FilterPrefsKeys.PRODUCT_FILTERS)
        }
    }
    
    suspend fun clearServiceFilters() {
        dataStore.edit { preferences ->
            preferences.remove(FilterPrefsKeys.SERVICE_FILTERS)
        }
    }
}

