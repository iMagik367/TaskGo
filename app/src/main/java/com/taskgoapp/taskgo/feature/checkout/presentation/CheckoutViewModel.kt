package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Address
import com.taskgoapp.taskgo.core.model.Card
import com.taskgoapp.taskgo.core.model.PaymentMethod
import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.core.model.onSuccess
import com.taskgoapp.taskgo.core.model.onFailure
import com.taskgoapp.taskgo.core.payment.PaymentGateway
import com.taskgoapp.taskgo.core.payment.PaymentGatewayRequest
import com.taskgoapp.taskgo.domain.repository.AddressRepository
import com.taskgoapp.taskgo.domain.repository.CardRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutCartItem(
    val productId: String,
    val title: String,
    val quantity: Int,
    val price: Double,
    val imageUrl: String?
)

sealed interface CheckoutProcessState {
    object Idle : CheckoutProcessState
    object Processing : CheckoutProcessState
    data class PaymentSheetReady(val clientSecret: String, val orderId: String) : CheckoutProcessState
    data class Success(val orderId: String, val trackingCode: String) : CheckoutProcessState
    data class Error(val message: String) : CheckoutProcessState
}

data class CheckoutUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cartItems: List<CheckoutCartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val availableAddresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val availablePaymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentMethod: PaymentMethod? = null,
    val checkoutProcess: CheckoutProcessState = CheckoutProcessState.Idle,
    val paymentIntentClientSecret: String? = null // Para PaymentSheet
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val addressRepository: AddressRepository,
    private val cardRepository: CardRepository,
    private val paymentGateway: PaymentGateway
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var cartJob: Job? = null
    private var addressJob: Job? = null
    private var paymentJob: Job? = null

    init {
        observeCart()
        observeAddresses()
        observePaymentMethods()
    }

    private fun observeCart() {
        cartJob?.cancel()
        cartJob = viewModelScope.launch {
            productsRepository.observeCart().collect { cart ->
                val details = cart.mapNotNull { cartItem ->
                    val product = productsRepository.getProduct(cartItem.productId)
                    product?.let {
                        CheckoutCartItem(
                            productId = cartItem.productId,
                            title = it.title,
                            quantity = cartItem.qty,
                            price = it.price,
                            imageUrl = it.imageUris.firstOrNull()
                        )
                    }
                }
                val subtotal = details.sumOf { it.price * it.quantity }
                val shipping = if (subtotal == 0.0 || subtotal >= 150.0) 0.0 else 19.9
                val total = subtotal + shipping
                _uiState.update {
                    it.copy(
                        cartItems = details,
                        subtotal = subtotal,
                        shipping = shipping,
                        total = total,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeAddresses() {
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            addressRepository.observeAddresses().collect { addresses ->
                val selected = when {
                    addresses.isEmpty() -> null
                    _uiState.value.selectedAddress == null -> addresses.firstOrNull()
                    addresses.any { it.id == _uiState.value.selectedAddress?.id } -> _uiState.value.selectedAddress
                    else -> addresses.firstOrNull()
                }
                _uiState.update {
                    it.copy(
                        availableAddresses = addresses,
                        selectedAddress = selected
                    )
                }
            }
        }
    }

    private fun observePaymentMethods() {
        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            cardRepository.observeCards().collect { cards ->
                // Sempre incluir PIX como opção disponível
                val pixMethod = PaymentMethod(
                    id = 0L,
                    type = PaymentType.PIX,
                    lastFourDigits = "PIX",
                    cardholderName = "",
                    expiryDate = "",
                    isDefault = false
                )
                
                val cardMethods = cards.map { card ->
                    PaymentMethod(
                        id = card.id.toLongOrNull() ?: 0L,
                        type = if (card.type.contains("Crédito", ignoreCase = true)) {
                            PaymentType.CREDIT_CARD
                        } else {
                            PaymentType.DEBIT_CARD
                        },
                        lastFourDigits = card.numberMasked.takeLast(4),
                        cardholderName = card.holder,
                        expiryDate = "${card.expMonth.toString().padStart(2, '0')}/${card.expYear}",
                        isDefault = false
                    )
                }
                
                // PIX sempre primeiro, depois cartões
                val methods = listOf(pixMethod) + cardMethods
                
                val selected = when {
                    _uiState.value.selectedPaymentMethod == null -> pixMethod // PIX como padrão
                    methods.any { it.type == _uiState.value.selectedPaymentMethod?.type && 
                                 (it.type != PaymentType.PIX || it.id == _uiState.value.selectedPaymentMethod?.id) } -> 
                        _uiState.value.selectedPaymentMethod
                    else -> pixMethod // Fallback para PIX
                }
                _uiState.update {
                    it.copy(
                        availablePaymentMethods = methods,
                        selectedPaymentMethod = selected
                    )
                }
            }
        }
    }

    fun selectAddress(address: Address) {
        _uiState.update { it.copy(selectedAddress = address) }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = paymentMethod) }
    }

    fun applySelection(addressId: String?, paymentType: PaymentType?) {
        viewModelScope.launch {
            if (!addressId.isNullOrBlank()) {
                addressRepository.getAddress(addressId)?.let { address ->
                    _uiState.update { it.copy(selectedAddress = address) }
                }
            }
            if (paymentType != null) {
                val method = _uiState.value.availablePaymentMethods.firstOrNull { it.type == paymentType }
                method?.let { selected ->
                    _uiState.update { it.copy(selectedPaymentMethod = selected) }
                }
            }
        }
    }

    fun finalizeOrder(addressId: String?, paymentType: PaymentType) {
        val resolvedAddressId = addressId?.takeIf { it.isNotBlank() }
            ?: _uiState.value.selectedAddress?.id
        if (resolvedAddressId.isNullOrBlank()) {
            _uiState.update {
                it.copy(checkoutProcess = CheckoutProcessState.Error("Selecione um endereço para continuar"))
            }
            return
        }

        _uiState.update { it.copy(checkoutProcess = CheckoutProcessState.Processing, error = null) }

        viewModelScope.launch {
            val result = paymentGateway.process(
                PaymentGatewayRequest(
                    paymentType = paymentType,
                    addressId = resolvedAddressId
                )
            )
            result
                .onSuccess { gatewayResult: com.taskgoapp.taskgo.core.payment.PaymentGatewayResult ->
                    // Se for cartão, precisamos apresentar o PaymentSheet
                    // Se for PIX, já está pronto para mostrar a tela de PIX
                    if (paymentType == PaymentType.CREDIT_CARD || paymentType == PaymentType.DEBIT_CARD) {
                        val clientSecret = gatewayResult.clientSecret
                        if (clientSecret != null) {
                            _uiState.update {
                                it.copy(
                                    checkoutProcess = CheckoutProcessState.PaymentSheetReady(
                                        clientSecret = clientSecret,
                                        orderId = gatewayResult.orderId
                                    ),
                                    paymentIntentClientSecret = clientSecret
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    checkoutProcess = CheckoutProcessState.Error(
                                        "Erro: clientSecret não retornado pelo servidor"
                                    )
                                )
                            }
                        }
                    } else {
                        // PIX - já está pronto
                        _uiState.update {
                            it.copy(
                                checkoutProcess = CheckoutProcessState.Success(
                                    orderId = gatewayResult.orderId,
                                    trackingCode = gatewayResult.trackingCode
                                )
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            checkoutProcess = CheckoutProcessState.Error(
                                throwable.message ?: "Erro ao processar pagamento"
                            )
                        )
                    }
                }
        }
    }

    fun resetCheckoutProcess() {
        _uiState.update { it.copy(checkoutProcess = CheckoutProcessState.Idle) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Called when PaymentSheet payment is completed successfully
     */
    fun onPaymentSheetSuccess(orderId: String, trackingCode: String) {
        _uiState.update {
            it.copy(
                checkoutProcess = CheckoutProcessState.Success(
                    orderId = orderId,
                    trackingCode = trackingCode
                ),
                paymentIntentClientSecret = null
            )
        }
    }
    
    /**
     * Called when PaymentSheet payment fails or is canceled
     */
    fun onPaymentSheetError(error: String) {
        _uiState.update {
            it.copy(
                checkoutProcess = CheckoutProcessState.Error(error),
                paymentIntentClientSecret = null
            )
        }
    }
}

