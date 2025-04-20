package com.example.phms

data class Medication(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val instructions: String
)
