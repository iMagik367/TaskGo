package com.example.taskgo.backend.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false))
                subject = "Recuperação de senha - TaskGo"
                setText("Use o código/token abaixo para redefinir sua senha no TaskGo:\n\n$token\n\nSe não foi você, ignore este e-mail.")
            }
            Transport.send(message, username, password)
        }
    }
}
