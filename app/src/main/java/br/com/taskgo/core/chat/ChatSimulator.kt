package br.com.taskgo.taskgo.core.chat

import com.example.taskgoapp.core.work.WorkManagerHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSimulator @Inject constructor(
    private val workManagerHelper: WorkManagerHelper
) {
    // Chat simulado desativado para produção
}
