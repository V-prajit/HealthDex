package com.example

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream
import io.github.cdimascio.dotenv.dotenv


fun main() {
    val dotenv = dotenv {
        directory = "./"
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    System.setProperty("GMAIL_EMAIL", dotenv["GMAIL_EMAIL"] ?: "")
    System.setProperty("GMAIL_APP_PASSWORD", dotenv["GMAIL_APP_PASSWORD"] ?: "")

    embeddedServer(Netty, port = 8085, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
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
