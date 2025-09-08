package com.example.taskgoapp.core.chat

import com.example.taskgoapp.core.model.ChatMessage
import com.example.taskgoapp.core.work.WorkManagerHelper
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSimulator @Inject constructor(
    private val workManagerHelper: WorkManagerHelper
) {
    
    private val responses = listOf(
        "Obrigado pela mensagem! Vou responder em breve.",
        "Entendi. Vou verificar e te retorno.",
        "Perfeito! Vamos agendar isso.",
        "Ótima pergunta! Deixe-me pensar sobre isso.",
        "Combinado! Fico no aguardo.",
        "Entendido. Qual o próximo passo?",
        "Excelente! Vamos fazer isso.",
        "Perfeito! Quando podemos começar?",
        "Ótimo! Vou preparar tudo.",
        "Combinado! Até logo."
    )
    
    suspend fun simulateResponse(threadId: String, userMessage: String) {
        // Simulate thinking time
        delay(2000)
        
        // Schedule a response after 5-15 seconds
        val delaySeconds = (5..15).random().toLong()
        workManagerHelper.scheduleChatResponse(threadId, delaySeconds)
    }
    
    fun getRandomResponse(): String {
        return responses.random()
    }
    
    fun getContextualResponse(userMessage: String): String {
        val message = userMessage.lowercase()
        
        return when {
            message.contains("preço") || message.contains("valor") || message.contains("custo") -> 
                "Vou calcular o valor e te envio uma proposta detalhada."
            message.contains("quando") || message.contains("data") || message.contains("horário") -> 
                "Vamos agendar para a próxima semana. Qual dia funciona melhor?"
            message.contains("obrigado") || message.contains("valeu") -> 
                "De nada! Fico feliz em ajudar."
            message.contains("sim") || message.contains("ok") || message.contains("perfeito") -> 
                "Ótimo! Vamos começar então."
            message.contains("não") || message.contains("não pode") -> 
                "Entendo. Vamos pensar em uma alternativa."
            message.contains("oi") || message.contains("olá") -> 
                "Olá! Como posso ajudá-lo hoje?"
            else -> getRandomResponse()
        }
    }
}
