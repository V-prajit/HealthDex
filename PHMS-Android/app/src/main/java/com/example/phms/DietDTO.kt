package com.example.phms

data class DietDTO(
    val id: Int? = null,
    val userId: String,
    val timestamp: String,
    val mealType: String,
    val calories: Int,
    val description: String? = null,
    val protein: Int? = null,
    val fats: Int? = null,
    val carbohydrates: Int? = null,
    val weight: Int? = null,
    val calorieGoal: Int? = null,
    val proteinGoal: Int? = null,
    val fatGoal: Int? = null,
    val carbGoal: Int? = null
)

