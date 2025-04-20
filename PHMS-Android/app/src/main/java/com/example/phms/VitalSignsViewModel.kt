package com.example.phms

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

class VitalSignsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = appContext.getSharedPreferences(VitalThresholds.PREFS_NAME, Context.MODE_PRIVATE)

    private val _thresholds = MutableStateFlow(loadThresholds())
    val thresholds: StateFlow<ThresholdValues> = _thresholds.asStateFlow()

    private val _vitalHistory = MutableStateFlow<List<ParsedVitalData>>(emptyList())
    val vitalHistory: StateFlow<List<ParsedVitalData>> = _vitalHistory.asStateFlow()

    private var generatorJob: Job? = null
    private val GENERATION_INTERVAL_MS = 3500L
    private val MAX_HISTORY_SIZE = 75
    private val INITIAL_HISTORY_POINTS = 15
    private var lastGeneratedData: ParsedVitalData? = null
    private val TAG = "VitalSignsViewModel"

    // --- Target HR Range ---
    private val NORMAL_HR_MIN = 70f
    private val NORMAL_HR_MAX = 90f
    private val STAY_NORMAL_HR_PROB = 0.95f

    // --- Target Glucose Range ---
    private val NORMAL_GLUCOSE_MIN = 85f
    private val NORMAL_GLUCOSE_MAX = 125f
    private val STAY_NORMAL_GLUCOSE_PROB = 0.99f // Higher probability for Glucose

    // Define safe margins inside absolute default thresholds
    private val ABS_HR_MIN = VitalThresholds.DEFAULT_HR_LOW + 2f
    private val ABS_HR_MAX = VitalThresholds.DEFAULT_HR_HIGH - 2f
    private val GLUCOSE_SAFE_MARGIN = 5f
    private val ABS_GLUCOSE_MIN = VitalThresholds.DEFAULT_GLUCOSE_LOW + GLUCOSE_SAFE_MARGIN // 75f
    private val ABS_GLUCOSE_MAX = VitalThresholds.DEFAULT_GLUCOSE_HIGH - GLUCOSE_SAFE_MARGIN // 135f
    // Other margins
    private val BP_SYS_SAFE_MARGIN = 5f
    private val BP_DIA_SAFE_MARGIN = 3f
    private val CHOL_SAFE_MARGIN = 2f


    init {
        val initialHistory = generateInitialHistory(INITIAL_HISTORY_POINTS)
        _vitalHistory.value = initialHistory
        lastGeneratedData = initialHistory.lastOrNull()
        startGeneratingVitals()
    }

    private fun loadThresholds(): ThresholdValues {
        // No changes here
        return ThresholdValues(
            hrHigh = prefs.getFloat(VitalThresholds.KEY_HR_HIGH, VitalThresholds.DEFAULT_HR_HIGH),
            hrLow = prefs.getFloat(VitalThresholds.KEY_HR_LOW, VitalThresholds.DEFAULT_HR_LOW),
            bpSysHigh = prefs.getFloat(VitalThresholds.KEY_BP_SYS_HIGH, VitalThresholds.DEFAULT_BP_SYS_HIGH),
            bpSysLow = prefs.getFloat(VitalThresholds.KEY_BP_SYS_LOW, VitalThresholds.DEFAULT_BP_SYS_LOW),
            bpDiaHigh = prefs.getFloat(VitalThresholds.KEY_BP_DIA_HIGH, VitalThresholds.DEFAULT_BP_DIA_HIGH),
            bpDiaLow = prefs.getFloat(VitalThresholds.KEY_BP_DIA_LOW, VitalThresholds.DEFAULT_BP_DIA_LOW),
            glucoseHigh = prefs.getFloat(VitalThresholds.KEY_GLUCOSE_HIGH, VitalThresholds.DEFAULT_GLUCOSE_HIGH),
            glucoseLow = prefs.getFloat(VitalThresholds.KEY_GLUCOSE_LOW, VitalThresholds.DEFAULT_GLUCOSE_LOW),
            cholesterolHigh = prefs.getFloat(VitalThresholds.KEY_CHOLESTEROL_HIGH, VitalThresholds.DEFAULT_CHOLESTEROL_HIGH),
            cholesterolLow = prefs.getFloat(VitalThresholds.KEY_CHOLESTEROL_LOW, VitalThresholds.DEFAULT_CHOLESTEROL_LOW)
        )
    }

    fun saveThresholds(newThresholds: ThresholdValues) {
        // No changes here
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().apply {
                putFloat(VitalThresholds.KEY_HR_HIGH, newThresholds.hrHigh)
                putFloat(VitalThresholds.KEY_HR_LOW, newThresholds.hrLow)
                putFloat(VitalThresholds.KEY_BP_SYS_HIGH, newThresholds.bpSysHigh)
                putFloat(VitalThresholds.KEY_BP_SYS_LOW, newThresholds.bpSysLow)
                putFloat(VitalThresholds.KEY_BP_DIA_HIGH, newThresholds.bpDiaHigh)
                putFloat(VitalThresholds.KEY_BP_DIA_LOW, newThresholds.bpDiaLow)
                putFloat(VitalThresholds.KEY_GLUCOSE_HIGH, newThresholds.glucoseHigh)
                putFloat(VitalThresholds.KEY_GLUCOSE_LOW, newThresholds.glucoseLow)
                putFloat(VitalThresholds.KEY_CHOLESTEROL_HIGH, newThresholds.cholesterolHigh)
                putFloat(VitalThresholds.KEY_CHOLESTEROL_LOW, newThresholds.cholesterolLow)
                apply()
            }
            _thresholds.value = newThresholds
        }
    }

     private fun generateInitialHistory(count: Int): List<ParsedVitalData> {
         val history = mutableListOf<ParsedVitalData>()
         var currentTimestamp = System.currentTimeMillis() - (count * GENERATION_INTERVAL_MS)
         var lastDataPoint: ParsedVitalData? = null

         // Start HR in normal target range, Glucose also in its target range
         val startHr = (NORMAL_HR_MIN + NORMAL_HR_MAX) / 2f
         val startGlucose = (NORMAL_GLUCOSE_MIN + NORMAL_GLUCOSE_MAX) / 2f // Start Glucose around 105
         // Others start in middle of defaults
         val startSys = (VitalThresholds.DEFAULT_BP_SYS_LOW + VitalThresholds.DEFAULT_BP_SYS_HIGH) / 2f
         val startDia = (VitalThresholds.DEFAULT_BP_DIA_LOW + VitalThresholds.DEFAULT_BP_DIA_HIGH) / 2f
         val startChol = (VitalThresholds.DEFAULT_CHOLESTEROL_LOW + VitalThresholds.DEFAULT_CHOLESTEROL_HIGH) / 2f

          lastDataPoint = ParsedVitalData(currentTimestamp, startHr, startGlucose, startSys, startDia, startChol)
          history.add(lastDataPoint)
          currentTimestamp += GENERATION_INTERVAL_MS

         for (i in 1 until count) {
             val newData = generateNextDataPoint(lastDataPoint, currentTimestamp)
             history.add(newData)
             lastDataPoint = newData
             currentTimestamp += GENERATION_INTERVAL_MS
         }
         return history
     }


    private fun startGeneratingVitals() {
        generatorJob?.cancel()

        generatorJob = viewModelScope.launch(Dispatchers.Default) {
             delay(GENERATION_INTERVAL_MS / 2)
            while (isActive) {
                val newData = generateNextDataPoint(lastGeneratedData, System.currentTimeMillis())
                lastGeneratedData = newData

                launch(Dispatchers.Main) {
                     _vitalHistory.update { currentHistory ->
                         (currentHistory + newData).takeLast(MAX_HISTORY_SIZE)
                     }
                     checkThresholds(newData, _thresholds.value)
                }

                delay(GENERATION_INTERVAL_MS)
            }
        }
    }


    private fun generateNextDataPoint(previousData: ParsedVitalData?, timestamp: Long): ParsedVitalData {
         // Base values
         val baseHr = previousData?.heartRate ?: ((NORMAL_HR_MIN + NORMAL_HR_MAX) / 2f)
         val baseGlucose = previousData?.glucose ?: ((NORMAL_GLUCOSE_MIN + NORMAL_GLUCOSE_MAX) / 2f) // Default glucose base
         val baseSys = previousData?.bpSystolic ?: ((VitalThresholds.DEFAULT_BP_SYS_LOW + VitalThresholds.DEFAULT_BP_SYS_HIGH) / 2f)
         val baseDia = previousData?.bpDiastolic ?: ((VitalThresholds.DEFAULT_BP_DIA_LOW + VitalThresholds.DEFAULT_BP_DIA_HIGH) / 2f)
         val baseChol = previousData?.cholesterol ?: ((VitalThresholds.DEFAULT_CHOLESTEROL_LOW + VitalThresholds.DEFAULT_CHOLESTEROL_HIGH) / 2f)

        // --- Deltas ---
        val hrDelta = Random.nextInt(-2, 3).toFloat()
        val glucoseDelta = Random.nextInt(-3, 4).toFloat() // Small glucose steps
        val sysDelta = Random.nextInt(-3, 4).toFloat()
        val diaDelta = Random.nextInt(-2, 3).toFloat()
        val cholDelta = Random.nextFloat() * 1.5f - 0.75f

        var nextHr = baseHr + hrDelta
        var nextGlucose = baseGlucose + glucoseDelta // Calculate potential next glucose
        val nextSys = baseSys + sysDelta
        val nextDia = baseDia + diaDelta
        val nextChol = baseChol + cholDelta

        // --- HR Specific Logic (Bias towards 70-90) ---
        val previousHrWasNormal = baseHr in NORMAL_HR_MIN..NORMAL_HR_MAX
        if (previousHrWasNormal) {
            if (Random.nextFloat() < STAY_NORMAL_HR_PROB) {
                 nextHr = nextHr.coerceIn(NORMAL_HR_MIN, NORMAL_HR_MAX)
            } else {
                 nextHr = nextHr.coerceIn(ABS_HR_MIN, ABS_HR_MAX)
            }
        } else {
             val target = if (baseHr < NORMAL_HR_MIN) NORMAL_HR_MIN else NORMAL_HR_MAX
             val pullDelta = (target - baseHr) * 0.2f
             nextHr += pullDelta
             nextHr = nextHr.coerceIn(ABS_HR_MIN, ABS_HR_MAX)
        }

        // --- Glucose Specific Logic (Bias towards 85-125) ---
         val previousGlucoseWasNormal = baseGlucose in NORMAL_GLUCOSE_MIN..NORMAL_GLUCOSE_MAX
         if (previousGlucoseWasNormal) {
             // 99% chance to force clamp within the normal 85-125 range
             if (Random.nextFloat() < STAY_NORMAL_GLUCOSE_PROB) {
                 nextGlucose = nextGlucose.coerceIn(NORMAL_GLUCOSE_MIN, NORMAL_GLUCOSE_MAX)
             } else {
                 // 1% chance: Allow movement outside 85-125, but clamp to absolute limits (75-135)
                 nextGlucose = nextGlucose.coerceIn(ABS_GLUCOSE_MIN, ABS_GLUCOSE_MAX)
             }
         } else {
             // If previous Glucose was outside 85-125, encourage return
             val targetGlucose = if (baseGlucose < NORMAL_GLUCOSE_MIN) NORMAL_GLUCOSE_MIN else NORMAL_GLUCOSE_MAX
             val pullDeltaGlucose = (targetGlucose - baseGlucose) * 0.25f // Pull back 25%
             nextGlucose += pullDeltaGlucose

             // Still clamp within absolute limits
             nextGlucose = nextGlucose.coerceIn(ABS_GLUCOSE_MIN, ABS_GLUCOSE_MAX)
         }

        // --- Clamp other values (BP, Cholesterol) ---
         val clampedSys = nextSys.coerceIn(
            VitalThresholds.DEFAULT_BP_SYS_LOW + BP_SYS_SAFE_MARGIN,
            VitalThresholds.DEFAULT_BP_SYS_HIGH - BP_SYS_SAFE_MARGIN
         )
         val clampedDia = nextDia.coerceIn(
            VitalThresholds.DEFAULT_BP_DIA_LOW + BP_DIA_SAFE_MARGIN,
            VitalThresholds.DEFAULT_BP_DIA_HIGH - BP_DIA_SAFE_MARGIN
         ).coerceAtMost(clampedSys - 15f)

        val clampedChol = nextChol.coerceIn(
             VitalThresholds.DEFAULT_CHOLESTEROL_LOW + CHOL_SAFE_MARGIN,
             VitalThresholds.DEFAULT_CHOLESTEROL_HIGH - CHOL_SAFE_MARGIN
         )

        // Use the calculated nextHr & nextGlucose (already clamped appropriately)
        val clampedHr = nextHr
        val clampedGlucose = nextGlucose


        return ParsedVitalData(
            timestampMs = timestamp,
            heartRate = clampedHr,
            glucose = clampedGlucose,
            bpSystolic = clampedSys,
            bpDiastolic = clampedDia,
            cholesterol = clampedChol
        )
    }


    private fun checkThresholds(data: ParsedVitalData, currentThresholds: ThresholdValues) {
        // No changes here
        data.heartRate?.let { hr ->
            if (hr > currentThresholds.hrHigh) logAlert("Heart Rate High", hr, currentThresholds.hrHigh)
            if (hr < currentThresholds.hrLow) logAlert("Heart Rate Low", hr, currentThresholds.hrLow)
        }
        data.bpSystolic?.let { sys ->
             if (sys > currentThresholds.bpSysHigh) logAlert("Systolic BP High", sys, currentThresholds.bpSysHigh)
             if (sys < currentThresholds.bpSysLow) logAlert("Systolic BP Low", sys, currentThresholds.bpSysLow)
        }
         data.bpDiastolic?.let { dia ->
             if (dia > currentThresholds.bpDiaHigh) logAlert("Diastolic BP High", dia, currentThresholds.bpDiaHigh)
             if (dia < currentThresholds.bpDiaLow) logAlert("Diastolic BP Low", dia, currentThresholds.bpDiaLow)
         }
        data.glucose?.let { gluc ->
            if (gluc > currentThresholds.glucoseHigh) logAlert("Glucose High", gluc, currentThresholds.glucoseHigh)
            if (gluc < currentThresholds.glucoseLow) logAlert("Glucose Low", gluc, currentThresholds.glucoseLow)
        }
        data.cholesterol?.let { chol ->
            if (chol > currentThresholds.cholesterolHigh) logAlert("Cholesterol High", chol, currentThresholds.cholesterolHigh)
             if (chol < currentThresholds.cholesterolLow) logAlert("Cholesterol Low", chol, currentThresholds.cholesterolLow)
        }
    }

    private fun logAlert(vitalName: String, value: Float, threshold: Float) {
             Log.w("VITAL_ALERT", "*** SIMULATED ALERT ***: $vitalName (${value.roundToInt()}) crossed threshold (${threshold.roundToInt()})")
    }


    override fun onCleared() {
        super.onCleared()
        generatorJob?.cancel()
    }
}