package com.example.phms

data class EmergencyContact(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val relationship: String,
    val notifyOnEmergency: Boolean = true
)