package com.example.phms

data class VitalSign(
  val id: Int?      = null,
  val userId: String,
  val type: String,
  val value: Double? = null,
  val unit: String,
  val timestamp: String,
  val manualSystolic: Double? = null,
  val manualDiastolic: Double? = null
)
