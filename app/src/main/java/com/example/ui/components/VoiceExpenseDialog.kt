package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ai.ExtractedTransaction
import com.example.ui.theme.AmberGold
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkOnSurface
import com.example.ui.theme.SophisticatedDarkOnSurfaceVariant
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceExpenseDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    onExpenseExtracted: (ExtractedTransaction) -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulse animation for microphone
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isListening) {
        if (isListening) {
            pulseScale.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    fun processSpokenText(text: String) {
        if (text.isBlank()) return
        isProcessing = true
        errorMessage = null
        viewModel.processVoiceExpense(text) { result ->
            isProcessing = false
            if (result != null && result.amount > 0) {
                onExpenseExtracted(result)
                onDismiss()
            } else if (result != null) {
                onExpenseExtracted(result)
                onDismiss()
            } else {
                errorMessage = "Could not parse transaction details. Try again or edit below."
            }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = results?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                spokenText = recognized
                processSpokenText(recognized)
            }
        }
    }

    fun launchSystemSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say expense, e.g. 'Spent 450 on Swiggy with UPI'")
        }
        try {
            isListening = true
            errorMessage = null
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            // Speech recognition intent not supported or available on device/emulator
            Toast.makeText(context, "Voice recognition service not available. You can type or pick sample below.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSystemSpeechRecognizer()
        } else {
            errorMessage = "Audio permission is required for voice recognition."
        }
    }

    fun startListening() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            launchSystemSpeechRecognizer()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-trigger microphone when dialog opens
    LaunchedEffect(Unit) {
        startListening()
    }

    val sampleQueries = listOf(
        "Spent 450 rupees on Swiggy dinner via UPI",
        "Paid 2500 for petrol using credit card for work",
        "Bought 1200 groceries from Blinkit",
        "Paid 1178 for Airtel broadband wifi bill",
        "Spent 350 on Starbucks coffee with cash"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_expense_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
            border = BorderStroke(1.dp, SophisticatedDarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AmberGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Voice Expense Input",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedDarkOnSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_voice_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SophisticatedDarkOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone Pulsing Target
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Brush.radialGradient(listOf(AmberGold.copy(alpha = 0.35f), Color.Transparent))
                            else Brush.radialGradient(listOf(SophisticatedPrimary.copy(alpha = 0.2f), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale.value)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isListening) AmberGold else SophisticatedPrimary)
                            .clickable { startListening() }
                            .testTag("mic_trigger_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Tap to Speak",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isProcessing) "AI is analyzing expense details..."
                           else if (isListening) "Listening... speak now"
                           else "Tap microphone to speak",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening) AmberGold else SophisticatedDarkOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Editable Spoken Text Input
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    placeholder = {
                        Text(
                            text = "e.g., Spent 550 on dinner via UPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = SophisticatedDarkOnSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_spoken_text_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SophisticatedPrimary,
                        unfocusedBorderColor = SophisticatedDarkBorder,
                        focusedContainerColor = SophisticatedDarkSurfaceVariant,
                        unfocusedContainerColor = SophisticatedDarkSurfaceVariant
                    ),
                    maxLines = 3
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Parse Button
                Button(
                    onClick = { processSpokenText(spokenText) },
                    enabled = spokenText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("parse_voice_expense_button"),
                    shape = RoundedCornerShape(14.dp),
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Parse & Add Expense",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick sample voice queries
                Text(
                    text = "Quick Sample Inputs (Tap to try)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedDarkOnSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleQueries.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SophisticatedDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                spokenText = sample
                                processSpokenText(sample)
                            }
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = SophisticatedDarkOnSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
