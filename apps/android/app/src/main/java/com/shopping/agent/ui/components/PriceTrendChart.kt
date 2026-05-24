package com.shopping.agent.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shopping.agent.ui.theme.PriceRed

/**
 * 价格趋势折线图 — Canvas 自绘
 * 红色折线（strokeWidth=3f）+ 浅红色半透明填充
 * 固定高度 120dp，X 轴最小标注，Y 轴无标注
 */
@Composable
fun PriceTrendChart(
    prices: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = PriceRed,
    fillColor: Color = PriceRed.copy(alpha = 0.12f),
    strokeWidth: Float = 3f,
) {
    if (prices.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height
        val paddingHorizontal = 4.dp.toPx()
        val paddingVertical = 8.dp.toPx()

        val chartWidth = width - paddingHorizontal * 2
        val chartHeight = height - paddingVertical * 2

        val minPrice = prices.minOrNull() ?: 0f
        val maxPrice = prices.maxOrNull() ?: 1f
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)

        // 计算每个数据点的坐标
        val points = prices.mapIndexed { index, price ->
            val x = paddingHorizontal + (chartWidth * index / (prices.size - 1).coerceAtLeast(1))
            val y = paddingVertical + chartHeight * (1 - (price - minPrice) / priceRange)
            Offset(x, y)
        }

        // 绘制填充区域
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingVertical + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, paddingVertical + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        fillColor,
                        fillColor.copy(alpha = 0.02f),
                    ),
                    startY = paddingVertical,
                    endY = paddingVertical + chartHeight,
                ),
            )
        }

        // 绘制折线
        if (points.size >= 2) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // X 轴最小标注 — 只画时间点小圆点
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = point,
            )
        }
    }
}
