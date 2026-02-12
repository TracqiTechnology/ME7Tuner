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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.GridColor
import java.text.DecimalFormat

data class ChartSeries(
    val name: String,
    val points: List<Pair<Double, Double>>,
    val color: Color,
    val showLine: Boolean = true,
    val showPoints: Boolean = false,
    val strokeWidth: Float = 2f
)

@Composable
fun LineChart(
    series: List<ChartSeries>,
    title: String = "",
    xAxisLabel: String = "",
    yAxisLabel: String = "",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (series.isEmpty() || series.all { it.points.isEmpty() }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        return
    }

    val allPoints = series.flatMap { it.points }
    val bounds = remember(allPoints) {
        val xMin = allPoints.minOf { it.first }
        val xMax = allPoints.maxOf { it.first }
        val yMin = allPoints.minOf { it.second }
        val yMax = allPoints.maxOf { it.second }
        val xPad = if (xMax - xMin == 0.0) 1.0 else (xMax - xMin) * 0.05
        val yPad = if (yMax - yMin == 0.0) 1.0 else (yMax - yMin) * 0.05
        ChartBounds(xMin - xPad, xMax + xPad, yMin - yPad, yMax + yPad)
    }

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
        if (series.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(s.color, radius = 5f)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(s.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(start = 50.dp, bottom = 30.dp, end = 16.dp, top = 8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawChartArea(bounds, series)
            }

            // Y-axis label
            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-46).dp)
                )
            }

            // X-axis label
            if (xAxisLabel.isNotEmpty()) {
                Text(
                    text = xAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 24.dp)
                )
            }
        }
    }
}

private data class ChartBounds(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)

private fun DrawScope.drawChartArea(bounds: ChartBounds, seriesList: List<ChartSeries>) {
    val w = size.width
    val h = size.height
    val xRange = bounds.xMax - bounds.xMin
    val yRange = bounds.yMax - bounds.yMin
    val formatter = DecimalFormat("#.##")

    fun mapX(x: Double) = ((x - bounds.xMin) / xRange * w).toFloat()
    fun mapY(y: Double) = (h - (y - bounds.yMin) / yRange * h).toFloat()

    // Grid lines
    val xTicks = niceTickValues(bounds.xMin, bounds.xMax, 8)
    val yTicks = niceTickValues(bounds.yMin, bounds.yMax, 6)

    for (tick in xTicks) {
        val x = mapX(tick)
        drawLine(GridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.5f)
        drawContext.canvas.nativeCanvas.apply {
            val paint = org.jetbrains.skia.Paint().apply {
                color = 0xFFF8F8F2.toInt()
                isAntiAlias = true
            }
            val font = org.jetbrains.skia.Font().apply { size = 10f }
            drawString(formatter.format(tick), x - 15f, h + 14f, font, paint)
        }
    }

    for (tick in yTicks) {
        val y = mapY(tick)
        drawLine(GridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
        drawContext.canvas.nativeCanvas.apply {
            val paint = org.jetbrains.skia.Paint().apply {
                color = 0xFFF8F8F2.toInt()
                isAntiAlias = true
            }
            val font = org.jetbrains.skia.Font().apply { size = 10f }
            drawString(formatter.format(tick), -44f, y + 4f, font, paint)
        }
    }

    // Border
    drawRect(GridColor, style = Stroke(1f))

    // Series
    for (series in seriesList) {
        if (series.points.isEmpty()) continue
        val sorted = series.points.sortedBy { it.first }

        if (series.showLine && sorted.size > 1) {
            val path = Path().apply {
                moveTo(mapX(sorted[0].first), mapY(sorted[0].second))
                for (i in 1 until sorted.size) {
                    lineTo(mapX(sorted[i].first), mapY(sorted[i].second))
                }
            }
            drawPath(path, series.color, style = Stroke(series.strokeWidth))
        }

        if (series.showPoints) {
            for (point in sorted) {
                drawCircle(series.color, radius = 3f, center = Offset(mapX(point.first), mapY(point.second)))
            }
        }
    }
}

internal fun niceTickValues(min: Double, max: Double, targetCount: Int): List<Double> {
    val range = max - min
    if (range <= 0) return listOf(min)
    val roughStep = range / targetCount
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(roughStep)))
    val residual = roughStep / magnitude

    val niceStep = when {
        residual <= 1.5 -> magnitude
        residual <= 3.0 -> 2.0 * magnitude
        residual <= 7.0 -> 5.0 * magnitude
        else -> 10.0 * magnitude
    }

    val ticks = mutableListOf<Double>()
    var tick = Math.ceil(min / niceStep) * niceStep
    while (tick <= max) {
        ticks.add(tick)
        tick += niceStep
    }
    return ticks
}
