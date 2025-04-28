package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq      // for the `eq` infix
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.ResultRow

// 1) Table definition
object Vitals : Table() {
    val id        = integer("id").autoIncrement()
    val userId    = varchar("user_id", 255)
    val type      = varchar("type", 100)
    val value         = double("value").nullable()
    val manualSystolic = double("manual_systolic").nullable()
    val manualDiastolic = double("manual_diastolic").nullable()
    val unit      = varchar("unit", 50)
    val timestamp = varchar("timestamp", 50)
    override val primaryKey = PrimaryKey(id)
}

// 2) DTO
@Serializable
data class VitalDTO(
    val id: Int?    = null,
    val userId: String,
    val type: String,
    val value: Double?,
    val unit: String,
    val timestamp: String,
    val manualSystolic: Double? = null,
    val manualDiastolic: Double? = null
)

object VitalsDAO {
    // exactly like NotesDAO.getNotesForUser:
    fun getAllByUser(userId: String): List<VitalDTO> = transaction {
        Vitals
          .selectAll()                              // non-deprecated
          .where { Vitals.userId eq userId }        // mirrors NotesSchema
          .orderBy(Vitals.id, SortOrder.DESC)
          .map { row: ResultRow ->
            VitalDTO(
              id                = row[Vitals.id],
              userId            = row[Vitals.userId],
              type              = row[Vitals.type],
              value             = row[Vitals.value],
              unit              = row[Vitals.unit],
              timestamp         = row[Vitals.timestamp],
              manualSystolic    = row[Vitals.manualSystolic],
              manualDiastolic   = row[Vitals.manualDiastolic]
            )
          }
    }

    // pick latest in Kotlin
    fun getLatest(userId: String, type: String): VitalDTO? {
        return getAllByUser(userId).firstOrNull { it.type == type }
    }

    fun add(v: VitalDTO): VitalDTO = transaction {
        val newId = Vitals.insert {
            it[userId]    = v.userId
            it[type]      = v.type
            it[value]     = v.value
            it[unit]      = v.unit
            it[timestamp] = v.timestamp
            it[manualSystolic]  = v.manualSystolic
            it[manualDiastolic] = v.manualDiastolic
        } get Vitals.id
        v.copy(id = newId)
    }

    fun update(v: VitalDTO): Boolean = transaction {
        if (v.id == null) return@transaction false
        Vitals.update({ Vitals.id eq v.id }) {
            it[type]      = v.type
            it[value]     = v.value
            it[unit]      = v.unit
            it[timestamp] = v.timestamp
            it[manualSystolic]  = v.manualSystolic
            it[manualDiastolic] = v.manualDiastolic
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        Vitals.deleteWhere { Vitals.id eq id } > 0
    }
}
