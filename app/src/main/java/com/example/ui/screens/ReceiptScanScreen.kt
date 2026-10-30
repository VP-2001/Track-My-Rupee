package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ExtractedTransaction
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceDim
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onOpenFullAddExpense: (ExtractedTransaction) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is needed to scan paper receipts", Toast.LENGTH_SHORT).show()
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingAi by remember { mutableStateOf(false) }
    var extractedResult by remember { mutableStateOf<ExtractedTransaction?>(null) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // Pick receipt from device storage as zero-permission fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            capturedBitmap = bitmap
                            isProcessingAi = true
                            scanErrorMessage = null
                        }
                        viewModel.scanReceiptImage(bitmap) { res ->
                            isProcessingAi = false
                            if (res != null) {
                                extractedResult = res
                                showReviewSheet = true
                            } else {
                                scanErrorMessage = "Could not parse receipt details. Please try another image."
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessingAi = false
                        scanErrorMessage = "Failed to load selected receipt: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    // Function to capture from CameraX
    fun captureReceipt() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (isProcessingAi) return

        isProcessingAi = true
        scanErrorMessage = null

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToRotatedBitmap(image)
                    image.close()
                    if (bitmap != null) {
                        capturedBitmap = bitmap
                        viewModel.scanReceiptImage(bitmap) { res ->
                            isProcessingAi = false
                            if (res != null) {
                                extractedResult = res
                                showReviewSheet = true
                            } else {
                                scanErrorMessage = "Could not extract clear details. Please reposition the receipt and retry."
                            }
                        }
                    } else {
                        isProcessingAi = false
                        scanErrorMessage = "Failed to process camera frame"
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    isProcessingAi = false
                    scanErrorMessage = "Capture failed: ${exception.localizedMessage ?: "Unknown error"}"
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission) {
                // Permission Fallback Screen
                CameraPermissionPrompt(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onNavigateBack = onNavigateBack
                )
            } else {
                // Live CameraX Viewfinder
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
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

                // Translucent Viewfinder Framing Reticle
                ReceiptFramingOverlay(isScanning = isProcessingAi)

                // Top Floating Bar (Back, Title, Flash, Lens)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("receipt_scan_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, SophisticatedPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gemini AI Receipt OCR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                camera?.cameraControl?.enableTorch(isFlashOn)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("receipt_scan_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isFlashOn) AmberGold else Color.White
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("receipt_scan_switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip Camera",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Error notification banner if scan had issues
                if (scanErrorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = scanErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { scanErrorMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Bottom Controls Bar (Gallery, Shutter Button, Quick Add)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isProcessingAi) "Gemini AI is analyzing receipt details..." else "Point camera at receipt & tap shutter to capture",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { galleryLauncher.launch("image/*") }
                                    .padding(8.dp)
                                    .testTag("receipt_scan_gallery_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Pick Receipt Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gallery",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }

                            // Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, SophisticatedPrimary, CircleShape)
                                    .clickable(enabled = !isProcessingAi) { captureReceipt() }
                                    .testTag("receipt_scan_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(54.dp),
                                        color = AmberGold,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }

                            // Manual or Voice entry fallback
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onOpenFullAddExpense(
                                            ExtractedTransaction(
                                                title = "",
                                                amount = 0.0,
                                                category = "Food & Dining",
                                                paymentMethod = "UPI",
                                                date = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    .padding(8.dp)
                                    .testTag("receipt_scan_manual_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Manual Entry",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Manual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // AI Processing Full-Screen Shimmer Overlay
            if (isProcessingAi) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = SophisticatedDarkSurface,
                        border = BorderStroke(1.dp, SophisticatedPrimary),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(52.dp),
                                color = SophisticatedPrimary,
                                strokeWidth = 4.dp
                            )

                            Text(
                                text = "Scanning Paper Receipt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedDarkOnSurface
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ScanningStepItem(step = "1. Processing image optics & orientation", isDone = true)
                                ScanningStepItem(step = "2. Reading merchant & establishment name", isDone = true)
                                ScanningStepItem(step = "3. Extracting total amount, tax, and date", isDone = false)
                                ScanningStepItem(step = "4. Categorizing transaction with Gemini AI", isDone = false)
                            }
                        }
                    }
                }
            }
        }
    }

    // Parsed Result Confirmation BottomSheet
    if (showReviewSheet && extractedResult != null) {
        ReceiptParsedReviewSheet(
            extracted = extractedResult!!,
            capturedBitmap = capturedBitmap,
            onDismiss = {
                showReviewSheet = false
            },
            onSave = { updatedExtracted ->
                scope.launch {
                    var localImagePath: String? = null
                    if (capturedBitmap != null) {
                        localImagePath = saveBitmapLocally(context, capturedBitmap!!)
                    }

                    viewModel.addExpense(
                        title = updatedExtracted.title,
                        amount = updatedExtracted.amount,
                        category = updatedExtracted.category,
                        paymentMethod = updatedExtracted.paymentMethod,
                        date = updatedExtracted.date,
                        notes = updatedExtracted.notes,
                        tag = updatedExtracted.tag,
                        receiptUri = localImagePath
                    )

                    showReviewSheet = false
                    capturedBitmap = null
                    extractedResult = null

                    Toast.makeText(
                        context,
                        "Saved ₹${ExpenseEntity.formatInr(updatedExtracted.amount)} at ${updatedExtracted.title} to Ledger!",
                        Toast.LENGTH_LONG
                    ).show()

                    onNavigateToTransactions()
                }
            },
            onRetake = {
                showReviewSheet = false
                capturedBitmap = null
                extractedResult = null
            },
            onOpenFullEditor = { updatedExtracted ->
                showReviewSheet = false
                onOpenFullAddExpense(updatedExtracted)
            }
        )
    }
}

@Composable
private fun ScanningStepItem(step: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AmberGold
            )
        }
        Text(
            text = step,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDone) SophisticatedDarkOnSurface else SophisticatedDarkOnSurfaceVariant
        )
    }
}

@Composable
private fun ReceiptFramingOverlay(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_anim"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameWidth = maxWidth * 0.82f
        val frameHeight = maxHeight * 0.58f

        // Central Receipt Target Box
        Box(
            modifier = Modifier
                .size(frameWidth, frameHeight)
                .align(Alignment.Center)
        ) {
            // Corner Reticle Guides
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        BorderStroke(2.dp, SophisticatedPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            // Animated Laser Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (frameHeight * laserOffset))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                AmberGold,
                                Color.White,
                                AmberGold,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Guidance hint inside frame
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Fit receipt within frame",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit,
    onPickGallery: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = SophisticatedDarkSurfaceDim,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("camera_prompt_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SophisticatedDarkOnSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Receipt Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurface
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SophisticatedDarkSurfaceVariant)
                    .border(2.dp, SophisticatedPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Required",
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Access Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SophisticatedDarkOnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Track My Rupee uses your camera to capture paper receipts and uses the Gemini AI API to automatically parse merchant names, amounts, transaction dates, and categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = SophisticatedDarkOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_camera_permission_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SophisticatedOnPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable Camera Access",
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onPickGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("prompt_pick_gallery_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SophisticatedDarkBorder)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SophisticatedDarkOnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload Receipt from Gallery",
                    fontWeight = FontWeight.SemiBold,
                    color = SophisticatedDarkOnSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptParsedReviewSheet(
    extracted: ExtractedTransaction,
    capturedBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSave: (ExtractedTransaction) -> Unit,
    onRetake: () -> Unit,
    onOpenFullEditor: (ExtractedTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var merchantTitle by remember(extracted) { mutableStateOf(extracted.title) }
    var amountStr by remember(extracted) { mutableStateOf(if (extracted.amount > 0) extracted.amount.toString() else "") }
    var selectedCategory by remember(extracted) { mutableStateOf(extracted.category) }
    var selectedPaymentMethod by remember(extracted) { mutableStateOf(extracted.paymentMethod) }
    var selectedDate by remember(extracted) { mutableLongStateOf(extracted.date) }
    var notesText by remember(extracted) { mutableStateOf(extracted.notes) }

    val formattedDate = remember(selectedDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(selectedDate))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SophisticatedDarkSurface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Receipt Parsed by Gemini AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                        Text(
                            text = "Review & adjust extracted transaction details",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SophisticatedDarkOnSurfaceVariant)
                }
            }

            // Preview Thumbnail & Total Amount Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SophisticatedDarkSurfaceVariant)
                    .border(1.dp, SophisticatedDarkBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Captured Receipt",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, SophisticatedDarkBorder, RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SophisticatedDarkSurfaceDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Extracted Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = SophisticatedDarkOnSurfaceVariant
                    )
                    Text(
                        text = ExpenseEntity.formatInr(amountStr.toDoubleOrNull() ?: 0.0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SophisticatedPrimary
                    )
                    Text(
                        text = "Date: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = SophisticatedDarkOnSurface
                    )
                }
            }

            // Editable Merchant Field
            OutlinedTextField(
                value = merchantTitle,
                onValueChange = { merchantTitle = it },
                label = { Text("Merchant / Store Name") },
                leadingIcon = {
                    Icon(Icons.Default.Store, contentDescription = null, tint = SophisticatedPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_merchant_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedDarkBorder,
                    focusedTextColor = SophisticatedDarkOnSurface,
                    unfocusedTextColor = SophisticatedDarkOnSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Editable Amount Field
            OutlinedTextField(
                value = amountStr,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amountStr = input
                    }
                },
                label = { Text("Total Amount (₹)") },
                leadingIcon = {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = AmberGold)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedDarkBorder,
                    focusedTextColor = SophisticatedDarkOnSurface,
                    unfocusedTextColor = SophisticatedDarkOnSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Category Selection Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category (AI Recommended)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SophisticatedDarkOnSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultCategories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = SophisticatedPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Payment Method Selection Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SophisticatedDarkOnSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Credit Card", "Cash", "Debit Card").forEach { method ->
                        val isSelected = selectedPaymentMethod.equals(method, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                                selectedLabelColor = AmberGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Extracted Line Items / Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Extracted Items & Notes") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedDarkBorder,
                    focusedTextColor = SophisticatedDarkOnSurface,
                    unfocusedTextColor = SophisticatedDarkOnSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Date adjustment shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = SophisticatedDarkOnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = { selectedDate = System.currentTimeMillis() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Today", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        selectedDate = cal.timeInMillis
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Yesterday", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("receipt_retake_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SophisticatedDarkBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SophisticatedDarkOnSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake", color = SophisticatedDarkOnSurface, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            return@Button
                        }
                        val title = merchantTitle.ifBlank { "Receipt Expense" }
                        onSave(
                            extracted.copy(
                                title = title,
                                amount = amount,
                                category = selectedCategory,
                                paymentMethod = selectedPaymentMethod,
                                date = selectedDate,
                                notes = notesText
                            )
                        )
                    },
                    enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("receipt_save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SophisticatedOnPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save to Ledger",
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedOnPrimary
                    )
                }
            }
        }
    }
}

private fun imageProxyToRotatedBitmap(image: ImageProxy): Bitmap? {
    return try {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveBitmapLocally(context: Context, bitmap: Bitmap): String? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}
