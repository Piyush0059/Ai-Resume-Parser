package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.model.AtsDetails
import com.example.data.model.JdMatchDetails
import com.example.data.model.ResumeData
import com.example.data.model.ResumeEntity
import com.example.data.repository.ResumeRepository
import com.example.util.ResumeParserUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class QueueStatus { PENDING, PROCESSING, COMPLETED, FAILED }

data class QueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val sizeString: String,
    val status: QueueStatus = QueueStatus.PENDING,
    val progress: Float = 0f,
    val error: String? = null
)

data class StageDiagnostic(
    val id: String,
    val name: String,
    val status: String, // "PENDING", "ACTIVE", "SUCCESS", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val resultSummary: String = "",
    val errorDetails: String = ""
)

data class DiagnosticsInfo(
    val currentStep: String = "None2",
    val overallStatus: String = "Idle",
    val steps: List<StageDiagnostic> = emptyList(),
    val apiResponseCode: String = "N/A",
    val modelUsed: String = "N/A",
    val lastExceptionClass: String = "N/A",
    val lastExceptionMessage: String = "N/A",
    val lastStackTrace: String = "N/A",
    val rawApiResponse: String = "N/A"
)

class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val _diagnostics = MutableStateFlow(DiagnosticsInfo())
    val diagnostics = _diagnostics.asStateFlow()

    fun initDiagnostics() {
        val initialSteps = listOf(
            StageDiagnostic("manifest_permissions", "Manifest & System Permissions", "PENDING"),
            StageDiagnostic("file_selection", "File Selection", "PENDING"),
            StageDiagnostic("file_read", "File Reading", "PENDING"),
            StageDiagnostic("pdf_extraction", "PDF Character Extraction", "PENDING"),
            StageDiagnostic("ocr", "OCR Preprocessing", "PENDING"),
            StageDiagnostic("gemini_request", "Gemini Request Generation", "PENDING"),
            StageDiagnostic("gemini_response", "Gemini Response Raw Receive", "PENDING"),
            StageDiagnostic("json_parsing", "JSON Sanitization & Parsing", "PENDING"),
            StageDiagnostic("ats_scoring", "ATS Scoring Analysis", "PENDING"),
            StageDiagnostic("ui_rendering", "UI Rendering Preparation", "PENDING")
        )
        _diagnostics.value = DiagnosticsInfo(
            currentStep = "None",
            overallStatus = "Idle",
            steps = initialSteps,
            apiResponseCode = "N/A",
            modelUsed = "N/A",
            lastExceptionClass = "N/A",
            lastExceptionMessage = "N/A",
            lastStackTrace = "N/A",
            rawApiResponse = "N/A"
        )
        
        // Auto validate critical permissions
        checkPermissionsAndValidate()
    }

    fun checkPermissionsAndValidate(): Boolean {
        updateDiagnosticStep("manifest_permissions", "ACTIVE")
        val context = getApplication<Application>()
        
        val requiredPermissions = listOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE
        )
        
        val missing = mutableListOf<String>()
        for (perm in requiredPermissions) {
            val granted = context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                missing.add(perm)
            }
        }
        
        return if (missing.isEmpty()) {
            updateDiagnosticStep(
                "manifest_permissions",
                "SUCCESS",
                resultSummary = "All required permissions (INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE) are declared and active."
            )
            true
        } else {
            val missingSummary = missing.joinToString(", ") { it.substringAfterLast(".") }
            updateDiagnosticStep(
                "manifest_permissions",
                "FAILED",
                errorDetails = "Missing system permissions: $missingSummary. Please ensure normal permissions are declared in AndroidManifest.xml"
            )
            false
        }
    }

    fun updateDiagnosticStep(
        stepId: String,
        status: String,
        durationMs: Long = 0,
        resultSummary: String = "",
        errorDetails: String = ""
    ) {
        val currentInfo = _diagnostics.value
        val updatedSteps = currentInfo.steps.map { step ->
            if (step.id == stepId) {
                step.copy(
                    status = status,
                    timestamp = System.currentTimeMillis(),
                    durationMs = durationMs,
                    resultSummary = resultSummary,
                    errorDetails = errorDetails
                )
            } else {
                step
            }
        }
        
        var currentStepName = currentInfo.currentStep
        val activeStep = updatedSteps.firstOrNull { it.status == "ACTIVE" }
        if (activeStep != null) {
            currentStepName = activeStep.name
        } else {
            val lastSuccessOrFailed = updatedSteps.lastOrNull { it.status == "SUCCESS" || it.status == "FAILED" }
            if (lastSuccessOrFailed != null) {
                currentStepName = "${lastSuccessOrFailed.name} (${lastSuccessOrFailed.status})"
            }
        }

        _diagnostics.value = currentInfo.copy(
            currentStep = currentStepName,
            steps = updatedSteps
        )
    }

    fun updateDiagnosticsError(
        e: Throwable,
        apiCode: String = "N/A",
        model: String = "N/A",
        rawResp: String = "N/A"
    ) {
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        val stackTraceStr = sw.toString()
        
        _diagnostics.value = _diagnostics.value.copy(
            overallStatus = "FAILED",
            apiResponseCode = apiCode,
            modelUsed = model,
            lastExceptionClass = e::class.java.simpleName,
            lastExceptionMessage = e.localizedMessage ?: "Unknown error",
            lastStackTrace = stackTraceStr,
            rawApiResponse = rawResp
        )
    }

    fun setDiagnosticsGlobalFields(
        overallStatus: String? = null,
        apiCode: String? = null,
        model: String? = null,
        rawResp: String? = null
    ) {
        val current = _diagnostics.value
        _diagnostics.value = current.copy(
            overallStatus = overallStatus ?: current.overallStatus,
            apiResponseCode = apiCode ?: current.apiResponseCode,
            modelUsed = model ?: current.modelUsed,
            rawApiResponse = rawResp ?: current.rawApiResponse
        )
    }

    private val repository: ResumeRepository
    val historyList: StateFlow<List<ResumeEntity>>

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // UI Input State variables
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri = _selectedUri.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName = _selectedFileName.asStateFlow()

    private val _selectedMimeType = MutableStateFlow<String?>(null)
    val selectedMimeType = _selectedMimeType.asStateFlow()

    private val _pastedText = MutableStateFlow("")
    val pastedText = _pastedText.asStateFlow()

    private val _pastedJd = MutableStateFlow("")
    val pastedJd = _pastedJd.asStateFlow()

    // Search and Candidate filter variable
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Parsing state tracking
    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _parseStatusText = MutableStateFlow("")
    val parseStatusText = _parseStatusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _parsedResult = MutableStateFlow<ResumeData?>(null)
    val parsedResult = _parsedResult.asStateFlow()

    private val _currentViewJson = MutableStateFlow<String?>(null)
    val currentViewJson = _currentViewJson.asStateFlow()

    // JDMaching state independent trigger
    private val _isMatchingJd = MutableStateFlow(false)
    val isMatchingJd = _isMatchingJd.asStateFlow()

    // Analytics Metrics in-memory overrides for rate calculation
    private val _parsingAttempts = MutableStateFlow(0)
    val parsingAttempts = _parsingAttempts.asStateFlow()

    private val _parsingSuccessCount = MutableStateFlow(0)
    val parsingSuccessCount = _parsingSuccessCount.asStateFlow()

    private val _totalProcessingTimeMillis = MutableStateFlow(0L)
    val totalProcessingTimeMillis = _totalProcessingTimeMillis.asStateFlow()

    // Advanced filtering state variables
    private val _filterMinAts = MutableStateFlow(0)
    val filterMinAts = _filterMinAts.asStateFlow()

    private val _filterLocation = MutableStateFlow("")
    val filterLocation = _filterLocation.asStateFlow()

    private val _filterDegree = MutableStateFlow("")
    val filterDegree = _filterDegree.asStateFlow()

    private val _filterCertification = MutableStateFlow("")
    val filterCertification = _filterCertification.asStateFlow()

    // Bulk queue management states
    private val _bulkQueue = MutableStateFlow<List<QueueItem>>(emptyList())
    val bulkQueue = _bulkQueue.asStateFlow()

    private val _isBulkProcessing = MutableStateFlow(false)
    val isBulkProcessing = _isBulkProcessing.asStateFlow()

    private val _bulkProgress = MutableStateFlow(0f)
    val bulkProgress = _bulkProgress.asStateFlow()

    private val _bulkStatusText = MutableStateFlow("")
    val bulkStatusText = _bulkStatusText.asStateFlow()

    // On-demand AI career companion generation states
    private val _coverLetterText = MutableStateFlow<String?>(null)
    val coverLetterText = _coverLetterText.asStateFlow()

    private val _isGeneratingCoverLetter = MutableStateFlow(false)
    val isGeneratingCoverLetter = _isGeneratingCoverLetter.asStateFlow()

    private val _candidateComparisonResult = MutableStateFlow<String?>(null)
    val candidateComparisonResult = _candidateComparisonResult.asStateFlow()

    private val _isComparingCandidates = MutableStateFlow(false)
    val isComparingCandidates = _isComparingCandidates.asStateFlow()

    // Theme preference state: "SYSTEM", "LIGHT", "DARK"
    private val _appThemeMode = MutableStateFlow("SYSTEM")
    val appThemeMode = _appThemeMode.asStateFlow()

    fun setAppThemeMode(mode: String) {
        _appThemeMode.value = mode
    }

    fun toggleAppTheme() {
        _appThemeMode.value = when (_appThemeMode.value) {
            "SYSTEM" -> "DARK"
            "DARK" -> "LIGHT"
            else -> "SYSTEM"
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ResumeRepository(database.resumeDao())
        historyList = repository.allResumes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        initDiagnostics()
    }

    // Filter candidate list dynamically matching advanced filters and search queries
    @Suppress("UNCHECKED_CAST")
    val filteredHistory: StateFlow<List<ResumeEntity>> = combine(
        historyList,
        _searchQuery,
        _filterMinAts,
        _filterLocation,
        _filterDegree,
        _filterCertification
    ) { flows ->
        val list = flows[0] as List<ResumeEntity>
        val query = flows[1] as String
        val minAts = flows[2] as Int
        val loc = flows[3] as String
        val deg = flows[4] as String
        val cert = flows[5] as String

        list.filter { entity ->
            try {
                val data = moshi.adapter(ResumeData::class.java).fromJson(entity.rawJson)
                if (data != null) {
                    val matchesQuery = query.isBlank() || (
                        data.name.contains(query, ignoreCase = true) ||
                        data.skills.any { it.contains(query, ignoreCase = true) } ||
                        data.technicalSkills.any { it.contains(query, ignoreCase = true) } ||
                        data.softSkills.any { it.contains(query, ignoreCase = true) } ||
                        data.location.contains(query, ignoreCase = true) ||
                        data.certificationsList.any { it.contains(query, ignoreCase = true) } ||
                        data.educationHistory.any {
                            it.mainInstitution.contains(query, ignoreCase = true) ||
                            it.degree.contains(query, ignoreCase = true)
                        } ||
                        data.experienceHistory.any {
                            it.companyName.contains(query, ignoreCase = true) ||
                            it.designation.contains(query, ignoreCase = true)
                        }
                    )

                    val matchesAts = data.atsAnalysis.overallScore >= minAts
                    val matchesLoc = loc.isBlank() || data.location.contains(loc, ignoreCase = true)
                    val matchesDeg = deg.isBlank() || data.educationHistory.any { it.degree.contains(deg, ignoreCase = true) || it.mainInstitution.contains(deg, ignoreCase = true) }
                    val matchesCert = cert.isBlank() || data.certificationsList.any { it.contains(cert, ignoreCase = true) } || data.certifications.any { it.contains(cert, ignoreCase = true) }

                    matchesQuery && matchesAts && matchesLoc && matchesDeg && matchesCert
                } else {
                    query.isBlank() || entity.title.contains(query, ignoreCase = true)
                }
            } catch (e: Exception) {
                query.isBlank() || entity.title.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateAdvancedFilters(minAts: Int, location: String, degree: String, certification: String) {
        _filterMinAts.value = minAts
        _filterLocation.value = location
        _filterDegree.value = degree
        _filterCertification.value = certification
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _filterMinAts.value = 0
        _filterLocation.value = ""
        _filterDegree.value = ""
        _filterCertification.value = ""
    }

    // Bulk queue management helpers
    fun addToBulkQueue(uris: List<Uri>, context: Context) {
        val currentList = _bulkQueue.value.toMutableList()
        uris.forEach { uri ->
            val name = ResumeParserUtils.getFileName(context, uri)
            val item = QueueItem(uri = uri, name = name, sizeString = "Document", status = QueueStatus.PENDING)
            currentList.add(item)
        }
        _bulkQueue.value = currentList
    }

    fun clearBulkQueue() {
        _bulkQueue.value = emptyList()
        _bulkProgress.value = 0f
        _bulkStatusText.value = ""
    }

    fun removeFromQueue(id: String) {
        _bulkQueue.value = _bulkQueue.value.filter { it.id != id }
    }

    fun processBulkQueue(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val queue = _bulkQueue.value.toMutableList()
            val pendingItems = queue.filter { it.status == QueueStatus.PENDING }
            if (pendingItems.isEmpty()) return@launch

            _isBulkProcessing.value = true
            val totalPending = pendingItems.size
            var processedCount = 0
            _bulkStatusText.value = "Initializing enterprise batch processor for ($totalPending) items..."

            for (i in queue.indices) {
                val item = queue[i]
                if (item.status != QueueStatus.PENDING) continue

                queue[i] = item.copy(status = QueueStatus.PROCESSING, progress = 0.25f)
                _bulkQueue.value = queue.toList()
                _bulkStatusText.value = "Processing (${i + 1}/${queue.size}): ${item.name}"

                try {
                    val mime = context.contentResolver.getType(item.uri) ?: ""
                    val isPdf = mime == "application/pdf" || item.name.endsWith(".pdf", ignoreCase = true)
                    val isImage = mime.startsWith("image/") || item.name.endsWith(".png", ignoreCase = true) || item.name.endsWith(".jpg", ignoreCase = true) || item.name.endsWith(".jpeg", ignoreCase = true)
                    val isMultimodal = isPdf || isImage

                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                        throw Exception("Google Gemini API key not found in build parameters.")
                    }

                    var parsedData: ResumeData? = null
                    var success = false

                    // STRATEGY A: Standard parsing
                    try {
                        val contentPart = if (isMultimodal) {
                            val mimeTypeForPart = if (isImage) {
                                mime.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                            } else {
                                "application/pdf"
                            }
                            val bytes = ResumeParserUtils.getUriBytes(context, item.uri)
                                ?: throw Exception("File storage not accessible or bytes empty.")
                            
                            val preprocessedBytes = if (isImage) {
                                ResumeParserUtils.preprocessImageBytes(bytes)
                            } else {
                                bytes
                            }
                            val base64 = ResumeParserUtils.bytesToBase64(preprocessedBytes)
                            Part(inlineData = InlineData(mimeType = mimeTypeForPart, data = base64))
                        } else {
                            val extracted = ResumeParserUtils.extractTextFromUri(context, item.uri, mime)
                            val textToParse = extracted ?: ""
                            if (textToParse.trim().isEmpty()) {
                                throw Exception("Non-multimodal layout is entirely empty.")
                            }
                            Part(text = textToParse)
                        }

                        val promptText = getPromptMarkup(currentJd = "")
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(contentPart, Part(text = promptText)))),
                            generationConfig = GenerationConfig(
                                responseMimeType = "application/json",
                                temperature = 0.15f
                            )
                        )

                        val response = runApiBlockWithRetry { model ->
                            RetrofitClient.service.generateContent(model, apiKey, request)
                        }
                        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: throw Exception("Empty stream context.")

                        parsedData = parseJsonSafely(ResumeData::class.java, jsonText)
                        success = true
                    } catch (strA: Throwable) {
                        android.util.Log.e("BULK_PIPELINE", "Strategy A failed for ${item.name}", strA)
                        val msg = strA.localizedMessage ?: ""
                        if (msg.contains("API key", ignoreCase = true)) {
                            throw strA
                        }
                    }

                    // STRATEGY B: Fallback rendering/extraction
                    if (!success || parsedData == null) {
                        if (isPdf) {
                            val pageBytesList = ResumeParserUtils.renderPdfToImages(context, item.uri, maxPages = 3)
                            if (pageBytesList.isEmpty()) {
                                throw Exception("Unable to process empty PDF document.")
                            }
                            val parts = pageBytesList.map { bytes ->
                                val base64 = ResumeParserUtils.bytesToBase64(bytes)
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))
                            }.toMutableList()
                            
                            parts.add(Part(text = getPromptMarkup(currentJd = "")))
                            val request = GenerateContentRequest(
                                contents = listOf(Content(parts = parts)),
                                generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.15f)
                            )
                            val response = runApiBlockWithRetry { model ->
                                RetrofitClient.service.generateContent(model, apiKey, request)
                            }
                            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: throw Exception("Fallback parser failed during Gemini PDF-to-Image request.")
                            
                            parsedData = parseJsonSafely(ResumeData::class.java, jsonText)
                        } else if (!isImage) {
                            val recoveredText = ResumeParserUtils.extractPrintableTextCompiled(context, item.uri)
                            if (recoveredText.trim().isEmpty()) {
                                throw Exception("Char extraction returned blank.")
                            }
                            val request = GenerateContentRequest(
                                contents = listOf(
                                    Content(parts = listOf(Part(text = recoveredText), Part(text = getPromptMarkup(currentJd = ""))))
                                ),
                                generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.15f)
                            )
                            val response = runApiBlockWithRetry { model ->
                                RetrofitClient.service.generateContent(model, apiKey, request)
                            }
                            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: throw Exception("Character fallback extraction failed.")
                            
                            parsedData = parseJsonSafely(ResumeData::class.java, jsonText)
                        } else {
                            throw Exception("Unable to process resume. Please try again.")
                        }
                    }

                    val finalData = parsedData ?: throw Exception("Unable to process resume. Please try again.")
                    val syncedResumeData = syncResumeFields(finalData)

                    val updatedJson = moshi.adapter(ResumeData::class.java).toJson(syncedResumeData)
                    val displayTitle = item.name.substringBeforeLast(".")

                    val entity = ResumeEntity(
                        title = displayTitle,
                        rawJson = updatedJson,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertResume(entity)

                    processedCount++
                    queue[i] = item.copy(status = QueueStatus.COMPLETED, progress = 1.0f)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    queue[i] = item.copy(status = QueueStatus.FAILED, progress = 0.0f, error = getLocalizedErrorMessage(e))
                }
                _bulkQueue.value = queue.toList()
                _bulkProgress.value = (processedCount.toFloat() / totalPending.toFloat())
            }

            _isBulkProcessing.value = false
            _bulkStatusText.value = "Batch processing complete: screened $processedCount out of $totalPending resumes."
        }
    }

    fun selectFile(uri: Uri, context: Context) {
        initDiagnostics()
        updateDiagnosticStep("file_selection", "ACTIVE")
        _selectedUri.value = uri
        _selectedMimeType.value = context.contentResolver.getType(uri)
        _selectedFileName.value = ResumeParserUtils.getFileName(context, uri)
        _pastedText.value = ""
        _errorMessage.value = null
        updateDiagnosticStep(
            "file_selection",
            "SUCCESS",
            resultSummary = "URI: $uri | Mime: ${_selectedMimeType.value} | File: ${_selectedFileName.value}"
        )
    }

    fun selectSampleText(text: String, sampleTitle: String) {
        initDiagnostics()
        updateDiagnosticStep("file_selection", "ACTIVE")
        _selectedUri.value = null
        _selectedFileName.value = sampleTitle
        _selectedMimeType.value = "text/plain"
        _pastedText.value = text
        _errorMessage.value = null
        updateDiagnosticStep(
            "file_selection",
            "SUCCESS",
            resultSummary = "Title: $sampleTitle | Text size: ${text.length} chars"
        )
    }

    fun clearFile() {
        _selectedUri.value = null
        _selectedFileName.value = null
        _selectedMimeType.value = null
        _errorMessage.value = null
    }

    fun updatePastedText(text: String) {
        _pastedText.value = text
        if (text.isNotEmpty()) {
            _selectedUri.value = null
            _selectedFileName.value = null
            _selectedMimeType.value = null
        }
    }

    fun updatePastedJd(jd: String) {
        _pastedJd.value = jd
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadFromHistory(resumeEntity: ResumeEntity) {
        try {
            val data = moshi.adapter(ResumeData::class.java).fromJson(resumeEntity.rawJson)
            _parsedResult.value = data
            _currentViewJson.value = resumeEntity.rawJson
            _selectedFileName.value = resumeEntity.title
            _errorMessage.value = null
            clearCoverLetter()
            clearCandidateComparison()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load the selected resume profile cleanly. Please try importing another file."
        }
    }

    fun deleteFromHistory(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteResume(id)
            if (_parsedResult.value != null) {
                _parsedResult.value = null
                _currentViewJson.value = null
            }
        }
    }

    fun clearResult() {
        _parsedResult.value = null
        _currentViewJson.value = null
        clearCoverLetter()
        clearCandidateComparison()
    }

    // High performance extraction logic
    fun parseResume(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isParsing.value = true
            _parseStatusText.value = "Preparing candidate document..."
            _errorMessage.value = null
            _parsingAttempts.value += 1
            val startTime = System.currentTimeMillis()

            try {
                setDiagnosticsGlobalFields(overallStatus = "RUNNING")
                _parseStatusText.value = "Evaluating file metadata..."
                
                val isPdf = _selectedUri.value != null && (
                        _selectedMimeType.value == "application/pdf" ||
                        _selectedFileName.value?.endsWith(".pdf", ignoreCase = true) == true
                )
                
                val isImage = _selectedUri.value != null && (
                        _selectedMimeType.value?.startsWith("image/") == true ||
                        _selectedFileName.value?.endsWith(".png", ignoreCase = true) == true ||
                        _selectedFileName.value?.endsWith(".jpg", ignoreCase = true) == true ||
                        _selectedFileName.value?.endsWith(".jpeg", ignoreCase = true) == true
                )

                val isMultimodal = isPdf || isImage

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    throw Exception("Google Gemini API Key is missing! Enter it securely inside the Secrets panel of AI Studio.")
                }

                var success = false
                var syncedResumeData: ResumeData? = null

                // 1. File Reading stage
                val readStart = System.currentTimeMillis()
                updateDiagnosticStep("file_read", "ACTIVE")
                val bytes = if (_selectedUri.value != null) {
                    try {
                        val parsedBytes = ResumeParserUtils.getUriBytes(context, _selectedUri.value!!)
                        updateDiagnosticStep(
                            "file_read",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - readStart,
                            resultSummary = "Read ${parsedBytes.size} bytes from Local storage SAF Provider."
                        )
                        parsedBytes
                    } catch (ex: Throwable) {
                        updateDiagnosticStep(
                            "file_read",
                            "FAILED",
                            durationMs = System.currentTimeMillis() - readStart,
                            errorDetails = "Storage access failed: ${ex.localizedMessage}\n${ex.stackTraceToString()}"
                        )
                        throw ex
                    }
                } else {
                    updateDiagnosticStep(
                        "file_read",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - readStart,
                        resultSummary = "No Uri selected. Pasted text directly input."
                    )
                    null
                }

                // 2. OCR Preprocessing stage
                val ocrStart = System.currentTimeMillis()
                updateDiagnosticStep("ocr", "ACTIVE")
                val finalBytes = if (isImage && bytes != null) {
                    try {
                        val preprocessBytes = ResumeParserUtils.preprocessImageBytes(bytes)
                        updateDiagnosticStep(
                            "ocr",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - ocrStart,
                            resultSummary = "Denoised and deskewed pixel buffer. Denoised size: ${preprocessBytes.size} bytes."
                        )
                        preprocessBytes
                    } catch (ex: Throwable) {
                        updateDiagnosticStep(
                            "ocr",
                            "SUCCESS", // Recoverable, warn in resultSummary
                            durationMs = System.currentTimeMillis() - ocrStart,
                            resultSummary = "Image prep failed: ${ex.localizedMessage}. Proceeding with original bytes."
                        )
                        bytes
                    }
                } else {
                    updateDiagnosticStep(
                        "ocr",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - ocrStart,
                        resultSummary = "Omitted preprocessing. Selected file is not an image."
                    )
                    bytes
                }

                // 3. PDF Parsing/Character extraction stage
                val pdfStart = System.currentTimeMillis()
                updateDiagnosticStep("pdf_extraction", "ACTIVE")
                if (isPdf && _selectedUri.value != null) {
                    try {
                        val printableSample = ResumeParserUtils.extractPrintableTextCompiled(context, _selectedUri.value!!)
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - pdfStart,
                            resultSummary = "Scanned PDF layout check. Text character extract size: ${printableSample.length} characters."
                        )
                    } catch (ex: Throwable) {
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "SUCCESS", // Recoverable since we have fallback strategy B
                            durationMs = System.currentTimeMillis() - pdfStart,
                            resultSummary = "PDF Text Extraction check warning: ${ex.localizedMessage}."
                        )
                    }
                } else if (_selectedUri.value != null && !isImage) {
                    try {
                        val extracted = ResumeParserUtils.extractTextFromUri(context, _selectedUri.value!!, _selectedMimeType.value)
                        val len = extracted?.length ?: 0
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - pdfStart,
                            resultSummary = "Read Word/Text document bytes: $len characters extracted."
                        )
                    } catch (ex: Throwable) {
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "FAILED",
                            durationMs = System.currentTimeMillis() - pdfStart,
                            errorDetails = "Word document text extraction failed: ${ex.localizedMessage}"
                        )
                        throw ex
                    }
                } else {
                    updateDiagnosticStep(
                        "pdf_extraction",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - pdfStart,
                        resultSummary = "Non-document or pasted text source. Omitted text extractor step."
                    )
                }

                // STRATEGY A: Standard parsing
                val reqStart = System.currentTimeMillis()
                updateDiagnosticStep("gemini_request", "ACTIVE")
                try {
                    val contentPart: Part = if (isMultimodal) {
                        _parseStatusText.value = "Extracting binary stream..."
                        val mime = if (isImage) {
                            _selectedMimeType.value?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                        } else {
                            "application/pdf"
                        }
                        _parseStatusText.value = "Preparing high-density prompt schema..."
                        Part(inlineData = InlineData(mimeType = mime, data = ResumeParserUtils.bytesToBase64(finalBytes!!)))
                    } else {
                        var textToParse = _pastedText.value
                        if (_selectedUri.value != null && textToParse.isEmpty()) {
                            _parseStatusText.value = "Analyzing MS Word / Text formats..."
                            textToParse = ResumeParserUtils.extractTextFromUri(context, _selectedUri.value!!, _selectedMimeType.value) ?: ""
                        }
                        if (textToParse.trim().isEmpty()) {
                            throw Exception("No content text found.")
                        }
                        Part(text = textToParse)
                    }

                    val currentJd = _pastedJd.value.trim()
                    _parseStatusText.value = "Formulating neural instructions..."

                    val systemPrompt = getPromptMarkup(currentJd = currentJd)

                    val request = GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(contentPart, Part(text = systemPrompt)))
                        ),
                        generationConfig = GenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.15f
                        )
                    )
                    
                    updateDiagnosticStep(
                        "gemini_request",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - reqStart,
                        resultSummary = "Constructed payload parts. MimeType parameter: ${if (isMultimodal) "multimodal" else "text"}"
                    )

                    val respStart = System.currentTimeMillis()
                    updateDiagnosticStep("gemini_response", "ACTIVE")
                    _parseStatusText.value = "Executing Gemini layout OCR..."
                    
                    val response = runApiBlockWithRetry { model ->
                        setDiagnosticsGlobalFields(model = model)
                        RetrofitClient.service.generateContent(model, apiKey, request)
                    }

                    val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: throw Exception("Empty response.")

                    setDiagnosticsGlobalFields(rawResp = jsonText, apiCode = "200 OK")
                    updateDiagnosticStep(
                        "gemini_response",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - respStart,
                        resultSummary = "Returned candidate successfully. Payload length: ${jsonText.length} bytes."
                    )

                    val jsonStart = System.currentTimeMillis()
                    updateDiagnosticStep("json_parsing", "ACTIVE")
                    _parseStatusText.value = "Normalizing JSON tree structure..."
                    
                    val resumeData = parseJsonSafely(ResumeData::class.java, jsonText)
                    syncedResumeData = syncResumeFields(resumeData)
                    
                    updateDiagnosticStep(
                        "json_parsing",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - jsonStart,
                        resultSummary = "JSON parsing succeeded under lenient mode constraints."
                    )

                    val atsStart = System.currentTimeMillis()
                    updateDiagnosticStep("ats_scoring", "ACTIVE")
                    val score = syncedResumeData.atsAnalysis.overallScore
                    updateDiagnosticStep(
                        "ats_scoring",
                        "SUCCESS",
                        durationMs = System.currentTimeMillis() - atsStart,
                        resultSummary = "Candidate ATS rating computed: $score% suitability details analyzed."
                    )
                    
                    success = true
                } catch (strategyAError: Throwable) {
                    android.util.Log.e("PARSING_PIPELINE", "Strategy A failed, checking fallbacks...", strategyAError)
                    updateDiagnosticStep(
                        "gemini_response",
                        "FAILED",
                        errorDetails = "Strategy A failed: ${strategyAError.localizedMessage}\n${strategyAError.stackTraceToString()}"
                    )
                    val msg = strategyAError.localizedMessage ?: ""
                    if (msg.contains("API key", ignoreCase = true)) {
                        updateDiagnosticsError(strategyAError, apiCode = "401 Unauthorized")
                        throw strategyAError
                    }
                }

                // STRATEGY B: Fallback (if Strategy A failed or returned nothing)
                if (!success || syncedResumeData == null) {
                    _parseStatusText.value = "Activating fallback pipeline & local rendering..."
                    if (isPdf) {
                        val pdfRenderStart = System.currentTimeMillis()
                        updateDiagnosticStep("pdf_extraction", "ACTIVE")
                        val pageBytesList = ResumeParserUtils.renderPdfToImages(context, _selectedUri.value!!, maxPages = 3)
                        if (pageBytesList.isEmpty()) {
                            throw Exception("Unable to process empty PDF document. Please try converting to text.")
                        }
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - pdfRenderStart,
                            resultSummary = "Local PdfRenderer: rasterized ${pageBytesList.size} PNG page elements."
                        )
                        
                        _parseStatusText.value = "Analyzing layout visual content (1-3 pages)..."
                        val parts = pageBytesList.map { pBytes ->
                            val base64 = ResumeParserUtils.bytesToBase64(pBytes)
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))
                        }.toMutableList()
                        
                        val currentJd = _pastedJd.value.trim()
                        parts.add(Part(text = getPromptMarkup(currentJd = currentJd)))
                        
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = parts)),
                            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.15f)
                        )
                        
                        val reqStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("gemini_request", "ACTIVE")
                        updateDiagnosticStep(
                            "gemini_request",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - reqStartFallback,
                            resultSummary = "Built visual PDF multimodal context fallback request."
                        )

                        _parseStatusText.value = "Performing advanced PDF-to-Image OCR..."
                        
                        val respStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("gemini_response", "ACTIVE")
                        
                        val response = runApiBlockWithRetry { model ->
                            setDiagnosticsGlobalFields(model = model)
                            RetrofitClient.service.generateContent(model, apiKey, request)
                        }
                        
                        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: throw Exception("Renderer fallback failed.")
                        
                        setDiagnosticsGlobalFields(rawResp = jsonText, apiCode = "200 OK")
                        updateDiagnosticStep(
                            "gemini_response",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - respStartFallback,
                            resultSummary = "Retrieved fallback rendering content from Gemini model, size: ${jsonText.length} bytes."
                        )

                        val jsonStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("json_parsing", "ACTIVE")
                        val resumeData = parseJsonSafely(ResumeData::class.java, jsonText)
                        syncedResumeData = syncResumeFields(resumeData)
                        updateDiagnosticStep(
                            "json_parsing",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - jsonStartFallback,
                            resultSummary = "Successfully parsed fallback parsed data."
                        )

                        val atsStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("ats_scoring", "ACTIVE")
                        val score = syncedResumeData.atsAnalysis.overallScore
                        updateDiagnosticStep(
                            "ats_scoring",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - atsStartFallback,
                            resultSummary = "ATS score loaded from fallback: $score%"
                        )
                        success = true
                    } else if (_selectedUri.value != null && !isImage) {
                        val stringScanStart = System.currentTimeMillis()
                        updateDiagnosticStep("pdf_extraction", "ACTIVE")
                        _parseStatusText.value = "Running deep string scanner recovery..."
                        val recoveredText = ResumeParserUtils.extractPrintableTextCompiled(context, _selectedUri.value!!)
                        if (recoveredText.trim().isEmpty()) {
                            throw Exception("Recovered stream found no text matches.")
                        }
                        updateDiagnosticStep(
                            "pdf_extraction",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - stringScanStart,
                            resultSummary = "Deep string scanner parsed ${recoveredText.length} bytes of raw textual elements."
                        )
                        
                        val currentJd = _pastedJd.value.trim()
                        val request = GenerateContentRequest(
                            contents = listOf(
                                Content(parts = listOf(Part(text = recoveredText), Part(text = getPromptMarkup(currentJd = currentJd))))
                            ),
                            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.15f)
                        )
                        
                        val reqStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("gemini_request", "ACTIVE")
                        updateDiagnosticStep(
                            "gemini_request",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - reqStartFallback,
                            resultSummary = "Constructed fallback printable character text schema."
                        )

                        val respStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("gemini_response", "ACTIVE")
                        val response = runApiBlockWithRetry { model ->
                            setDiagnosticsGlobalFields(model = model)
                            RetrofitClient.service.generateContent(model, apiKey, request)
                        }
                        
                        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: throw Exception("Character extraction fallback failed.")
                        
                        setDiagnosticsGlobalFields(rawResp = jsonText, apiCode = "200")
                        updateDiagnosticStep(
                            "gemini_response",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - respStartFallback,
                            resultSummary = "API call success on direct string stream: fetched ${jsonText.length} bytes."
                        )

                        val jsonStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("json_parsing", "ACTIVE")
                        val resumeData = parseJsonSafely(ResumeData::class.java, jsonText)
                        syncedResumeData = syncResumeFields(resumeData)
                        updateDiagnosticStep(
                            "json_parsing",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - jsonStartFallback,
                            resultSummary = "Completed parsing fallback string context parser JSON."
                        )

                        val atsStartFallback = System.currentTimeMillis()
                        updateDiagnosticStep("ats_scoring", "ACTIVE")
                        val score = syncedResumeData.atsAnalysis.overallScore
                        updateDiagnosticStep(
                            "ats_scoring",
                            "SUCCESS",
                            durationMs = System.currentTimeMillis() - atsStartFallback,
                            resultSummary = "Parsed ATS score successfully: $score%"
                        )
                        success = true
                    } else {
                        throw Exception("Unable to process resume. Please try again.")
                    }
                }

                // 8. DB Saving and UI rendering preparation UI Stage
                val uiStart = System.currentTimeMillis()
                updateDiagnosticStep("ui_rendering", "ACTIVE")
                
                val finalSyncedData = syncedResumeData ?: throw Exception("Unable to process resume. Please try again.")
                val updatedJson = moshi.adapter(ResumeData::class.java).toJson(finalSyncedData)

                _parseStatusText.value = "Writing profile record to Room SQLite databases..."
                val displayTitle = if (!_selectedFileName.value.isNullOrEmpty()) {
                    _selectedFileName.value!!.substringBeforeLast(".")
                } else if (finalSyncedData.name.isNotEmpty()) {
                    "${finalSyncedData.name} - Resume"
                } else {
                    "Pasted Resume Analysis " + System.currentTimeMillis() % 10000
                }

                val entity = ResumeEntity(
                    title = displayTitle,
                    rawJson = updatedJson,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertResume(entity)

                _parsedResult.value = finalSyncedData
                _currentViewJson.value = updatedJson
                _parsingSuccessCount.value += 1
                _totalProcessingTimeMillis.value += (System.currentTimeMillis() - startTime)
                _parseStatusText.value = "Parsing pipeline completed successfully!"
                
                setDiagnosticsGlobalFields(overallStatus = "SUCCESS")
                updateDiagnosticStep(
                    "ui_rendering",
                    "SUCCESS",
                    durationMs = System.currentTimeMillis() - uiStart,
                    resultSummary = "Injected profile record into SQLLite. Recomposed and refreshed active dashboards."
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                
                // Trace failed stage
                val currentStepId = when (_diagnostics.value.currentStep) {
                    "File Reading" -> "file_read"
                    "PDF Character Extraction" -> "pdf_extraction"
                    "OCR Preprocessing" -> "ocr"
                    "Gemini Request Generation" -> "gemini_request"
                    "Gemini Response Raw Receive" -> "gemini_response"
                    "JSON Sanitization & Parsing" -> "json_parsing"
                    "ATS Scoring Analysis" -> "ats_scoring"
                    "UI Rendering Preparation" -> "ui_rendering"
                    else -> "file_read"
                }
                updateDiagnosticStep(
                    currentStepId,
                    "FAILED",
                    errorDetails = "${e::class.java.simpleName}: ${e.localizedMessage}\nStack trace:\n${e.stackTraceToString()}"
                )
                
                var httpCode = "N/A"
                if (e is retrofit2.HttpException) {
                    httpCode = "${e.code()} ${e.message()}"
                }
                updateDiagnosticsError(e, apiCode = httpCode)
                
                _errorMessage.value = getLocalizedErrorMessage(e)
            } finally {
                _isParsing.value = false
            }
        }
    }

    private fun syncResumeFields(resumeData: ResumeData): ResumeData {
        return resumeData.copy(
            education = resumeData.educationHistory.map {
                listOfNotNull(it.degree, it.mainInstitution, it.passingYear).filter { s -> s.isNotEmpty() }.joinToString(" - ")
            }.ifEmpty { resumeData.education },
            experience = resumeData.experienceHistory.map {
                listOfNotNull(it.designation, it.companyName, it.duration).filter { s -> s.isNotEmpty() }.joinToString(" at ")
            }.ifEmpty { resumeData.experience },
            certifications = resumeData.certificationsList.ifEmpty { resumeData.certifications },
            projects = resumeData.projectsList.ifEmpty { resumeData.projects }
        )
    }

    private fun getPromptMarkup(currentJd: String): String {
        return """
            You are an elite, enterprise-grade AI Resume Parser and ATS Screening engine. 
            Analyze the provided resume document and extract its content to produce a comprehensive profile JSON.
            
            CRITICAL EXTRACTION DIRECTIONS:
            1. AUTOMATIC TRANSLATION: If the resume is written in Hindi, Urdu, French, German, Spanish, Arabic, Chinese or any other language, automatically detect the language and translate the extracted fields into standard professional English.
            2. SMART SKILLS RECONCILIATION:
               - Combine and normalize similar skills (e.g., "Python Programming", "Python Developer", "Python3" -> "Python").
               - Detect duplicates and remove them.
               - Infer latent / hidden skills implied by the candidate's work experience descriptions (e.g., if they built a Django REST API, automatically infer and add "Python", "Django", "REST API" to "technicalSkills").
               - Group skills into "technicalSkills" (languages, platforms, tools) and "softSkills" (collaboration, leadership).
               - Categorize skills exactly under the "categorizedSkills" JSON keys.
            3. CRITICAL CONFIDENCE RATING (0.0% to 100.0%):
               - Compute realistic factual confidence statistics for critical parsed fields: name, email, phone, location, skills, experience, education, certifications. Set these inside the "confidence" JSON block.
            4. ATS SCORE ENGINE & SUGGESTIONS (0 to 100):
               - overallScore: Cumulative ATS performance score.
               - formattingScore: Based on structure, layout density, and contact details completeness.
               - skillCoverageScore: Depth and alignment of technology skills.
               - keywordOptimizationScore: Relevant industry keyword matches.
               - educationQualityScore: Value of college credentials and coursework.
               - experienceQualityScore: Professional progression and metrics-driven items.
               - projectsScore: Quality, relevance, and engineering complexity of listed items.
               - certificationsScore: Industry relevance of credentials.
               - Group bulleted actionable suggestions under: improvements, skillSuggestions, experienceSuggestions, educationSuggestions, projectsSuggestions, certificationsSuggestions.
            
            5. AUTOMATED COMPANION MODULES:
               - resumeSummary: Create a sharp 3-sentence elevator pitch outlining the candidate's premium capabilities.
               - careerGuidance: Outline professional growth recommendations (e.g., "Focus on acquiring Kubernetes certification to transition into DevOps").
               - interviewQuestions: Generate a list of 5 expert-level context-specific interview questions.

            ${if (currentJd.isNotEmpty()) """
            6. JOB DESCRIPTION MATCH: Evaluate this resume against the following pasted Job Description:
               ---
               $currentJd
               ---
               Generate the "jdMatch" details:
               - matchPercentage: Alignment score from 0 to 100.
               - missingSkills: Skills specified in the JD that are not present.
               - matchingSkills: Key skills present in both the JD and resume.
               - criticalMissingSkills: Core skills crucial for performing the role that the candidate lacks.
               - candidateRanking: Classify fitting status ("Top Tier", "Mid Tier", "Ineligible").
               - recommendations: 3 specific steps for the candidate to align with this role.
            """ else ""}

            You MUST strictly return raw JSON in the exact structure below, with NO trailing commas, and NO markdown code blocks.
            JSON Structure:
            {
              "name": "Full Name",
              "email": "Email Address",
              "phone": "Phone Number",
              "dateOfBirth": "DOB",
              "location": "Primary Location/Address",
              "linkedinUrl": "LinkedIn URL",
              "githubUrl": "GitHub URL",
              "portfolioUrl": "Portfolio URL",
              "skills": ["Skill 1", "Skill 2"],
              "technicalSkills": ["Tech 1", "Tech 2"],
              "softSkills": ["Soft 1", "Soft 2"],
              "categorizedSkills": {
                "programmingLanguages": ["Java", "Kotlin"],
                "frameworks": ["Spring Boot", "Jetpack Compose"],
                "databases": ["PostgreSQL"],
                "cloud": ["GCP"],
                "devOps": ["Docker"],
                "aiMl": ["PyTorch"]
              },
              "languagesKnown": ["English"],
              "educationHistory": [
                {
                  "mainInstitution": "College/University",
                  "degree": "Degree Major",
                  "passingYear": "Passing Year"
                }
              ],
              "experienceHistory": [
                {
                  "companyName": "Company Name",
                  "designation": "Designation",
                  "startDate": "Start",
                  "endDate": "End",
                  "duration": "Duration"
                }
              ],
              "projectsList": ["Project and details"],
              "achievementsList": ["Achievement"],
              "certificationsList": ["Credential"],
              "expectedSalary": "Expected",
              "currentSalary": "Current",
              "noticePeriod": "Notice",
              "detectedLanguage": "English",
              "atsAnalysis": {
                "overallScore": 85,
                "formattingScore": 90,
                "skillCoverageScore": 80,
                "keywordOptimizationScore": 85,
                "educationQualityScore": 75,
                "experienceQualityScore": 90,
                "projectsScore": 80,
                "certificationsScore": 85,
                "improvements": ["Improvement suggestion 1"],
                "skillSuggestions": ["Highlight Cloud Native services"],
                "experienceSuggestions": ["Add quantitative business impact metrics"],
                "educationSuggestions": ["Add GPA if honors"],
                "projectsSuggestions": ["Detail architectural patterns"],
                "certificationsSuggestions": ["Add GCP Cloud Architect"]
              },
              ${if (currentJd.isNotEmpty()) """
              "jdMatch": {
                "matchPercentage": 75,
                "missingSkills": ["React", "TypeScript"],
                "matchingSkills": ["Kotlin", "Android", "Java"],
                "criticalMissingSkills": ["React"],
                "candidateRanking": "Top Tier",
                "recommendations": ["Recommendation 1"]
              },
              """ else ""}
              "confidence": {
                "name": 99.2,
                "email": 100.0,
                "phone": 95.0,
                "location": 90.0,
                "skills": 88.5,
                "experience": 92.0,
                "education": 95.0,
                "certifications": 90.0
              },
              "resumeSummary": "Elevator summary of 3 sentences",
              "careerGuidance": "Career recommendations text",
              "interviewQuestions": ["Technical question 1", "Question 2"]
            }

            Constraint: If any field is entirely missing, set to "" or empty list. Return ONLY raw valid JSON matching model keys exactly.
        """.trimIndent()
    }

    // Direct Job Description alignment on the screen post-parse
    fun matchJobDescription(jobDescription: String) {
        val currentResume = _parsedResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isMatchingJd.value = true
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    throw Exception("Google Gemini API Key is missing. Add it securely to Secrets panel in AI Studio.")
                }

                val resumeJson = moshi.adapter(ResumeData::class.java).toJson(currentResume)
                val promptText = """
                    You are an expert recruitment coordinator. Align this candidate's resume with the job description provided.
                    Compute custom critical missing skills and a classification ranking (e.g. "Top Tier", "Mid Tier", "Ineligible").
                    
                    Resume Data:
                    $resumeJson
                    
                    Job Description:
                    $jobDescription
                    
                    You MUST return ONLY valid JSON matching this exact model with no formatting blocks:
                    {
                      "matchPercentage": 85,
                      "missingSkills": ["Docker", "Kubernetes"],
                      "matchingSkills": ["Kotlin", "Jetpack Compose", "Android SDK"],
                      "criticalMissingSkills": ["Docker"],
                      "candidateRanking": "Top Tier",
                      "recommendations": ["Recommendation step 1", "Recommendation step 2"]
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    )
                )

                val response = runApiBlockWithRetry { model ->
                    RetrofitClient.service.generateContent(model, apiKey, request)
                }
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    val jdMatch = parseJsonSafely(JdMatchDetails::class.java, text)
                    val updatedProfile = currentResume.copy(jdMatch = jdMatch)
                    _parsedResult.value = updatedProfile
                    _currentViewJson.value = moshi.adapter(ResumeData::class.java).toJson(updatedProfile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isMatchingJd.value = false
            }
        }
    }

    // On-demand generators for extra companions
    fun generateCoverLetter(jdText: String) {
        val currentResume = _parsedResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingCoverLetter.value = true
            _coverLetterText.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    throw Exception("Gemini API key is not configured.")
                }
                val promptText = """
                    Write a highly personalized, compelling, and professional Cover Letter matching the candidate's resume back to the Job Description.
                    Format the generated response in clean Markdown with professional spacing.
                    
                    Candidate Profile Summary:
                    Name: ${currentResume.name}
                    Email: ${currentResume.email}
                    Skills: ${currentResume.skills.joinToString(", ")}
                    Experience: ${currentResume.experienceHistory.joinToString("; ") { "${it.designation} at ${it.companyName}" }}
                    
                    Target Job Description:
                    $jdText
                    
                    Write the complete cover letter directly. Do not include conversational intro or notes.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                val response = runApiBlockWithRetry { model ->
                    RetrofitClient.service.generateContent(model, apiKey, request)
                }
                _coverLetterText.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Failed to generate Cover Letter."
            } catch (e: Throwable) {
                _coverLetterText.value = "Error generating Cover Letter: ${getLocalizedErrorMessage(e)}"
            } finally {
                _isGeneratingCoverLetter.value = false
            }
        }
    }

    fun clearCoverLetter() {
        _coverLetterText.value = null
    }

    fun compareCandidates(selectedEntities: List<ResumeEntity>) {
        if (selectedEntities.size < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            _isComparingCandidates.value = true
            _candidateComparisonResult.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    throw Exception("Gemini API key is not configured.")
                }
                
                val resumesJson = selectedEntities.joinToString("\n---\n") { it.rawJson }
                val promptText = """
                    You are a Lead Talent Acquisition Consultant. Perform a rigorous, multi-factor, side-by-side comparative analysis of these candidates' resumes.
                    Create an elegant Markdown Table comparing:
                    - Candidate Name
                    - Overall ATS Score
                    - Key Core Strengths
                    Area of Improvement
                    - Fit Assessment
                    
                    Then, present critical hiring recommendation summaries identifying who is the strongest fit and why. Keep the style modern, professional, and clear.
                    
                    Candidates Data:
                    $resumesJson
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(temperature = 0.3f)
                )

                val response = runApiBlockWithRetry { model ->
                    RetrofitClient.service.generateContent(model, apiKey, request)
                }
                _candidateComparisonResult.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Failed to analyze candidate details."
            } catch (e: Throwable) {
                _candidateComparisonResult.value = "Error during comparison: ${getLocalizedErrorMessage(e)}"
            } finally {
                _isComparingCandidates.value = false
            }
        }
    }

    fun clearCandidateComparison() {
        _candidateComparisonResult.value = null
    }

    // EXPORT UTILITIES (CSV, JSON, Plain text report) sharing using Storage Access Framework/FileProvider
    fun exportCandidateProfile(context: Context, format: String) {
        val current = _parsedResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportContent: String
                val extension: String
                val mimeType: String

                when (format.uppercase()) {
                    "JSON" -> {
                        exportContent = _currentViewJson.value ?: moshi.adapter(ResumeData::class.java).toJson(current)
                        extension = "json"
                        mimeType = "application/json"
                    }
                    "CSV" -> {
                        val csvBuilder = StringBuilder()
                        csvBuilder.append("FieldName,Value\n")
                        csvBuilder.append("Name,\"${current.name.replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("Email,\"${current.email.replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("Phone,\"${current.phone.replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("Location,\"${current.location.replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("ATS Score,\"${current.atsAnalysis.overallScore}\"\n")
                        csvBuilder.append("Skills,\"${current.skills.joinToString(", ").replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("Detected Language,\"${current.detectedLanguage}\"\n")
                        csvBuilder.append("Expected Salary,\"${current.expectedSalary.replace("\"", "\"\"")}\"\n")
                        csvBuilder.append("Notice Period,\"${current.noticePeriod.replace("\"", "\"\"")}\"\n")
                        exportContent = csvBuilder.toString()
                        extension = "csv"
                        mimeType = "text/csv"
                    }
                    else -> { // Plain Text / PDF placeholder report
                        val report = StringBuilder()
                        report.append("=========================================\n")
                        report.append("       AI RESUME PROFILE REPORT          \n")
                        report.append("=========================================\n")
                        report.append("Candidate Name     : ${current.name}\n")
                        report.append("Email Address      : ${current.email}\n")
                        report.append("Phone Number       : ${current.phone}\n")
                        report.append("Current Location   : ${current.location}\n")
                        report.append("\n[ATS ANALYSIS RESULTS]\n")
                        report.append("Overall ATS Match Score   : ${current.atsAnalysis.overallScore}/100\n")
                        report.append("Formatting Integrity      : ${current.atsAnalysis.formattingScore}/100\n")
                        report.append("Skill Stack Coverage      : ${current.atsAnalysis.skillCoverageScore}/100\n")
                        report.append("Keyword Optimization      : ${current.atsAnalysis.keywordOptimizationScore}/100\n")
                        report.append("\n[TECHNICAL SKILLS]\n")
                        report.append(current.technicalSkills.joinToString("\n- ", "- ", "\n"))
                        report.append("\n[WORK EXPERIENCE HISTORY]\n")
                        current.experienceHistory.forEach { exp ->
                            report.append("- ${exp.designation} at ${exp.companyName} (${exp.duration})\n")
                        }
                        exportContent = report.toString()
                        extension = "txt"
                        mimeType = "text/plain"
                    }
                }

                val safeName = current.name.replace(" ", "_").ifEmpty { "Candidate" }
                val tempFile = File(context.cacheDir, "${safeName}_Screening_Report.$extension")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(exportContent.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Candidate Screening Report - ${current.name}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Save or Export Profile Report"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLocalizedErrorMessage(e: Throwable): String {
        val msg = e.localizedMessage ?: ""
        
        // Log original details safely
        android.util.Log.e("ResumeViewModel_Error", "Detailed error captured in localization engine: $msg", e)
        
        return when {
            msg.contains("Unable to process resume", ignoreCase = true) ->
                "Unable to process resume. Please try again."
            e is java.net.UnknownHostException || e is java.net.ConnectException || msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("ConnectException", ignoreCase = true) ->
                "Network connection unavailable. Please check your Wi-Fi or mobile data connectivity and try again."
            e is java.net.SocketTimeoutException || msg.contains("SocketTimeoutException", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "The server request timed out. Processing complex or high-density resumes can take longer. Please try again."
            msg.contains("503") || msg.contains("Unavailable", ignoreCase = true) || msg.contains("Service Unavailable", ignoreCase = true) ->
                "Service is temporarily unavailable. Please try again in a few moments."
            msg.contains("500") || msg.contains("502") || msg.contains("504") || msg.contains("Server Error", ignoreCase = true) || msg.contains("Internal Server Error", ignoreCase = true) ->
                "Service is temporarily unavailable. Please try again in a few moments."
            msg.contains("API key", ignoreCase = true) || msg.contains("unauthorized", ignoreCase = true) || msg.contains("Key not found", ignoreCase = true) ->
                "Google Gemini API Key is missing or invalid! Please verify it is entered correctly in the AI Studio Secrets panel."
            msg.contains("limit", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("quota exceeded", ignoreCase = true) || msg.contains("429") ->
                "Gemini API rate limit exceeded. Please wait a moment before trying again."
            msg.contains("JsonReader", ignoreCase = true) || msg.contains("malformed", ignoreCase = true) || msg.contains("Moshi", ignoreCase = true) || msg.contains("Json", ignoreCase = true) || msg.contains("parse", ignoreCase = true) ->
                "Unable to process resume. Please try again."
            else -> {
                "Unable to process resume due to a transient pipeline error. Please try again."
            }
        }
    }

    private suspend fun <T> runApiBlockWithRetry(
        maxAttemptsPerModel: Int = 2,
        initialDelayMillis: Long = 1000L,
        backoffFactor: Double = 2.0,
        apiCall: suspend (modelName: String) -> T
    ): T {
        if (!isNetworkAvailable(getApplication())) {
            throw java.io.IOException("Network connection unavailable. Please check your Wi-Fi or mobile data connectivity and try again.")
        }

        val modelsToTry = listOf(
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-2.5-flash",
            "gemini-3.1-pro-preview"
        )

        var lastException: Throwable = RuntimeException("An unexpected processing pipeline error occurred.")

        for (modelName in modelsToTry) {
            var currentDelay = initialDelayMillis
            for (attempt in 1..maxAttemptsPerModel) {
                try {
                    android.util.Log.d("API_RETRY", "Trying API call with model $modelName, attempt $attempt")
                    return apiCall(modelName)
                } catch (e: Throwable) {
                    lastException = e
                    android.util.Log.e("API_RETRY", "API execution with model $modelName attempt $attempt failed: ${e.localizedMessage}", e)
                    
                    val msg = e.localizedMessage ?: ""
                    
                    if (msg.contains("API key", ignoreCase = true)) {
                        throw e
                    }
                    
                    if (attempt < maxAttemptsPerModel) {
                        kotlinx.coroutines.delay(currentDelay)
                        currentDelay = (currentDelay * backoffFactor).toLong()
                    }
                }
            }
        }
        throw lastException
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }

    private fun sanitizeAndRepairJson(rawResponse: String): String {
        var cleanJson = rawResponse.trim()
        
        // 1. Strip markdown wrapper blocks
        if (cleanJson.contains("```")) {
            cleanJson = cleanJson.removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
        
        // 2. Extract boundaries using first '{' and last '}'
        val firstCurly = cleanJson.indexOf('{')
        val lastCurly = cleanJson.lastIndexOf('}')
        if (firstCurly != -1 && lastCurly != -1 && lastCurly > firstCurly) {
            cleanJson = cleanJson.substring(firstCurly, lastCurly + 1)
        }

        // 3. Remove all null-valued keys entirely to allow default arguments in constructors
        cleanJson = cleanJson.replace(Regex("\"[a-zA-Z0-9_]+\"\\s*:\\s*null\\s*,?"), "")
        
        // 4. Clean consecutive duplicate commas (e.g. ,,)
        cleanJson = cleanJson.replace(Regex(",\\s*,"), ",")
        
        // 5. Clean trailing commas in objects and arrays
        cleanJson = cleanJson.replace(Regex(",\\s*\\}"), "}")
        cleanJson = cleanJson.replace(Regex(",\\s*\\]"), "]")
        
        return cleanJson
    }

    private fun <T> parseJsonSafely(jsonClass: Class<T>, rawResponse: String): T {
        val tag = "JsonRepair"
        android.util.Log.d(tag, "Raw response received for parsing: $rawResponse")
        
        val sanitized = sanitizeAndRepairJson(rawResponse)
        
        // Try parsing sanitized JSON with lenient reader
        try {
            val buffer = okio.Buffer().writeUtf8(sanitized)
            val reader = com.squareup.moshi.JsonReader.of(buffer)
            reader.isLenient = true
            val parsedResult = moshi.adapter(jsonClass).fromJson(reader)
            if (parsedResult != null) return parsedResult
        } catch (e: Throwable) {
            android.util.Log.e(tag, "Parse failed on sanitized JSON. Running secondary recursive recovery...", e)
        }
        
        // Secondary Recovery: If still failing, force basic regex cleanups and try again
        val doubleSanitized = sanitized
            .replace(Regex(",\\s*\\}"), "}")
            .replace(Regex(",\\s*\\]"), "]")
            
        try {
            val buffer = okio.Buffer().writeUtf8(doubleSanitized)
            val reader = com.squareup.moshi.JsonReader.of(buffer)
            reader.isLenient = true
            val parsedResult = moshi.adapter(jsonClass).fromJson(reader)
            if (parsedResult != null) return parsedResult
        } catch (e2: Throwable) {
            android.util.Log.e(tag, "Secondary recursive recovery failed.", e2)
        }
        
        throw Exception("Unable to process resume. Please try again.")
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResumeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ResumeViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
