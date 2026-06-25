package com.flypigs.ntfyapp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.data.local.dao.CategoryCount
import com.flypigs.ntfyapp.domain.model.MessageCategory

@Composable
fun PieChart(
    data: List<CategoryCount>,
    modifier: Modifier = Modifier
) {
    val defaultColor = MaterialTheme.colorScheme.outline
    val chartColors = remember {
        MessageCategory.entries.associate { it.name to it.fallbackColor }
    }
    // 在 Composable 上下文中获取 theme-aware 颜色映射
    val themeChartColors = MessageCategory.entries.associate { it.name to it.color }

    // ─── 动画 ────────────────────────────────────────────────
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

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
        // Pie chart — 动画驱动 sweepAngle 从 0 渐进到目标值
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .padding(8.dp)
        ) {
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val progress = animProgress.value

            var startAngle = -90f
            data.forEach { stat ->
                val sweepAngle = (stat.count.toFloat() / total) * 360f * progress
                val color = themeChartColors[stat.category] ?: chartColors[stat.category] ?: defaultColor
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += (stat.count.toFloat() / total) * 360f * progress
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEach { stat ->
                val category = try {
                    MessageCategory.valueOf(stat.category)
                } catch (_: Exception) {
                    MessageCategory.OTHER
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = themeChartColors[stat.category] ?: chartColors[stat.category] ?: category.fallbackColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${category.displayName} ${stat.count}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
