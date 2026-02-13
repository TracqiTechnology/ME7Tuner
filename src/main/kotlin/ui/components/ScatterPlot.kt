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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.GridColor
import java.text.DecimalFormat

@Composable
fun ScatterPlot(
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
        ScatterBounds(xMin - xPad, xMax + xPad, yMin - yPad, yMax + yPad)
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
                val w = size.width
                val h = size.height
                val xRange = bounds.xMax - bounds.xMin
                val yRange = bounds.yMax - bounds.yMin
                val formatter = DecimalFormat("#.##")

                fun mapX(x: Double) = ((x - bounds.xMin) / xRange * w).toFloat()
                fun mapY(y: Double) = (h - (y - bounds.yMin) / yRange * h).toFloat()

                // Grid
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

                drawRect(GridColor, style = Stroke(1f))

                // Series
                for (s in series) {
                    val sorted = s.points.filter { it.first.isFinite() && it.second.isFinite() }.sortedBy { it.first }
                    if (sorted.isEmpty()) continue

                    // Draw points (scatter)
                    if (s.showPoints) {
                        for (point in sorted) {
                            drawCircle(s.color, radius = 4f, center = Offset(mapX(point.first), mapY(point.second)))
                        }
                    }

                    // Optional line connections
                    if (s.showLine && sorted.size > 1) {
                        val path = Path().apply {
                            moveTo(mapX(sorted[0].first), mapY(sorted[0].second))
                            for (i in 1 until sorted.size) {
                                lineTo(mapX(sorted[i].first), mapY(sorted[i].second))
                            }
                        }
                        drawPath(path, s.color, style = Stroke(s.strokeWidth))
                    }
                }
            }

            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-46).dp)
                )
            }

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

private data class ScatterBounds(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)
