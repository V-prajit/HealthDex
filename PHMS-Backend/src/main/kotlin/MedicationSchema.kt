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

object Medications : Table() {
    val id           = integer("id").autoIncrement()
    val userId       = varchar("userId", 50)
    val name         = varchar("name", 100)
    val dosage       = varchar("dosage", 50)
    val time         = varchar("time", 50)         // store ISO‑string
    val taken        = bool("taken").default(false)

    // New fields to match what the client sends
    val category     = varchar("category", 100).default("")
    val frequency    = varchar("frequency", 50).default("")
    val instructions = text("instructions").nullable()

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class MedicationDTO(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val dosage: String,
    val time: String,
    val taken: Boolean = false,
    val category: String = "",
    val frequency: String = "",
    val instructions: String? = null
)

object MedicationDAO {
    fun addMedication(entry: MedicationDTO): MedicationDTO = transaction {
        val insertedId = Medications.insert {
            it[userId]       = entry.userId
            it[name]         = entry.name
            it[dosage]       = entry.dosage
            it[time]         = entry.time
            it[taken]        = entry.taken
            it[category]     = entry.category
            it[frequency]    = entry.frequency
            it[instructions] = entry.instructions
        } get Medications.id

        entry.copy(id = insertedId)
    }

    fun getAllMedicationsByUser(user: String): List<MedicationDTO> = transaction {
        Medications
          .selectAll()
          .where { Medications.userId eq user }
          .orderBy(Medications.time, SortOrder.DESC)
          .map { row: ResultRow ->
              MedicationDTO(
                  id           = row[Medications.id],
                  userId       = row[Medications.userId],
                  name         = row[Medications.name],
                  dosage       = row[Medications.dosage],
                  time         = row[Medications.time],
                  taken        = row[Medications.taken],
                  category     = row[Medications.category],
                  frequency    = row[Medications.frequency],
                  instructions = row[Medications.instructions]
              )
          }
    }

    fun getLatestMedicationByUser(user: String): MedicationDTO? =
        getAllMedicationsByUser(user).firstOrNull()

    fun updateMedication(entry: MedicationDTO): Boolean = transaction {
        entry.id?.let { id ->
            Medications.update({ Medications.id eq id }) {
                it[name]         = entry.name
                it[dosage]       = entry.dosage
                it[time]         = entry.time
                it[taken]        = entry.taken
                it[category]     = entry.category
                it[frequency]    = entry.frequency
                it[instructions] = entry.instructions
            } > 0
        } ?: false
    }

    fun deleteMedication(id: Int): Boolean = transaction {
        Medications.deleteWhere { Medications.id eq id } > 0
    }
}
