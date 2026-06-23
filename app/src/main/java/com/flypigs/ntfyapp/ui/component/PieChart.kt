package com.flypigs.ntfyapp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flypigs.ntfyapp.data.local.dao.CategoryCount
import com.flypigs.ntfyapp.domain.model.MessageCategory

private val chartColors = listOf(
    Color(0xFF1976D2), // NODE_CHANGE
    Color(0xFFD32F2F), // SYSTEM_ALERT
    Color(0xFF388E3C), // RECOVERY
    Color(0xFFF57C00), // UPDATE
    Color(0xFF757575)  // OTHER
)

@Composable
fun PieChart(
    data: List<CategoryCount>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val total = data.sumOf { it.count }
    if (total == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart
        Canvas(
            modifier = Modifier.size(140.dp)
        ) {
            var startAngle = -90f
            data.forEachIndexed { index, stat ->
                val sweepAngle = (stat.count.toFloat() / total) * 360f
                val colorIndex = getCategoryIndex(stat.category)
                drawArc(
                    color = chartColors[colorIndex % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.forEachIndexed { index, stat ->
                val colorIndex = getCategoryIndex(stat.category)
                val percentage = (stat.count * 100 / total)
                val displayName = getCategoryDisplayName(stat.category)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = chartColors[colorIndex % chartColors.size])
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$displayName ${percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun getCategoryIndex(category: String): Int {
    return try {
        MessageCategory.valueOf(category).ordinal
    } catch (e: Exception) {
        4 // OTHER
    }
}

private fun getCategoryDisplayName(category: String): String {
    return try {
        MessageCategory.valueOf(category).displayName
    } catch (e: Exception) {
        "其他"
    }
}
