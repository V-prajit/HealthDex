package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder

object Diets : Table() {
    val id          = integer("id").autoIncrement()
    val userId      = varchar("userId", 50)
    val timestamp   = varchar("timestamp", 50)   // store ISO‐string
    val mealType    = varchar("mealType", 50)
    val calories    = integer("calories")
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class DietDTO(
    val id: Int? = null,
    val userId: String,
    val timestamp: String,
    val mealType: String,
    val calories: Int,
    val description: String? = null
)

object DietDAO {
    fun addDiet(entry: DietDTO): DietDTO = transaction {
        val insertedId = Diets.insert {
            it[userId]      = entry.userId
            it[timestamp]   = entry.timestamp
            it[mealType]    = entry.mealType
            it[calories]    = entry.calories
            it[description] = entry.description
        } get Diets.id
        entry.copy(id = insertedId)
    }

    fun getAllDietsByUser(user: String): List<DietDTO> = transaction {
        Diets.selectAll()
             .where { Diets.userId eq user }
             .orderBy(Diets.timestamp, SortOrder.DESC)
             .map { row: ResultRow ->
                DietDTO(
                  id          = row[Diets.id],
                  userId      = row[Diets.userId],
                  timestamp   = row[Diets.timestamp],
                  mealType    = row[Diets.mealType],
                  calories    = row[Diets.calories],
                  description = row[Diets.description]
                )
             }
    }

    fun getLatestDietByUser(user: String): DietDTO? = getAllDietsByUser(user).firstOrNull()

    fun updateDiet(entry: DietDTO): Boolean = transaction {
        entry.id?.let { id ->
            Diets.update({ Diets.id eq id }) {
                it[timestamp]   = entry.timestamp
                it[mealType]    = entry.mealType
                it[calories]    = entry.calories
                it[description] = entry.description
            } > 0
        } ?: false
    }

    fun deleteDiet(id: Int): Boolean = transaction {
        Diets.deleteWhere { Diets.id eq id } > 0
    }
}
