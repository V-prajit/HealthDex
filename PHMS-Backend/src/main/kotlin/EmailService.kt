package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

object EmailService {
    private val props = Properties().apply {
        put("mail.smtp.host", "smtp.gmail.com")
        put("mail.smtp.port", "587")
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.ssl.protocols", "TLSv1.2")
    }

    private val username = System.getProperty("GMAIL_EMAIL", "")
    private val password = System.getProperty("GMAIL_APP_PASSWORD", "")

    suspend fun sendVitalAlertEmail(
        recipientEmail: String,
        recipientName: String,
        patientName: String,
        vitalName: String,
        value: Float,
        threshold: Float,
        isHigh: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            val direction = if (isHigh) "above" else "below"
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username, "PHMS Alert System"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                subject = "URGENT: Vital Sign Alert for $patientName"
                setContent("""
                    <html>
                        <body style="font-family: Arial, sans-serif;">
                            <h2 style="color: #d32f2f;">Vital Sign Alert</h2>
                            <p>Dear $recipientName,</p>
                            <p>This is an automated alert regarding $patientName's vital signs.</p>
                            <div style="background-color: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="color: #d32f2f; font-weight: bold;">ALERT: $vitalName is $direction normal threshold</p>
                                <ul>
                                    <li>Current Value: $value</li>
                                    <li>Threshold: $threshold</li>
                                </ul>
                            </div>
                            <p>Please check on the patient as soon as possible.</p>
                            <p><em>This is an automated message from the Personal Health Management System.</em></p>
                        </body>
                    </html>
                """.trimIndent(), "text/html; charset=utf-8")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            println("Error sending email: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}