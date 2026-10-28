package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportTransactionsDialog(
    viewModel: ExpenseViewModel,
    filteredExpenses: List<ExpenseEntity>,
    allExpenses: List<ExpenseEntity>,
    onOpenMonthlyReports: () -> Unit,
    onOpenCustomDateReport: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var exportAll by remember { mutableStateOf(false) }

    val activeExpenses = if (exportAll) allExpenses else filteredExpenses
    val totalAmount = activeExpenses.sumOf { it.amount }
    val scopeLabel = if (exportAll) "All Transactions" else "Current Filtered View"

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SophisticatedDarkSurfaceDim,
            border = BorderStroke(1.dp, SophisticatedDarkBorder),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("export_transactions_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                        Surface(
                            shape = CircleShape,
                            color = SophisticatedPrimary.copy(alpha = 0.18f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Export Transaction History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Text(
                                text = "Share with accountant or save for record-keeping",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("export_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope Selector Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedDarkSurface,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SELECT DATA SCOPE TO EXPORT:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !exportAll,
                                onClick = { exportAll = false },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = "Filtered View (${filteredExpenses.size})",
                                            fontWeight = if (!exportAll) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = ExpenseEntity.formatInr(filteredExpenses.sumOf { it.amount }),
                                            fontSize = 11.sp,
                                            color = if (!exportAll) Color.Black else SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SophisticatedPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SophisticatedDarkSurfaceVariant,
                                    labelColor = SophisticatedDarkOnSurface
                                ),
                                border = BorderStroke(1.dp, if (!exportAll) SophisticatedPrimary else SophisticatedDarkBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scope_chip_filtered")
                            )

                            FilterChip(
                                selected = exportAll,
                                onClick = { exportAll = true },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = "All Items (${allExpenses.size})",
                                            fontWeight = if (exportAll) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = ExpenseEntity.formatInr(allExpenses.sumOf { it.amount }),
                                            fontSize = 11.sp,
                                            color = if (exportAll) Color.Black else SophisticatedDarkOnSurfaceVariant
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SophisticatedPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SophisticatedDarkSurfaceVariant,
                                    labelColor = SophisticatedDarkOnSurface
                                ),
                                border = BorderStroke(1.dp, if (exportAll) SophisticatedPrimary else SophisticatedDarkBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scope_chip_all")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Exporting $scopeLabel: ${activeExpenses.size} items totalling ${ExpenseEntity.formatInr(totalAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Export Formats
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. PDF Document Card
                    item {
                        ExportFormatCard(
                            badgeText = "OFFICIAL STATEMENT / TAX & AUDIT",
                            badgeColor = DangerRed,
                            icon = Icons.Default.PictureAsPdf,
                            iconTint = DangerRed,
                            title = "PDF Financial Statement (.pdf)",
                            description = "Print-ready A4 document with summary metrics, category budget adherence, and detailed transaction ledger with notes and currency in ₹ INR.",
                            onDownload = {
                                val pdfFile = viewModel.exportPdfReport(
                                    title = "Statement — $scopeLabel",
                                    expensesList = activeExpenses
                                )
                                val (saved, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    pdfFile,
                                    "application/pdf"
                                )
                                Toast.makeText(
                                    context,
                                    if (saved) "Saved PDF to Downloads: ${pdfFile.name}" else msg,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onShare = {
                                val pdfFile = viewModel.exportPdfReport(
                                    title = "Statement — $scopeLabel",
                                    expensesList = activeExpenses
                                )
                                ReportExportManager.shareFile(
                                    context,
                                    pdfFile,
                                    "application/pdf",
                                    "Financial Statement PDF ($scopeLabel)"
                                )
                            },
                            downloadTag = "export_pdf_download_button",
                            shareTag = "export_pdf_share_button"
                        )
                    }

                    // 2. CSV Spreadsheet Card
                    item {
                        ExportFormatCard(
                            badgeText = "EXCEL / GOOGLE SHEETS / ACCOUNTANT DATA",
                            badgeColor = SophisticatedPrimary,
                            icon = Icons.Default.Description,
                            iconTint = SophisticatedPrimary,
                            title = "Standard CSV Spreadsheet (.csv)",
                            description = "UTF-8 comma-separated format with standard headers (ID, Date, Title, Amount, Category, Payment Method, Bank, Credit Card, Tags, Notes) for direct accountant software import.",
                            onDownload = {
                                val csvFile = viewModel.exportCsvReport(
                                    prefix = if (exportAll) "All_Transactions" else "Filtered_Transactions",
                                    expensesList = activeExpenses
                                )
                                val (saved, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    csvFile,
                                    "text/csv"
                                )
                                Toast.makeText(
                                    context,
                                    if (saved) "Saved CSV to Downloads: ${csvFile.name}" else msg,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onShare = {
                                val csvFile = viewModel.exportCsvReport(
                                    prefix = if (exportAll) "All_Transactions" else "Filtered_Transactions",
                                    expensesList = activeExpenses
                                )
                                ReportExportManager.shareFile(
                                    context,
                                    csvFile,
                                    "text/csv",
                                    "Expense Ledger CSV ($scopeLabel)"
                                )
                            },
                            downloadTag = "export_csv_download_button",
                            shareTag = "export_csv_share_button"
                        )
                    }

                    // 3. Excel Workbook Card
                    item {
                        ExportFormatCard(
                            badgeText = "STYLED WORKBOOK / DASHBOARD",
                            badgeColor = SuccessGreen,
                            icon = Icons.Default.TableChart,
                            iconTint = SuccessGreen,
                            title = "Excel Workbook (.xlsx)",
                            description = "Formatted spreadsheet with auto-styled columns, totals formulas, and category summaries for Microsoft Excel or Google Sheets.",
                            onDownload = {
                                val excelFile = if (exportAll) {
                                    viewModel.exportFullExcelDashboard()
                                } else {
                                    viewModel.exportExcelSpreadsheet()
                                }
                                val (saved, msg) = ReportExportManager.saveToDownloads(
                                    context,
                                    excelFile,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                Toast.makeText(
                                    context,
                                    if (saved) "Saved Excel to Downloads: ${excelFile.name}" else msg,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onShare = {
                                val excelFile = if (exportAll) {
                                    viewModel.exportFullExcelDashboard()
                                } else {
                                    viewModel.exportExcelSpreadsheet()
                                }
                                ReportExportManager.shareFile(
                                    context,
                                    excelFile,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "Expense Ledger Excel ($scopeLabel)"
                                )
                            },
                            downloadTag = "export_excel_download_button",
                            shareTag = "export_excel_share_button"
                        )
                    }

                    // 4. Quick Shortcuts to Monthly & Date Range Reports
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SophisticatedDarkSurface,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "OTHER SPECIALIZED REPORTS:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismissRequest()
                                            onOpenMonthlyReports()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_monthly_reports_shortcut_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = SophisticatedPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Monthly Report", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onDismissRequest()
                                            onOpenCustomDateReport()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_custom_date_report_shortcut_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Date Range", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    downloadTag: String,
    shareTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SophisticatedDarkSurface,
        border = BorderStroke(1.dp, SophisticatedDarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SophisticatedDarkOnSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download button (Save for record-keeping)
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag(downloadTag),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SophisticatedDarkBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Downloads",
                        tint = SophisticatedDarkOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Downloads",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedDarkOnSurface
                    )
                }

                // Share button (Send to accountant / app)
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(shareTag),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = iconTint.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                }
            }
        }
    }
}
