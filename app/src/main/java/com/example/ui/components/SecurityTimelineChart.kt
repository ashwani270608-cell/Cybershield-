package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChartCritical
import com.example.ui.theme.ChartHigh
import com.example.ui.theme.ChartLow
import com.example.ui.theme.ChartMedium
import com.example.ui.theme.ChartScore
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

data class TimelineDataPoint(
    val dateLabel: String,
    val score: Float,
    val critical: Float,
    val high: Float,
    val medium: Float,
    val low: Float
)

@Composable
fun SecurityTimelineChart(
    modifier: Modifier = Modifier,
    dataPoints: List<TimelineDataPoint> = defaultTimelineData()
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                if (selectedIndex != null && selectedIndex!! in dataPoints.indices) {
                    val p = dataPoints[selectedIndex!!]
                    Text(
                        text = "${p.dateLabel}: Score ${p.score.toInt()} | H:${p.high.toInt()} M:${p.medium.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChartScore
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas drawing timeline curves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val pointWidth = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / pointWidth).toInt().coerceIn(0, dataPoints.lastIndex)
                                selectedIndex = index
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

                    // Draw subtle grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (chartHeight / gridLines) * i
                        drawLine(
                            color = Color(0x1A94A3B8),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Helper to map values
                    fun getY(value: Float, maxVal: Float = 100f): Float {
                        val normalized = (value / maxVal).coerceIn(0f, 1f)
                        return chartHeight - (normalized * (chartHeight - 16.dp.toPx()))
                    }

                    // Helper to draw a line path
                    fun drawMetricLine(color: Color, extractor: (TimelineDataPoint) -> Float, maxVal: Float) {
                        val path = Path()
                        dataPoints.forEachIndexed { i, dp ->
                            val x = i * stepX
                            val y = getY(extractor(dp), maxVal)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        dataPoints.forEachIndexed { i, dp ->
                            val x = i * stepX
                            val y = getY(extractor(dp), maxVal)
                            drawCircle(
                                color = color,
                                radius = if (selectedIndex == i) 5.dp.toPx() else 3.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Draw Lines
                    drawMetricLine(ChartScore, { it.score }, 100f)
                    drawMetricLine(ChartCritical, { it.critical }, 35f)
                    drawMetricLine(ChartHigh, { it.high }, 35f)
                    drawMetricLine(ChartMedium, { it.medium }, 35f)
                    drawMetricLine(ChartLow, { it.low }, 35f)

                    // Draw selected vertical guide
                    selectedIndex?.let { idx ->
                        val selX = idx * stepX
                        drawLine(
                            color = Color(0x66FFFFFF),
                            start = Offset(selX, 0f),
                            end = Offset(selX, chartHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }

            // Date labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jan 08", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                Text("Jan 16", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                Text("Jan 24", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                Text("Feb 02", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                Text("Feb 14", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                Text("Mar 06", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = ChartScore, label = "Score")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = ChartCritical, label = "Critical")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = ChartHigh, label = "High")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = ChartMedium, label = "Medium")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = ChartLow, label = "Low")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CyberTextSecondary,
            fontSize = 11.sp
        )
    }
}

fun defaultTimelineData(): List<TimelineDataPoint> = listOf(
    TimelineDataPoint("Jan 08", 68f, 0f, 9f, 28f, 14f),
    TimelineDataPoint("Jan 10", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 13", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 16", 71f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 19", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 22", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 25", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 28", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Jan 31", 70f, 0f, 10f, 27f, 14f),
    TimelineDataPoint("Feb 02", 73f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Feb 06", 72f, 0f, 10f, 29f, 14f),
    TimelineDataPoint("Feb 10", 70f, 0f, 10f, 31f, 17f),
    TimelineDataPoint("Feb 14", 69f, 0f, 10f, 33f, 18f),
    TimelineDataPoint("Feb 18", 71f, 0f, 10f, 30f, 17f),
    TimelineDataPoint("Feb 24", 72f, 0f, 10f, 30f, 16f),
    TimelineDataPoint("Mar 06", 73f, 0f, 10f, 31f, 17f)
)
