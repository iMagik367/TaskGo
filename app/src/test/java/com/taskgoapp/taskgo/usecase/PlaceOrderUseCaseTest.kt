package com.taskgoapp.taskgo.usecase

import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import com.taskgoapp.taskgo.domain.usecase.PlaceOrderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.junit.Assert.*

class PlaceOrderUseCaseTest {

    @Mock
    private lateinit var productsRepository: ProductsRepository

    @Mock
    private lateinit var ordersRepository: OrdersRepository

    @Mock
    private lateinit var trackingRepository: TrackingRepository

    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        placeOrderUseCase = PlaceOrderUseCase(
            productsRepository,
            ordersRepository,
            trackingRepository
        )
    }

    @Test
    fun `placeOrder should succeed with valid cart`() = runTest {
        // Given
        val cart = listOf(
            CartItem("product1", 2),
            CartItem("product2", 1)
        )
        val product1 = Product(
            id = "product1",
            title = "Product 1",
            price = 10.0,
            description = "Description 1",
            sellerName = "Seller 1",
            imageUris = emptyList()
        )
        val product2 = Product(
            id = "product2",
            title = "Product 2",
            price = 20.0,
            description = "Description 2",
            sellerName = "Seller 2",
            imageUris = emptyList()
        )

        whenever(productsRepository.observeCart()).thenReturn(flowOf(cart))
        whenever(productsRepository.getProduct("product1")).thenReturn(product1)
        whenever(productsRepository.getProduct("product2")).thenReturn(product2)
        whenever(ordersRepository.createOrder(cart, 40.0, "Pix", "address1"))
            .thenReturn("order123")

        // When
        val result = placeOrderUseCase("Pix", "address1")

        // Then
        assertEquals("order123", result)
    }

    @Test
    fun `placeOrder should fail with empty cart`() = runTest {
        // Given
        whenever(productsRepository.observeCart()).thenReturn(flowOf(emptyList()))

        // When & Then
        try {
            placeOrderUseCase("Pix", "address1")
            fail("Deveria ter lançado exceção")
        } catch (e: IllegalArgumentException) {
            assertEquals("Carrinho vazio", e.message)
        }
    }

}
