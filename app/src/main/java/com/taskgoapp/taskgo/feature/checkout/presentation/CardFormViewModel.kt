package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Card
import com.taskgoapp.taskgo.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class CardFormViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardFormUiState())
    val uiState: StateFlow<CardFormUiState> = _uiState.asStateFlow()

    fun saveCard(
        holder: String,
        cardNumber: String,
        expMonth: Int,
        expYear: Int,
        cvc: String,
        type: String // "Crédito" ou "Débito"
    ) {
        if (holder.isEmpty() || cardNumber.isEmpty() || cvc.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Preencha todos os campos")
            return
        }

        // Validar número do cartão (deve ter pelo menos 13 dígitos)
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        if (cleanNumber.length < 13 || cleanNumber.length > 19) {
            _uiState.value = _uiState.value.copy(error = "Número do cartão inválido")
            return
        }

        // Validar CVC (deve ter 3 ou 4 dígitos)
        val cleanCvc = cvc.replace(Regex("[^0-9]"), "")
        if (cleanCvc.length < 3 || cleanCvc.length > 4) {
            _uiState.value = _uiState.value.copy(error = "CVC inválido")
            return
        }

        // Validar validade
        if (expMonth < 1 || expMonth > 12) {
            _uiState.value = _uiState.value.copy(error = "Mês inválido")
            return
        }

        if (expYear < 2024) {
            _uiState.value = _uiState.value.copy(error = "Ano inválido")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Detectar bandeira do cartão (heurística simples)
                val brand = detectCardBrand(cleanNumber)

                // Mascarar número do cartão (mostrar apenas últimos 4 dígitos)
                val maskedNumber = "**** **** **** ${cleanNumber.takeLast(4)}"

                val card = Card(
                    id = "", // Novo cartão
                    holder = holder,
                    numberMasked = maskedNumber,
                    brand = brand,
                    expMonth = expMonth,
                    expYear = expYear,
                    type = type
                )

                cardRepository.upsertCard(card)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao salvar cartão"
                )
            }
        }
    }

    private fun detectCardBrand(cardNumber: String): String {
        return when {
            cardNumber.startsWith("4") -> "Visa"
            cardNumber.startsWith("5") || cardNumber.startsWith("2") -> "Mastercard"
            cardNumber.startsWith("3") -> "American Express"
            cardNumber.startsWith("6") -> "Discover"
            else -> "Desconhecida"
        }
    }

    fun resetState() {
        _uiState.value = CardFormUiState()
    }
}

