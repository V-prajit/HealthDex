package com.example.phms

data class DietGoalDTO(
    val id: Int? = null,
    val userId: String,
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 75,
    val fatGoal: Int = 65,
    val carbGoal: Int = 300
)