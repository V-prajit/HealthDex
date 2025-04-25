package com.example.phms.data.model

data class VitalSign(
  val id: Int?      = null,
  val userId: String,
  val type: String,
  val value: Double,
  val unit: String,
  val timestamp: String
)
