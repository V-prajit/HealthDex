package com.example

import io.ktor.server.engine.embeddedServer
import com.example.Medications
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory

fun main() {
    // You can keep the logger instance here if you like, but it won't log much
    val logger = LoggerFactory.getLogger("main")
    logger.info("Attempting to start Ktor server...") // This likely still won't show

    embeddedServer(Netty, port = 8085, host = "0.0.0.0") {
        module() // Call the module where setup now happens
    }.start(wait = true)
}

fun Application.module() {
    val log = environment.log
    log.info("Inside Application.module()")

    try {
        log.info("Loading .env configuration...")
        val dotenv = dotenv {
            directory = "./"
            ignoreIfMalformed = true // Or false if you want errors
            ignoreIfMissing = false // Make sure it crashes if missing
        }

        val emailFromEnv = dotenv["GMAIL_EMAIL"]
        val appPassword = dotenv["GMAIL_APP_PASSWORD"]

        log.info("GMAIL_EMAIL read from .env: {}", emailFromEnv) // Use Ktor logger

        if (emailFromEnv == null || appPassword == null) {
            log.error("Required environment variables GMAIL_EMAIL or GMAIL_APP_PASSWORD not found in .env file!")
            // Decide if you want to stop the application here, maybe throw an exception
            throw IllegalStateException("Missing required environment variables in .env")
        }

        // Set system properties IF truly needed, otherwise just use the variables
        System.setProperty("GMAIL_EMAIL", emailFromEnv)
        System.setProperty("GMAIL_APP_PASSWORD", appPassword)
        log.info(".env variables loaded and system properties set.")

    } catch (e: Exception) {
        log.error("Failed to load configuration from .env file!", e)
        // Stop the application from starting if config fails
        throw IllegalStateException("Failed to initialize environment configuration", e)
    }

    install(ContentNegotiation){
        json()
    }

    initFirebase()
    DatabaseFactory.init()
    configureSerialization()
    configureRouting()
}

fun initFirebase() {
    val serviceAccount = FileInputStream("src/main/resources/serviceAccountKey.json")
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options)
    }
}
