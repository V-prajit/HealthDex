package com.example.phms.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


val ChartRed = Color(0xFFE57373)
val ChartBlue = Color(0xFF64B5F6)
val ChartOrange = Color(0xFFFFB74D)
val ChartPurple = Color(0xFFBA68C8)
val ChartGridColor = Color.Gray.copy(alpha = 0.3f)


data class ChartDataPoint(val timestampMs: Long, val value: Float)
data class BPChartDataPoint(val timestampMs: Long, val systolic: Float, val diastolic: Float)


// Function to calculate optimal Y-axis range, prioritizing thresholds
private fun calculateYAxisRange(
    dataMin: Float?, dataMax: Float?, // Min/max from the actual data points
    lowThreshold: Float?, highThreshold: Float?, // User-set thresholds
    defaultMin: Float = 50f, defaultMax: Float = 150f // Fallback range if no data/thresholds
): Pair<Float, Float> {

    // Determine the core range based on thresholds if available
    val coreMin = lowThreshold ?: (dataMin ?: defaultMin)
    val coreMax = highThreshold ?: (dataMax ?: defaultMax)
    var span = coreMax - coreMin
    if (span <= 0) span = defaultMax - defaultMin // Ensure span is positive

    // Define buffer (e.g., 15% of span, minimum 10 units)
    val buffer = max(10f, span * 0.15f)

    // Initial range based on thresholds + buffer
    var finalMin = floor(coreMin - buffer)
    var finalMax = ceil(coreMax + buffer)

    // Expand range *only if needed* to include data points outside the threshold+buffer range
    dataMin?.let { finalMin = min(finalMin, floor(it - buffer / 2)) } // Smaller buffer for data outliers
    dataMax?.let { finalMax = max(finalMax, ceil(it + buffer / 2)) }

    // Ensure min is not greater than max after adjustments
    if (finalMin >= finalMax) {
        finalMin = floor(coreMin - buffer)
        finalMax = finalMin + span + 2*buffer // Reset based on core span + buffers
    }

    return Pair(finalMin, finalMax)
}


@Composable
fun SimpleLineChart(
    modifier: Modifier = Modifier,
    title: String,
    data: List<ChartDataPoint>,
    highThreshold: Float?,
    lowThreshold: Float?,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    thresholdColor: Color = MaterialTheme.colorScheme.error,
    pointColor: Color = lineColor.copy(alpha = 0.8f),
    gridColor: Color = ChartGridColor,
    yAxisLabelCount: Int = 5
) {
    if (data.isEmpty()) {
        Text("$title: No data", modifier = modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
        return
    }

    val density = LocalDensity.current
    val textPaint = remember { /* ... Paint setup ... */
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }
    val yAxisPadding = with(density) { 35.dp.toPx()}
    val xAxisPadding = with(density) { 20.dp.toPx()}


    val filteredData = data.filter { !it.value.isNaN() }
    if (filteredData.isEmpty()) {
         Text("$title: No valid data", modifier = modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
         return
     }

    val minValData = filteredData.minOfOrNull { it.value }
    val maxValData = filteredData.maxOfOrNull { it.value }
    val minTs = filteredData.minOfOrNull { it.timestampMs } ?: 0L
    val maxTs = filteredData.maxOfOrNull { it.timestampMs } ?: 1L

    // --- Use the new Y-axis calculation ---
    val (finalMinVal, finalMaxVal) = calculateYAxisRange(
        dataMin = minValData,
        dataMax = maxValData,
        lowThreshold = lowThreshold,
        highThreshold = highThreshold,
        // Provide reasonable defaults for this specific chart type if needed
        defaultMin = 50f, defaultMax = 120f // Example defaults for HR
    )
    val finalValueRange = max(1f, finalMaxVal - finalMinVal)
    val timeRange = max(1L, maxTs - minTs)


    Column(modifier = modifier.padding(bottom = 16.dp)) {
         Text(title, style = MaterialTheme.typography.titleMedium)
         Spacer(Modifier.height(8.dp))

         BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(170.dp).padding(end = 8.dp)) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            val chartWidth = canvasWidth - yAxisPadding
            val chartHeight = canvasHeight - xAxisPadding

            val path = remember(filteredData, chartWidth, chartHeight, finalMinVal, finalValueRange, timeRange, minTs) { /* ... Path calc ... */
                Path().apply {
                    if (filteredData.size > 1) {
                        filteredData.forEachIndexed { index, point ->
                            val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - (((point.value - finalMinVal) / finalValueRange) * chartHeight)
                            val clampedY = y.coerceIn(0f, chartHeight)
                            if (index == 0) moveTo(x, clampedY) else lineTo(x, clampedY)
                        }
                    }
                }
            }

             fun getYForValue(value: Float): Float {
                 return (chartHeight - (((value - finalMinVal) / finalValueRange) * chartHeight)).coerceIn(0f, chartHeight)
             }

              val yLabels = remember(finalMinVal, finalMaxVal, yAxisLabelCount) {
                  if (finalValueRange <= 0) return@remember emptyList() // Avoid division by zero if range is bad
                   // Ensure labels are reasonably spaced integers
                  val step = max(1f, finalValueRange / (yAxisLabelCount - 1))
                  val niceMin = floor(finalMinVal / step) * step
                  val niceMax = ceil(finalMaxVal / step) * step
                  val niceRange = niceMax - niceMin
                  val niceStep = max(1f, niceRange / (yAxisLabelCount - 1))

                  List(yAxisLabelCount) { i ->
                      (niceMin + niceStep * i)
                  }.distinct() // Avoid duplicate labels if range is small
              }


            Canvas(modifier = Modifier.matchParentSize()) {
                // Draw Axes
                drawLine(Color.Gray, Offset(yAxisPadding, 0f), Offset(yAxisPadding, chartHeight))
                drawLine(Color.Gray, Offset(yAxisPadding, chartHeight), Offset(yAxisPadding + chartWidth, chartHeight))

                // Draw Y-Axis Labels and Grid Lines
                yLabels.forEach { value ->
                    val y = getYForValue(value)
                    drawLine(gridColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                    drawContext.canvas.nativeCanvas.drawText(value.roundToInt().toString(), yAxisPadding - 6.dp.toPx(), y + textPaint.textSize / 3, textPaint)
                }

                // Draw Data Line and Points
                if (filteredData.size > 1) {
                    drawPath(path, lineColor, style = Stroke(width = 2.5f.dp.toPx()))
                     filteredData.forEach { point ->
                         val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                         val y = getYForValue(point.value)
                         drawCircle(pointColor, radius = 3.dp.toPx(), center = Offset(x, y))
                     }
                } else if (filteredData.size == 1) {
                     val point = filteredData.first()
                     val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                     val y = getYForValue(point.value)
                     drawCircle(pointColor, radius = 4.dp.toPx(), center = Offset(x,y))
                 }

                // Draw Threshold Lines
                val thresholdStroke = Stroke(width = 2f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                highThreshold?.let {
                    if(it >= finalMinVal && it <= finalMaxVal){ // Check it's within the final calculated range
                        val y = getYForValue(it)
                        drawLine(thresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect)
                    }
                }
                lowThreshold?.let {
                    if(it >= finalMinVal && it <= finalMaxVal){
                        val y = getYForValue(it)
                        drawLine(thresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect)
                    }
                }
            }
        }
     }
}


@Composable
fun BloodPressureChart(
    modifier: Modifier = Modifier,
    title: String = "Blood Pressure (mmHg)",
    data: List<BPChartDataPoint>,
    sysHighThreshold: Float?,
    sysLowThreshold: Float?,
    diaHighThreshold: Float?,
    diaLowThreshold: Float?,
    systolicColor: Color = ChartRed,
    diastolicColor: Color = ChartBlue,
    thresholdColor: Color = MaterialTheme.colorScheme.error,
    gridColor: Color = ChartGridColor,
    yAxisLabelCount: Int = 5
) {
     if (data.isEmpty()) {
        Text("$title: No data", modifier = modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
        return
    }
     val density = LocalDensity.current
     val textPaint = remember { /* ... Paint setup ... */
         Paint().apply {
             color = android.graphics.Color.DKGRAY
             textSize = with(density) { 10.sp.toPx() }
             textAlign = Paint.Align.RIGHT
         }
     }
     val yAxisPadding = with(density) { 35.dp.toPx()}
     val xAxisPadding = with(density) { 20.dp.toPx()}


     val filteredData = data.filter { !it.systolic.isNaN() && !it.diastolic.isNaN() }
      if (filteredData.isEmpty()) {
          Text("$title: No valid data", modifier = modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
          return
      }

     val minSysData = filteredData.minOfOrNull { it.systolic }
     val maxSysData = filteredData.maxOfOrNull { it.systolic }
     val minDiaData = filteredData.minOfOrNull { it.diastolic }
     val maxDiaData = filteredData.maxOfOrNull { it.diastolic }
     val minTs = filteredData.minOfOrNull { it.timestampMs } ?: 0L
     val maxTs = filteredData.maxOfOrNull { it.timestampMs } ?: 1L

     val minValData = listOfNotNull(minSysData, minDiaData).minOrNull()
     val maxValData = listOfNotNull(maxSysData, maxDiaData).maxOrNull()


     // --- Use the new Y-axis calculation ---
     // Consider all thresholds and data points for BP range
      val (finalMinVal, finalMaxVal) = calculateYAxisRange(
         dataMin = minValData,
         dataMax = maxValData,
         // Pass the *outermost* thresholds for initial range setting
         lowThreshold = listOfNotNull(sysLowThreshold, diaLowThreshold).minOrNull(),
         highThreshold = listOfNotNull(sysHighThreshold, diaHighThreshold).maxOrNull(),
         // Example defaults for BP
         defaultMin = 50f, defaultMax = 160f
     )
     val finalValueRange = max(1f, finalMaxVal - finalMinVal)
     val timeRange = max(1L, maxTs - minTs)


     Column(modifier = modifier.padding(bottom = 16.dp)) {
         Text(title, style = MaterialTheme.typography.titleMedium)
         Spacer(Modifier.height(8.dp))

          BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(170.dp).padding(end = 8.dp)) {
             val canvasWidth = constraints.maxWidth.toFloat()
             val canvasHeight = constraints.maxHeight.toFloat()
             val chartWidth = canvasWidth - yAxisPadding
             val chartHeight = canvasHeight - xAxisPadding

             val systolicPath = remember(filteredData, chartWidth, chartHeight, finalMinVal, finalValueRange, timeRange, minTs) { /* ... Path calc ... */
                 Path().apply {
                     if (filteredData.size > 1) {
                         filteredData.forEachIndexed { index, point ->
                              val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                              val y = chartHeight - (((point.systolic - finalMinVal) / finalValueRange) * chartHeight)
                              val clampedY = y.coerceIn(0f, chartHeight)
                              if (index == 0) moveTo(x, clampedY) else lineTo(x, clampedY)
                         }
                     }
                 }
             }
             val diastolicPath = remember(filteredData, chartWidth, chartHeight, finalMinVal, finalValueRange, timeRange, minTs) { /* ... Path calc ... */
                 Path().apply {
                      if (filteredData.size > 1) {
                          filteredData.forEachIndexed { index, point ->
                              val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                              val y = chartHeight - (((point.diastolic - finalMinVal) / finalValueRange) * chartHeight)
                              val clampedY = y.coerceIn(0f, chartHeight)
                              if (index == 0) moveTo(x, clampedY) else lineTo(x, clampedY)
                          }
                      }
                 }
             }

              fun getYForValue(value: Float): Float {
                  return (chartHeight - (((value - finalMinVal) / finalValueRange) * chartHeight)).coerceIn(0f, chartHeight)
              }

               val yLabels = remember(finalMinVal, finalMaxVal, yAxisLabelCount) { /* ... Label calc ... */
                   if (finalValueRange <= 0) return@remember emptyList()
                   val step = max(1f, finalValueRange / (yAxisLabelCount - 1))
                   val niceMin = floor(finalMinVal / step) * step
                   val niceMax = ceil(finalMaxVal / step) * step
                   val niceRange = niceMax - niceMin
                   val niceStep = max(1f, niceRange / (yAxisLabelCount - 1))
                   List(yAxisLabelCount) { i ->
                       (niceMin + niceStep * i)
                   }.distinct()
               }

            Canvas(modifier = Modifier.matchParentSize()) {
                // Draw Axes
                drawLine(Color.Gray, Offset(yAxisPadding, 0f), Offset(yAxisPadding, chartHeight))
                drawLine(Color.Gray, Offset(yAxisPadding, chartHeight), Offset(yAxisPadding + chartWidth, chartHeight))

                 // Draw Y-Axis Labels and Grid Lines
                 yLabels.forEach { value ->
                     val y = getYForValue(value)
                     drawLine(gridColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                     drawContext.canvas.nativeCanvas.drawText(value.roundToInt().toString(), yAxisPadding - 6.dp.toPx(), y + textPaint.textSize / 3, textPaint)
                 }


                // Draw Data Lines and Points
                if (filteredData.size > 1) {
                     drawPath(systolicPath, systolicColor, style = Stroke(width = 2.5f.dp.toPx()))
                     drawPath(diastolicPath, diastolicColor, style = Stroke(width = 2.5f.dp.toPx()))
                     filteredData.forEach { point ->
                          val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                          val ySys = getYForValue(point.systolic)
                          val yDia = getYForValue(point.diastolic)
                          drawCircle(systolicColor.copy(alpha = 0.8f), radius = 3.dp.toPx(), center = Offset(x, ySys))
                          drawCircle(diastolicColor.copy(alpha = 0.8f), radius = 3.dp.toPx(), center = Offset(x, yDia))
                     }
                } else if (filteredData.size == 1) {
                     val point = filteredData.first()
                     val x = yAxisPadding + ((point.timestampMs - minTs).toFloat() / timeRange) * chartWidth
                     val ySys = getYForValue(point.systolic)
                     val yDia = getYForValue(point.diastolic)
                     drawCircle(systolicColor.copy(alpha = 0.8f), radius = 4.dp.toPx(), center = Offset(x,ySys))
                     drawCircle(diastolicColor.copy(alpha = 0.8f), radius = 4.dp.toPx(), center = Offset(x,yDia))
                 }


                // Draw Threshold Lines
                 val thresholdStroke = Stroke(width = 2f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                 sysHighThreshold?.let { if(it >= finalMinVal && it <= finalMaxVal) { val y = getYForValue(it); drawLine(thresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect, cap = StrokeCap.Butt) }}
                 sysLowThreshold?.let { if(it >= finalMinVal && it <= finalMaxVal) { val y = getYForValue(it); drawLine(thresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect, cap = StrokeCap.Round) }}
                 val diaThresholdColor = thresholdColor.copy(alpha = 0.7f)
                 diaHighThreshold?.let { if(it >= finalMinVal && it <= finalMaxVal) { val y = getYForValue(it); drawLine(diaThresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect, cap = StrokeCap.Butt) }}
                 diaLowThreshold?.let { if(it >= finalMinVal && it <= finalMaxVal) { val y = getYForValue(it); drawLine(diaThresholdColor, Offset(yAxisPadding, y), Offset(yAxisPadding + chartWidth, y), strokeWidth = thresholdStroke.width, pathEffect = thresholdStroke.pathEffect, cap = StrokeCap.Round) }}
            }
        }
         // Legend for BP Chart
          Row(
              modifier = Modifier.fillMaxWidth().padding(start = (yAxisPadding / LocalDensity.current.density).dp, top = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
              LegendItem(color = systolicColor, text = "Systolic")
              LegendItem(color = diastolicColor, text = "Diastolic")
          }

     }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), color = color, shape = MaterialTheme.shapes.small) {}
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}