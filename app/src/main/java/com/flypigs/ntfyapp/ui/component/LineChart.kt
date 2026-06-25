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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.data.local.dao.DailyCount

@Composable
fun LineChart(
    data: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    // ─── 动画 ────────────────────────────────────────────────
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
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
            val progress = animProgress.value

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

            if (data.size < 2) {
                if (data.size == 1) {
                    val x = paddingLeft + chartWidth / 2
                    val y = 16f + chartHeight * (1 - data[0].count / maxCount) * progress
                    drawCircle(color = dotColor, radius = 6f, center = Offset(x, y))
                }
                return@Canvas
            }

            // Line path — 动画驱动：只绘制 progress 比例的路径
            val path = Path()
            val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)
            val visibleCount = (data.size * progress).toInt().coerceAtLeast(2)
            val lastFraction = (data.size * progress) - visibleCount + 1  // 末尾插值

            data.forEachIndexed { index, stat ->
                if (index >= visibleCount && index > 0) return@forEachIndexed

                val x = paddingLeft + index * stepX
                val y = 16f + chartHeight * (1 - stat.count / maxCount)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // 动画可见范围内才画点
                if (index < visibleCount) {
                    drawCircle(color = dotColor, radius = 4f, center = Offset(x, y))
                }
            }

            // 插值最后一个过渡点
            if (visibleCount < data.size && visibleCount > 0) {
                val prevStat = data[visibleCount - 1]
                val nextStat = data[visibleCount]
                val prevX = paddingLeft + (visibleCount - 1) * stepX
                val prevY = 16f + chartHeight * (1 - prevStat.count / maxCount)
                val nextX = paddingLeft + visibleCount * stepX
                val nextY = 16f + chartHeight * (1 - nextStat.count / maxCount)
                val interpX = prevX + (nextX - prevX) * lastFraction.coerceIn(0f, 1f)
                val interpY = prevY + (nextY - prevY) * lastFraction.coerceIn(0f, 1f)
                path.lineTo(interpX, interpY)
            }

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
        if (parts.size == 3) "${parts[1]}/${parts[2]}" else dateStr
    } catch (e: Exception) { dateStr }
}
