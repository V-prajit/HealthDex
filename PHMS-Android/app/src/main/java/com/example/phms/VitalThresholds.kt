package com.example.phms

object VitalThresholds {
    const val PREFS_NAME = "vital_thresholds_prefs"

    const val KEY_HR_HIGH = "hr_high"
    const val KEY_HR_LOW = "hr_low"
    const val KEY_BP_SYS_HIGH = "bp_sys_high"
    const val KEY_BP_SYS_LOW = "bp_sys_low"
    const val KEY_BP_DIA_HIGH = "bp_dia_high"
    const val KEY_BP_DIA_LOW = "bp_dia_low"
    const val KEY_GLUCOSE_HIGH = "glucose_high"
    const val KEY_GLUCOSE_LOW = "glucose_low"
    const val KEY_CHOLESTEROL_HIGH = "cholesterol_high"
    const val KEY_CHOLESTEROL_LOW = "cholesterol_low"

    // --- Updated Default values ---
    const val DEFAULT_HR_HIGH = 100f
    const val DEFAULT_HR_LOW = 60f
    const val DEFAULT_BP_SYS_HIGH = 140f
    const val DEFAULT_BP_SYS_LOW = 95f
    const val DEFAULT_BP_DIA_HIGH = 85f // <-- Changed from 90f to prevent overlap
    const val DEFAULT_BP_DIA_LOW = 60f
    const val DEFAULT_GLUCOSE_HIGH = 140f
    const val DEFAULT_GLUCOSE_LOW = 70f
    const val DEFAULT_CHOLESTEROL_HIGH = 200f
    const val DEFAULT_CHOLESTEROL_LOW = 100f // Example low threshold
}

// Data class remains the same
data class ThresholdValues(
    val hrHigh: Float,
    val hrLow: Float,
    val bpSysHigh: Float,
    val bpSysLow: Float,
    val bpDiaHigh: Float,
    val bpDiaLow: Float,
    val glucoseHigh: Float,
    val glucoseLow: Float,
    val cholesterolHigh: Float,
    val cholesterolLow: Float
)