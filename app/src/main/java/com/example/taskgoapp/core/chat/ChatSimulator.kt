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
    // Chat simulado desativado para produção
}
