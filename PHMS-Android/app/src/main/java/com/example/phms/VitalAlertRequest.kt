package com.example.phms

data class VitalAlertRequest(
    val userId: String,
    val vitalName: String,
    val value: Float,
    val threshold: Float,
    val isHigh: Boolean
)