package com.example.phms.data.model

data class ParsedVitalData(
    val timestampMs: Long,
    val heartRate: Float?,
    val glucose: Float?,
    val bpSystolic: Float?,
    val bpDiastolic: Float?,
    val cholesterol: Float?
)