package com.example.ui.components

import android.graphics.Paint as NativePaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySummary
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedTertiary
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Recharts visualization modes for the Daily Trend and Category Breakdown Dashboard
 */
enum class RechartsDashboardTab(val label: String, val iconDescription: String) {
    COMBINED("Overview", "Split line & category breakdown"),
    DAILY_LINE("Daily Curve", "Interactive spline line with average reference"),
    CATEGORY_PIE("Categories", "Recharts interactive slice & donut breakdown")
}

data class RechartsDailyPoint(
    val dayNumber: Int,
    val dayLabel: String,
    val fullDate: String,
    val timestamp: Long,
    val totalAmount: Double,
    val transactionCount: Int,
    val topItemTitle: String = "",
    val topItemAmount: Double = 0.0
)

/**
 * Recharts-inspired Dashboard Component rendering an interactive line chart of daily spending patterns
 * and an interactive pie/donut chart for expense categorization over the current month.
 */
@Composable
fun RechartsDailyTrendAndCategoryDashboard(
    monthExpenses: List<ExpenseEntity>,
    categorySummaries: List<CategorySummary>,
    modifier: Modifier = Modifier,
    onNavigateToAnalytics: (() -> Unit)? = null,
    onNavigateToTransactions: (() -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null
) {
    val cal = remember { Calendar.getInstance() }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    val currentDayOfMonth = remember { cal.get(Calendar.DAY_OF_MONTH) }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }

    var selectedTab by remember { mutableStateOf(RechartsDashboardTab.COMBINED) }
    var isDonutStyle by remember { mutableStateOf(true) }

    // Aggregate daily data points for current month (days 1..daysInMonth)
    val dailyPoints = remember(monthExpenses, daysInMonth) {
        val sdfLabel = SimpleDateFormat("dd MMM", Locale.getDefault())
        val sdfFull = SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault())

        val dayGroups = monthExpenses.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_MONTH)
        }

        (1..daysInMonth).map { day ->
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val txs = dayGroups[day] ?: emptyList()
            val total = txs.sumOf { it.amount }
            val topTx = txs.maxByOrNull { it.amount }

            RechartsDailyPoint(
                dayNumber = day,
                dayLabel = "$day",
                fullDate = sdfFull.format(dayCal.time),
                timestamp = dayCal.timeInMillis,
                totalAmount = total,
                transactionCount = txs.size,
                topItemTitle = topTx?.title ?: "",
                topItemAmount = topTx?.amount ?: 0.0
            )
        }
    }

    val totalMonthSpent = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val activeSpendingDays = remember(dailyPoints) { dailyPoints.count { it.totalAmount > 0 } }
    val dailyAverage = remember(totalMonthSpent, currentDayOfMonth) {
        if (currentDayOfMonth > 0) totalMonthSpent / currentDayOfMonth else 0.0
    }
    val highestDay = remember(dailyPoints) { dailyPoints.maxByOrNull { it.totalAmount } }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(1.5.dp, SophisticatedDarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_daily_line_chart_dashboard")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: Recharts Branding, Month Tag & Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SophisticatedPrimary.copy(alpha = 0.25f),
                                        SophisticatedTertiary.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Recharts",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Monthly Spending Patterns",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SophisticatedPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Recharts",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$currentMonthName • Daily curve & category distribution",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                if (onNavigateToAnalytics != null) {
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Full Analytics",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Quick KPI Strip (Total Outflow, Daily Avg, Highest Day)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KPI 1: Total Spent
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total Outflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ExpenseEntity.formatInr(totalMonthSpent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${monthExpenses.size} txs",
                            fontSize = 10.sp,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                // KPI 2: Daily Average
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Daily Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ExpenseEntity.formatInr(dailyAverage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                        Text(
                            text = "Day $currentDayOfMonth of $daysInMonth",
                            fontSize = 10.sp,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                // KPI 3: Peak Spending Day
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Peak Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (highestDay != null && highestDay.totalAmount > 0) ExpenseEntity.formatInr(highestDay.totalAmount) else "₹0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                        Text(
                            text = if (highestDay != null && highestDay.totalAmount > 0) "Day ${highestDay.dayNumber}" else "None",
                            fontSize = 10.sp,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            // 3. Tab Pill Switcher (Combined Overview vs Daily Line vs Category Pie)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophisticatedDarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RechartsDashboardTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SophisticatedPrimary else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 6.dp)
                            .testTag("recharts_tab_${tab.name.lowercase(Locale.ROOT)}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            // 4. Content Area Based on Selected Tab
            when (selectedTab) {
                RechartsDashboardTab.COMBINED -> {
                    // Combined View: Daily Line Chart at top, followed by Category Distribution below
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RechartsDailySpendingLineChart(
                            points = dailyPoints,
                            dailyAverage = dailyAverage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )

                        RechartsCategoryPieSection(
                            categorySummaries = categorySummaries,
                            isDonutStyle = isDonutStyle,
                            onToggleDonutStyle = { isDonutStyle = it },
                            onCategoryClick = onCategoryClick
                        )
                    }
                }

                RechartsDashboardTab.DAILY_LINE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Outflow Curve & Scrubber",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SophisticatedPrimary)
                                )
                                Text("Spend Curve", fontSize = 11.sp, color = SophisticatedDarkOnSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AmberGold)
                                )
                                Text("Daily Avg", fontSize = 11.sp, color = SophisticatedDarkOnSurfaceVariant)
                            }
                        }

                        RechartsDailySpendingLineChart(
                            points = dailyPoints,
                            dailyAverage = dailyAverage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    }
                }

                RechartsDashboardTab.CATEGORY_PIE -> {
                    RechartsCategoryPieSection(
                        categorySummaries = categorySummaries,
                        isDonutStyle = isDonutStyle,
                        onToggleDonutStyle = { isDonutStyle = it },
                        onCategoryClick = onCategoryClick
                    )
                }
            }
        }
    }
}

/**
 * Interactive Recharts-styled Line & Area Chart for Daily Spending Patterns
 */
@Composable
fun RechartsDailySpendingLineChart(
    points: List<RechartsDailyPoint>,
    dailyAverage: Double,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(SophisticatedDarkSurfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No transactions recorded for this month", color = SophisticatedDarkOnSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    val maxAmount = remember(points) {
        val peak = points.maxOfOrNull { it.totalAmount } ?: 0.0
        if (peak <= 0.0) 1000.0 else peak * 1.15
    }

    // Animation progress for path entry
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
    }

    // Touch scrubber state (interactive Recharts tooltip)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var touchXPosition by remember { mutableStateOf<Float?>(null) }

    val activePoint = if (selectedIndex in points.indices) points[selectedIndex] else null

    Column(modifier = Modifier.fillMaxWidth()) {
        // Active Scrubber Tooltip Banner
        AnimatedVisibility(
            visible = activePoint != null && activePoint.totalAmount > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (activePoint != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("recharts_daily_tooltip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activePoint.fullDate,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedPrimary
                            )
                            if (activePoint.topItemTitle.isNotBlank()) {
                                Text(
                                    text = "Top: ${activePoint.topItemTitle} (${ExpenseEntity.formatInr(activePoint.topItemAmount)})",
                                    fontSize = 10.sp,
                                    color = SophisticatedDarkOnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = ExpenseEntity.formatInr(activePoint.totalAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "${activePoint.transactionCount} entries",
                                fontSize = 10.sp,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Canvas Chart
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SophisticatedDarkSurfaceVariant)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchXPosition = offset.x
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            touchXPosition = change.position.x
                        },
                        onDragEnd = {
                            // Keep last scrub point for 3 seconds or retain
                        },
                        onDragCancel = {
                            touchXPosition = null
                            selectedIndex = -1
                        }
                    )
                }
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        touchXPosition = offset.x
                    }
                }
                .testTag("recharts_daily_line_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val leftPad = 48f
                val rightPad = 24f
                val topPad = 28f
                val bottomPad = 32f

                val chartWidth = size.width - leftPad - rightPad
                val chartHeight = size.height - topPad - bottomPad

                if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                // 1. Cartesian Grid Lines (Recharts style: 3 dashed horizontal reference lines)
                val gridPaint = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                val gridLineColor = Color(0xFF334155).copy(alpha = 0.6f)
                val textNativePaint = NativePaint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 20f
                    isAntiAlias = true
                }

                val gridLevels = listOf(1.0f, 0.66f, 0.33f, 0.0f)
                for (lvl in gridLevels) {
                    val y = topPad + chartHeight * (1f - lvl)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1f,
                        pathEffect = gridPaint
                    )
                    val labelVal = maxAmount * lvl
                    val labelText = if (labelVal >= 1000) "₹${(labelVal / 1000).toInt()}k" else "₹${labelVal.toInt()}"
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        8f,
                        y + 6f,
                        textNativePaint
                    )
                }

                // 2. Average Reference Line (Dashed Amber Line - Recharts <ReferenceLine />)
                if (dailyAverage > 0 && dailyAverage <= maxAmount) {
                    val avgY = topPad + chartHeight * (1f - (dailyAverage / maxAmount).toFloat())
                    drawLine(
                        color = AmberGold.copy(alpha = 0.8f),
                        start = Offset(leftPad, avgY),
                        end = Offset(size.width - rightPad, avgY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                // 3. Compute coordinates for daily curve
                val coords = points.mapIndexed { index, point ->
                    val x = leftPad + index * stepX
                    val ratio = (point.totalAmount / maxAmount).toFloat()
                    val y = topPad + chartHeight * (1f - (ratio * animProgress.value))
                    Offset(x, y)
                }

                // Update selectedIndex based on touch position
                touchXPosition?.let { tx ->
                    val clampedX = tx.coerceIn(leftPad, leftPad + chartWidth)
                    val idx = ((clampedX - leftPad) / stepX).roundToInt().coerceIn(0, points.size - 1)
                    selectedIndex = idx
                }

                // 4. Construct Smooth Bézier Curve Path (Recharts spline)
                val linePath = Path()
                val areaPath = Path()

                if (coords.isNotEmpty()) {
                    linePath.moveTo(coords.first().x, coords.first().y)
                    areaPath.moveTo(coords.first().x, topPad + chartHeight)
                    areaPath.lineTo(coords.first().x, coords.first().y)

                    for (i in 0 until coords.size - 1) {
                        val current = coords[i]
                        val next = coords[i + 1]
                        val controlPoint1 = Offset(current.x + (next.x - current.x) / 2f, current.y)
                        val controlPoint2 = Offset(current.x + (next.x - current.x) / 2f, next.y)

                        linePath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            next.x, next.y
                        )
                        areaPath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            next.x, next.y
                        )
                    }

                    areaPath.lineTo(coords.last().x, topPad + chartHeight)
                    areaPath.close()

                    // Draw Area Gradient (Recharts <Area fill="url(#colorUv)" />)
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                SophisticatedPrimary.copy(alpha = 0.45f),
                                SophisticatedPrimary.copy(alpha = 0.02f)
                            ),
                            startY = topPad,
                            endY = topPad + chartHeight
                        )
                    )

                    // Draw Smooth Line (Recharts <Line type="monotone" stroke="#6366f1" />)
                    drawPath(
                        path = linePath,
                        color = SophisticatedPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 5. Draw Active Scrubber Dot and Vertical Line Cursor
                if (selectedIndex in coords.indices) {
                    val activeCoord = coords[selectedIndex]

                    // Vertical Cursor Line
                    drawLine(
                        color = SophisticatedPrimary.copy(alpha = 0.6f),
                        start = Offset(activeCoord.x, topPad),
                        end = Offset(activeCoord.x, topPad + chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // Outer Halo
                    drawCircle(
                        color = SophisticatedPrimary.copy(alpha = 0.25f),
                        radius = 10.dp.toPx(),
                        center = activeCoord
                    )
                    // Solid Point
                    drawCircle(
                        color = SophisticatedPrimary,
                        radius = 5.dp.toPx(),
                        center = activeCoord
                    )
                    // Center White Core
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = activeCoord
                    )
                }

                // 6. X-Axis Day Labels (every 5 days)
                val dayLabelPaint = NativePaint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 20f
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }

                for (i in points.indices) {
                    val dayNum = points[i].dayNumber
                    if (dayNum == 1 || dayNum % 5 == 0 || dayNum == points.size) {
                        val x = leftPad + i * stepX
                        drawContext.canvas.nativeCanvas.drawText(
                            "$dayNum",
                            x,
                            size.height - 8f,
                            dayLabelPaint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Interactive Pie / Donut Chart for Expense Categorization
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsCategoryPieSection(
    categorySummaries: List<CategorySummary>,
    isDonutStyle: Boolean,
    onToggleDonutStyle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null
) {
    if (categorySummaries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(SophisticatedDarkSurfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No category records available for this month", color = SophisticatedDarkOnSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val totalAmount = remember(categorySummaries) { categorySummaries.sumOf { it.totalAmount } }
    val activeItem = categorySummaries.getOrNull(selectedIndex) ?: categorySummaries.first()

    // Smooth entry animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySummaries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Controls (Title & Donut/Pie Toggle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Expense Categorization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )
                Text(
                    text = "Tap slices or legend below to explore",
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Pie vs Donut Toggle Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDonutStyle) SophisticatedPrimary else Color.Transparent)
                            .clickable { onToggleDonutStyle(true) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("recharts_toggle_donut")
                    ) {
                        Text(
                            text = "Donut",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDonutStyle) Color.Black else SophisticatedDarkOnSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isDonutStyle) SophisticatedPrimary else Color.Transparent)
                            .clickable { onToggleDonutStyle(false) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("recharts_toggle_pie")
                    ) {
                        Text(
                            text = "Pie",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!isDonutStyle) Color.Black else SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // Circular Chart & Central Callout Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(categorySummaries) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVector = offset - center
                            var angle = Math.toDegrees(atan2(touchVector.y.toDouble(), touchVector.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var cumulative = 0f
                            for ((index, item) in categorySummaries.withIndex()) {
                                val sweep = if (totalAmount > 0) (item.totalAmount / totalAmount).toFloat() * 360f else 0f
                                if (angle >= cumulative && angle < cumulative + sweep) {
                                    selectedIndex = index
                                    break
                                }
                                cumulative += sweep
                            }
                        }
                    }
                    .testTag("recharts_category_pie_canvas")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.width / 2f - 16.dp.toPx()
                val strokeWidth = if (isDonutStyle) 34.dp.toPx() else baseRadius

                var startAngle = -90f

                for ((index, item) in categorySummaries.withIndex()) {
                    val sweepAngle = if (totalAmount > 0) {
                        ((item.totalAmount / totalAmount).toFloat() * 360f) * animProgress.value
                    } else 0f

                    val isSelected = index == selectedIndex
                    val sliceColor = ChartColors[index % ChartColors.size]

                    // Subtle slice expansion on selection (Recharts ActiveShape behavior)
                    val radiusOffset = if (isSelected) 6.dp.toPx() else 0f
                    val bisectorAngle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                    val shiftOffset = if (isSelected) {
                        Offset(
                            (cos(bisectorAngle) * 5.dp.toPx()).toFloat(),
                            (sin(bisectorAngle) * 5.dp.toPx()).toFloat()
                        )
                    } else Offset.Zero

                    val arcCenter = center + shiftOffset
                    val drawRadius = baseRadius + radiusOffset

                    if (isDonutStyle) {
                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(arcCenter.x - drawRadius, arcCenter.y - drawRadius),
                            size = Size(drawRadius * 2, drawRadius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    } else {
                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(arcCenter.x - drawRadius, arcCenter.y - drawRadius),
                            size = Size(drawRadius * 2, drawRadius * 2)
                        )
                    }

                    startAngle += sweepAngle
                }
            }

            // Central Detail for Donut mode
            if (isDonutStyle) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = activeItem.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ExpenseEntity.formatInr(activeItem.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SophisticatedPrimary
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", activeItem.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }
        }

        // Selected Category Callout Card with Filter Action
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SophisticatedDarkSurfaceVariant,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val color = ChartColors[selectedIndex % ChartColors.size]
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Column {
                        Text(
                            text = activeItem.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${activeItem.transactionCount} transactions recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ExpenseEntity.formatInr(activeItem.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )

                    if (onCategoryClick != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SophisticatedPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onCategoryClick(activeItem.category) }
                        ) {
                            Text(
                                text = "View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recharts Interactive Legend (Chips with color dots and percentages)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categorySummaries.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val color = ChartColors[index % ChartColors.size]

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SophisticatedDarkSurface else SophisticatedDarkSurfaceDim,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) color else SophisticatedDarkBorder
                    ),
                    modifier = Modifier
                        .clickable { selectedIndex = index }
                        .testTag("recharts_pie_slice_item_${item.category.lowercase(Locale.ROOT)}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SophisticatedDarkOnSurface else SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", item.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
