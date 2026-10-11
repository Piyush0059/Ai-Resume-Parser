package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.AtsDetails
import com.example.data.model.ResumeData
import com.example.data.model.ResumeEntity
import com.example.ui.viewmodel.ResumeViewModel
import com.example.ui.viewmodel.QueueItem
import com.example.ui.viewmodel.QueueStatus
import com.example.util.SampleResumes
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    val parsedResult by viewModel.parsedResult.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
    val parseStatusText by viewModel.parseStatusText.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val currentViewJson by viewModel.currentViewJson.collectAsStateWithLifecycle()

    var showJsonDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    var showOpeningSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1600)
        showOpeningSplash = false
    }

    // When parsing completes successfully, switch to Dashboard screen
    LaunchedEffect(parsedResult) {
        if (parsedResult != null && !isParsing) {
            selectedTab = 1
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                                    contentDescription = "Resume Parser Pro Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Resume Parser Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enterprise ATS Screening Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // System theme toggle button (System -> Dark -> Light)
                        IconButton(
                            onClick = {
                                viewModel.toggleAppTheme()
                                val nextMode = when (appThemeMode) {
                                    "SYSTEM" -> "Dark Mode enabled"
                                    "DARK" -> "Light Mode enabled"
                                    else -> "System Default Theme enabled"
                                }
                                Toast.makeText(context, nextMode, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(40.dp).testTag("top_bar_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (appThemeMode) {
                                    "DARK" -> Icons.Default.DarkMode
                                    "LIGHT" -> Icons.Default.LightMode
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Toggle Theme Mode: Currently $appThemeMode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                showDiagnosticsDialog = true
                            },
                            modifier = Modifier.size(40.dp).testTag("top_bar_diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Developer Diagnostics",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Resume Parser Pro is fully active & secure.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Parse Resume") },
                    label = { Text("Parser Upload") },
                    modifier = Modifier.testTag("nav_parse_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard Profile") },
                    label = { Text("ATS Screening") },
                    modifier = Modifier.testTag("nav_dashboard_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Profile History Logs") },
                    label = { Text("Local Database") },
                    modifier = Modifier.testTag("nav_history_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ParseScreen(
                        viewModel = viewModel,
                        isParsing = isParsing,
                        parseStatusText = parseStatusText,
                        errorMessage = errorMessage,
                        context = context
                    )
                    1 -> DashboardScreen(
                        viewModel = viewModel,
                        parsedResult = parsedResult,
                        onViewJson = { showJsonDialog = true },
                        onNavigateToUpload = { selectedTab = 0 }
                    )
                    2 -> HistoryScreen(
                        viewModel = viewModel,
                        historyList = historyList,
                        filteredHistory = filteredHistory,
                        onSelect = { entity ->
                            viewModel.loadFromHistory(entity)
                            selectedTab = 1
                        },
                        onDelete = { id ->
                            viewModel.deleteFromHistory(id)
                        }
                    )
                }
            }

            if (showJsonDialog && currentViewJson != null) {
                JsonViewDialog(
                    jsonString = currentViewJson!!,
                    onDismiss = { showJsonDialog = false }
                )
            }

            if (showDiagnosticsDialog) {
                DeveloperDiagnosticsDialog(
                    viewModel = viewModel,
                    onDismiss = { showDiagnosticsDialog = false }
                )
            }

            // Beautiful opening animation overlay
            AnimatedVisibility(
                visible = showOpeningSplash,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(400)) + shrinkOut(tween(400), shrinkTowards = Alignment.Center),
                modifier = Modifier.fillMaxSize()
            ) {
                OpeningSplashScreen()
            }
        }
    }
}

@Composable
fun ParseScreen(
    viewModel: ResumeViewModel,
    isParsing: Boolean,
    parseStatusText: String,
    errorMessage: String?,
    context: Context
) {
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val pastedText by viewModel.pastedText.collectAsStateWithLifecycle()
    val pastedJd by viewModel.pastedJd.collectAsStateWithLifecycle()

    var showJdInput by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.selectFile(uri, context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Welcoming Headline Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Fast extract",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Production AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Extract structured profiles with Google Gemini. Support scans, mobile PDFs, text captures, and automatic language translation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section: File Selector
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (selectedFileName != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedFileName == null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Document Pick Symbol",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Select Candidate Resume",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PDF, DOCX, TXT, or Image Scans (JPG/PNG)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            enabled = !isParsing,
                            modifier = Modifier.testTag("upload_file_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Files")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Loaded Check",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = selectedFileName ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearFile() },
                                enabled = !isParsing,
                                modifier = Modifier.testTag("remove_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Selection",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Enterprise Batch Screen / Bulk Resumes Tool (PHASE 8)
        item {
            val bulkQueue by viewModel.bulkQueue.collectAsStateWithLifecycle()
            val isBulkProcessing by viewModel.isBulkProcessing.collectAsStateWithLifecycle()
            val bulkProgress by viewModel.bulkProgress.collectAsStateWithLifecycle()
            val bulkStatusText by viewModel.bulkStatusText.collectAsStateWithLifecycle()
            var isBulkCardExpanded by remember { mutableStateOf(false) }

            val multipleFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (!uris.isNullOrEmpty()) {
                    viewModel.addToBulkQueue(uris, context)
                    isBulkCardExpanded = true
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (bulkQueue.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isBulkCardExpanded = !isBulkCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Bulk Upload",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Enterprise Batch Parser",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (bulkQueue.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(
                                                text = "${bulkQueue.size} Files",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Screen 10, 50, or 100+ resumes asynchronously",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isBulkCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand collapsible",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isBulkCardExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Button(
                            onClick = { multipleFilePickerLauncher.launch("*/*") },
                            enabled = !isBulkProcessing,
                            modifier = Modifier.fillMaxWidth().testTag("bulk_browse_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Multiple Resumes (PDF / Image)")
                        }

                        if (bulkQueue.isNotEmpty()) {
                            // Queue operations status text
                            if (isBulkProcessing || bulkStatusText.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = bulkStatusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(bulkProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { bulkProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }

                            // Files in queue list
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PROCESSING QUEUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .padding(4.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(bulkQueue) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = item.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    when (item.status) {
                                                        QueueStatus.PENDING -> {
                                                            Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            if (!isBulkProcessing) {
                                                                IconButton(
                                                                    onClick = { viewModel.removeFromQueue(item.id) },
                                                                    modifier = Modifier.size(24.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                        }
                                                        QueueStatus.PROCESSING -> {
                                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        QueueStatus.COMPLETED -> {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = "Succeeded", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                                        }
                                                        QueueStatus.FAILED -> {
                                                            IconButton(
                                                                onClick = { Toast.makeText(context, "${item.error}", Toast.LENGTH_LONG).show() },
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(Icons.Default.Warning, contentDescription = "Fail info", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearBulkQueue() },
                                    enabled = !isBulkProcessing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear Queue")
                                }

                                Button(
                                    onClick = { viewModel.processBulkQueue(context) },
                                    enabled = !isBulkProcessing,
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Batch Screen")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Optional Job Description Pre-aligner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showJdInput = !showJdInput },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = "JD Matcher",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Target Job Description (Optional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Paste Target JD for on-the-fly fit scoring",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand collapsible",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showJdInput) {
                        OutlinedTextField(
                            value = pastedJd,
                            onValueChange = { viewModel.updatePastedJd(it) },
                            placeholder = { Text("Paste Job specifications for skills GAP analysis report...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            enabled = !isParsing,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Section: Instant Test Samples
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "INSTANT TALENT SAMPLES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { viewModel.selectSampleText(SampleResumes.softwareEngineer, "Software Engineer Sample.txt") },
                        label = { Text("Software Engineer", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                        enabled = !isParsing,
                        modifier = Modifier.testTag("sample_swe_chip"),
                        shape = RoundedCornerShape(20.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = !isParsing, borderColor = MaterialTheme.colorScheme.outline)
                    )
                    AssistChip(
                        onClick = { viewModel.selectSampleText(SampleResumes.marketingManager, "Marketing Manager Sample.txt") },
                        label = { Text("Marketing Manager", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                        enabled = !isParsing,
                        modifier = Modifier.testTag("sample_mkt_chip"),
                        shape = RoundedCornerShape(20.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = !isParsing, borderColor = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }

        // Divider
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR PASTE RAW TEXT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
        }

        // Text input field
        item {
            OutlinedTextField(
                value = pastedText,
                onValueChange = { viewModel.updatePastedText(it) },
                label = { Text("Copy-Paste Resume Text") },
                placeholder = { Text("Copy and paste the full text of candidate resume here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("resume_raw_text_input"),
                enabled = !isParsing,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                maxLines = 10
            )
        }

        // Process Action button or Loader
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isParsing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = parseStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.parseResume(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("extract_information_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extract & Analyze Resume",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Error Display
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error Logo",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = { viewModel.parseResume(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry Pipeline")
                        }
                    }
                }
            }
        }

        // Dynamic Privacy Notice
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Privacy Policy compliance",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recruiter Privacy Compliance: Resumes processed by this enterprise module are strictly structured inside direct local memories and never utilized for LLM public training datasets.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: ResumeViewModel,
    parsedResult: ResumeData?,
    onViewJson: () -> Unit,
    onNavigateToUpload: () -> Unit
) {
    val context = LocalContext.current
    val isMatchingJd by viewModel.isMatchingJd.collectAsStateWithLifecycle()
    var targetJdText by remember { mutableStateOf("") }
    var scoreByPassingTime by remember { mutableStateOf(false) }

    if (parsedResult == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Upload Document Indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "No Candidate Selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Once you upload a resume or select a candidate profile from the local database folders, the full score analysis sheet will render here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToUpload,
                modifier = Modifier.testTag("dashboard_parse_new_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go to Parser Screen")
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Export Floating Options menu card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Export Report:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        IconButton(
                            onClick = { viewModel.exportCandidateProfile(context, "JSON") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("JSON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { viewModel.exportCandidateProfile(context, "CSV") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("CSV", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { viewModel.exportCandidateProfile(context, "TXT") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("TXT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onViewJson,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("floating_json_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Raw JSON Model")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            // PDF Success Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Document symbol",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                val cleanFileName = remember(parsedResult.name) {
                                    val safeName = parsedResult.name.replace(" ", "_")
                                    if (safeName.isNotEmpty()) "Resume_${safeName}.pdf" else "Resume_Document.pdf"
                                }
                                Text(
                                    text = cleanFileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Multilingual Extraction Approved (${parsedResult.detectedLanguage})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success check",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Candidate Summary Card Header with Initials avatar & quick metadata
            item {
                val initials = remember(parsedResult.name) {
                    parsedResult.name.split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .map { it[0].uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "AI" }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = parsedResult.name.ifEmpty { "Not Identified" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extracted digital profile report safely cataloged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ContactRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = parsedResult.email.ifEmpty { "Not specified in resume" }
                            )
                            ContactRow(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = parsedResult.phone.ifEmpty { "Not specified in resume" }
                            )
                            ContactRow(
                                icon = Icons.Default.LocationOn,
                                label = "Location",
                                value = parsedResult.location.ifEmpty { "Not specified" }
                            )
                            ContactRow(
                                icon = Icons.Default.Event,
                                label = "Date Of Birth",
                                value = parsedResult.dateOfBirth.ifEmpty { "Not listed" }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                            var showConfidence by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showConfidence = !showConfidence },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Text("Extraction Metadata & Confidence Levels", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                    }
                                    Icon(
                                        imageVector = if (showConfidence) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (showConfidence) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                Triple("Name Extraction", parsedResult.confidence.name, Icons.Default.Badge),
                                                Triple("Contact Details", parsedResult.confidence.email, Icons.Default.ContactMail),
                                                Triple("Socio-Location", parsedResult.confidence.location, Icons.Default.LocationOn),
                                                Triple("Technical Skills", parsedResult.confidence.skills, Icons.Default.Build),
                                                Triple("Experience Timeline", parsedResult.confidence.experience, Icons.Default.Timeline),
                                                Triple("Education verification", parsedResult.confidence.education, Icons.Default.School),
                                                Triple("Certifications Audit", parsedResult.confidence.certifications, Icons.Default.WorkspacePremium),
                                            ).forEach { (field, conf, icon) ->
                                                val confPercent = conf.toInt()
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Text(field, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        LinearProgressIndicator(
                                                            progress = { confPercent / 100f },
                                                            modifier = Modifier.width(60.dp).height(3.dp).clip(CircleShape),
                                                            color = if (confPercent >= 80) Color(0xFF2E7D32) else if (confPercent >= 50) Color(0xFFFF9800) else Color(0xFFD32F2F)
                                                        )
                                                        Text("$confPercent%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (confPercent >= 80) Color(0xFF2E7D32) else if (confPercent >= 50) Color(0xFFFF9800) else Color(0xFFD32F2F))
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
            }

            // ATS Overall Score card with Circular alignment metric and breakdown sliders
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "REAL-TIME ATS SCREENING METRICS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Indicator Overall Score
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(96.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { parsedResult.atsAnalysis.overallScore / 100f },
                                    modifier = Modifier.size(96.dp),
                                    strokeWidth = 8.dp,
                                    color = when {
                                        parsedResult.atsAnalysis.overallScore >= 80 -> Color(0xFF4CAF50)
                                        parsedResult.atsAnalysis.overallScore >= 50 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${parsedResult.atsAnalysis.overallScore}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ATS FIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Sub-scores Breakdown Sliders
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProgressScoreRow("Formatting Quality", parsedResult.atsAnalysis.formattingScore)
                                ProgressScoreRow("Skills Coverage", parsedResult.atsAnalysis.skillCoverageScore)
                                ProgressScoreRow("Keyword Density", parsedResult.atsAnalysis.keywordOptimizationScore)
                                ProgressScoreRow("Education Rating", parsedResult.atsAnalysis.educationQualityScore)
                                ProgressScoreRow("Experience Quality", parsedResult.atsAnalysis.experienceQualityScore)
                            }
                        }

                        if (parsedResult.atsAnalysis.improvements.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "RECOMMENDED IMPROVEMENTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            parsedResult.atsAnalysis.improvements.forEach { improvement ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Improve",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Text(
                                        text = improvement,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Job Description Matcher and Gap Analysis card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "JOB DESCRIPTION ALIGNMENT SCORECARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (parsedResult.jdMatch != null) {
                            val match = parsedResult.jdMatch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "JD Alignment Grade",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Skill compatibility ratio with target profile",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        match.matchPercentage >= 80 -> Color(0xFFE8F5E9)
                                        match.matchPercentage >= 50 -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFFFEBEE)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "${match.matchPercentage}% MATCH",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            match.matchPercentage >= 80 -> Color(0xFF2E7D32)
                                            match.matchPercentage >= 50 -> Color(0xFFE65100)
                                            else -> Color(0xFFC62828)
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            if (match.matchingSkills.isNotEmpty()) {
                                Text(
                                    text = "COMPATIBLE SKILLS PRESENT (${match.matchingSkills.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    match.matchingSkills.forEach { skill ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(skill, style = MaterialTheme.typography.labelSmall) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFE8F5E9),
                                                labelColor = Color(0xFF2E7D32)
                                            ),
                                            border = null
                                        )
                                    }
                                }
                            }

                            if (match.missingSkills.isNotEmpty()) {
                                Text(
                                    text = "SKILLS GAP TO BRIDGE (${match.missingSkills.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    match.missingSkills.forEach { skill ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(skill, style = MaterialTheme.typography.labelSmall) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFFFEBEE),
                                                labelColor = Color(0xFFC62828)
                                            ),
                                            border = null
                                        )
                                    }
                                }
                            }

                            if (match.recommendations.isNotEmpty()) {
                                Text(
                                    text = "DEVELOPMENT RECOMMENDATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                match.recommendations.forEach { recommendation ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Step",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Text(
                                            text = recommendation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (match.candidateRanking.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AI Screening Fit Tier:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    val badgeColors = when (match.candidateRanking.lowercase().trim()) {
                                        "top tier", "strong fit", "top fit" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                        "mid tier", "medium fit", "mid fit" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                                        else -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = badgeColors.first) {
                                        Text(
                                            text = match.candidateRanking.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = badgeColors.second,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (match.criticalMissingSkills.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "⚠️ CRITICAL SKILLS UNDER-REPRESENTATION (AUTO-ELIMINATION RISK)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    match.criticalMissingSkills.forEach { skill ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(skill, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFFFEBEE),
                                                labelColor = Color(0xFFC62828)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFC62828)),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Re-assess panel
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (parsedResult.jdMatch != null) "Select alternative specs" else "Paste alternative job details to generate gap analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = targetJdText,
                                onValueChange = { targetJdText = it },
                                placeholder = { Text("Paste new job requirement here...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                enabled = !isMatchingJd,
                                textStyle = MaterialTheme.typography.bodySmall
                            )

                            Button(
                                onClick = {
                                    viewModel.matchJobDescription(targetJdText)
                                },
                                enabled = !isMatchingJd && targetJdText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isMatchingJd) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validating Skills alignment...")
                                } else {
                                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (parsedResult.jdMatch != null) "Re-Evaluate Match Ratio" else "Evaluate Match Alignment")
                                }
                            }
                        }
                    }
                }
            }

            // Technical and Soft Skills grid
            item {
                var showCategorizedSkills by remember { mutableStateOf(false) }
                val cats = parsedResult.categorizedSkills
                val hasCategorized = cats.programmingLanguages.isNotEmpty() || cats.frameworks.isNotEmpty() || cats.databases.isNotEmpty() || cats.cloud.isNotEmpty() || cats.devOps.isNotEmpty() || cats.aiMl.isNotEmpty()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "EXTRACTED SKILLS & COGNITIVE BAGDES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (parsedResult.technicalSkills.isNotEmpty()) {
                            Text(
                                text = "TECHNICAL EXPERTISE",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parsedResult.technicalSkills.forEach { skill ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = skill,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }
                        }

                        if (parsedResult.softSkills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "INTERPERSONAL & SOFT SKILLS",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parsedResult.softSkills.forEach { skill ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = skill,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }

                        if (hasCategorized) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showCategorizedSkills = !showCategorizedSkills },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text("Analyze Categorized Core Competencies", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Icon(
                                    imageVector = if (showCategorizedSkills) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (showCategorizedSkills) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val categorMap = listOf(
                                        "Languages" to cats.programmingLanguages,
                                        "Frameworks/API" to cats.frameworks,
                                        "Databases" to cats.databases,
                                        "Cloud Infra" to cats.cloud,
                                        "DevOps Tools" to cats.devOps,
                                        "AI/ML & NLP" to cats.aiMl
                                    )
                                    categorMap.forEach { (category, skillList) ->
                                        if (skillList.isNotEmpty()) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        skillList.forEach { skill ->
                                                            SuggestionChip(
                                                                onClick = {},
                                                                label = { Text(skill, style = MaterialTheme.typography.labelSmall) },
                                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                                ),
                                                                border = null
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
                }
            }

            // EXPERIENCE & EDUCATION TIMELINE FLOWS
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "EXPERIENCE & EDUCATION STORYLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Timeline Items of Experience
                            if (parsedResult.experienceHistory.isNotEmpty()) {
                                parsedResult.experienceHistory.forEachIndexed { idx, exp ->
                                    TimelineNode(
                                        title = "${exp.designation} at ${exp.companyName}",
                                        subtitle = "${exp.startDate} - ${exp.endDate} (${exp.duration})",
                                        isFirst = idx == 0,
                                        icon = Icons.Default.WorkOutline
                                    )
                                }
                            }

                            // Timeline Items of Education
                            if (parsedResult.educationHistory.isNotEmpty()) {
                                parsedResult.educationHistory.forEach { edu ->
                                    TimelineNode(
                                        title = edu.degree,
                                        subtitle = "${edu.mainInstitution} | Class of ${edu.passingYear}",
                                        isFirst = false,
                                        icon = Icons.Default.School
                                    )
                                }
                            }

                            if (parsedResult.experienceHistory.isEmpty() && parsedResult.educationHistory.isEmpty()) {
                                Text(
                                    text = "No history chronological records identified.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Projects & Highlight Credentials section
            if (parsedResult.projectsList.isNotEmpty() || parsedResult.certificationsList.isNotEmpty() || parsedResult.achievementsList.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "PROJECTS & CREDENTIALS ARTIFACTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (parsedResult.projectsList.isNotEmpty()) {
                                    Text(
                                        text = "HIGHLIGHTED PROJECTS",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    parsedResult.projectsList.forEach { proj ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountTree,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Text(
                                                text = proj,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (parsedResult.certificationsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "PROFESSIONAL CERTIFICATIONS",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    parsedResult.certificationsList.forEach { cert ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Text(
                                                text = cert,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (parsedResult.achievementsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "STANDOUT ACHIEVEMENTS",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    parsedResult.achievementsList.forEach { ach ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Text(
                                                text = ach,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Salaries and Notice Period Core Career block
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "RECRUITER PIPELINE CAREER ESTIMATES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Current Salary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(parsedResult.currentSalary.ifEmpty { "Not specified" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Expected Salary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(parsedResult.expectedSalary.ifEmpty { "Not specified" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Notice Period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(parsedResult.noticePeriod.ifEmpty { "Immediate" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (parsedResult.languagesKnown.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "LANGUAGES KNOWN: " + parsedResult.languagesKnown.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Automated AI Career Companion Tools Card (PHASE 16)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("ai_career_companion_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Companion",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "AI CAREER COMPANION ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Automated interview coaching, professional summary & cover letter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Summary Tab
                        if (parsedResult.resumeSummary.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("PRO CANDIDATE SUMMARY (ELEVATOR PITCH)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(parsedResult.resumeSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        // Guidance Tab
                        if (parsedResult.careerGuidance.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("STANDOUT CAREER DEVELOPMENT GUIDANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text(parsedResult.careerGuidance, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        // Interview Questions Coaching section
                        if (parsedResult.interviewQuestions.isNotEmpty()) {
                            var showInterviewSection by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showInterviewSection = !showInterviewSection },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text("Coaching: ${parsedResult.interviewQuestions.size} Personalized Interview Questions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = if (showInterviewSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (showInterviewSection) {
                                    parsedResult.interviewQuestions.forEachIndexed { i, qna ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) { 
                                                    Text(
                                                        "${i+1}", 
                                                        color = MaterialTheme.colorScheme.onPrimary, 
                                                        modifier = Modifier.padding(2.dp),
                                                        fontSize = 10.sp
                                                    ) 
                                                }
                                                Text(qna, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Cover Letter Generator Panel
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val coverLetterText by viewModel.coverLetterText.collectAsStateWithLifecycle()
                        val isGeneratingCoverLetter by viewModel.isGeneratingCoverLetter.collectAsStateWithLifecycle()
                        val clipboard = LocalClipboardManager.current

                        Text("PERSIONALIZED COVER LETTER GENERATOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        if (coverLetterText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("AI Cover Letter Draft", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(coverLetterText!!))
                                                    Toast.makeText(context, "Cover letter draft copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearCoverLetter() },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    Text(coverLetterText!!, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    // Use local targetJdText if pasted inside dashboard scorecard, or fall back to pastedJd from ParseScreen!
                                    val jd = targetJdText.ifBlank { viewModel.pastedJd.value }
                                    if (jd.isBlank()) {
                                        Toast.makeText(context, "Please paste or select job description details first to generate custom cover letters.", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.generateCoverLetter(jd)
                                    }
                                },
                                enabled = !isGeneratingCoverLetter,
                                modifier = Modifier.fillMaxWidth().testTag("cover_letter_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isGeneratingCoverLetter) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Synthesizing tailored cover letter...")
                                } else {
                                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Customized Cover Letter")
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
fun ProgressScoreRow(
    label: String,
    score: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            Text("$score/100", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun TimelineNode(
    title: String,
    subtitle: String,
    isFirst: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFirst) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(bottom = 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: ResumeViewModel,
    historyList: List<ResumeEntity>,
    filteredHistory: List<ResumeEntity>,
    onSelect: (ResumeEntity) -> Unit,
    onDelete: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val attempts by viewModel.parsingAttempts.collectAsStateWithLifecycle()
    val successes by viewModel.parsingSuccessCount.collectAsStateWithLifecycle()
    val totalTime by viewModel.totalProcessingTimeMillis.collectAsStateWithLifecycle()

    val candidateComparisonResult by viewModel.candidateComparisonResult.collectAsStateWithLifecycle()
    val isComparingCandidates by viewModel.isComparingCandidates.collectAsStateWithLifecycle()
    var selectedEntitiesForComparison by remember { mutableStateOf(emptySet<ResumeEntity>()) }
    val context = LocalContext.current

    val rate = remember(attempts, successes, historyList.size) {
        val totalAttempts = attempts.coerceAtLeast(historyList.size)
        val totalSuccess = successes.coerceAtLeast(historyList.size)
        if (totalAttempts > 0) (totalSuccess.toFloat() / totalAttempts * 100).toInt() else 100
    }

    val avgSpeed = remember(totalTime, successes, historyList.size) {
        val count = successes.coerceAtLeast(historyList.size)
        if (count > 0) {
            val df = DecimalFormat("#.#")
            df.format((totalTime.toFloat() / 1000f) / count.toFloat().coerceAtLeast(1f)) + "s"
        } else "4.2s"
    }

    val avgAtsScore = remember(historyList) {
        if (historyList.isNotEmpty()) {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val overallScores = historyList.mapNotNull {
                try {
                    val data = moshi.adapter(ResumeData::class.java).fromJson(it.rawJson)
                    data?.atsAnalysis?.overallScore ?: 70
                } catch (e: Exception) {
                    70
                }
            }
            overallScores.average().toInt()
        } else 0
    }

    // Modal view for Comparative report (PHASE 16)
    if (candidateComparisonResult != null) {
        val clipboard = LocalClipboardManager.current
        Dialog(onDismissRequest = { viewModel.clearCandidateComparison() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("AI Side-by-Side Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.clearCandidateComparison() }) { 
                            Icon(Icons.Default.Close, contentDescription = "Close dialog") 
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Text(
                                    text = candidateComparisonResult!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.clearCandidateComparison() }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { 
                            Text("Dismiss") 
                        }
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(candidateComparisonResult!!))
                                Toast.makeText(context, "Comparison report copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Report")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Real-Time SQLite analytics dashboard metrics cards
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REAL-TIME SQL DATABASE ANALYTICS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Total Parsed",
                            value = "${historyList.size}",
                            icon = Icons.Default.History,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Success Rate",
                            value = "$rate%",
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Avg Pipeline",
                            value = avgSpeed,
                            icon = Icons.Default.Bolt,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Mean ATS Score",
                            value = "$avgAtsScore",
                            icon = Icons.Default.QueryStats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Selected Side-by-Side Multi-Comparative Card (PHASE 16)
        if (selectedEntitiesForComparison.isNotEmpty() || isComparingCandidates) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Side-by-Side Candidate Screen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Select 2 or more candidates to cross-compare features, alignment, and gaps.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            if (!isComparingCandidates) {
                                TextButton(onClick = { selectedEntitiesForComparison = emptySet() }) {
                                    Text("Clear All", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isComparingCandidates) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Engaging cognitive side-by-side matrices...", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedEntitiesForComparison.size < 2) {
                                        Toast.makeText(context, "Select at least 2 candidates to initiate side-by-side comparison report.", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.compareCandidates(selectedEntitiesForComparison.toList())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                enabled = selectedEntitiesForComparison.size >= 2
                            ) {
                                Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Evaluate ${selectedEntitiesForComparison.size} Screened Candidates")
                            }
                        }
                    }
                }
            }
        }

        // LIVE FILTER / ADVANCED SEARCH TEXT BOX
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Filter Talent (Degree, Skill, College, Cert or Location)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("database_search_input"),
                placeholder = { Text("Type 'Python', 'B.S.', 'Stanford'...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (filteredHistory.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Candidates Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing the search query keyword or parse new documents to archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Screened Candidates (${filteredHistory.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(filteredHistory) { item ->
                val dateStr = remember(item.timestamp) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                    sdf.format(Date(item.timestamp))
                }

                // Parse individual scores for list item display
                val candidateData = remember(item.rawJson) {
                    try {
                        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                            .adapter(ResumeData::class.java).fromJson(item.rawJson)
                    } catch (e: Exception) {
                        null
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .testTag("history_item_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSelected = selectedEntitiesForComparison.contains(item)
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedEntitiesForComparison = if (checked) {
                                    selectedEntitiesForComparison + item
                                } else {
                                    selectedEntitiesForComparison - item
                                }
                            },
                            modifier = Modifier.testTag("candidate_select_checkbox_${item.id}")
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                // Render Badge Overall ATS fit
                                if (candidateData != null) {
                                    val atsScore = candidateData.atsAnalysis.overallScore
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            atsScore >= 80 -> Color(0xFFE8F5E9)
                                            atsScore >= 50 -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                    ) {
                                        Text(
                                            text = "ATS: $atsScore",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = when {
                                                atsScore >= 80 -> Color(0xFF2E7D32)
                                                atsScore >= 50 -> Color(0xFFE65100)
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Muted Profile archive - parsed on $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (candidateData != null && candidateData.technicalSkills.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Main skills: " + candidateData.technicalSkills.take(3).joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier.testTag("delete_history_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete from local Database",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (value.isNotEmpty() && value != "Not specified" && value != "Not listed" && !value.contains("Not specified")) {
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(value))
                    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun JsonViewDialog(
    jsonString: String,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted JSON Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close View")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = jsonString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(jsonString))
                            Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("copy_json_dialog_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON")
                    }
                }
            }
        }
    }
}

@Composable
fun OpeningSplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background aura gradient
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer(
                        scaleX = scaleAnim * pulseScale,
                        scaleY = scaleAnim * pulseScale,
                        alpha = alphaAnim
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                        contentDescription = "Resume Parser Pro Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            ) {
                Text(
                    text = "Resume Parser Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Enterprise AI Screening & ATS Intelligence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Initializing neural parsing engine...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
