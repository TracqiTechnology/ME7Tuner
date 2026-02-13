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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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

    val allPoints = series.flatMap { it.points }.filter { it.first.isFinite() && it.second.isFinite() }

    if (allPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        return
    }

    val bounds = remember(allPoints) {
        val xMin = allPoints.minOf { it.first }
        val xMax = allPoints.maxOf { it.first }
        val yMin = allPoints.minOf { it.second }
        val yMax = allPoints.maxOf { it.second }
        val xPad = if (xMax - xMin == 0.0) 1.0 else (xMax - xMin) * 0.05
        val yPad = if (yMax - yMin == 0.0) 1.0 else (yMax - yMin) * 0.05
        ChartBounds(xMin - xPad, xMax + xPad, yMin - yPad, yMax + yPad)
    }

    val density = LocalDensity.current
    val leftMarginPx = with(density) { 50.dp.toPx() }
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

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawChartArea(bounds, series, leftMarginPx, topMarginPx, rightMarginPx, bottomMarginPx, textMeasurer)
            }

            // Y-axis label
            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                )
            }

            // X-axis label
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

private data class ChartBounds(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)

private fun DrawScope.drawChartArea(
    bounds: ChartBounds,
    seriesList: List<ChartSeries>,
    leftMargin: Float,
    topMargin: Float,
    rightMargin: Float,
    bottomMargin: Float,
    textMeasurer: TextMeasurer
) {
    val chartW = size.width - leftMargin - rightMargin
    val chartH = size.height - topMargin - bottomMargin
    val xRange = bounds.xMax - bounds.xMin
    val yRange = bounds.yMax - bounds.yMin
    val formatter = DecimalFormat("#.##")
    val tickLabelStyle = TextStyle(color = Color(0xFFF8F8F2), fontSize = 10.sp)

    fun mapX(x: Double) = (leftMargin + (x - bounds.xMin) / xRange * chartW).toFloat()
    fun mapY(y: Double) = (topMargin + chartH - (y - bounds.yMin) / yRange * chartH).toFloat()

    // Grid lines
    val xTicks = niceTickValues(bounds.xMin, bounds.xMax, 8)
    val yTicks = niceTickValues(bounds.yMin, bounds.yMax, 6)

    for (tick in xTicks) {
        val x = mapX(tick)
        drawLine(GridColor, Offset(x, topMargin), Offset(x, topMargin + chartH), strokeWidth = 0.5f)
        drawLine(GridColor, Offset(x, topMargin + chartH), Offset(x, topMargin + chartH + 5f), strokeWidth = 1f)
        val tickText = textMeasurer.measure(formatter.format(tick), tickLabelStyle)
        drawText(tickText, topLeft = Offset(x - tickText.size.width / 2f, topMargin + chartH + 2f))
    }

    for (tick in yTicks) {
        val y = mapY(tick)
        drawLine(GridColor, Offset(leftMargin, y), Offset(leftMargin + chartW, y), strokeWidth = 0.5f)
        drawLine(GridColor, Offset(leftMargin - 5f, y), Offset(leftMargin, y), strokeWidth = 1f)
        val tickText = textMeasurer.measure(formatter.format(tick), tickLabelStyle)
        drawText(tickText, topLeft = Offset(leftMargin - tickText.size.width - 4f, y - tickText.size.height / 2f))
    }

    // Border
    drawRect(
        GridColor,
        topLeft = Offset(leftMargin, topMargin),
        size = androidx.compose.ui.geometry.Size(chartW, chartH),
        style = Stroke(1f)
    )

    // Series
    for (series in seriesList) {
        val sorted = series.points.filter { it.first.isFinite() && it.second.isFinite() }.sortedBy { it.first }
        if (sorted.isEmpty()) continue

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
