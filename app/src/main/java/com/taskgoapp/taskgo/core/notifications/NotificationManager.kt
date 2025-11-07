package com.taskgoapp.taskgo.core.notifications

import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskgoapp.taskgo.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val CHANNEL_ID = "taskgo.default"
        const val CHANNEL_NAME = "TaskGo"
        const val CHANNEL_DESCRIPTION = "Notificações do TaskGo"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNotification(
        id: Int,
        title: String,
        message: String,
        actionText: String? = null
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        if (actionText != null) {
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                actionText,
                null // PendingIntent would go here
            )
        }
        
        with(NotificationManagerCompat.from(context)) {
            if (areNotificationsEnabled()) {
                notify(id, builder.build())
            }
        }
    }
    
    fun showOrderShippedNotification(orderId: String) {
        showNotification(
            id = orderId.hashCode(),
            title = "Pedido Enviado",
            message = "Seu pedido foi despachado e está a caminho!",
            actionText = "Acompanhar"
        )
    }
    
    fun showProposalApprovedNotification(proposalId: String) {
        showNotification(
            id = proposalId.hashCode(),
            title = "Proposta Aprovada",
            message = "Sua proposta foi aceita pelo cliente!",
            actionText = "Ver Detalhes"
        )
    }
    
    fun showNewMessageNotification(threadId: String, senderName: String) {
        showNotification(
            id = threadId.hashCode(),
            title = "Nova Mensagem",
            message = "$senderName enviou uma mensagem",
            actionText = "Responder"
        )
    }
    
    fun showUpdateAvailableNotification() {
        showNotification(
            id = "update".hashCode(),
            title = "Atualização Disponível",
            message = "Uma nova versão do TaskGo está disponível",
            actionText = "Atualizar"
        )
    }
    
    fun showServiceOrderPublishedNotification(orderId: String) {
        showNotification(
            id = orderId.hashCode(),
            title = "Ordem de Serviço Publicada",
            message = "Sua ordem foi publicada e já recebeu propostas!",
            actionText = "Ver Propostas"
        )
    }
}
