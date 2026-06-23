package com.flypigs.ntfyapp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flypigs.ntfyapp.data.local.dao.TopicCount

private val barColors = listOf(
    Color(0xFF1976D2),
    Color(0xFF26A69A),
    Color(0xFFF57C00),
    Color(0xFF388E3C),
    Color(0xFFD32F2F),
    Color(0xFF7B1FA2),
    Color(0xFF0097A7),
    Color(0xFF689F38)
)

@Composable
fun BarChart(
    data: List<TopicCount>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxCount = data.maxOf { it.count }.toFloat().coerceAtLeast(1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEachIndexed { index, stat ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic name
                Text(
                    text = stat.topic,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(80.dp),
                    fontSize = 12.sp,
                    maxLines = 1
                )

                // Bar
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                ) {
                    val barWidth = (stat.count / maxCount) * size.width
                    val color = barColors[index % barColors.size]
                    drawRoundRect(
                        color = color,
                        topLeft = Offset.Zero,
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Count
                Text(
                    text = stat.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
