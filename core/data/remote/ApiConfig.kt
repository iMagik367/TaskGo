package br.com.taskgo.core.data.remote

object ApiConfig {
    const val PRODUCTION_BASE_URL = "https://taskgo-api.onrender.com/v1/"
    const val ADMIN_PANEL_URL = "https://taskgo-admin.onrender.com"
    
    // URLs específicas da API
    const val LOGIN_ENDPOINT = "auth/login"
    const val REGISTER_ENDPOINT = "auth/register"
    const val REFRESH_TOKEN_ENDPOINT = "auth/refresh"
    
    // Função para obter a URL base baseada no ambiente
    fun getBaseUrl(): String {
        return PRODUCTION_BASE_URL
    }

    // Função para obter a URL do painel admin
    fun getAdminPanelUrl(): String {
        return ADMIN_PANEL_URL
    }
}