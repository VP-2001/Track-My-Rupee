package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.components.EditCardSuggestionsDialog
import com.example.ui.components.ExportDateReportDialog
import com.example.ui.components.LocalBackupRestoreCard
import com.example.ui.viewmodel.ExpenseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle(emptyList())
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle(emptyList())
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val securityManager = viewModel.securityManager
    var isLockEnabled by remember { mutableStateOf(securityManager.isLockEnabled()) }
    val googleEmail by securityManager.googleUserEmail.collectAsStateWithLifecycle()
    val lastSyncTime by securityManager.lastSyncTime.collectAsStateWithLifecycle()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinText by remember { mutableStateOf("") }
    var showAccountDialog by remember { mutableStateOf(false) }
    var customAccountEmailInput by remember { mutableStateOf("") }
    var downloadResultSheet by remember { mutableStateOf<Pair<File, String>?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPurgeSampleDialog by remember { mutableStateOf(false) }
    var showCardSuggestionsDialog by remember { mutableStateOf(false) }
    var showExportDateReportDialog by remember { mutableStateOf(false) }
    var exportDateReportInitialTab by remember { mutableIntStateOf(0) }

    val totalSpent = allExpenses.sumOf { it.amount }

    fun handleDownload(file: File, mimeType: String, description: String) {
        val (success, msg) = ReportExportManager.saveToDownloads(context, file, mimeType)
        if (success) {
            Toast.makeText(context, "Downloaded: ${file.name}", Toast.LENGTH_LONG).show()
            downloadResultSheet = Pair(file, mimeType)
        } else {
            Toast.makeText(context, "Saved to cache: $msg", Toast.LENGTH_LONG).show()
            downloadResultSheet = Pair(file, mimeType)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Reports, Exports & Security",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Export financial summaries, manage biometric security, and sync across devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Hero Card: Local Encrypted Backup & Restore (Direct High Visibility)
        item {
            LocalBackupRestoreCard(
                viewModel = viewModel
            )
        }

        // 2. Master Excel Dashboard Export
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, com.example.ui.theme.SuccessGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = com.example.ui.theme.SuccessGreen.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = "Excel",
                                    tint = com.example.ui.theme.SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PERSONAL DASHBOARD READY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SuccessGreen
                                )
                            }
                        }

                        Text(
                            text = "${allExpenses.size} items",
                            style = MaterialTheme.typography.labelMedium,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Export Everything to Excel (.xlsx)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generates a complete multi-sheet workbook tailored for building your own personal finance dashboard in Microsoft Excel or Google Sheets. Includes enriched date dimensions (Year, Month, Day, Day-of-week) for pivot tables & charts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7 Included Sheets Badge Grid
                    Text(
                        text = "Included Sheets in Workbook:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1. KPIs", "2. Raw Ledger", "3. Categories", "4. Monthly").forEach { sheet ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = com.example.ui.theme.SophisticatedDarkSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sheet,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5. Payment Modes", "6. Tags", "7. Budgets").forEach { sheet ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = com.example.ui.theme.SophisticatedDarkSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sheet,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val excel = viewModel.exportFullExcelDashboard()
                                handleDownload(
                                    excel,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "Personal Finance Master Dashboard Excel"
                                )
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.SuccessGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Excel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download (.xlsx)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val excel = viewModel.exportFullExcelDashboard()
                                ReportExportManager.shareFile(
                                    context,
                                    excel,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "Personal Finance Master Dashboard Excel"
                                )
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = com.example.ui.theme.SophisticatedDarkSurface)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Period Excel Exports (All 18 Fields + KPIs):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportDateReportInitialTab = 0
                                showExportDateReportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reports_monthly_excel_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = com.example.ui.theme.SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Monthly Report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                exportDateReportInitialTab = 1
                                showExportDateReportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reports_custom_range_excel_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = com.example.ui.theme.SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custom Range", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. Export Formats Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Other Export Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SophisticatedDarkOnSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // PDF Button
                    ExportOptionRow(
                        title = "Print-Ready PDF Financial Report",
                        subtitle = "Includes summary cards, category budget tables & ledger in ₹ INR",
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = com.example.ui.theme.DangerRed,
                        onDownload = {
                            val pdf = viewModel.exportPdfReport()
                            handleDownload(pdf, "application/pdf", "Expense Report PDF")
                        },
                        onShare = {
                            val pdf = viewModel.exportPdfReport()
                            ReportExportManager.shareFile(context, pdf, "application/pdf", "Expense Report PDF")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = com.example.ui.theme.SophisticatedDarkSurface)

                    // Excel .xlsx Filtered Button
                    ExportOptionRow(
                        title = "Filtered Ledger Excel (.xlsx)",
                        subtitle = "Export current filter/search selection to Excel spreadsheet",
                        icon = Icons.Default.TableChart,
                        iconTint = com.example.ui.theme.SuccessGreen,
                        onDownload = {
                            val excel = viewModel.exportExcelSpreadsheet()
                            handleDownload(excel, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Expense Ledger Excel")
                        },
                        onShare = {
                            val excel = viewModel.exportExcelSpreadsheet()
                            ReportExportManager.shareFile(context, excel, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Expense Ledger Excel")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = com.example.ui.theme.SophisticatedDarkSurface)

                    // Full Monthly Report in Excel
                    ExportOptionRow(
                        title = "Full Monthly Report (.xlsx)",
                        subtitle = "Complete 18-column ledger, KPIs, budgets & daily trends for any month",
                        icon = Icons.Default.CalendarMonth,
                        iconTint = com.example.ui.theme.SuccessGreen,
                        onDownload = {
                            exportDateReportInitialTab = 0
                            showExportDateReportDialog = true
                        },
                        onShare = {
                            exportDateReportInitialTab = 0
                            showExportDateReportDialog = true
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = com.example.ui.theme.SophisticatedDarkSurface)

                    // Custom Date Range Excel Report
                    ExportOptionRow(
                        title = "Custom Date Range Report (.xlsx)",
                        subtitle = "Comprehensive multi-sheet workbook for any custom date boundaries",
                        icon = Icons.Default.DateRange,
                        iconTint = com.example.ui.theme.SuccessGreen,
                        onDownload = {
                            exportDateReportInitialTab = 1
                            showExportDateReportDialog = true
                        },
                        onShare = {
                            exportDateReportInitialTab = 1
                            showExportDateReportDialog = true
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = com.example.ui.theme.SophisticatedDarkSurface)

                    // CSV Button
                    ExportOptionRow(
                        title = "Standard CSV Export (.csv)",
                        subtitle = "Clean comma-separated values for database import and analysis",
                        icon = Icons.Default.Description,
                        iconTint = com.example.ui.theme.SophisticatedPrimary,
                        onDownload = {
                            val csv = viewModel.exportCsvReport()
                            handleDownload(csv, "text/csv", "Expense Ledger CSV")
                        },
                        onShare = {
                            val csv = viewModel.exportCsvReport()
                            ReportExportManager.shareFile(context, csv, "text/csv", "Expense Ledger CSV")
                        }
                    )
                }
            }
        }

        // 2. Biometric & App Lock Security
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                val bioSupported = remember { securityManager.canUseBiometrics() }
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Security", tint = com.example.ui.theme.SophisticatedPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Biometric & PIN App Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    "Startup unlock requires Fingerprint or Face unlock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && securityManager.getStoredPinHash() == null) {
                                    showPinSetupDialog = true
                                } else {
                                    isLockEnabled = enabled
                                    securityManager.setLockEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (isLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (bioSupported) com.example.ui.theme.SophisticatedPrimaryContainer.copy(alpha = 0.25f)
                            else com.example.ui.theme.SophisticatedDarkSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.SophisticatedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (bioSupported) "Biometrics Active: Fingerprint / Face prompt on launch (PIN fallback: 1234)"
                                    else "PIN Code Active (Default PIN: 1234)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Passcode / PIN Code", style = MaterialTheme.typography.bodyMedium, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                            OutlinedButton(onClick = { showPinSetupDialog = true }) {
                                Icon(Icons.Default.Password, contentDescription = "Set PIN", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change PIN")
                            }
                        }
                    }
                }
            }
        }

        // 2.1 Credit Card Suggestions Customizer
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = "Credit Cards",
                                tint = com.example.ui.theme.TagShoppingAmberText
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Credit Card Suggestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    "Add & customize RuPay & standard credit cards for quick selection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showCardSuggestionsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.TagShoppingAmber.copy(alpha = 0.25f),
                                contentColor = com.example.ui.theme.TagShoppingAmberText
                            )
                        ) {
                            Text("Edit Cards")
                        }
                    }
                }
            }
        }

        // 3. Google Account & Encrypted Cloud Sync Vault
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = com.example.ui.theme.SophisticatedPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cloud Vault & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                                Text("Client-side Encrypted Backup", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account row
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = com.example.ui.theme.SophisticatedDarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Account", modifier = Modifier.size(32.dp), tint = com.example.ui.theme.SophisticatedPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = googleEmail ?: "Local Vault (Independent)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (googleEmail != null) {
                                            if (lastSyncTime > 0) "Last synced: ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastSyncTime))}" else "Ready to sync"
                                        } else {
                                            "100% on-device private storage"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (googleEmail == null) {
                                Button(
                                    onClick = {
                                        customAccountEmailInput = ""
                                        showAccountDialog = true
                                    }
                                ) {
                                    Text("Link Account")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        securityManager.signOutGoogle()
                                    }
                                ) {
                                    Text("Sign Out")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Backup and Restore Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performCloudBackup() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isAiLoading
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Backup")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.performCloudRestore() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isAiLoading
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Restore")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }

                    if (syncMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.SophisticatedPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }



        // 5. Data Management & Clean Slate
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Data Management",
                            tint = com.example.ui.theme.DangerRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Data Management",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Text(
                                "Purge sample transactions or wipe ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPurgeSampleDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Remove False Data", maxLines = 1)
                        }

                        Button(
                            onClick = { showClearDataDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.DangerRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear All Data", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 5. Sync Audit Logs
        if (syncLogs.isNotEmpty()) {
            item {
                Text(
                    text = "Sync Activity History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(syncLogs.take(5)) { log ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("${log.action} (${log.itemCount} items)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(log.details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        ModalBottomSheet(
            onDismissRequest = { showPinSetupDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = "PIN", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Set Security PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Enter a 4-digit PIN to lock your financial records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPinText,
                    onValueChange = { if (it.length <= 4) newPinText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (newPinText.length == 4) {
                            securityManager.setPin(newPinText)
                            securityManager.setLockEnabled(true)
                            isLockEnabled = true
                            showPinSetupDialog = false
                            newPinText = ""
                            Toast.makeText(context, "PIN code configured!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newPinText.length == 4,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save & Enable App Lock")
                }
            }
        }
    }

    // Download Completed BottomSheet
    downloadResultSheet?.let { (file, mimeType) ->
        ModalBottomSheet(
            onDismissRequest = { downloadResultSheet = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(com.example.ui.theme.SuccessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Downloaded",
                        tint = com.example.ui.theme.SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (file.name.endsWith(".enc")) "Encrypted Backup Secured!" else "File Downloaded Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (file.name.endsWith(".enc")) "AES-256 encrypted database state saved to Downloads" else "Saved to your device's Downloads folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (file.name.endsWith(".xlsx")) Icons.Default.TableChart
                            else if (file.name.endsWith(".pdf")) Icons.Default.PictureAsPdf
                            else if (file.name.endsWith(".enc")) Icons.Default.Lock
                            else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (file.name.endsWith(".enc")) com.example.ui.theme.SophisticatedPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${(file.length() / 1024).coerceAtLeast(1)} KB • Downloads/${file.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val opened = ReportExportManager.openFile(context, file, mimeType)
                            if (!opened) {
                                ReportExportManager.shareFile(context, file, mimeType, "Open ${file.name}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open File", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            ReportExportManager.shareFile(context, file, mimeType, "Share ${file.name}")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }

    // Link Account Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Account",
                        tint = com.example.ui.theme.SophisticatedPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link User Account", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your email or account identifier to associate with encrypted cloud backups and sync logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customAccountEmailInput,
                        onValueChange = { customAccountEmailInput = it },
                        label = { Text("Account Email / ID") },
                        placeholder = { Text("e.g. user@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = customAccountEmailInput.trim().ifBlank { "user@example.com" }
                        securityManager.signInGoogle(email)
                        showAccountDialog = false
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Purge Sample / False Data Dialog
    if (showPurgeSampleDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeSampleDialog = false },
            title = { Text("Remove False / Sample Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will remove all pre-seeded demo expenses and starter mock records from your database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purgeSampleData()
                        showPurgeSampleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DangerRed)
                ) {
                    Text("Remove False Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeSampleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Ledger Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all expense transactions, custom budgets, and sync logs. You will start with a completely clean, empty ledger.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DangerRed)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }





    if (showCardSuggestionsDialog) {
        EditCardSuggestionsDialog(
            viewModel = viewModel,
            onDismiss = { showCardSuggestionsDialog = false }
        )
    }

    if (showExportDateReportDialog) {
        ExportDateReportDialog(
            viewModel = viewModel,
            initialTab = exportDateReportInitialTab,
            onDismissRequest = { showExportDateReportDialog = false }
        )
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDownload() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
