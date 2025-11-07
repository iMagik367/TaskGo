package com.taskgoapp.taskgo.core.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskgo_prefs", Context.MODE_PRIVATE)

    // User Profile Data
    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    
    fun saveUserEmail(email: String) = prefs.edit().putString("user_email", email).apply()
    fun getUserEmail(): String = prefs.getString("user_email", "") ?: ""
    
    fun saveUserPhone(phone: String) = prefs.edit().putString("user_phone", phone).apply()
    fun getUserPhone(): String = prefs.getString("user_phone", "") ?: ""
    
    fun saveUserCpf(cpf: String) = prefs.edit().putString("user_cpf", cpf).apply()
    fun getUserCpf(): String = prefs.getString("user_cpf", "") ?: ""
    
    fun saveUserProfession(profession: String) = prefs.edit().putString("user_profession", profession).apply()
    fun getUserProfession(): String = prefs.getString("user_profession", "") ?: ""
    
    fun saveUserAvatarUri(avatarUri: String) = prefs.edit().putString("user_avatar_uri", avatarUri).apply()
    fun getUserAvatarUri(): String? = prefs.getString("user_avatar_uri", null)

    // Product Data
    fun saveProductName(name: String) = prefs.edit().putString("product_name", name).apply()
    fun getProductName(): String = prefs.getString("product_name", "") ?: ""
    
    fun saveProductDescription(description: String) = prefs.edit().putString("product_description", description).apply()
    fun getProductDescription(): String = prefs.getString("product_description", "") ?: ""
    
    fun saveProductPrice(price: String) = prefs.edit().putString("product_price", price).apply()
    fun getProductPrice(): String = prefs.getString("product_price", "") ?: ""
    
    fun saveProductCategory(category: String) = prefs.edit().putString("product_category", category).apply()
    fun getProductCategory(): String = prefs.getString("product_category", "") ?: ""
    
    fun saveProductAddress(address: String) = prefs.edit().putString("product_address", address).apply()
    fun getProductAddress(): String = prefs.getString("product_address", "") ?: ""
    
    fun saveProductImages(images: List<String>) = prefs.edit().putStringSet("product_images", images.toSet()).apply()
    fun getProductImages(): List<String> = prefs.getStringSet("product_images", emptySet())?.toList() ?: emptyList()
    
    fun saveUserProfileImages(images: List<String>) = prefs.edit().putStringSet("user_profile_images", images.toSet()).apply()
    fun getUserProfileImages(): List<String> = prefs.getStringSet("user_profile_images", emptySet())?.toList() ?: emptyList()

    // Cart Data
    fun saveCartQuantity(quantity: Int) = prefs.edit().putInt("cart_quantity", quantity).apply()
    fun getCartQuantity(): Int = prefs.getInt("cart_quantity", 1)

    // Settings Data
    fun saveNotificationEnabled(enabled: Boolean) = prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    fun getNotificationEnabled(): Boolean = prefs.getBoolean("notifications_enabled", true)
    
    fun saveSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_enabled", enabled).apply()
    fun getSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)
    
    fun saveLanguage(language: String) = prefs.edit().putString("language", language).apply()
    fun getLanguage(): String = prefs.getString("language", "pt") ?: "pt"

    // Auth
    fun saveAuthToken(token: String?) {
        if (token.isNullOrBlank()) {
            prefs.edit().remove("auth_token").apply()
        } else {
            prefs.edit().putString("auth_token", token).apply()
        }
    }
    fun getAuthToken(): String? = prefs.getString("auth_token", null)

    fun getAuthRole(): String? {
        val token = getAuthToken() ?: return null
        val parts = token.split('.')
        if (parts.size < 2) return null
        return try {
            val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            val json = org.json.JSONObject(String(payloadBytes))
            json.optString("role").ifBlank { null }
        } catch (_: Exception) { null }
    }

    // Clear all data
    fun clearAll() = prefs.edit().clear().apply()
}
