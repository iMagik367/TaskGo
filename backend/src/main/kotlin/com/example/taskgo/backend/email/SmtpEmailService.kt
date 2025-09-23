package com.example.taskgo.backend.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

class SmtpEmailService(
    private val smtpHost: String,
    private val smtpPort: Int,
    private val username: String,
    private val password: String,
    private val from: String
) : EmailService {
    override suspend fun sendPasswordReset(email: String, token: String) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort.toString())
            }
            val session = Session.getInstance(props, null)
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(from))
            message.setRecipients(Message.RecipientType.TO, email)
            message.subject = "Recuperação de senha - TaskGo"
            message.setText("Use o código/token abaixo para redefinir sua senha no TaskGo:\n\n$token\n\nSe não foi você, ignore este e-mail.")
            
            val transport = session.getTransport("smtp")
            transport.connect(smtpHost, smtpPort, username, password)
            transport.sendMessage(message, message.allRecipients)
            transport.close()
        }
    }
}
