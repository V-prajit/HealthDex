package com.example.phms

data class DietDTO(
    val id: Int? = null,
    val userId: String,
    val timestamp: String,     // ISO8601 string
    val mealType: String,
    val calories: Int,
    val description: String? = null
)
