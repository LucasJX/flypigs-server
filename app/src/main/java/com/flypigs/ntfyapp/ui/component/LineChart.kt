package com.flypigs.ntfyapp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flypigs.ntfyapp.data.local.dao.DailyCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LineChart(
    data: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val lineColor = Color(0xFF1976D2)
    val dotColor = Color(0xFF1976D2)
    val gridColor = Color(0xFFE0E0E0)
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val maxCount = data.maxOf { it.count }.toFloat().coerceAtLeast(1f)
            val paddingLeft = 40f
            val paddingBottom = 30f
            val chartWidth = size.width - paddingLeft - 16f
            val chartHeight = size.height - paddingBottom - 16f

            // Grid lines
            for (i in 0..4) {
                val y = 16f + chartHeight * (1 - i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - 16f, y),
                    strokeWidth = 1f
                )
            }

            if (data.size < 2) return@Canvas

            // Line path
            val path = Path()
            val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

            data.forEachIndexed { index, stat ->
                val x = paddingLeft + index * stepX
                val y = 16f + chartHeight * (1 - stat.count / maxCount)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw dot
                drawCircle(
                    color = dotColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }

        // Date labels
        if (data.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = if (data.size > 7) {
                    listOf(data.first(), data[data.size / 2], data.last())
                } else {
                    data
                }
                labels.forEach { stat ->
                    Text(
                        text = formatDateLabel(stat.date),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun formatDateLabel(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            "${parts[1]}/${parts[2]}"
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
