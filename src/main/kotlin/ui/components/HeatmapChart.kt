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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.math.map.Map3d
import ui.theme.GridColor
import java.text.DecimalFormat

private val HeatmapBlue = Color(0xFF1976D2)
private val HeatmapNeutral = Color(0xFF424242)
private val HeatmapRed = Color(0xFFD32F2F)

@Composable
fun HeatmapChart(
    originalMap: Map3d,
    calculatedMap: Map3d,
    title: String = "Timing Delta (Original \u2212 Calculated)",
    xAxisLabel: String = "Load (%)",
    yAxisLabel: String = "RPM",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val deltaData = remember(originalMap, calculatedMap) {
        val rows = originalMap.yAxis.size
        val cols = originalMap.xAxis.size
        val matrix = Array(rows) { r ->
            Array(cols) { c ->
                val origVal = originalMap.zAxis[r][c]
                val calcVal = calculatedMap.lookup(originalMap.xAxis[c], originalMap.yAxis[r])
                origVal - calcVal
            }
        }
        val flat = matrix.flatMap { it.toList() }
        val maxAbs = flat.maxOfOrNull { kotlin.math.abs(it) } ?: 1.0
        Triple(matrix, flat, maxAbs.coerceAtLeast(0.01))
    }

    val deltaMatrix = deltaData.first
    val maxAbs = deltaData.third

    val density = LocalDensity.current
    val leftMarginPx = with(density) { 60.dp.toPx() }
    val bottomMarginPx = with(density) { 30.dp.toPx() }
    val rightMarginPx = with(density) { 80.dp.toPx() }
    val topMarginPx = with(density) { 8.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val formatter = remember { DecimalFormat("#.#") }

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp)
            )
        }

        // Legend row
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(12.dp)) { drawCircle(HeatmapBlue, radius = 5f) }
            Text("Calc. advanced more", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(8.dp))
            Canvas(modifier = Modifier.size(12.dp)) { drawCircle(HeatmapNeutral, radius = 5f) }
            Text("No change", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(8.dp))
            Canvas(modifier = Modifier.size(12.dp)) { drawCircle(HeatmapRed, radius = 5f) }
            Text("Orig. advanced more", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartW = size.width - leftMarginPx - rightMarginPx
                val chartH = size.height - topMarginPx - bottomMarginPx
                val rows = originalMap.yAxis.size
                val cols = originalMap.xAxis.size
                if (rows == 0 || cols == 0) return@Canvas

                val cellW = chartW / cols
                val cellH = chartH / rows
                val tickLabelStyle = TextStyle(color = Color(0xFFF8F8F2), fontSize = 9.sp)
                val cellTextStyle = TextStyle(color = Color(0xFFF8F8F2), fontSize = 8.sp)

                // Draw cells
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val delta = deltaMatrix[r][c]
                        val norm = (delta / maxAbs).toFloat().coerceIn(-1f, 1f)
                        val cellColor = if (norm >= 0) {
                            lerp(HeatmapNeutral, HeatmapRed, norm)
                        } else {
                            lerp(HeatmapNeutral, HeatmapBlue, -norm)
                        }

                        val x = leftMarginPx + c * cellW
                        // Flip rows so lowest RPM is at bottom
                        val y = topMarginPx + (rows - 1 - r) * cellH

                        drawRect(cellColor, topLeft = Offset(x, y), size = Size(cellW, cellH))
                        drawRect(
                            GridColor,
                            topLeft = Offset(x, y),
                            size = Size(cellW, cellH),
                            style = Stroke(0.5f)
                        )

                        // Draw delta text in cell if big enough
                        if (cellW > 30f && cellH > 14f) {
                            val text = formatter.format(delta)
                            val measured = textMeasurer.measure(text, cellTextStyle)
                            drawText(
                                measured,
                                topLeft = Offset(
                                    x + (cellW - measured.size.width) / 2f,
                                    y + (cellH - measured.size.height) / 2f
                                )
                            )
                        }
                    }
                }

                // Y-axis labels (RPM) — on the left
                for (r in 0 until rows) {
                    val y = topMarginPx + (rows - 1 - r) * cellH + cellH / 2f
                    val label = formatter.format(originalMap.yAxis[r])
                    val measured = textMeasurer.measure(label, tickLabelStyle)
                    drawText(
                        measured,
                        topLeft = Offset(leftMarginPx - measured.size.width - 4f, y - measured.size.height / 2f)
                    )
                }

                // X-axis labels (Load) — on the bottom
                val maxXLabels = (chartW / 40f).toInt().coerceAtLeast(1)
                val xLabelStep = (cols / maxXLabels).coerceAtLeast(1)
                for (c in 0 until cols step xLabelStep) {
                    val x = leftMarginPx + c * cellW + cellW / 2f
                    val label = formatter.format(originalMap.xAxis[c])
                    val measured = textMeasurer.measure(label, tickLabelStyle)
                    drawText(
                        measured,
                        topLeft = Offset(x - measured.size.width / 2f, topMarginPx + chartH + 2f)
                    )
                }

                // Color scale legend bar on the right
                val legendX = leftMarginPx + chartW + 8f
                val legendW = 16f
                val legendH = chartH
                val legendSteps = 50
                for (i in 0 until legendSteps) {
                    val frac = 1f - (i.toFloat() / legendSteps) // top = positive, bottom = negative
                    val norm = frac * 2f - 1f // -1..1
                    val color = if (norm >= 0) lerp(HeatmapNeutral, HeatmapRed, norm)
                    else lerp(HeatmapNeutral, HeatmapBlue, -norm)
                    val stepH = legendH / legendSteps
                    drawRect(
                        color,
                        topLeft = Offset(legendX, topMarginPx + i * stepH),
                        size = Size(legendW, stepH + 1f)
                    )
                }
                drawRect(
                    GridColor,
                    topLeft = Offset(legendX, topMarginPx),
                    size = Size(legendW, legendH),
                    style = Stroke(1f)
                )

                // Legend labels
                val topLabel = textMeasurer.measure("+${formatter.format(maxAbs)}", tickLabelStyle)
                drawText(topLabel, topLeft = Offset(legendX + legendW + 2f, topMarginPx))
                val zeroLabel = textMeasurer.measure("0", tickLabelStyle)
                drawText(zeroLabel, topLeft = Offset(legendX + legendW + 2f, topMarginPx + legendH / 2f - zeroLabel.size.height / 2f))
                val bottomLabel = textMeasurer.measure("-${formatter.format(maxAbs)}", tickLabelStyle)
                drawText(bottomLabel, topLeft = Offset(legendX + legendW + 2f, topMarginPx + legendH - bottomLabel.size.height))

                // Border
                drawRect(
                    GridColor,
                    topLeft = Offset(leftMarginPx, topMarginPx),
                    size = Size(chartW, chartH),
                    style = Stroke(1f)
                )
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
