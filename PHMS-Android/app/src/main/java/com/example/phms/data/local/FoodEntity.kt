package com.example.phms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phms.model.nutrition.NutritionInfo

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val name: String,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
) {
    fun toDomain() = NutritionInfo(calories, protein, fat, carbs)
}
