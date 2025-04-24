package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object Diets : Table() {
    val id             = integer("id").autoIncrement()
    val userId         = varchar("userId", 50)
    val timestamp      = varchar("timestamp", 50)   // store ISO‐string
    val mealType       = varchar("mealType", 50)
    val calories       = integer("calories")
    val description    = text("description").nullable()

    val protein        = integer("protein").nullable()
    val fats           = integer("fats").nullable()
    val carbohydrates  = integer("carbohydrates").nullable()
    val weight         = integer("weight").nullable()

    val calorieGoal    = integer("calorieGoal").nullable()
    val proteinGoal    = integer("proteinGoal").nullable()
    val fatGoal        = integer("fatGoal").nullable()
    val carbGoal       = integer("carbGoal").nullable()

    override val primaryKey = PrimaryKey(id)
}

@Serializable
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

object DietDAO {
    fun addDiet(entry: DietDTO): DietDTO = transaction {
        val insertedId = Diets.insert {
            it[userId]         = entry.userId
            it[timestamp]      = entry.timestamp
            it[mealType]       = entry.mealType
            it[calories]       = entry.calories
            it[description]    = entry.description
            it[protein]        = entry.protein
            it[fats]           = entry.fats
            it[carbohydrates]  = entry.carbohydrates
            it[weight]         = entry.weight
            it[calorieGoal]    = entry.calorieGoal
            it[proteinGoal]    = entry.proteinGoal
            it[fatGoal]        = entry.fatGoal
            it[carbGoal]       = entry.carbGoal
        } get Diets.id
        entry.copy(id = insertedId)
    }

    fun getAllDietsByUser(user: String): List<DietDTO> = transaction {
        Diets.selectAll()
            .where { Diets.userId eq user }
            .orderBy(Diets.timestamp, SortOrder.DESC)
            .map { row: ResultRow ->
                DietDTO(
                    id             = row[Diets.id],
                    userId         = row[Diets.userId],
                    timestamp      = row[Diets.timestamp],
                    mealType       = row[Diets.mealType],
                    calories       = row[Diets.calories],
                    description    = row[Diets.description],
                    protein        = row[Diets.protein],
                    fats           = row[Diets.fats],
                    carbohydrates  = row[Diets.carbohydrates],
                    weight         = row[Diets.weight],
                    calorieGoal    = row[Diets.calorieGoal],
                    proteinGoal    = row[Diets.proteinGoal],
                    fatGoal        = row[Diets.fatGoal],
                    carbGoal       = row[Diets.carbGoal]
                )
            }
    }

    fun getLatestDietByUser(user: String): DietDTO? = getAllDietsByUser(user).firstOrNull()

    fun updateDiet(entry: DietDTO): Boolean = transaction {
        entry.id?.let { id ->
            Diets.update({ Diets.id eq id }) {
                it[timestamp]      = entry.timestamp
                it[mealType]       = entry.mealType
                it[calories]       = entry.calories
                it[description]    = entry.description
                it[protein]        = entry.protein
                it[fats]           = entry.fats
                it[carbohydrates]  = entry.carbohydrates
                it[weight]         = entry.weight
                it[calorieGoal]    = entry.calorieGoal
                it[proteinGoal]    = entry.proteinGoal
                it[fatGoal]        = entry.fatGoal
                it[carbGoal]       = entry.carbGoal
            } > 0
        } ?: false
    }

    fun deleteDiet(id: Int): Boolean = transaction {
        Diets.deleteWhere { Diets.id eq id } > 0
    }
}
