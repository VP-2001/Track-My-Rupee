package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.export.ReportExportManager
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedOnPrimaryContainer
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBackupRestoreCard(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFullSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle(emptyList())
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var showBackupDialog by remember { mutableStateOf(false) }
    var usePasswordProtection by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }

    var showRestorePasswordDialog by remember { mutableStateOf<ByteArray?>(null) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var backupResultSheet by remember { mutableStateOf<File?>(null) }

    var showCsvBackupDialog by remember { mutableStateOf(false) }
    var csvBackupResultSheet by remember { mutableStateOf<File?>(null) }

    // File picker using OpenDocument contract to reliably pick .enc files
    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.restoreFromEncryptedBackupFile(bytes, null) { success, msg ->
                        if (success) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        } else {
                            // If default passphrase failed, ask user for custom password
                            showRestorePasswordDialog = bytes
                        }
                    }
                } else {
                    Toast.makeText(context, "Empty or invalid backup file selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("local_backup_restore_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        border = BorderStroke(1.dp, SophisticatedDarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SophisticatedPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = "Local Backup & Restore",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Local Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "AES-256",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Save or restore encrypted snapshots on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snapshot Info Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SophisticatedDarkSurfaceDim,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Active Ledger Snapshot",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "${allExpenses.size} Transactions Recorded • 100% Offline Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Encrypted",
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hardware Key",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Encrypted Backup & Restore)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Create Local Backup
                Button(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("create_local_backup_button"),
                    enabled = !isAiLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    } else {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Create Backup", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Backup", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // 2. Restore Local Backup
                OutlinedButton(
                    onClick = {
                        restoreFilePickerLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("restore_local_backup_button"),
                    enabled = !isAiLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SophisticatedDarkOnSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SophisticatedDarkBorder)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = "Restore File", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore File", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            // 3. Room DB CSV Ledger Export Feature
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SophisticatedDarkSurfaceDim,
                border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCsvBackupDialog = true }
                    .testTag("export_csv_backup_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SophisticatedPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = "CSV Backup",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Export Room DB to CSV",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedDarkOnSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SophisticatedPrimaryContainer
                                ) {
                                    Text(
                                        text = "External Backup",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "RFC-4180 spreadsheet with all ${allExpenses.size} records",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedDarkOnSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SophisticatedPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ".CSV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Export CSV",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (onNavigateToFullSettings != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onNavigateToFullSettings,
                        modifier = Modifier.testTag("view_all_vault_settings_btn")
                    ) {
                        Text(
                            text = "Cloud Sync & Biometrics Settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = SophisticatedPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    // Create Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SdStorage, contentDescription = null, tint = SophisticatedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Local Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "An AES-256 encrypted file (.enc) containing all ${allExpenses.size} transactions will be saved to your device's Downloads directory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SophisticatedDarkOnSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SophisticatedDarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Custom Passphrase",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SophisticatedDarkOnSurface
                                    )
                                    Text(
                                        text = "Require a custom password to restore",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = usePasswordProtection,
                                    onCheckedChange = { usePasswordProtection = it }
                                )
                            }

                            if (usePasswordProtection) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = backupPassword,
                                    onValueChange = { backupPassword = it },
                                    label = { Text("Backup Passphrase") },
                                    placeholder = { Text("e.g. SecretPassword123") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = if (usePasswordProtection && backupPassword.isNotBlank()) backupPassword else null
                        viewModel.backupToLocalStorage(pass) { file, saved, msg ->
                            if (file.exists()) {
                                backupResultSheet = file
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_create_backup_btn")
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Password Prompt Dialog
    if (showRestorePasswordDialog != null) {
        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = null
                restorePasswordInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Password", tint = SophisticatedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Password Required", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This backup is encrypted with a custom passphrase. Enter it below to decrypt and restore your financial records.")
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        label = { Text("Passphrase") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bytes = showRestorePasswordDialog
                        if (bytes != null) {
                            viewModel.restoreFromEncryptedBackupFile(bytes, restorePasswordInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    showRestorePasswordDialog = null
                                    restorePasswordInput = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = Color.Black)
                ) {
                    Text("Decrypt & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = null
                    restorePasswordInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Backup Result Sheet with Direct Share
    if (backupResultSheet != null) {
        val file = backupResultSheet!!
        ModalBottomSheet(
            onDismissRequest = { backupResultSheet = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SuccessGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                    }
                }

                Text(
                    text = "Backup Saved Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )

                Text(
                    text = "File: ${file.name}\nLocation: Downloads folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SophisticatedDarkOnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            ReportExportManager.shareFile(
                                context,
                                file,
                                "application/octet-stream",
                                "Encrypted Finance Backup"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_backup_file_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Send File")
                    }

                    OutlinedButton(
                        onClick = { backupResultSheet = null },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(48.dp)
                    ) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Room DB CSV Backup Dialog
    if (showCsvBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCsvBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = SophisticatedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Room DB to CSV", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Export all ${allExpenses.size} financial records from the local Room database into a standard RFC-4180 CSV spreadsheet file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SophisticatedDarkOnSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SophisticatedDarkSurfaceVariant,
                        border = BorderStroke(1.dp, SophisticatedDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Included Columns & Features:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                            Text(
                                text = "• ID, Date, Title, Amount (INR), Category, Payment Method, Bank, Credit Card, Tag, Notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• UTF-8 BOM encoding for seamless Microsoft Excel & Google Sheets compatibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = SophisticatedDarkOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• Full offline extraction directly from Room DB",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val csv = viewModel.exportCsvReport("Room_Ledger_Backup", allExpenses)
                        val (success, msg) = ReportExportManager.saveToDownloads(
                            context,
                            csv,
                            "text/csv"
                        )
                        if (csv.exists()) {
                            csvBackupResultSheet = csv
                        }
                        Toast.makeText(
                            context,
                            if (success) "Exported ${allExpenses.size} records to Downloads: ${csv.name}" else msg,
                            Toast.LENGTH_LONG
                        ).show()
                        showCsvBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("confirm_export_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Save CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success CSV Backup Result Sheet with Direct Share & Open
    if (csvBackupResultSheet != null) {
        val file = csvBackupResultSheet!!
        ModalBottomSheet(
            onDismissRequest = { csvBackupResultSheet = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SuccessGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                    }
                }

                Text(
                    text = "Room DB CSV Backup Ready!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SophisticatedDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "File: ${file.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Location: Downloads directory",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                        Text(
                            text = "Total Records: ${allExpenses.size} items • RFC-4180 CSV",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            ReportExportManager.shareFile(
                                context,
                                file,
                                "text/csv",
                                "Room DB Expense Ledger (CSV Backup)"
                            )
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("share_csv_backup_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share CSV Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            val opened = ReportExportManager.openFile(context, file, "text/csv")
                            if (!opened) {
                                Toast.makeText(context, "No app found to open CSV files directly. You can find it in your Downloads folder.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("open_csv_backup_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SophisticatedDarkOnSurface),
                        border = BorderStroke(1.dp, SophisticatedDarkBorder)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }

                    OutlinedButton(
                        onClick = { csvBackupResultSheet = null },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp)
                    ) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
