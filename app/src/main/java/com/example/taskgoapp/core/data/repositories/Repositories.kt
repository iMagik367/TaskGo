package com.example.taskgoapp.core.data.repositories

import com.example.taskgoapp.core.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface MarketplaceRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductById(id: Long): Flow<Product?>
}

@Singleton
class MarketplaceRepositoryImpl @Inject constructor() : MarketplaceRepository {
    
    override fun getProducts(): Flow<List<Product>> = flow {
        delay(500)
        emit(listOf(
            Product(
                id = 1L,
                name = "Guarda Roupa 6 Portas",
                description = "Guarda roupa 6 portas com espelho, acabamento em MDF, cor branca. Perfeito para quartos modernos.",
                price = 899.90,
                seller = User(
                    id = 1L,
                    name = "João Silva",
                    email = "joao@email.com",
                    phone = "(11) 99999-9999",
                    accountType = AccountType.SELLER,
                    rating = 4.8,
                    reviewCount = 156,
                    city = "São Paulo",
                    timeOnTaskGo = "2 anos"
                ),
                category = "Móveis"
            ),
            Product(
                id = 2L,
                name = "Furadeira sem fio 18V",
                description = "Furadeira 18V com 2 baterias, ideal para trabalhos domésticos e profissionais.",
                price = 299.90,
                seller = User(
                    id = 2L,
                    name = "Maria Santos",
                    email = "maria@email.com",
                    phone = "(11) 88888-8888",
                    accountType = AccountType.SELLER,
                    rating = 4.6,
                    reviewCount = 89,
                    city = "Rio de Janeiro",
                    timeOnTaskGo = "1 ano"
                ),
                category = "Ferramentas"
            ),
            Product(
                id = 3L,
                name = "Forno de Embutir 30L",
                description = "Forno elétrico 30L com timer, função grill e limpeza pirolítica. Ideal para cozinhas modernas.",
                price = 599.90,
                seller = User(
                    id = 3L,
                    name = "Pedro Costa",
                    email = "pedro@email.com",
                    phone = "(11) 77777-7777",
                    accountType = AccountType.SELLER,
                    rating = 4.9,
                    reviewCount = 234,
                    city = "Belo Horizonte",
                    timeOnTaskGo = "3 anos"
                ),
                category = "Eletrodomésticos"
            ),
            Product(
                id = 4L,
                name = "Martelo 500g",
                description = "Martelo 500g com cabo de madeira, resistente e durável. Perfeito para trabalhos de construção.",
                price = 45.90,
                seller = User(
                    id = 4L,
                    name = "Ana Oliveira",
                    email = "ana@email.com",
                    phone = "(11) 66666-6666",
                    accountType = AccountType.SELLER,
                    rating = 4.5,
                    reviewCount = 67,
                    city = "Curitiba",
                    timeOnTaskGo = "6 meses"
                ),
                category = "Ferramentas"
            )
        ))
    }
    
    override fun getProductById(id: Long): Flow<Product?> = flow {
        delay(300)
        val products = listOf(
            Product(
                id = 1L,
                name = "Guarda Roupa 6 Portas",
                description = "Guarda roupa 6 portas com espelho, acabamento em MDF, cor branca. Perfeito para quartos modernos. Inclui gavetas e cabides.",
                price = 899.90,
                seller = User(
                    id = 1L,
                    name = "João Silva",
                    email = "joao@email.com",
                    phone = "(11) 99999-9999",
                    accountType = AccountType.SELLER,
                    rating = 4.8,
                    reviewCount = 156,
                    city = "São Paulo",
                    timeOnTaskGo = "2 anos"
                ),
                category = "Móveis"
            ),
            Product(
                id = 2L,
                name = "Furadeira sem fio 18V",
                description = "Furadeira 18V com 2 baterias, ideal para trabalhos domésticos e profissionais.",
                price = 299.90,
                seller = User(
                    id = 2L,
                    name = "Maria Santos",
                    email = "maria@email.com",
                    phone = "(11) 88888-8888",
                    accountType = AccountType.SELLER,
                    rating = 4.6,
                    reviewCount = 89,
                    city = "Rio de Janeiro",
                    timeOnTaskGo = "1 ano"
                ),
                category = "Ferramentas"
            ),
            Product(
                id = 3L,
                name = "Forno de Embutir 30L",
                description = "Forno elétrico 30L com timer, função grill e limpeza pirolítica. Ideal para cozinhas modernas.",
                price = 599.90,
                seller = User(
                    id = 3L,
                    name = "Pedro Costa",
                    email = "pedro@email.com",
                    phone = "(11) 77777-7777",
                    accountType = AccountType.SELLER,
                    rating = 4.9,
                    reviewCount = 234,
                    city = "Belo Horizonte",
                    timeOnTaskGo = "3 anos"
                ),
                category = "Eletrodomésticos"
            ),
            Product(
                id = 4L,
                name = "Martelo 500g",
                description = "Martelo 500g com cabo de madeira, resistente e durável. Perfeito para trabalhos de construção.",
                price = 45.90,
                seller = User(
                    id = 4L,
                    name = "Ana Oliveira",
                    email = "ana@email.com",
                    phone = "(11) 66666-6666",
                    accountType = AccountType.SELLER,
                    rating = 4.5,
                    reviewCount = 67,
                    city = "Curitiba",
                    timeOnTaskGo = "6 meses"
                ),
                category = "Ferramentas"
            )
        )
        emit(products.find { it.id == id })
    }
}