// model/nutrition/FoodModels.kt
package com.example.phms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class SearchResponse(val foods: List<FoodHit> = emptyList())

@JsonClass(generateAdapter = true)
data class FoodHit(val fdcId: Long, val description: String)

/** Handles *both* nutrient shapes.  */
@JsonClass(generateAdapter = true)
data class Nutrient(
    // flat variant fields ---------------
    @Json(name = "nutrientName")
    val flatName: String? = null,
    val unitName: String? = null,
    val value: Double? = null,

    // nested variant fields -------------
    val amount: Double? = null,
    val nutrient: NestedMeta? = null
) {
    /** Unified accessor for the name. */
    val name: String?
        get() = flatName ?: nutrient?.name

    /** Unified accessor for the numeric value (kcal / g). */
    val quantity: Double
        get() = value ?: amount ?: 0.0
}

@JsonClass(generateAdapter = true)
data class NestedMeta(
    val name: String?,
    val unitName: String?
)

@JsonClass(generateAdapter = true)
data class FoodItem(
    val description: String,
    val foodNutrients: List<Nutrient>
)

data class NutritionInfo(
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

fun FoodItem.toNutritionInfo(): NutritionInfo {
    fun grab(key: String) =
        foodNutrients.firstOrNull { it.name.equals(key, ignoreCase = true) }
            ?.quantity ?: 0.0

    return NutritionInfo(
        calories = grab("Energy").roundToInt(),
        protein  = grab("Protein").roundToInt(),
        fat      = grab("Total lipid (fat)").roundToInt(),
        carbs    = grab("Carbohydrate, by difference").roundToInt()
    )
}
