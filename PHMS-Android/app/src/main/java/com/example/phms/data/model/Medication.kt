package com.example.phms.data.model

import java.time.Instant

data class Medication(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val instructions: String,
    val time: String = Instant.now().toString()
)
