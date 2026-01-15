package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreProductsRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardMetrics(
    // Prestador
    val servicesCount: Int = 0,
    val ordersReceived: Int = 0,
    val proposalsSent: Int = 0,
    val completedOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    
    // Vendedor
    val productsCount: Int = 0,
    val productsSold: Int = 0,
    val totalSales: Double = 0.0,
    val monthlySales: Double = 0.0,
    
    // Cliente
    val serviceOrdersCreated: Int = 0,
    val productsPurchased: Int = 0,
    val totalSpent: Double = 0.0,
    val monthlySpent: Double = 0.0,
    
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val servicesRepository: FirestoreServicesRepository,
    private val orderRepository: FirestoreOrderRepository,
    private val productsRepository: ProductsRepository,
    private val firestoreProductsRepository: FirestoreProductsRepository
) : ViewModel() {
    
    private val _metrics = MutableStateFlow(DashboardMetrics())
    val metrics: StateFlow<DashboardMetrics> = _metrics.asStateFlow()
    
    private val _accountType = MutableStateFlow<AccountType?>(null)
    
    init {
        loadAccountType()
    }
    
    private fun loadAccountType() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                _accountType.value = user?.accountType
                loadMetrics(user?.accountType ?: AccountType.CLIENTE)
            }
        }
    }
    
    private fun loadMetrics(accountType: AccountType) {
        val currentUser = firebaseAuth.currentUser ?: return
        
        viewModelScope.launch {
            _metrics.value = _metrics.value.copy(isLoading = true, error = null)
            
            try {
                when (accountType) {
                    AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> {
                        // Parceiro carrega métricas combinadas de serviços + produtos
                        loadPartnerMetrics(currentUser.uid)
                    }
                    else -> loadClientMetrics(currentUser.uid)
                }
            } catch (e: Exception) {
                _metrics.value = _metrics.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar métricas: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadProviderMetrics(userId: String) {
        combine(
            servicesRepository.observeProviderServices(userId),
            orderRepository.observeOrders(userId, "provider")
        ) { services, orders ->
            val completedOrders = orders.filter { it.status == "completed" || it.status == "accepted" }
            val proposalsSent = orders.count { it.proposalDetails != null }
            
            // Calcular receita (soma dos valores das propostas aceitas)
            val totalRevenue = completedOrders
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            // Receita mensal (últimos 30 dias)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val monthlyRevenue = completedOrders
                .filter { it.updatedAt?.time ?: 0L >= thirtyDaysAgo }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            _metrics.value = DashboardMetrics(
                servicesCount = services.size,
                ordersReceived = orders.size,
                proposalsSent = proposalsSent,
                completedOrders = completedOrders.size,
                totalRevenue = totalRevenue,
                monthlyRevenue = monthlyRevenue,
                isLoading = false,
                error = null
            )
        }.collect { }
    }
    
    private suspend fun loadSellerMetrics(userId: String) {
        combine(
            firestoreProductsRepository.observeProductsBySeller(userId),
            orderRepository.observeOrders(userId, "client")
        ) { myProducts, orders ->
            
            // Calcular vendas (orders com status "completed" ou "accepted")
            val completedOrders = orders.filter { it.status == "completed" || it.status == "accepted" }
            val productsSold = completedOrders.size
            
            // Calcular receita (soma dos valores das propostas aceitas)
            val totalSales = completedOrders
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            // Vendas mensais (últimos 30 dias)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val monthlySales = completedOrders
                .filter { it.updatedAt?.time ?: 0L >= thirtyDaysAgo }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            _metrics.value = DashboardMetrics(
                productsCount = myProducts.size,
                productsSold = productsSold,
                totalSales = totalSales,
                monthlySales = monthlySales,
                isLoading = false,
                error = null
            )
        }.collect { }
    }
    
    private suspend fun loadPartnerMetrics(userId: String) {
        // Carregar métricas combinadas de serviços + produtos para Parceiro
        combine(
            servicesRepository.observeProviderServices(userId),
            firestoreProductsRepository.observeProductsBySeller(userId),
            orderRepository.observeOrders(userId, "partner"), // Usar "partner" para buscar ordens de serviço (tratado como provider)
            orderRepository.observeOrders(userId, "client") // Orders como cliente também podem incluir produtos vendidos
        ) { services, products, serviceOrders, productOrders ->
            // Métricas de serviços
            val completedServiceOrders = serviceOrders.filter { it.status == "completed" || it.status == "accepted" }
            val proposalsSent = serviceOrders.count { it.proposalDetails != null }
            val serviceRevenue = completedServiceOrders
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            // Métricas de produtos
            val completedProductOrders = productOrders.filter { it.status == "completed" || it.status == "accepted" }
            val productsSold = completedProductOrders.size
            val productSales = completedProductOrders
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            // Receitas mensais (últimos 30 dias)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val monthlyServiceRevenue = completedServiceOrders
                .filter { it.updatedAt?.time ?: 0L >= thirtyDaysAgo }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            val monthlyProductSales = completedProductOrders
                .filter { it.updatedAt?.time ?: 0L >= thirtyDaysAgo }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            _metrics.value = DashboardMetrics(
                servicesCount = services.size,
                productsCount = products.size,
                ordersReceived = serviceOrders.size,
                proposalsSent = proposalsSent,
                completedOrders = completedServiceOrders.size,
                productsSold = productsSold,
                totalRevenue = serviceRevenue, // Receita de serviços
                totalSales = productSales, // Vendas de produtos
                monthlyRevenue = monthlyServiceRevenue,
                monthlySales = monthlyProductSales,
                isLoading = false,
                error = null
            )
        }.collect { }
    }
    
    private suspend fun loadClientMetrics(userId: String) {
        orderRepository.observeOrders(userId, "client").collect { orders ->
            val serviceOrdersCreated = orders.size
            val productsPurchased = orders.count { it.proposalDetails != null }
            
            // Calcular gastos
            val totalSpent = orders
                .filter { it.status == "completed" || it.status == "accepted" }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            // Gastos mensais
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val monthlySpent = orders
                .filter { (it.updatedAt?.time ?: 0L) >= thirtyDaysAgo }
                .filter { it.status == "completed" || it.status == "accepted" }
                .mapNotNull { it.proposalDetails?.price }
                .sum()
            
            _metrics.value = DashboardMetrics(
                serviceOrdersCreated = serviceOrdersCreated,
                productsPurchased = productsPurchased,
                totalSpent = totalSpent,
                monthlySpent = monthlySpent,
                isLoading = false,
                error = null
            )
        }
    }
    
    fun refresh() {
        _accountType.value?.let { loadMetrics(it) }
    }
}

