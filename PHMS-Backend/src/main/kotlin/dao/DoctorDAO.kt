package com.example.dao

import com.example.models.Doctors
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


@Serializable
data class Doctor(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val specialization: String,
    val phone: String,
    val email: String,
    val address: String,
    val notes: String? = null,
    val notifyOnEmergency: Boolean = false
)

object DoctorDAO {
    fun addDoctor(doctor: Doctor): Doctor = transaction {
        val id = Doctors.insert {
            it[userId] = doctor.userId
            it[name] = doctor.name
            it[specialization] = doctor.specialization
            it[phone] = doctor.phone
            it[email] = doctor.email
            it[address] = doctor.address
            it[notes] = doctor.notes
            it[notifyOnEmergency] = doctor.notifyOnEmergency
        } get Doctors.id

        doctor.copy(id = id.value)
    }

    fun getDoctorById(id: Int): Doctor? = transaction {
        Doctors.selectAll()
            .where { Doctors.id eq id }
            .map { row ->
                Doctor(
                    id = row[Doctors.id].value,
                    userId = row[Doctors.userId],
                    name = row[Doctors.name],
                    specialization = row[Doctors.specialization],
                    phone = row[Doctors.phone],
                    email = row[Doctors.email],
                    address = row[Doctors.address],
                    notes = row[Doctors.notes],
                    notifyOnEmergency = row[Doctors.notifyOnEmergency]
                )
            }.singleOrNull()
    }

    fun getDoctorsByUserId(userId: String): List<Doctor> = transaction {
        Doctors.selectAll()
            .where { Doctors.userId eq userId }
            .map { row ->
                Doctor(
                    id = row[Doctors.id].value,
                    userId = row[Doctors.userId],
                    name = row[Doctors.name],
                    specialization = row[Doctors.specialization],
                    phone = row[Doctors.phone],
                    email = row[Doctors.email],
                    address = row[Doctors.address],
                    notes = row[Doctors.notes],
                    notifyOnEmergency = row[Doctors.notifyOnEmergency]
                )
            }
    }

    fun updateDoctor(doctor: Doctor): Boolean = transaction {
        if (doctor.id == null) return@transaction false

        Doctors.update({ Doctors.id eq doctor.id }) {
            it[name] = doctor.name
            it[specialization] = doctor.specialization
            it[phone] = doctor.phone
            it[email] = doctor.email
            it[address] = doctor.address
            it[notes] = doctor.notes
            it[notifyOnEmergency] = doctor.notifyOnEmergency
        } > 0
    }

    fun deleteDoctor(id: Int): Boolean = transaction {
        Doctors.deleteWhere { Doctors.id eq id } > 0
    }
}