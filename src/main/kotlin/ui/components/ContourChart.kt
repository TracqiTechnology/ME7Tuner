package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.math.map.Map3d
import ui.theme.ChartCyan
import ui.theme.ChartOrange
import ui.theme.GridColor
import java.text.DecimalFormat

/**
 * Marching squares contour extraction.
 * Returns line segments as pairs of (x, y) coordinates in data space.
 */
internal fun marchingSquaresContours(
    xAxis: Array<Double>,
    yAxis: Array<Double>,
    zAxis: Array<Array<Double>>,
    level: Double
): List<Pair<Pair<Double, Double>, Pair<Double, Double>>> {
    val segments = mutableListOf<Pair<Pair<Double, Double>, Pair<Double, Double>>>()
    val rows = yAxis.size
    val cols = xAxis.size
    if (rows < 2 || cols < 2) return segments

    for (r in 0 until rows - 1) {
        for (c in 0 until cols - 1) {
            val v00 = zAxis[r][c]
            val v01 = zAxis[r][c + 1]
            val v10 = zAxis[r + 1][c]
            val v11 = zAxis[r + 1][c + 1]

            val x0 = xAxis[c]
            val x1 = xAxis[c + 1]
            val y0 = yAxis[r]
            val y1 = yAxis[r + 1]

            // Classify corners: 1 if above level
            val idx = ((if (v00 >= level) 8 else 0) or
                    (if (v01 >= level) 4 else 0) or
                    (if (v11 >= level) 2 else 0) or
                    (if (v10 >= level) 1 else 0))

            if (idx == 0 || idx == 15) continue

            // Edge interpolation helpers
            fun interpTop(): Pair<Double, Double> {
                val t = (level - v00) / (v01 - v00)
                return Pair(x0 + t * (x1 - x0), y0)
            }
            fun interpRight(): Pair<Double, Double> {
                val t = (level - v01) / (v11 - v01)
                return Pair(x1, y0 + t * (y1 - y0))
            }
            fun interpBottom(): Pair<Double, Double> {
                val t = (level - v10) / (v11 - v10)
                return Pair(x0 + t * (x1 - x0), y1)
            }
            fun interpLeft(): Pair<Double, Double> {
                val t = (level - v00) / (v10 - v00)
                return Pair(x0, y0 + t * (y1 - y0))
            }

            // Standard 16-case marching squares lookup
            when (idx) {
                1 -> segments.add(Pair(interpLeft(), interpBottom()))
                2 -> segments.add(Pair(interpBottom(), interpRight()))
                3 -> segments.add(Pair(interpLeft(), interpRight()))
                4 -> segments.add(Pair(interpTop(), interpRight()))
                5 -> {
                    // Saddle — use average to disambiguate
                    val avg = (v00 + v01 + v10 + v11) / 4.0
                    if (avg >= level) {
                        segments.add(Pair(interpTop(), interpLeft()))
                        segments.add(Pair(interpBottom(), interpRight()))
                    } else {
                        segments.add(Pair(interpTop(), interpRight()))
                        segments.add(Pair(interpLeft(), interpBottom()))
                    }
                }
                6 -> segments.add(Pair(interpTop(), interpBottom()))
                7 -> segments.add(Pair(interpTop(), interpLeft()))
                8 -> segments.add(Pair(interpTop(), interpLeft()))
                9 -> segments.add(Pair(interpTop(), interpBottom()))
                10 -> {
                    // Saddle
                    val avg = (v00 + v01 + v10 + v11) / 4.0
                    if (avg >= level) {
                        segments.add(Pair(interpTop(), interpRight()))
                        segments.add(Pair(interpLeft(), interpBottom()))
                    } else {
                        segments.add(Pair(interpTop(), interpLeft()))
                        segments.add(Pair(interpBottom(), interpRight()))
                    }
                }
                11 -> segments.add(Pair(interpTop(), interpRight()))
                12 -> segments.add(Pair(interpLeft(), interpRight()))
                13 -> segments.add(Pair(interpBottom(), interpRight()))
                14 -> segments.add(Pair(interpLeft(), interpBottom()))
            }
        }
    }
    return segments
}

@Composable
fun ContourChart(
    originalMap: Map3d,
    calculatedMap: Map3d,
    title: String = "Timing Contours",
    xAxisLabel: String = "Load (%)",
    yAxisLabel: String = "RPM",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val contourData = remember(originalMap, calculatedMap) {
        val origZFlat = originalMap.zAxis.flatMap { it.toList() }
        val calcZFlat = calculatedMap.zAxis.flatMap { it.toList() }
        val allZ = origZFlat + calcZFlat
        val globalMin = allZ.minOrNull() ?: 0.0
        val globalMax = allZ.maxOrNull() ?: 1.0
        val levels = niceTickValues(globalMin, globalMax, 8)

        val origContours = levels.map { level ->
            level to marchingSquaresContours(originalMap.xAxis, originalMap.yAxis, originalMap.zAxis, level)
        }
        val calcContours = levels.map { level ->
            level to marchingSquaresContours(calculatedMap.xAxis, calculatedMap.yAxis, calculatedMap.zAxis, level)
        }

        // Compute plot bounds from both maps' axes
        val xMin = minOf(originalMap.xAxis.firstOrNull() ?: 0.0, calculatedMap.xAxis.firstOrNull() ?: 0.0)
        val xMax = maxOf(originalMap.xAxis.lastOrNull() ?: 1.0, calculatedMap.xAxis.lastOrNull() ?: 1.0)
        val yMin = minOf(originalMap.yAxis.firstOrNull() ?: 0.0, calculatedMap.yAxis.firstOrNull() ?: 0.0)
        val yMax = maxOf(originalMap.yAxis.lastOrNull() ?: 1.0, calculatedMap.yAxis.lastOrNull() ?: 1.0)

        data class ContourData(
            val origContours: List<Pair<Double, List<Pair<Pair<Double, Double>, Pair<Double, Double>>>>>,
            val calcContours: List<Pair<Double, List<Pair<Pair<Double, Double>, Pair<Double, Double>>>>>,
            val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double
        )
        ContourData(origContours, calcContours, xMin, xMax, yMin, yMax)
    }

    val density = LocalDensity.current
    val leftMarginPx = with(density) { 60.dp.toPx() }
    val bottomMarginPx = with(density) { 30.dp.toPx() }
    val rightMarginPx = with(density) { 16.dp.toPx() }
    val topMarginPx = with(density) { 8.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp)
            )
        }

        // Legend
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) { drawCircle(ChartCyan, radius = 5f) }
                Spacer(Modifier.width(4.dp))
                Text("Original", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) { drawCircle(ChartOrange, radius = 5f) }
                Spacer(Modifier.width(4.dp))
                Text("Calculated", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartW = size.width - leftMarginPx - rightMarginPx
                val chartH = size.height - topMarginPx - bottomMarginPx
                val formatter = DecimalFormat("#.##")
                val tickLabelStyle = TextStyle(color = Color(0xFFF8F8F2), fontSize = 10.sp)

                val xPad = (contourData.xMax - contourData.xMin) * 0.02
                val yPad = (contourData.yMax - contourData.yMin) * 0.02
                val xMin = contourData.xMin - xPad
                val xMax = contourData.xMax + xPad
                val yMin = contourData.yMin - yPad
                val yMax = contourData.yMax + yPad
                val xRange = xMax - xMin
                val yRange = yMax - yMin

                fun mapX(x: Double) = (leftMarginPx + (x - xMin) / xRange * chartW).toFloat()
                fun mapY(y: Double) = (topMarginPx + chartH - (y - yMin) / yRange * chartH).toFloat()

                // Grid lines
                val xTicks = niceTickValues(xMin, xMax, 8)
                val yTicks = niceTickValues(yMin, yMax, 6)

                for (tick in xTicks) {
                    val x = mapX(tick)
                    drawLine(GridColor, Offset(x, topMarginPx), Offset(x, topMarginPx + chartH), strokeWidth = 0.5f)
                    val tickText = textMeasurer.measure(formatter.format(tick), tickLabelStyle)
                    drawText(tickText, topLeft = Offset(x - tickText.size.width / 2f, topMarginPx + chartH + 2f))
                }

                for (tick in yTicks) {
                    val y = mapY(tick)
                    drawLine(GridColor, Offset(leftMarginPx, y), Offset(leftMarginPx + chartW, y), strokeWidth = 0.5f)
                    val tickText = textMeasurer.measure(formatter.format(tick), tickLabelStyle)
                    drawText(tickText, topLeft = Offset(leftMarginPx - tickText.size.width - 4f, y - tickText.size.height / 2f))
                }

                // Border
                drawRect(
                    GridColor,
                    topLeft = Offset(leftMarginPx, topMarginPx),
                    size = androidx.compose.ui.geometry.Size(chartW, chartH),
                    style = Stroke(1f)
                )

                // Draw contour segments
                fun drawContours(
                    contours: List<Pair<Double, List<Pair<Pair<Double, Double>, Pair<Double, Double>>>>>,
                    color: Color,
                    strokeWidth: Float
                ) {
                    for ((_, segments) in contours) {
                        for ((p1, p2) in segments) {
                            drawLine(
                                color,
                                Offset(mapX(p1.first), mapY(p1.second)),
                                Offset(mapX(p2.first), mapY(p2.second)),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
                }

                drawContours(contourData.origContours, ChartCyan, 2f)
                drawContours(contourData.calcContours, ChartOrange, 2f)
            }

            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)
                )
            }

            if (xAxisLabel.isNotEmpty()) {
                Text(
                    text = xAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
