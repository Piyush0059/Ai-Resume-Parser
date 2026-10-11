package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.ResumeViewModel
import com.example.ui.viewmodel.DiagnosticsInfo
import com.example.ui.viewmodel.StageDiagnostic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperDiagnosticsDialog(
    viewModel: ResumeViewModel,
    onDismiss: () -> Unit
) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showRawResponse by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostics",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Developer Diagnostics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("diagnostics_close_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog"
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary status card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Pipeline Status Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Status Badge
                                    val statusColor = when (diagnostics.overallStatus) {
                                        "SUCCESS" -> Color(0xFF2E7D32)
                                        "FAILED" -> MaterialTheme.colorScheme.error
                                        "RUNNING" -> Color(0xFFEF6C00)
                                        else -> Color.Gray
                                    }
                                    StatusBadge(label = "Status: ${diagnostics.overallStatus}", color = statusColor)
                                    StatusBadge(label = "Model: ${diagnostics.modelUsed}", color = MaterialTheme.colorScheme.secondary)
                                    StatusBadge(label = "HTTP API: ${diagnostics.apiResponseCode}", color = MaterialTheme.colorScheme.tertiary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Current Step:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = diagnostics.currentStep,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (diagnostics.lastExceptionClass != "N/A") {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Root Pipeline Failure Exception:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "${diagnostics.lastExceptionClass}: ${diagnostics.lastExceptionMessage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stages List
                    item {
                        Text(
                            text = "Pipeline Stage Execution Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(diagnostics.steps) { step ->
                        StageRow(step = step)
                    }

                    // Stack trace
                    if (diagnostics.lastStackTrace != "N/A" && diagnostics.lastStackTrace.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Diagnostic Stack Trace",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(diagnostics.lastStackTrace))
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Trace")
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    tonalElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                item {
                                                    Text(
                                                        text = diagnostics.lastStackTrace,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Gemini JSON API raw response
                    if (diagnostics.rawApiResponse != "N/A" && diagnostics.rawApiResponse.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Raw Response Chunk",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row {
                                        TextButton(onClick = { showRawResponse = !showRawResponse }) {
                                            Text(if (showRawResponse) "Hide" else "Show Payload")
                                        }
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(diagnostics.rawApiResponse))
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Body")
                                        }
                                    }
                                }

                                if (showRawResponse) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        tonalElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            ) {
                                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                    item {
                                                        Text(
                                                            text = diagnostics.rawApiResponse,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.initDiagnostics()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f).testTag("diagnostics_reset_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Logs")
                    }

                    Button(
                        onClick = {
                            val report = buildMarkdownReport(diagnostics)
                            clipboardManager.setText(AnnotatedString(report))
                        },
                        modifier = Modifier.weight(1f).testTag("diagnostics_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Markdown")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StageRow(step: StageDiagnostic) {
    val stepColor = when (step.status) {
        "SUCCESS" -> Color(0xFF2E7D32)
        "FAILED" -> MaterialTheme.colorScheme.error
        "ACTIVE" -> Color(0xFFEF6C00)
        else -> Color.Gray
    }

    val stepIcon = when (step.status) {
        "SUCCESS" -> Icons.Default.CheckCircle
        "FAILED" -> Icons.Default.Error
        "ACTIVE" -> Icons.Default.Autorenew
        else -> Icons.Default.RadioButtonUnchecked
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (step.status == "ACTIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = stepIcon,
                        contentDescription = step.status,
                        tint = stepColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = step.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (step.durationMs > 0) {
                        Text(
                            text = "${step.durationMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(label = step.status, color = stepColor)
                }
            }

            if (step.resultSummary.isNotEmpty()) {
                Text(
                    text = step.resultSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }

            if (step.errorDetails.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 30.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = step.errorDetails,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun buildMarkdownReport(diagnostics: DiagnosticsInfo): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp = df.format(Date())
    val sb = java.lang.StringBuilder()
    sb.append("# Resume Parser Diagnostics Report\n")
    sb.append("Generated on: $timestamp\n\n")
    sb.append("## Summary\n")
    sb.append("- **Overall Status:** ${diagnostics.overallStatus}\n")
    sb.append("- **Active step:** ${diagnostics.currentStep}\n")
    sb.append("- **Model requested:** ${diagnostics.modelUsed}\n")
    sb.append("- **HTTP response status:** ${diagnostics.apiResponseCode}\n\n")

    if (diagnostics.lastExceptionClass != "N/A") {
        sb.append("## Failure Root Exception\n")
        sb.append("```\n")
        sb.append("${diagnostics.lastExceptionClass}: ${diagnostics.lastExceptionMessage}\n")
        sb.append("```\n\n")
    }

    sb.append("## Chronological Phase Verification \n")
    for (step in diagnostics.steps) {
        val symbol = when (step.status) {
            "SUCCESS" -> "✅"
            "FAILED" -> "❌"
            "ACTIVE" -> "⏳"
            else -> "▫️"
        }
        sb.append("### $symbol ${step.name} (${step.status})\n")
        if (step.durationMs > 0) {
            sb.append("- **Duration:** ${step.durationMs}ms\n")
        }
        if (step.resultSummary.isNotEmpty()) {
            sb.append("- **Detail:** ${step.resultSummary}\n")
        }
        if (step.errorDetails.isNotEmpty()) {
            sb.append("- **Error details:**\n```\n${step.errorDetails}\n```\n")
        }
        sb.append("\n")
    }

    if (diagnostics.lastStackTrace != "N/A" && diagnostics.lastStackTrace.isNotEmpty()) {
        sb.append("## Complete Stack Trace\n")
        sb.append("```\n")
        sb.append(diagnostics.lastStackTrace)
        sb.append("\n```\n")
    }

    return sb.toString()
}
