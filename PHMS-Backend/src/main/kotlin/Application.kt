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
import com.example.dao.AlertDAO
import com.example.dao.VitalSignDAO
import com.example.service.MonitoringService

fun main() {
    embeddedServer(Netty, port = 8085, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    initFirebase()
    DatabaseFactory.init()
    configureSerialization()
    configureRouting()

    MonitoringService(VitalSignDAO(), AlertDAO()).start(this)
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
