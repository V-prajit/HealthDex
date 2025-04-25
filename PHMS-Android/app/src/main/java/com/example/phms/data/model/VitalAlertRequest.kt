package com.example.phms.data.model

data class VitalAlertRequest(
    val userId: String,
    val vitalName: String,
    val value: Float,
    val threshold: Float,
    val isHigh: Boolean
)