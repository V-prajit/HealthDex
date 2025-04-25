package com.example.phms.data.model

data class Appointment(
    val id: Int? = null,
    val userId: String,
    val doctorId: Int,
    val doctorName: String? = null,
    val date: String,
    val time: String,
    val duration: Int,
    val reason: String,
    val notes: String? = null,
    val status: String = "scheduled",
    val reminders: Boolean = true
)