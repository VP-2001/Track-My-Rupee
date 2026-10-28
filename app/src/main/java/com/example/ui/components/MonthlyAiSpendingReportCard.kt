package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.export.ReportExportManager
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonthlyAiSpendingReport
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
import com.example.ui.viewmodel.AvailableMonth
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.MonthlyAiReportUiState
import java.util.Calendar

/**
 * Interactive card that uses Gemini AI to generate and display a comprehensive
 * natural language summary report of the user's spending habits at the end of each month.
 */
@Composable
fun MonthlyAiSpendingReportCard(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    initialMonth: Int? = null,
    initialYear: Int? = null
) {
    val context = LocalContext.current
    val reportState by viewModel.monthlyAiReportState.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()

    val currentCal = remember { Calendar.getInstance() }
    var selectedMonthIndex by remember { mutableStateOf(0) } // 0 is latest/current month

    // Active month being evaluated
    val activeMonth: AvailableMonth? = availableMonths.getOrNull(selectedMonthIndex)
    val activeMonthYear = activeMonth?.year ?: currentCal.get(Calendar.YEAR)
    val activeMonthNum = activeMonth?.month ?: (currentCal.get(Calendar.MONTH) + 1)
    val activeMonthName = activeMonth?.label ?: "This Month"

    // Checked commitments map
    val checkedTips = remember { mutableStateMapOf<Int, Boolean>() }

    // PDF Export Sheet state
    var exportPdfSheetData by remember { mutableStateOf<Pair<MonthlyAiSpendingReport, File>?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SophisticatedDarkSurfaceDim,
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    SophisticatedPrimary.copy(alpha = 0.6f),
                    SophisticatedTertiary.copy(alpha = 0.4f),
                    SophisticatedPrimary.copy(alpha = 0.2f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_ai_spending_report_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Gemini Badge & Month Selector
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
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
                                text = "Gemini Monthly Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SophisticatedPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Gemini 3.8",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Natural language spending habits & review",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                if (reportState is MonthlyAiReportUiState.Success) {
                    val currentReport = (reportState as MonthlyAiReportUiState.Success).report
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val pdfFile = viewModel.exportAiReportPdf(currentReport)
                                exportPdfSheetData = Pair(currentReport, pdfFile)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("header_export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.generateMonthlyAiSpendingReport(
                                    year = activeMonthYear,
                                    month = activeMonthNum,
                                    forceRefresh = true
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("refresh_ai_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = SophisticatedDarkOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Month selection chips if multiple months available
            if (availableMonths.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayMonths = availableMonths.take(4)
                    for (index in displayMonths.indices) {
                        val m = displayMonths[index]
                        FilterChip(
                            selected = selectedMonthIndex == index,
                            onClick = {
                                selectedMonthIndex = index
                                viewModel.generateMonthlyAiSpendingReport(
                                    year = m.year,
                                    month = m.month,
                                    forceRefresh = false
                                )
                            },
                            label = { Text(m.shortLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SophisticatedPrimary,
                                containerColor = SophisticatedDarkSurfaceVariant,
                                labelColor = SophisticatedDarkOnSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedMonthIndex == index,
                                borderColor = if (selectedMonthIndex == index) SophisticatedPrimary else SophisticatedDarkBorder
                            )
                        )
                    }
                }
            }

            // Content based on UI State
            when (val state = reportState) {
                is MonthlyAiReportUiState.Idle -> {
                    // Welcome & Prompt to Generate
                    ReportPromptCard(
                        monthName = activeMonthName,
                        onGenerate = {
                            viewModel.generateMonthlyAiSpendingReport(
                                year = activeMonthYear,
                                month = activeMonthNum,
                                forceRefresh = true
                            )
                        }
                    )
                }

                is MonthlyAiReportUiState.Loading -> {
                    // Loading State
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SophisticatedDarkSurfaceVariant,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = SophisticatedPrimary,
                                modifier = Modifier.size(38.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Gemini is analyzing your spending habits...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Evaluating ${state.monthName} category velocity, recurring bills, and budget discipline",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                is MonthlyAiReportUiState.Error -> {
                    // Error State
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Unable to generate summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.generateMonthlyAiSpendingReport(
                                        year = activeMonthYear,
                                        month = activeMonthNum,
                                        forceRefresh = true
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary),
                                modifier = Modifier.testTag("retry_ai_report_button")
                            ) {
                                Text("Retry Analysis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is MonthlyAiReportUiState.Success -> {
                    val report = state.report
                    ReportSuccessContent(
                        report = report,
                        checkedTips = checkedTips,
                        onToggleTip = { index ->
                            checkedTips[index] = !(checkedTips[index] ?: false)
                        },
                        onExportPdf = {
                            val pdfFile = viewModel.exportAiReportPdf(report)
                            exportPdfSheetData = Pair(report, pdfFile)
                        },
                        onShare = {
                            shareReport(context, report)
                        },
                        onCopy = {
                            copyReportToClipboard(context, report)
                        }
                    )
                }
            }
        }
    }

    if (exportPdfSheetData != null) {
        val (rep, file) = exportPdfSheetData!!
        ExportAiPdfModalBottomSheet(
            report = rep,
            pdfFile = file,
            onDismiss = { exportPdfSheetData = null }
        )
    }
}

@Composable
private fun ReportPromptCard(
    monthName: String,
    onGenerate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SophisticatedDarkSurfaceVariant,
        border = BorderStroke(1.dp, SophisticatedDarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SophisticatedPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = null,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "End-of-Month Financial Habits Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                    Text(
                        text = "Synthesizes your real Room DB records with Gemini AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            Text(
                text = "Get a personalized natural language executive summary of your spending personality for $monthName, including top expense drivers, recurring commitments ratio, and actionable savings steps for next month.",
                style = MaterialTheme.typography.bodySmall,
                color = SophisticatedDarkOnSurfaceVariant,
                lineHeight = 18.sp
            )

            Button(
                onClick = onGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("generate_ai_monthly_report_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate $monthName AI Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ReportSuccessContent(
    report: MonthlyAiSpendingReport,
    checkedTips: Map<Int, Boolean>,
    onToggleTip: (Int) -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Health Score & Comparison Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Health Score Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Financial Health Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val scoreColor = when {
                            report.spendingHabitRating >= 80 -> SuccessGreen
                            report.spendingHabitRating >= 65 -> AmberGold
                            else -> DangerRed
                        }
                        Text(
                            text = "${report.spendingHabitRating}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = report.spendingHabitRatingLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Month Comparison Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Month Outflow",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${"%,.0f".format(report.totalSpent)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.previousMonthComparisonText,
                        fontSize = 10.sp,
                        color = if (report.previousMonthComparisonText.contains("saved", ignoreCase = true)) SuccessGreen else SophisticatedDarkOnSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }
        }

        // 2. Executive Summary Quote Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SophisticatedPrimary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Executive Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.executiveSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SophisticatedDarkOnSurface,
                    lineHeight = 22.sp
                )
            }
        }

        // 3. Category Outflows & Habits Narrative
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedDarkSurfaceVariant,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = SophisticatedSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Category Habits & Drivers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.categoryHabitsInsight,
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurfaceVariant,
                    lineHeight = 19.sp
                )
            }
        }

        // 4. Recurring Commitments & Fixed Burden
        if (report.recurringImpactInsight.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = SophisticatedTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Recurring Bills & Subscriptions Impact",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.recurringImpactInsight,
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // 5. Behavioral Observations
        if (report.keyBehavioralObservations.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Behavioral Patterns & Timing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                    report.keyBehavioralObservations.forEach { obs ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "•",
                                color = SophisticatedPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = obs,
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Actionable Savings Advice with Checkable Commitments
        if (report.actionableSavingsTips.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Next Month Savings Commitments",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                    }

                    report.actionableSavingsTips.forEachIndexed { index, tip ->
                        val isChecked = checkedTips[index] ?: false
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) SuccessGreen.copy(alpha = 0.1f) else SophisticatedDarkSurfaceDim,
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) SuccessGreen.copy(alpha = 0.4f) else SophisticatedDarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTip(index) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleTip(index) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SuccessGreen,
                                        uncheckedColor = SophisticatedDarkOnSurfaceVariant
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChecked) SophisticatedDarkOnSurface else SophisticatedDarkOnSurfaceVariant,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Motivational Closing Thought
        if (report.encouragementQuote.isNotBlank()) {
            Text(
                text = report.encouragementQuote,
                style = MaterialTheme.typography.bodySmall,
                color = SophisticatedPrimary,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        // 8. Action Buttons (Export PDF / Share / Copy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportPdf,
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .testTag("export_ai_report_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("share_ai_report_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SophisticatedDarkSurfaceVariant,
                    contentColor = SophisticatedDarkOnSurface
                ),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier
                    .weight(0.9f)
                    .height(44.dp)
                    .testTag("copy_ai_report_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SophisticatedDarkSurfaceVariant,
                    contentColor = SophisticatedDarkOnSurface
                ),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = SophisticatedDarkOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun formatReportText(report: MonthlyAiSpendingReport): String {
    return buildString {
        appendLine("✨ GEMINI MONTHLY SPENDING REPORT: ${report.monthName} ${report.year}")
        appendLine("Health Score: ${report.spendingHabitRating}/100 (${report.spendingHabitRatingLabel})")
        appendLine("Total Outflow: ₹${"%,.2f".format(report.totalSpent)} (${report.transactionCount} transactions)")
        appendLine("Month Comparison: ${report.previousMonthComparisonText}")
        appendLine()
        appendLine("📌 EXECUTIVE SUMMARY:")
        appendLine(report.executiveSummary)
        appendLine()
        appendLine("📊 CATEGORY HABITS:")
        appendLine(report.categoryHabitsInsight)
        if (report.recurringImpactInsight.isNotBlank()) {
            appendLine()
            appendLine("🔁 RECURRING COMMITMENTS:")
            appendLine(report.recurringImpactInsight)
        }
        if (report.keyBehavioralObservations.isNotEmpty()) {
            appendLine()
            appendLine("🔍 KEY BEHAVIORAL PATTERNS:")
            report.keyBehavioralObservations.forEach { appendLine("• $it") }
        }
        if (report.actionableSavingsTips.isNotEmpty()) {
            appendLine()
            appendLine("💡 NEXT MONTH SAVINGS TARGETS:")
            report.actionableSavingsTips.forEach { appendLine("• $it") }
        }
        if (report.encouragementQuote.isNotBlank()) {
            appendLine()
            appendLine(report.encouragementQuote)
        }
        appendLine()
        appendLine("Generated with Gemini 3.8 Flash from secure on-device records.")
    }
}

private fun shareReport(context: Context, report: MonthlyAiSpendingReport) {
    val reportText = formatReportText(report)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Financial Spending Report - ${report.monthName} ${report.year}")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Spending Report"))
}

private fun copyReportToClipboard(context: Context, report: MonthlyAiSpendingReport) {
    val reportText = formatReportText(report)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Monthly Financial Report", reportText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportAiPdfModalBottomSheet(
    report: MonthlyAiSpendingReport,
    pdfFile: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SophisticatedDarkSurfaceDim,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        modifier = Modifier.testTag("export_ai_pdf_modal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                            .background(DangerRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export AI Spending Report PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${report.monthName} ${report.year} • Health Score: ${report.spendingHabitRating}/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_export_ai_pdf_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SophisticatedDarkOnSurfaceVariant
                    )
                }
            }

            // File Details Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedDarkSurfaceVariant,
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Document Format", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                        Text("Adobe PDF (.pdf)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("File Name", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                        Text(
                            text = if (pdfFile.name.length > 28) pdfFile.name.take(25) + "..." else pdfFile.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Approx. File Size", style = MaterialTheme.typography.bodySmall, color = SophisticatedDarkOnSurfaceVariant)
                        val sizeKb = (pdfFile.length() / 1024f).coerceAtLeast(1.0f)
                        Text("%.1f KB".format(sizeKb), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SophisticatedDarkOnSurface)
                    }
                }
            }

            // Status feedback banner
            if (saveStatusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = saveStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Action 1: Save to Downloads
            Button(
                onClick = {
                    isSaving = true
                    val (success, msg) = ReportExportManager.saveToDownloads(context, pdfFile, "application/pdf")
                    isSaving = false
                    saveStatusMessage = if (success) "Saved to Downloads folder: ${pdfFile.name}" else msg
                    Toast.makeText(context, if (success) "Saved to Downloads!" else msg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_ai_pdf_to_downloads_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Downloads Folder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Action 2: Share PDF via System Sheet
            Button(
                onClick = {
                    ReportExportManager.shareFile(
                        context = context,
                        file = pdfFile,
                        mimeType = "application/pdf",
                        title = "Gemini AI Spending Habits Report - ${report.monthName} ${report.year}"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_ai_pdf_file_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share PDF Document", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Action 3: Open / View PDF
            OutlinedButton(
                onClick = {
                    val opened = ReportExportManager.openFile(context, pdfFile, "application/pdf")
                    if (!opened) {
                        Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_ai_pdf_file_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SophisticatedDarkOnSurface
                ),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open / View PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

