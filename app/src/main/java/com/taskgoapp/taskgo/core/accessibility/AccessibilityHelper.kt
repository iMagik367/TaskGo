package com.taskgoapp.taskgo.core.accessibility

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

// Accessibility property keys
val ContentDescriptionKey = SemanticsPropertyKey<String>("ContentDescription")
val TestTagKey = SemanticsPropertyKey<String>("TestTag")

// Extension functions for accessibility
fun SemanticsPropertyReceiver.contentDescription(description: String) {
    this[ContentDescriptionKey] = description
}

fun SemanticsPropertyReceiver.testTag(tag: String) {
    this[TestTagKey] = tag
}

// Common accessibility strings
object AccessibilityStrings {
    const val BACK_BUTTON = "Botão voltar"
    const val SEARCH_BUTTON = "Botão buscar"
    const val CART_BUTTON = "Botão carrinho"
    const val NOTIFICATIONS_BUTTON = "Botão notificações"
    const val PROFILE_BUTTON = "Botão perfil"
    const val ADD_BUTTON = "Botão adicionar"
    const val REMOVE_BUTTON = "Botão remover"
    const val EDIT_BUTTON = "Botão editar"
    const val DELETE_BUTTON = "Botão excluir"
    const val SEND_BUTTON = "Botão enviar"
    const val SAVE_BUTTON = "Botão salvar"
    const val CANCEL_BUTTON = "Botão cancelar"
    const val CONFIRM_BUTTON = "Botão confirmar"
    const val ACCEPT_BUTTON = "Botão aceitar"
    const val REJECT_BUTTON = "Botão rejeitar"
    
    fun starRating(rating: Int) = "Avaliação $rating estrelas"
    fun productImage(productName: String) = "Imagem do produto $productName"
    fun userAvatar(userName: String) = "Avatar do usuário $userName"
    fun qrCode() = "Código QR"
    fun creditCard() = "Cartão de crédito"
    fun debitCard() = "Cartão de débito"
    fun pixPayment() = "Pagamento via Pix"
    
    // Form fields
    fun textField(label: String) = "Campo de texto $label"
    fun emailField() = "Campo de e-mail"
    fun passwordField() = "Campo de senha"
    fun phoneField() = "Campo de telefone"
    fun cepField() = "Campo de CEP"
    fun priceField() = "Campo de preço"
    
    // Navigation
    fun tabItem(tabName: String) = "Aba $tabName"
    fun navigationItem(itemName: String) = "Item de navegação $itemName"
    
    // Lists and cards
    fun productCard(productName: String) = "Card do produto $productName"
    fun serviceCard(serviceName: String) = "Card do serviço $serviceName"
    fun orderCard(orderId: String) = "Card do pedido $orderId"
    fun messageCard(senderName: String) = "Card de mensagem de $senderName"
    
    // Status indicators
    fun loadingState() = "Carregando"
    fun errorState() = "Erro"
    fun emptyState() = "Nenhum item encontrado"
    fun successState() = "Sucesso"
    
    // Actions
    fun addToCart(productName: String) = "Adicionar $productName ao carrinho"
    fun removeFromCart(productName: String) = "Remover $productName do carrinho"
    fun increaseQuantity(productName: String) = "Aumentar quantidade de $productName"
    fun decreaseQuantity(productName: String) = "Diminuir quantidade de $productName"
    fun viewDetails(itemName: String) = "Ver detalhes de $itemName"
    fun editItem(itemName: String) = "Editar $itemName"
    fun deleteItem(itemName: String) = "Excluir $itemName"
    
    // Notifications
    fun notificationItem(title: String) = "Notificação: $title"
    fun unreadNotification() = "Notificação não lida"
    fun readNotification() = "Notificação lida"
    
    // Tracking
    fun trackingStep(stepName: String, isCompleted: Boolean) = 
        "Etapa $stepName ${if (isCompleted) "concluída" else "pendente"}"
    fun trackingCode(code: String) = "Código de rastreamento $code"
    
    // Chat
    fun messageBubble(isOwn: Boolean, message: String) = 
        "${if (isOwn) "Sua mensagem" else "Mensagem recebida"}: $message"
    fun typingIndicator() = "Digitando"
    fun messageInput() = "Campo de entrada de mensagem"
    
    // Settings
    fun switchSetting(settingName: String, isEnabled: Boolean) = 
        "Configuração $settingName ${if (isEnabled) "ativada" else "desativada"}"
    fun languageOption(language: String) = "Idioma $language"
    fun themeOption(theme: String) = "Tema $theme"
}
