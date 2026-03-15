package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import domain.math.map.Map3d
import ui.theme.*

@Composable
fun TimingCharts(
    originalMap: Map3d?,
    calculatedMap: Map3d?,
    modifier: Modifier = Modifier,
    heatmapTitle: String = "Timing Delta (Original − Calculated)",
    sliceTitle: String = "Timing vs RPM at Selected Loads",
    sliceYLabel: String = "Timing (grad KW)",
    contourTitle: String = "Timing Contours"
) {
    if (originalMap == null || calculatedMap == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Load a map to view charts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("Delta Heatmap", "Load Slices", "Contours")

    Column(modifier = modifier) {
        SecondaryTabRow(selectedTabIndex = selectedSubTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> HeatmapChart(
                originalMap = originalMap,
                calculatedMap = calculatedMap,
                title = heatmapTitle,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)
            )
            1 -> LoadSliceChart(
                originalMap = originalMap,
                calculatedMap = calculatedMap,
                title = sliceTitle,
                yAxisLabel = sliceYLabel,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)
            )
            2 -> ContourChart(
                originalMap = originalMap,
                calculatedMap = calculatedMap,
                title = contourTitle,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)
            )
        }
    }
}

private val sliceColors = listOf(ChartBlue, ChartGreen, ChartOrange, ChartMagenta, ChartCyan)

@Composable
private fun LoadSliceChart(
    originalMap: Map3d,
    calculatedMap: Map3d,
    title: String = "Timing vs RPM at Selected Loads",
    yAxisLabel: String = "Timing (grad KW)",
    modifier: Modifier = Modifier
) {
    val seriesList = remember(originalMap, calculatedMap) {
        val cols = originalMap.xAxis.size
        if (cols == 0) return@remember emptyList<ChartSeries>()

        // Pick ~5 representative load column indices
        val indices = when {
            cols <= 5 -> (0 until cols).toList()
            else -> listOf(0, cols / 4, cols / 2, 3 * cols / 4, cols - 1).distinct()
        }

        indices.flatMapIndexed { colorIdx, colIdx ->
            val loadValue = originalMap.xAxis[colIdx]
            val color = sliceColors[colorIdx % sliceColors.size]
            val loadLabel = "%.0f%%".format(loadValue)

            val origPoints = originalMap.yAxis.mapIndexedNotNull { r, rpm ->
                if (r < originalMap.zAxis.size && colIdx < originalMap.zAxis[r].size) {
                    Pair(rpm, originalMap.zAxis[r][colIdx])
                } else null
            }

            val calcPoints = originalMap.yAxis.map { rpm ->
                Pair(rpm, calculatedMap.lookup(loadValue, rpm))
            }

            listOf(
                ChartSeries(
                    name = "Orig $loadLabel",
                    points = origPoints,
                    color = color,
                    strokeWidth = 3f
                ),
                ChartSeries(
                    name = "Calc $loadLabel",
                    points = calcPoints,
                    color = color.copy(alpha = 0.6f),
                    strokeWidth = 1.5f
                )
            )
        }
    }

    LineChart(
        series = seriesList,
        title = title,
        xAxisLabel = "RPM",
        yAxisLabel = yAxisLabel,
        modifier = modifier
    )
}
