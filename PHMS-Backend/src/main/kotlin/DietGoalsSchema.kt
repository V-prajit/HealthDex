package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object DietGoals : Table() {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 255)
    val calorieGoal = integer("calorie_goal").default(2000)
    val proteinGoal = integer("protein_goal").default(75)
    val fatGoal = integer("fat_goal").default(65)
    val carbGoal = integer("carb_goal").default(300)

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class DietGoalDTO(
    val id: Int? = null,
    val userId: String,
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 75,
    val fatGoal: Int = 65,
    val carbGoal: Int = 300
)

object DietGoalDAO {
    fun getGoalsByUserId(userId: String): DietGoalDTO? = transaction {
        DietGoals.selectAll()
            .where { DietGoals.userId eq userId }
            .map { row ->
                DietGoalDTO(
                    id = row[DietGoals.id],
                    userId = row[DietGoals.userId],
                    calorieGoal = row[DietGoals.calorieGoal],
                    proteinGoal = row[DietGoals.proteinGoal],
                    fatGoal = row[DietGoals.fatGoal],
                    carbGoal = row[DietGoals.carbGoal]
                )
            }
            .firstOrNull()
    }

    fun setGoals(goals: DietGoalDTO): DietGoalDTO = transaction {
        val existingGoals = DietGoals.selectAll()
            .where { DietGoals.userId eq goals.userId }
            .firstOrNull()

        if (existingGoals != null) {
            DietGoals.update({ DietGoals.userId eq goals.userId }) {
                it[calorieGoal] = goals.calorieGoal
                it[proteinGoal] = goals.proteinGoal
                it[fatGoal] = goals.fatGoal
                it[carbGoal] = goals.carbGoal
            }

            goals.copy(id = existingGoals[DietGoals.id])
        } else {
            val id = DietGoals.insert {
                it[userId] = goals.userId
                it[calorieGoal] = goals.calorieGoal
                it[proteinGoal] = goals.proteinGoal
                it[fatGoal] = goals.fatGoal
                it[carbGoal] = goals.carbGoal
            } get DietGoals.id

            goals.copy(id = id)
        }
    }
}