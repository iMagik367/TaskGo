package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Address
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.Card
import com.taskgoapp.taskgo.core.model.PaymentMethod
import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.domain.repository.AddressRepository
import com.taskgoapp.taskgo.domain.repository.CardRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAddress: Address? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val cartItems: List<CartItem> = emptyList()
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val addressRepository: AddressRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = productsRepository
        .observeCart()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val addresses: StateFlow<List<Address>> = addressRepository
        .observeAddresses()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val cards: StateFlow<List<Card>> = cardRepository
        .observeCards()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Carregar primeiro endereço disponível
                val addressesList = addresses.value
                val defaultAddress = addressesList.firstOrNull()
                
                // Carregar primeiro cartão disponível e converter para PaymentMethod
                val cardsList = cards.value
                val defaultCard = cardsList.firstOrNull()
                
                val paymentMethod = defaultCard?.let { card ->
                    // Extrair últimos 4 dígitos do número mascarado
                    val lastFourDigits = card.numberMasked.takeLast(4)
                    val expiryDate = "${card.expMonth.toString().padStart(2, '0')}/${card.expYear}"
                    
                    PaymentMethod(
                        id = card.id.toLongOrNull() ?: 0L,
                        type = if (card.type.contains("Crédito", ignoreCase = true)) 
                            PaymentType.CREDIT_CARD 
                        else 
                            PaymentType.DEBIT_CARD,
                        lastFourDigits = lastFourDigits,
                        cardholderName = card.holder,
                        expiryDate = expiryDate,
                        isDefault = false // TODO: Adicionar campo isDefault no modelo Card
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedAddress = defaultAddress,
                    selectedPaymentMethod = paymentMethod
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar dados"
                )
            }
        }
    }

    fun selectAddress(address: Address) {
        _uiState.value = _uiState.value.copy(selectedAddress = address)
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = paymentMethod)
    }

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

