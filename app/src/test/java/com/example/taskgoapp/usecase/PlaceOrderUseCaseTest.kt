package com.example.taskgoapp.usecase

import com.example.taskgoapp.core.model.CartItem
import com.example.taskgoapp.core.model.Product
import com.example.taskgoapp.domain.repository.OrdersRepository
import com.example.taskgoapp.domain.repository.ProductsRepository
import com.example.taskgoapp.domain.repository.TrackingRepository
import com.example.taskgoapp.domain.usecase.PlaceOrderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertTrue(result.isSuccess)
        assertEquals("order123", result.getOrNull())
    }

    @Test
    fun `placeOrder should fail with empty cart`() = runTest {
        // Given
        whenever(productsRepository.observeCart()).thenReturn(flowOf(emptyList()))

        // When
        val result = placeOrderUseCase("Pix", "address1")

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Carrinho vazio", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getOrderSummary should calculate correct totals`() = runTest {
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

        whenever(productsRepository.getProduct("product1")).thenReturn(product1)
        whenever(productsRepository.getProduct("product2")).thenReturn(product2)

        // When
        val summary = placeOrderUseCase.getOrderSummary(cart)

        // Then
        assertEquals(40.0, summary.subtotal)
        assertEquals(15.0, summary.shipping) // Free shipping over R$ 100
        assertEquals(55.0, summary.total)
        assertEquals(3, summary.itemCount)
    }

    @Test
    fun `getOrderSummary should apply free shipping for orders over 100`() = runTest {
        // Given
        val cart = listOf(
            CartItem("product1", 10) // 10 * 10 = 100
        )
        val product1 = Product(
            id = "product1",
            title = "Product 1",
            price = 10.0,
            description = "Description 1",
            sellerName = "Seller 1",
            imageUris = emptyList()
        )

        whenever(productsRepository.getProduct("product1")).thenReturn(product1)

        // When
        val summary = placeOrderUseCase.getOrderSummary(cart)

        // Then
        assertEquals(100.0, summary.subtotal)
        assertEquals(0.0, summary.shipping) // Free shipping
        assertEquals(100.0, summary.total)
    }
}
