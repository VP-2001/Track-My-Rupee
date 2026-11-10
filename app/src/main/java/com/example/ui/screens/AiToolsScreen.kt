package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ai.ExtractedTransaction
import com.example.ai.StatementParseResult
import com.example.data.model.ExpenseEntity
import com.example.export.ReportExportManager
import com.example.ui.components.LocalBackupRestoreCard
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    viewModel: ExpenseViewModel,
    initialTab: Int = 0,
    onOpenAddExpenseWithPreFill: (ExtractedTransaction) -> Unit,
    onOpenReceiptScannerScreen: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("Receipt Scanner", "Bank Statement Converter", "Voice Assistant", "AI Advisor Chat", "On-Device ML", "Backup Vault")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_tools_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "AI Powered",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Smart Finance Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = com.example.ui.theme.SophisticatedDarkSurfaceDim,
            contentColor = com.example.ui.theme.SophisticatedPrimary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ReceiptScannerTab(
                viewModel = viewModel,
                onPreFill = onOpenAddExpenseWithPreFill,
                onOpenFullScreenScanner = onOpenReceiptScannerScreen
            )
            1 -> BankStatementTab(viewModel = viewModel)
            2 -> VoiceAssistantTab(viewModel = viewModel, onPreFill = onOpenAddExpenseWithPreFill)
            3 -> AiChatScreen(viewModel = viewModel)
            4 -> OnDeviceMlTab(viewModel = viewModel)
            5 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    LocalBackupRestoreCard(viewModel = viewModel)
                }
            }
        }
    }
}

// 1. LIVE CAMERAX RECEIPT SCANNER
@Composable
fun ReceiptScannerTab(
    viewModel: ExpenseViewModel,
    onPreFill: (ExtractedTransaction) -> Unit,
    onOpenFullScreenScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val extractedTx by viewModel.aiExtractedTransaction.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.scanReceiptImage(bitmap) { res ->
                        if (res != null) onPreFill(res)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Scan Receipts & Bills with CameraX",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Point camera at any receipt to auto-extract merchant, total amount, category, and date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFullScreenScanner() },
                shape = RoundedCornerShape(16.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = com.example.ui.theme.SophisticatedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open Full Screen Camera Scanner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Full viewfinder with laser reticle & instant review",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = com.example.ui.theme.SophisticatedPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Camera Viewport
        item {
            if (!hasCameraPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera Permission", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Camera access is needed to scan receipts", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                                    try {
                                        cameraProvider.unbindAll()
                                        camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (_: Exception) {}
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Camera controls overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    camera?.cameraControl?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Torch",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                            }
                        }

                        // Scanning animation guide rect
                        Box(
                            modifier = Modifier
                                .size(240.dp, 160.dp)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }

        // Action Buttons: Capture & Gallery
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        if (bitmap != null) {
                                            viewModel.scanReceiptImage(bitmap) { res ->
                                                if (res != null) onPreFill(res)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        super.onError(exception)
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !isAiLoading
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing Receipt...")
                    } else {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Capture")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Scan")
                    }
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Image")
                }
            }
        }

        // Extracted Result Card
        if (extractedTx != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Extracted", tint = com.example.ui.theme.SophisticatedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Receipt Extracted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Merchant: ${extractedTx!!.title}", fontWeight = FontWeight.SemiBold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                        Text("Amount: ${ExpenseEntity.formatInr(extractedTx!!.amount)}", color = com.example.ui.theme.SophisticatedPrimary, fontWeight = FontWeight.Bold)
                        Text("Category: ${extractedTx!!.category} • Method: ${extractedTx!!.paymentMethod}", color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                        if (extractedTx!!.notes.isNotBlank()) {
                            Text("Summary: ${extractedTx!!.notes}", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.addExpense(
                                        title = extractedTx!!.title,
                                        amount = extractedTx!!.amount,
                                        category = extractedTx!!.category,
                                        paymentMethod = extractedTx!!.paymentMethod,
                                        date = extractedTx!!.date,
                                        notes = extractedTx!!.notes,
                                        tag = extractedTx!!.tag
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Directly")
                            }

                            OutlinedButton(
                                onClick = { onPreFill(extractedTx!!) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit First")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. BANK STATEMENT PDF / TEXT TO TABLE & EXCEL CONVERTER
@Composable
fun BankStatementTab(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val statementResult by viewModel.statementParseResult.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var selectedBank by remember { mutableStateOf("HDFC Bank") }
    val bankOptions = listOf("HDFC Bank", "SBI Card", "ICICI Bank", "Axis Bank", "Kotak Mahindra")

    var statementInputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Bank Statement & PDF Converter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Convert bank statements or credit card bills (HDFC, SBI, ICICI, Axis, Kotak) to Excel / Ledger table.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Bank Selector Chips
        item {
            Text("Select Bank / Provider", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bankOptions) { bank ->
                    FilterChip(
                        selected = selectedBank == bank,
                        onClick = { selectedBank = bank },
                        label = { Text(bank) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Statement Text or Sample trigger
        item {
            OutlinedTextField(
                value = statementInputText,
                onValueChange = { statementInputText = it },
                label = { Text("Paste Statement Raw Text or PDF Extract") },
                placeholder = { Text("e.g. 14/08/2026 UPI/41234981/Swiggy 540.00 DR...") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.parseStatementText(statementInputText, selectedBank)
                    },
                    modifier = Modifier.weight(1.2f).height(48.dp),
                    enabled = !isAiLoading
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.TableChart, contentDescription = "Parse")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract Statement")
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.parseStatementText("", selectedBank)
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Load $selectedBank Sample")
                }
            }
        }

        if (syncMessage != null) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(syncMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Statement Table Preview & Actions
        if (statementResult != null) {
            val result = statementResult!!
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                            Column {
                                Text(
                                    text = "${result.bankName} Statement",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "${result.transactions.size} transactions • Total Debits: ${ExpenseEntity.formatInr(result.totalDebits)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Table Rows
                        result.transactions.forEachIndexed { idx, tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) com.example.ui.theme.SophisticatedDarkSurface.copy(alpha = 0.5f) else Color.Transparent)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                                    Text("${tx.category} • ${tx.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                                }
                                Text(ExpenseEntity.formatInr(tx.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Import to Ledger & Export to Excel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.importTransactions(result.transactions)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Import")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import to Ledger")
                            }

                            OutlinedButton(
                                onClick = {
                                    val excel = viewModel.exportExcelSpreadsheet()
                                    ReportExportManager.shareFile(context, excel, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "${result.bankName} Statement Excel")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Excel")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export .XLSX")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. VOICE EXPENSE ASSISTANT
@Composable
fun VoiceAssistantTab(
    viewModel: ExpenseViewModel,
    onPreFill: (ExtractedTransaction) -> Unit
) {
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var spokenInput by remember { mutableStateOf("") }
    var voiceExtracted by remember { mutableStateOf<ExtractedTransaction?>(null) }

    val presetQueries = listOf(
        "Spent 450 rupees on Swiggy dinner via UPI",
        "Paid 2500 for petrol using credit card for work",
        "Bought 1200 rupees groceries from Blinkit",
        "Paid 1178 for Airtel broadband wifi bill"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Voice Expense Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Speak or write your expense naturally. AI extracts amount, category, merchant, and payment method.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Voice Input Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = spokenInput,
                        onValueChange = { spokenInput = it },
                        label = { Text("Spoken or Written Expense") },
                        placeholder = { Text("e.g. Spent 650 on Starbucks coffee with UPI") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (spokenInput.isNotBlank()) {
                                viewModel.processVoiceExpense(spokenInput) { res ->
                                    voiceExtracted = res
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = spokenInput.isNotBlank() && !isAiLoading
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = "Parse")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Parse with AI")
                        }
                    }
                }
            }
        }

        // Quick Sample Voice Queries
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Try Sample Voice Queries", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            presetQueries.forEach { query ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            spokenInput = query
                            viewModel.processVoiceExpense(query) { res ->
                                voiceExtracted = res
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Sample", tint = com.example.ui.theme.SophisticatedPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "\"$query\"", style = MaterialTheme.typography.bodyMedium, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                    }
                }
            }
        }

        // Extracted Result Card
        if (voiceExtracted != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Parsed", tint = com.example.ui.theme.SophisticatedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice Expense Parsed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Title: ${voiceExtracted!!.title}", fontWeight = FontWeight.SemiBold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                        Text("Amount: ${ExpenseEntity.formatInr(voiceExtracted!!.amount)}", color = com.example.ui.theme.SophisticatedPrimary, fontWeight = FontWeight.Bold)
                        Text("Category: ${voiceExtracted!!.category} • Method: ${voiceExtracted!!.paymentMethod} • Tag: ${voiceExtracted!!.tag}", color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.addExpense(
                                        title = voiceExtracted!!.title,
                                        amount = voiceExtracted!!.amount,
                                        category = voiceExtracted!!.category,
                                        paymentMethod = voiceExtracted!!.paymentMethod,
                                        date = voiceExtracted!!.date,
                                        notes = voiceExtracted!!.notes,
                                        tag = voiceExtracted!!.tag
                                    )
                                    spokenInput = ""
                                    voiceExtracted = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Directly")
                            }

                            OutlinedButton(
                                onClick = { onPreFill(voiceExtracted!!) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit & Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnDeviceMlTab(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val mlStats by viewModel.onDeviceMlStats.collectAsStateWithLifecycle()
    var testMerchant by remember { mutableStateOf("Swiggy") }
    var testAmountText by remember { mutableStateOf("380") }
    var testNotes by remember { mutableStateOf("Dinner biryani") }

    val livePrediction = remember(testMerchant, testAmountText, testNotes, mlStats) {
        val amt = testAmountText.toDoubleOrNull() ?: 0.0
        viewModel.predictCategoryOnDevice(testMerchant, amt, testNotes)
    }

    val sampleMerchants = listOf(
        "Swiggy" to "380",
        "Blinkit" to "520",
        "Airtel Wifi" to "1180",
        "Shell Petrol" to "2000",
        "Amazon India" to "1499",
        "Apollo Pharmacy" to "650",
        "Zerodha SIP" to "5000",
        "Netflix" to "649",
        "Urban Company" to "899",
        "Uber Cab" to "280"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card: Model Status & Architecture
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("on_device_ml_status_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "On-Device ML",
                                    tint = com.example.ui.theme.SophisticatedPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "On-Device ML Classifier",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurface
                                )
                                Text(
                                    text = "Predicts categories locally before server AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = com.example.ui.theme.SuccessGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "This machine learning model runs 100% on-device. It trains on your personal Room database spending patterns using Naive Bayes probability, N-gram tokenization, and Gaussian amount profiling to instantly suggest categories without relying on server-side APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = com.example.ui.theme.SophisticatedDarkSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Learned Records", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                                Text("${mlStats.trainedTransactionsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedPrimary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = com.example.ui.theme.SophisticatedDarkSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Learned Merchants", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                                Text("${mlStats.uniqueMerchantsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SophisticatedDarkOnSurface)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = com.example.ui.theme.SophisticatedDarkSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("API Savings", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                                Text("90%+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.example.ui.theme.SuccessGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.retrainOnDeviceMlModel()
                            android.widget.Toast.makeText(context, "Model retrained from ${mlStats.trainedTransactionsCount} historical records!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("retrain_on_device_ml_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.SophisticatedPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retrain Model from Ledger", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Interactive Prediction Sandbox
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = com.example.ui.theme.SophisticatedDarkSurfaceDim,
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SophisticatedDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("on_device_ml_sandbox_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = com.example.ui.theme.SophisticatedPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Category Prediction Sandbox",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SophisticatedDarkOnSurface
                        )
                    }
                    Text(
                        text = "Test instant on-device category inference as you type",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Sample Pills
                    Text("Quick Sample Merchants:", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sampleMerchants.forEach { (m, a) ->
                            FilterChip(
                                selected = testMerchant.equals(m, ignoreCase = true),
                                onClick = {
                                    testMerchant = m
                                    testAmountText = a
                                    testNotes = "$m purchase"
                                },
                                label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = testMerchant,
                            onValueChange = { testMerchant = it },
                            label = { Text("Merchant / Description") },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("ml_sandbox_merchant_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = testAmountText,
                            onValueChange = { testAmountText = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ml_sandbox_amount_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Prediction Output Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = com.example.ui.theme.SophisticatedDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (livePrediction.isConfident) com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.7f) else com.example.ui.theme.SophisticatedDarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ml_sandbox_result_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = com.example.ui.theme.SophisticatedPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Predicted Category",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = com.example.ui.theme.SophisticatedPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${(livePrediction.confidence * 100).toInt()}% Match",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.SophisticatedPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = livePrediction.predictedCategory,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface,
                                modifier = Modifier.testTag("ml_sandbox_predicted_category")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = livePrediction.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Confidence Distribution Breakdown
                            Text(
                                text = "Candidate Confidence Breakdown:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.ui.theme.SophisticatedDarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            livePrediction.topCandidates.forEach { candidate ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = candidate.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (candidate.category == livePrediction.predictedCategory) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant,
                                        fontWeight = if (candidate.category == livePrediction.predictedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${(candidate.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (candidate.category == livePrediction.predictedCategory) com.example.ui.theme.SophisticatedPrimary else com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
