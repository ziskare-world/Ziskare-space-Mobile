package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.ResourceHistoryPoint
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple

@Composable
fun MetricGauge(
    value: Float,
    label: String,
    prefix: String = "",
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
            .height(84.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "$prefix${value.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = color
                )
            }

            // Simple micro-linear layout visualizer bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value / 100f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }

            // Trend text indicator
            Text(
                text = when {
                    value > 80f -> "CRITICAL WORKLOAD"
                    value > 65f -> "MODERATE WARNING"
                    else -> "STABLE SYSTEM LOAD"
                },
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = when {
                    value > 80f -> MaterialTheme.colorScheme.error
                    value > 65f -> Color(0xFFFFA726)
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}

@Composable
fun RealTimeHistoryGraph(
    history: List<ResourceHistoryPoint>,
    height: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val maxCpu = 100f
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME TELEMETRY STREAM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CPU", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(RoyalPurple)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RAM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val width = size.width
            val graphHeight = size.height

            // 1. Draw helper horizontal status grid lines (25%, 50%, 75%, 100%)
            val lineStroke = Stroke(
                width = density.run { 1.dp.toPx() },
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f), 0f
                )
            )

            for (i in 1..3) {
                val y = graphHeight - (graphHeight * (i * 0.25f))
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = lineStroke.pathEffect
                )
            }

            // We calculate horizontal scale step point-to-point step width spacing
            val stepX = width / (history.size - 1).coerceAtLeast(1)

            val cpuPoints = mutableListOf<Offset>()
            val ramPoints = mutableListOf<Offset>()

            history.forEachIndexed { index, point ->
                val x = index * stepX
                val cpuY = graphHeight - (graphHeight * (point.cpu / maxCpu))
                val ramY = graphHeight - (graphHeight * (point.ram / maxCpu))

                cpuPoints.add(Offset(x, cpuY))
                ramPoints.add(Offset(x, ramY))
            }

            // Helper function to create safe visual curved lines
            fun drawMetricCurve(points: List<Offset>, color: Color) {
                if (points.isEmpty()) return
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val current = points[i]
                        val previous = points[i - 1]
                        val controlX = (previous.x + current.x) / 2
                        cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = with(density) { 2.5.dp.toPx() })
                )
            }

            // Draw paths
            clipRect {
                drawMetricCurve(cpuPoints, NeonCyan)
                drawMetricCurve(ramPoints, RoyalPurple)
            }
        }

        // Horizontal visual timestamp ticks below graph canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(history.firstOrNull(), history.getOrNull(history.size / 2), history.lastOrNull()).forEach { point ->
                if (point != null) {
                    Text(
                        text = point.timestamp,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
