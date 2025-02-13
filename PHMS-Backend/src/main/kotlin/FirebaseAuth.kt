package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken

fun verifyFirebaseToken(token: String): FirebaseToken? {
    return try {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
        println("User authenticated: ${decodedToken.uid}")
        decodedToken
    } catch (e: Exception){
        println("Invalid Firebase token: ${e.message}")
        null
    }
}