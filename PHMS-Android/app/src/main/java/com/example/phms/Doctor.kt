package com.example.phms

data class Doctor(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val specialization: String,
    val phone: String,
    val email: String,
    val address: String,
    val notes: String? = null,
    val notifyOnEmergency: Boolean = false
)