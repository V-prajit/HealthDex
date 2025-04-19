package com.example.dao

import com.example.models.Appointments
import com.example.models.Doctors
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class Appointment(
    val id: Int? = null,
    val userId: String,
    val doctorId: Int,
    val doctorName: String? = null, // For easier display
    val date: String,
    val time: String,
    val duration: Int,
    val reason: String,
    val notes: String? = null,
    val status: String = "scheduled",
    val reminders: Boolean = true
)

object AppointmentDAO {
    fun addAppointment(appointment: Appointment): Appointment = transaction {
        val id = Appointments.insert {
            it[userId] = appointment.userId
            it[doctorId] = appointment.doctorId
            it[date] = appointment.date
            it[time] = appointment.time
            it[duration] = appointment.duration
            it[reason] = appointment.reason
            it[notes] = appointment.notes
            it[status] = appointment.status
            it[reminders] = appointment.reminders
        } get Appointments.id

        appointment.copy(id = id.value)
    }

    fun getAppointmentById(id: Int): Appointment? = transaction {
        (Appointments innerJoin Doctors)
            .selectAll()
            .where { Appointments.id eq id }
            .map { row ->
                Appointment(
                    id = row[Appointments.id].value,
                    userId = row[Appointments.userId],
                    doctorId = row[Appointments.doctorId].value,
                    doctorName = row[Doctors.name],
                    date = row[Appointments.date],
                    time = row[Appointments.time],
                    duration = row[Appointments.duration],
                    reason = row[Appointments.reason],
                    notes = row[Appointments.notes],
                    status = row[Appointments.status],
                    reminders = row[Appointments.reminders]
                )
            }.singleOrNull()
    }

    fun getAppointmentsByUserId(userId: String): List<Appointment> = transaction {
        (Appointments innerJoin Doctors)
            .selectAll()
            .where { Appointments.userId eq userId }
            .orderBy(Appointments.date, SortOrder.DESC)
            .orderBy(Appointments.time, SortOrder.DESC)
            .map { row ->
                Appointment(
                    id = row[Appointments.id].value,
                    userId = row[Appointments.userId],
                    doctorId = row[Appointments.doctorId].value,
                    doctorName = row[Doctors.name],
                    date = row[Appointments.date],
                    time = row[Appointments.time],
                    duration = row[Appointments.duration],
                    reason = row[Appointments.reason],
                    notes = row[Appointments.notes],
                    status = row[Appointments.status],
                    reminders = row[Appointments.reminders]
                )
            }
    }

    fun getUpcomingAppointments(userId: String): List<Appointment> = transaction {
        val today = LocalDate.now().toString()

        (Appointments innerJoin Doctors)
            .selectAll()
            .where {
                (Appointments.userId eq userId) and
                        ((Appointments.date greater today) or
                                ((Appointments.date eq today) and (Appointments.status eq "scheduled")))
            }
            .orderBy(Appointments.date, SortOrder.ASC)
            .orderBy(Appointments.time, SortOrder.ASC)
            .map { row ->
                Appointment(
                    id = row[Appointments.id].value,
                    userId = row[Appointments.userId],
                    doctorId = row[Appointments.doctorId].value,
                    doctorName = row[Doctors.name],
                    date = row[Appointments.date],
                    time = row[Appointments.time],
                    duration = row[Appointments.duration],
                    reason = row[Appointments.reason],
                    notes = row[Appointments.notes],
                    status = row[Appointments.status],
                    reminders = row[Appointments.reminders]
                )
            }
    }

    fun updateAppointment(appointment: Appointment): Boolean = transaction {
        if (appointment.id == null) return@transaction false

        Appointments.update({ Appointments.id eq appointment.id }) {
            it[doctorId] = appointment.doctorId
            it[date] = appointment.date
            it[time] = appointment.time
            it[duration] = appointment.duration
            it[reason] = appointment.reason
            it[notes] = appointment.notes
            it[status] = appointment.status
            it[reminders] = appointment.reminders
        } > 0
    }

    fun deleteAppointment(id: Int): Boolean = transaction {
        Appointments.deleteWhere { Appointments.id eq id } > 0
    }
}