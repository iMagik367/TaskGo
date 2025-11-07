package com.taskgoapp.taskgo.core.navigation

import androidx.navigation.NavController
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository

/**
 * Helper para proteção de rotas baseado em autenticação
 */
object AuthNavigationHelper {
    
    /**
     * Rotas que requerem autenticação
     */
    private val protectedRoutes = setOf(
        "home",
        "profile",
        "messages",
        "services",
        "products",
        "notifications",
        "settings",
        "configuracoes",
        "meus_pedidos",
        "meus_servicos",
        "meus_produtos",
        "cart",
        "checkout",
        "orders",
        "chat"
    )
    
    /**
     * Rotas públicas (não requerem autenticação)
     */
    private val publicRoutes = setOf(
        "splash",
        "login_person",
        "login_store",
        "signup",
        "signup_success",
        "forgot_password"
    )
    
    /**
     * Verifica se uma rota requer autenticação
     */
    fun isProtectedRoute(route: String): Boolean {
        return protectedRoutes.any { route.startsWith(it) } ||
               route.contains("/") && protectedRoutes.any { route.startsWith("$it/") }
    }
    
    /**
     * Verifica se uma rota é pública
     */
    fun isPublicRoute(route: String): Boolean {
        return publicRoutes.contains(route) || 
               route.startsWith("login") || 
               route.startsWith("signup")
    }
    
    /**
     * Navega para uma rota protegida, redirecionando para login se não autenticado
     */
    fun navigateToProtectedRoute(
        navController: NavController,
        authRepository: FirebaseAuthRepository,
        route: String
    ) {
        if (isProtectedRoute(route)) {
            if (!authRepository.isLoggedIn()) {
                // Redirecionar para login se não autenticado
                navController.navigate("login_person") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            } else {
                // Navegar normalmente se autenticado
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        } else {
            // Navegar normalmente para rotas públicas
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }
}

