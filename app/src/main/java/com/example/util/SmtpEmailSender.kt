package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

data class SmtpConfig(
    val senderEmail: String,
    val appPassword: String,
    val host: String = "smtp.gmail.com",
    val port: Int = 465,
    val useSsl: Boolean = true
)

sealed class SendResult {
    data class Success(val recipientEmail: String) : SendResult()
    data class Failure(val recipientEmail: String, val errorMessage: String) : SendResult()
    data class Skipped(val recipientEmail: String, val reason: String) : SendResult()
}

class SmtpEmailSender {

    suspend fun sendHtmlEmail(
        config: SmtpConfig,
        recipientEmail: String,
        recipientName: String,
        subjectTemplate: String,
        htmlTemplate: String
    ): SendResult = withContext(Dispatchers.IO) {
        try {
            if (config.senderEmail.isBlank() || config.appPassword.isBlank()) {
                return@withContext SendResult.Failure(
                    recipientEmail,
                    "يرجى إدخال البريد الإلكتروني المرسل وكلمة مرور التطبيق (App Password)"
                )
            }

            // Replace dynamic placeholders in subject and HTML body
            val currentDateStr = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date())
            val formattedSubject = subjectTemplate
                .replace("{email}", recipientEmail)
                .replace("{name}", if (recipientName.isNotBlank()) recipientName else recipientEmail.substringBefore("@"))
                .replace("{date}", currentDateStr)

            val formattedHtml = htmlTemplate
                .replace("{email}", recipientEmail)
                .replace("{name}", if (recipientName.isNotBlank()) recipientName else recipientEmail.substringBefore("@"))
                .replace("{date}", currentDateStr)

            val props = Properties().apply {
                put("mail.smtp.host", config.host.ifBlank { "smtp.gmail.com" })
                put("mail.smtp.port", config.port.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.timeout", "15000")
                put("mail.smtp.connectiontimeout", "15000")

                if (config.useSsl || config.port == 465) {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.socketFactory.port", config.port.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    // Remove any spaces from Gmail App Password if pasted with spaces
                    val cleanPassword = config.appPassword.replace(" ", "")
                    return PasswordAuthentication(config.senderEmail.trim(), cleanPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.senderEmail.trim()))
                setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail.trim()))
                setSubject(formattedSubject, "UTF-8")
                setContent(formattedHtml, "text/html; charset=utf-8")
                sentDate = java.util.Date()
            }

            Transport.send(message)
            Log.i("SmtpEmailSender", "Email sent successfully to $recipientEmail")
            SendResult.Success(recipientEmail)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "فشل غير معروف في الاتصال وخادم SMTP"
            Log.e("SmtpEmailSender", "Failed sending email to $recipientEmail: $errorMsg", e)
            SendResult.Failure(recipientEmail, errorMsg)
        }
    }

    suspend fun testSmtpConnection(config: SmtpConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.host", config.host.ifBlank { "smtp.gmail.com" })
                put("mail.smtp.port", config.port.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.timeout", "10000")
                put("mail.smtp.connectiontimeout", "10000")

                if (config.useSsl || config.port == 465) {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.socketFactory.port", config.port.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.senderEmail.trim(), config.appPassword.replace(" ", ""))
                }
            })

            val transport = session.getTransport("smtp")
            transport.connect(config.host, config.senderEmail.trim(), config.appPassword.replace(" ", ""))
            transport.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
