package com.example.ui.components

import java.util.Locale
import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySummary
import com.example.data.model.DailySpending
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyTrendData
import com.example.data.model.OneYearGrowthSummary
import com.example.data.model.PaymentMethodSummary
import com.example.data.model.SixMonthGrowthSummary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Consistent Financial Vibrant Chart Palette
val ChartColors = listOf(
    Color(0xFF0D9488), // Teal
    Color(0xFF6366F1), // Indigo
    Color(0xFFF59E0B), // Amber Gold
    Color(0xFFEC4899), // Pink
    Color(0xFF10B981), // Emerald
    Color(0xFF8B5CF6), // Purple
    Color(0xFF3B82F6), // Blue
    Color(0xFFEF4444), // Red
    Color(0xFF14B8A6), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16)  // Lime
)

// 1. DONUT & PIE CHART (Interactive with slice tap, expanding radius, center label)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractivePieDonutChart(
    data: List<CategorySummary>,
    isDonut: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No expense data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val totalAmount = data.sumOf { it.totalAmount }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val activeItem = data.getOrNull(selectedIndex) ?: data.first()

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var currentStart = 0f
                            data.forEachIndexed { index, item ->
                                val sweep = (item.totalAmount / totalAmount).toFloat() * 360f
                                if (angle >= currentStart && angle <= currentStart + sweep) {
                                    selectedIndex = index
                                    return@detectTapGestures
                                }
                                currentStart += sweep
                            }
                        }
                    }
            ) {
                val strokeWidth = if (isDonut) 46.dp.toPx() else size.minDimension / 2f
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                val chartRadius = (size.minDimension - 24.dp.toPx()) / 2f
                data.forEachIndexed { index, item ->
                    val sweepAngle = ((item.totalAmount / totalAmount).toFloat() * 360f) * progress.value
                    val color = ChartColors[index % ChartColors.size]
                    val isSelected = index == selectedIndex

                    if (isDonut) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 10.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    } else {
                        val bisectorRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                        val popoutDistance = if (isSelected) 8.dp.toPx() else 0f
                        val shiftX = (kotlin.math.cos(bisectorRad) * popoutDistance).toFloat()
                        val shiftY = (kotlin.math.sin(bisectorRad) * popoutDistance).toFloat()

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - chartRadius + shiftX, center.y - chartRadius + shiftY),
                            size = Size(chartRadius * 2, chartRadius * 2)
                        )

                        // Recharts-style sector divider border
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - chartRadius + shiftX, center.y - chartRadius + shiftY),
                            size = Size(chartRadius * 2, chartRadius * 2),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            if (isDonut) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = activeItem.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = ExpenseEntity.formatInr(activeItem.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", activeItem.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        if (!isDonut) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, ChartColors[selectedIndex % ChartColors.size].copy(alpha = 0.6f)),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("pie_chart_active_tooltip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ChartColors[selectedIndex % ChartColors.size])
                    )
                    Text(
                        text = activeItem.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ExpenseEntity.formatInr(activeItem.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", activeItem.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Legend FlowRow
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { selectedIndex = index }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ChartColors[index % ChartColors.size])
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.category} (${item.percentage.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// 2. INTERACTIVE BAR CHART & STACKED BAR CHART
@Composable
fun InteractiveBarChart(
    data: List<DailySpending>,
    isStackedByPayment: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No daily spending recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(data.size - 1) }
    val maxVal = (data.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        val selectedItem = data.getOrNull(selectedIndex)
        if (selectedItem != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${selectedItem.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ExpenseEntity.formatInr(selectedItem.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val slotWidth = size.width / data.size
                        val tappedIdx = (tapOffset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = tappedIdx
                    }
                }
        ) {
            val count = data.size
            val barWidth = (size.width / count) * 0.55f
            val spacing = size.width / count

            // Draw horizontal guide lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = size.height - (size.height / gridLines) * i
                drawLine(
                    color = surfaceVariant.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            data.forEachIndexed { index, item ->
                val barHeight = ((item.amount / maxVal).toFloat() * size.height * 0.88f) * progress.value
                val x = index * spacing + (spacing - barWidth) / 2f
                val y = size.height - barHeight
                val isSelected = index == selectedIndex

                if (isStackedByPayment) {
                    // Multi-segment mock representation
                    val seg1 = barHeight * 0.6f
                    val seg2 = barHeight * 0.4f
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.8f))),
                        topLeft = Offset(x, size.height - seg1),
                        size = Size(barWidth, seg1),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryColor, secondaryColor.copy(alpha = 0.8f))),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, seg2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            if (isSelected) listOf(Color(0xFF0D9488), Color(0xFF14B8A6))
                            else listOf(primaryColor.copy(alpha = 0.85f), primaryColor.copy(alpha = 0.5f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                if (isSelected) {
                    drawCircle(
                        color = Color(0xFF0D9488),
                        radius = 4.dp.toPx(),
                        center = Offset(x + barWidth / 2f, y - 6.dp.toPx())
                    )
                }
            }
        }
    }
}

// 3. SMOOTH LINE & GRADIENT AREA CHART
@Composable
fun InteractiveAreaLineChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var activeIndex by remember { mutableIntStateOf(data.size - 1) }
    val maxVal = (data.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeItem = data.getOrNull(activeIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        if (activeItem != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${activeItem.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Spending: ${ExpenseEntity.formatInr(activeItem.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
                        val idx = (offset.x / stepX + 0.5f).toInt().coerceIn(0, data.size - 1)
                        activeIndex = idx
                    }
                }
        ) {
            val count = data.size
            if (count < 2) return@Canvas

            val stepX = size.width / (count - 1)
            val path = Path()
            val fillPath = Path()

            val points = data.mapIndexed { idx, item ->
                val x = idx * stepX
                val y = size.height - ((item.amount / maxVal).toFloat() * size.height * 0.85f * progress.value)
                Offset(x, y)
            }

            path.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, size.height)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, size.height)
            fillPath.close()

            // Draw glowing gradient area
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f))
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw active marker
            val activePoint = points.getOrNull(activeIndex)
            if (activePoint != null) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(activePoint.x, 0f),
                    end = Offset(activePoint.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = activePoint
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = activePoint
                )
            }
        }
    }
}

// 4. RADAR CHART (Category Distribution vs Average)
@Composable
fun InteractiveRadarChart(
    categories: List<CategorySummary>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val items = categories.take(6)
    val maxVal = (items.maxOfOrNull { it.totalAmount } ?: 1000.0).coerceAtLeast(100.0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val progress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.78f
            val count = items.size
            if (count < 3) return@Canvas

            val angleStep = (2 * PI / count).toFloat()

            // Draw concentric web polygons
            val levels = 4
            for (level in 1..levels) {
                val r = radius * (level.toFloat() / levels)
                val webPath = Path()
                for (i in 0 until count) {
                    val angle = -PI / 2 + i * angleStep
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
                }
                webPath.close()
                drawPath(
                    path = webPath,
                    color = onSurface.copy(alpha = 0.12f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw spokes & labels
            val dataPath = Path()
            val paint = NativePaint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = 24f
                textAlign = NativePaint.Align.CENTER
                isAntiAlias = true
            }

            for (i in 0 until count) {
                val angle = -PI / 2 + i * angleStep
                val spokeX = center.x + radius * cos(angle).toFloat()
                val spokeY = center.y + radius * sin(angle).toFloat()
                drawLine(
                    color = onSurface.copy(alpha = 0.15f),
                    start = center,
                    end = Offset(spokeX, spokeY),
                    strokeWidth = 1.dp.toPx()
                )

                // Label
                val labelR = radius + 22.dp.toPx()
                val lx = center.x + labelR * cos(angle).toFloat()
                val ly = center.y + labelR * sin(angle).toFloat() + 8f
                drawContext.canvas.nativeCanvas.drawText(items[i].category.take(8), lx, ly, paint)

                // Data polygon
                val itemVal = items[i].totalAmount
                val dataR = (itemVal / maxVal).toFloat() * radius * progress.value
                val dx = center.x + dataR * cos(angle).toFloat()
                val dy = center.y + dataR * sin(angle).toFloat()

                if (i == 0) dataPath.moveTo(dx, dy) else dataPath.lineTo(dx, dy)
            }
            dataPath.close()

            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    listOf(primaryColor.copy(alpha = 0.45f), primaryColor.copy(alpha = 0.1f)),
                    center = center,
                    radius = radius
                )
            )
            drawPath(
                path = dataPath,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// 5. RECHARTS INTERACTIVE 1-YEAR & 6-MONTH MONTHLY SPENDING TRENDS & FINANCIAL GROWTH LINE CHART
enum class TrendRange(val label: String, val badge: String) {
    ONE_YEAR("1 Year", "12M"),
    SIX_MONTHS("6 Months", "6M")
}

@Composable
fun RechartsMonthlyTrendLineChart(
    summary: SixMonthGrowthSummary? = null,
    oneYearSummary: OneYearGrowthSummary? = null,
    initialRange: TrendRange = TrendRange.ONE_YEAR,
    onViewMonthlyReport: ((year: Int, month: Int) -> Unit)? = null,
    onViewAllReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(initialRange) }

    // Active trend points based on selected range
    val data: List<MonthlyTrendData> = remember(selectedRange, oneYearSummary, summary) {
        when (selectedRange) {
            TrendRange.ONE_YEAR -> oneYearSummary?.trendPoints ?: summary?.trendPoints ?: emptyList()
            TrendRange.SIX_MONTHS -> summary?.trendPoints ?: oneYearSummary?.trendPoints?.takeLast(6) ?: emptyList()
        }
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No monthly spending history available yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val avgSpend: Double = remember(selectedRange, oneYearSummary, summary, data) {
        when (selectedRange) {
            TrendRange.ONE_YEAR -> oneYearSummary?.averageMonthlySpend ?: summary?.averageMonthlySpend ?: 0.0
            TrendRange.SIX_MONTHS -> summary?.averageMonthlySpend ?: (if (data.isNotEmpty()) data.sumOf { it.totalSpent } / data.size else 0.0)
        }
    }

    val overallGrowthPercent: Double = remember(selectedRange, oneYearSummary, summary) {
        when (selectedRange) {
            TrendRange.ONE_YEAR -> oneYearSummary?.overallGrowthPercent ?: summary?.overallGrowthPercent ?: 0.0
            TrendRange.SIX_MONTHS -> summary?.overallGrowthPercent ?: 0.0
        }
    }

    val isSpendingDecreased: Boolean = remember(selectedRange, oneYearSummary, summary) {
        when (selectedRange) {
            TrendRange.ONE_YEAR -> oneYearSummary?.isSpendingDecreased ?: (summary?.isSpendingDecreased ?: false)
            TrendRange.SIX_MONTHS -> summary?.isSpendingDecreased ?: false
        }
    }

    var activeIndex by remember(data) { mutableIntStateOf(data.size - 1) }
    var isAreaFillVisible by remember { mutableStateOf(true) }

    val maxVal = (data.maxOfOrNull { it.totalSpent } ?: 1000.0).coerceAtLeast(100.0)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
    }

    val primaryColor = Color(0xFF0D9488) // Recharts signature Teal
    val secondaryColor = Color(0xFF6366F1) // Indigo
    val avgLineColor = Color(0xFFF59E0B) // Amber
    val growthGreen = Color(0xFF10B981) // Emerald
    val growthRed = Color(0xFFEF4444) // Coral Red
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val safeActiveIndex = activeIndex.coerceIn(0, (data.size - 1).coerceAtLeast(0))
    val activeItem = data.getOrNull(safeActiveIndex) ?: data.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_monthly_trend_container")
    ) {
        // 1. Header with Controls, Time Range & Monthly Reports Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedRange == TrendRange.ONE_YEAR) "1-Year Spending Trends" else "6-Month Spending Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Interactive financial growth line chart",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // All Monthly Reports Button
                if (onViewAllReports != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clickable { onViewAllReports() }
                            .testTag("view_all_monthly_reports_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "All Monthly Reports",
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Reports",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }

                // Line vs Area View Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isAreaFillVisible) primaryColor else Color.Transparent)
                                .clickable { isAreaFillVisible = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("trend_line_mode_button")
                        ) {
                            Text(
                                text = "Line",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (!isAreaFillVisible) Color.Black else onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAreaFillVisible) primaryColor else Color.Transparent)
                                .clickable { isAreaFillVisible = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("trend_area_mode_button")
                        ) {
                            Text(
                                text = "Area",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAreaFillVisible) Color.Black else onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Financial Growth Stat Cards Row & Range Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period Average Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (selectedRange == TrendRange.ONE_YEAR) "1-Year Average" else "6-Mo Average",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = ExpenseEntity.formatInr(avgSpend),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = avgLineColor
                    )
                }
            }

            // Financial Growth Status Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSpendingDecreased) growthGreen.copy(alpha = 0.12f) else growthRed.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    if (isSpendingDecreased) growthGreen.copy(alpha = 0.35f) else growthRed.copy(alpha = 0.35f)
                ),
                modifier = Modifier.weight(1.1f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isSpendingDecreased) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (isSpendingDecreased) growthGreen else growthRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Financial Growth",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                        val formattedGrowth = String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(overallGrowthPercent))
                        Text(
                            text = if (isSpendingDecreased) "-$formattedGrowth% Spend" else "+$formattedGrowth% Spend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpendingDecreased) growthGreen else growthRed
                        )
                    }
                }
            }

            // Range Selector Pills: 1Y vs 6M
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedRange == TrendRange.ONE_YEAR) primaryColor else Color.Transparent)
                            .clickable { selectedRange = TrendRange.ONE_YEAR }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("trend_range_1y")
                    ) {
                        Text(
                            text = "1Y",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRange == TrendRange.ONE_YEAR) Color.Black else onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedRange == TrendRange.SIX_MONTHS) primaryColor else Color.Transparent)
                            .clickable { selectedRange = TrendRange.SIX_MONTHS }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("trend_range_6m")
                    ) {
                        Text(
                            text = "6M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRange == TrendRange.SIX_MONTHS) Color.Black else onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Recharts Floating Tooltip for Active Month with "View Monthly Report" Action
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_trend_tooltip")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                            Text(
                                text = activeItem.fullMonthName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${activeItem.transactionCount} txs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (safeActiveIndex > 0) {
                            val momChange = activeItem.momChangePercent
                            val momStr = String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(momChange))
                            Text(
                                text = if (momChange <= 0) "🔻 $momStr% lower than previous month" else "🔺 $momStr% higher than previous month",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (momChange <= 0) growthGreen else growthRed
                            )
                        } else {
                            Text(
                                text = "Baseline start of trend period",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ExpenseEntity.formatInr(activeItem.totalSpent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        val diffAvg = activeItem.totalSpent - avgSpend
                        val diffStr = ExpenseEntity.formatInr(kotlin.math.abs(diffAvg))
                        Text(
                            text = if (diffAvg <= 0) "$diffStr below avg" else "$diffStr above avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (diffAvg <= 0) growthGreen else avgLineColor
                        )
                    }
                }

                // Interactive "View Monthly Report" Action for Active Month
                if (onViewMonthlyReport != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor,
                            modifier = Modifier
                                .clickable { onViewMonthlyReport(activeItem.year, activeItem.month) }
                                .testTag("view_monthly_report_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = "View Report",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "View ${activeItem.shortMonth} Report",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Recharts Interactive Canvas (CartesianGrid, ReferenceLine, Monotone Line & Gradient Area)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("monthly_trend_line_chart")
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val count = data.size
                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                        val tappedIdx = (offset.x / stepX + 0.5f).toInt().coerceIn(0, count - 1)
                        activeIndex = tappedIdx
                    }
                }
        ) {
            val count = data.size
            if (count < 2) return@Canvas

            val chartPaddingTop = 20.dp.toPx()
            val chartPaddingBottom = 30.dp.toPx()
            val chartPaddingStart = 16.dp.toPx()
            val chartPaddingEnd = 16.dp.toPx()

            val plotWidth = size.width - chartPaddingStart - chartPaddingEnd
            val plotHeight = size.height - chartPaddingTop - chartPaddingBottom
            val stepX = plotWidth / (count - 1)

            val effectiveMax = (maxVal * 1.15).coerceAtLeast(100.0)

            // A. Recharts CartesianGrid: Horizontal dashed lines
            val gridLineCount = 4
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            for (g in 0..gridLineCount) {
                val gridY = chartPaddingTop + (plotHeight * (g.toFloat() / gridLineCount))
                drawLine(
                    color = onSurface.copy(alpha = 0.08f),
                    start = Offset(chartPaddingStart, gridY),
                    end = Offset(size.width - chartPaddingEnd, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // B. Recharts ReferenceLine: Period Average Line
            if (avgSpend > 0) {
                val avgY = chartPaddingTop + plotHeight - ((avgSpend / effectiveMax).toFloat() * plotHeight * progress.value)
                if (avgY in chartPaddingTop..(chartPaddingTop + plotHeight)) {
                    drawLine(
                        color = avgLineColor.copy(alpha = 0.75f),
                        start = Offset(chartPaddingStart, avgY),
                        end = Offset(size.width - chartPaddingEnd, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // Reference label on canvas
                    val avgTag = if (selectedRange == TrendRange.ONE_YEAR) "1Y AVG" else "6M AVG"
                    val avgPaint = NativePaint().apply {
                        color = android.graphics.Color.rgb(245, 158, 11)
                        textSize = 22f
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(avgTag, size.width - chartPaddingEnd - 65f, avgY - 6f, avgPaint)
                }
            }

            // C. Calculate Points for the active months
            val points = data.mapIndexed { idx, item ->
                val x = chartPaddingStart + (idx * stepX)
                val y = chartPaddingTop + plotHeight - ((item.totalSpent / effectiveMax).toFloat() * plotHeight * progress.value)
                Offset(x, y)
            }

            // D. Draw Smooth Monotone Bézier Curve & Gradient Area
            val path = Path()
            val fillPath = Path()

            path.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, chartPaddingTop + plotHeight)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, chartPaddingTop + plotHeight)
            fillPath.close()

            // Draw Area Fill if enabled
            if (isAreaFillVisible) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            secondaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = chartPaddingTop,
                        endY = chartPaddingTop + plotHeight
                    )
                )
            }

            // Draw Line Stroke
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // E. Active Cursor Line on selected point
            val activePoint = points.getOrNull(safeActiveIndex)
            if (activePoint != null) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.45f),
                    start = Offset(activePoint.x, chartPaddingTop),
                    end = Offset(activePoint.x, chartPaddingTop + plotHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            }

            // F. Draw Data Dots and X-Axis Labels
            val textPaint = NativePaint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = if (count > 6) 22f else 28f
                textAlign = NativePaint.Align.CENTER
                isAntiAlias = true
            }

            val activeTextPaint = NativePaint().apply {
                color = android.graphics.Color.WHITE
                textSize = if (count > 6) 24f else 30f
                isFakeBoldText = true
                textAlign = NativePaint.Align.CENTER
                isAntiAlias = true
            }

            points.forEachIndexed { idx, pt ->
                val isSelected = idx == safeActiveIndex

                if (isSelected) {
                    // Outer glow ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = pt
                    )
                    // Inner active dot
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = pt
                    )
                } else {
                    // Normal dot
                    drawCircle(
                        color = surfaceColor,
                        radius = if (count > 6) 4.dp.toPx() else 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = if (count > 6) 3.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                }

                // X-Axis month label
                val labelY = chartPaddingTop + plotHeight + 22.dp.toPx()
                val monthLabel = data[idx].shortMonth
                drawContext.canvas.nativeCanvas.drawText(
                    monthLabel,
                    pt.x,
                    labelY,
                    if (isSelected) activeTextPaint else textPaint
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Month Selectors / Scrubbing Bar (Horizontal scroll for 12 months)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trend_months_lazy_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(data) { idx, item ->
                val isSelected = idx == safeActiveIndex
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable { activeIndex = idx }
                        .testTag("trend_month_selector_${item.shortMonth.lowercase()}")
                ) {
                    Text(
                        text = item.shortMonth,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

