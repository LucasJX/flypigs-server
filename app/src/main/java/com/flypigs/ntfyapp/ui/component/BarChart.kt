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
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.data.local.dao.TopicCount

@Composable
fun BarChart(
    data: List<TopicCount>,
    modifier: Modifier = Modifier
) {
    val barColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer
    )

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
