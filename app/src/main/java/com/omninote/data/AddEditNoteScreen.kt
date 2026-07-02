package com.omninote.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omninote.data.NoteEntity
import com.omninote.ui.components.MarkdownContent
import com.omninote.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: Int?,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val existingNote = remember(noteId, allNotes) {
        allNotes.find { it.id == noteId }
    }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var activeTagsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLocked by remember { mutableStateOf(false) }
    var lockPin by remember { mutableStateOf<String?>(null) }

    // Tab state: false = Edit Mode, true = Preview Mode
    var isPreviewMode by remember { mutableStateOf(false) }

    // Bottom Sheet State for Note Settings (Vibe, Tags, Analytics)
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var isInitialized by remember(noteId) { mutableStateOf(false) }

    // Initialize states once when existingNote is loaded
    LaunchedEffect(existingNote) {
        if (existingNote != null && !isInitialized) {
            title = existingNote.title
            content = existingNote.content
            isPinned = existingNote.isPinned
            selectedColor = existingNote.colorHex
            isLocked = existingNote.isLocked
            lockPin = existingNote.lockPin
            activeTagsList = existingNote.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            isInitialized = true
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.sharedText != null) {
            val text = viewModel.sharedText
            if (content.isEmpty()) {
                content = text ?: ""
            } else {
                content += "\n\n$text"
            }
        }
        if (viewModel.sharedUris != null) {
            val uris = viewModel.sharedUris!!
            uris.forEach { uri ->
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val internalUri = copyUriToInternalStorage(context, uri, "shared_file")
                
                if (mimeType.startsWith("image/")) {
                     content += "\n![Shared Image]($internalUri)\n"
                } else {
                     var displayFileName = "Shared_File"
                     try {
                         context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                             val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                             if (nameIdx != -1 && cursor.moveToFirst()) {
                                 val n = cursor.getString(nameIdx)
                                 if(!n.isNullOrBlank()) displayFileName = n
                             }
                         }
                     } catch (e: Exception) {}
                     
                     if (mimeType.startsWith("audio/")) {
                         content += "\n[voice:$internalUri]($internalUri)\n"
                     } else {
                         content += "\n[file:$displayFileName]($internalUri)\n"
                     }
                }
            }
        }
        viewModel.consumeSharedContent()
    }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    // State for the redesigned Premium / More formatting tools popup window
    var showPremiumToolsDialog by remember { mutableStateOf(false) }

    // Text color highlight wizard variables
    var showColorHighlightWizard by remember { mutableStateOf(false) }
    var wizardTextToFormat by remember { mutableStateOf("") }
    var wizardTextColorSelected by remember { mutableStateOf<String?>(null) }
    var wizardBgColorSelected by remember { mutableStateOf<String?>(null) }

    // Media attachment pickers & launchers state
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            coroutineScope.launch {
                val internalUri = copyUriToInternalStorage(context, it, "image.jpg")
                content += "\n![Image]($internalUri)\n"
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            coroutineScope.launch {
                var displayFileName = "Attached_File"
                try {
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1 && cursor.moveToFirst()) {
                            val n = cursor.getString(nameIdx)
                            if (!n.isNullOrBlank()) displayFileName = n
                        }
                    }
                } catch (e: Exception) {}
                
                val internalUri = copyUriToInternalStorage(context, it, displayFileName)
                content += "\n[file:$displayFileName]($internalUri)\n"
            }
        }
    }

    // Audio recording state
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var tempAudioFile by remember { mutableStateOf<java.io.File?>(null) }
    var recordTimeSeconds by remember { mutableStateOf(0) }

    // Table generator state
    var showTableGeneratorDialog by remember { mutableStateOf(false) }
    var tableColumnsCount by remember { mutableStateOf("2") }
    var tableRowsCount by remember { mutableStateOf("3") }

    // Speech Dictation state
    var showSpeechDictationDialog by remember { mutableStateOf(false) }
    var dictatedText by remember { mutableStateOf("") }
    var isListeningSpeech by remember { mutableStateOf(false) }
    var dictationStatusMessage by remember { mutableStateOf("Tap 'Listen' and start speaking...") }
    var isAiRefining by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val recognitionListener = remember {
        object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListeningSpeech = true
                dictationStatusMessage = "Listening... Speak clearly."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningSpeech = false
                dictationStatusMessage = "Finished speaking, processing..."
            }
            override fun onError(error: Int) {
                isListeningSpeech = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error. Make sure Google speech features are enabled."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match. Speak again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy."
                    SpeechRecognizer.ERROR_SERVER -> "Server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout. Speak again."
                    else -> "Recognition error ($error)."
                }
                dictationStatusMessage = message
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    dictatedText = matches[0]
                    dictationStatusMessage = "Finished transcription."
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    dictatedText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val dictationAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSpeechDictationDialog = true
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required for Voice Dictation", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndRequestDictation() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showSpeechDictationDialog = true
        } else {
            dictationAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    fun localRefineText(text: String): String {
        var refined = text.trim()
        if (refined.isEmpty()) return ""
        
        // Replace spoken punctuation words in both Arabic and English
        val replacements = mapOf(
            "نقطة" to ".",
            "فاصلة" to "،",
            "علامة استفهام" to "؟",
            "سطر جديد" to "\n",
            "فقرة جديدة" to "\n\n",
            "period" to ".",
            "comma" to ",",
            "question mark" to "?",
            "new line" to "\n",
            "next line" to "\n"
        )
        
        for ((key, value) in replacements) {
            refined = refined.replace(Regex("(?i)\\b$key\\b"), value)
        }
        
        // Capitalize English sentences
        refined = refined.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { sentence ->
                sentence.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .joinToString(" ")
            
        // Clean up spacing
        refined = refined
            .replace(Regex(" +"), " ")
            .replace(" .", ".")
            .replace(" ,", ",")
            .replace(" ?", "?")
            .replace(" ،", "،")
            .replace(" ؟", "؟")
            .replace("\n ", "\n")
            .replace(" \n", "\n")
            
        return refined.trim()
    }

    suspend fun refineTextWithAI(prompt: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = com.omninote.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE" || apiKey.startsWith("YOUR_")) {
            return@withContext "Error: Please configure your Gemini API Key in the Secrets panel."
        }
        
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val contentObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        val requestBodyJson = JSONObject()
            .put("contents", JSONArray().put(contentObj))
            .toString()
            
        val request = okhttp3.Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBodyJson.toRequestBody(mediaType))
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Error: API call failed (${response.code})"
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val firstPart = parts.getJSONObject(0)
                firstPart.getString("text")
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.message}"
        }
    }

    // Modern Multi-version Permission Launchers and Check Functions
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceRecorderDialog = true
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required to record audio voice notes", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        val partialGranted = permissionsMap[android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        val imageGranted = permissionsMap[android.Manifest.permission.READ_MEDIA_IMAGES] == true || permissionsMap[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true

        if (allGranted || partialGranted || imageGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            android.widget.Toast.makeText(context, "Storage/Media permission is required to select images", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val filePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            filePickerLauncher.launch("*/*")
        } else {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                filePickerLauncher.launch("*/*")
            } else {
                android.widget.Toast.makeText(context, "Storage permission is required to attach files", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkAndRequestRecordAudio() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showVoiceRecorderDialog = true
        } else {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    fun checkAndRequestImageStorage() {
        val sdkInt = android.os.Build.VERSION.SDK_INT
        val permissionsToRequest = when {
            sdkInt >= 34 -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            sdkInt >= 33 -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val hasAll = permissionsToRequest.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (hasAll) {
            imagePickerLauncher.launch("image/*")
        } else {
            imagePermissionLauncher.launch(permissionsToRequest)
        }
    }

    fun checkAndRequestFileStorage() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            filePickerLauncher.launch("*/*")
        } else {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                filePickerLauncher.launch("*/*")
            } else {
                filePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    val colorOptions = listOf(
        null, // Default
        "#E6E3FF", // Electric Indigo Light
        "#2D26A0", // Electric Indigo Dark
        "#FFF0CC", // Warm Amber Light
        "#9A5500", // Warm Amber Dark
        "#FFD6EC", // Dusty Rose Light
        "#8B2060"  // Dusty Rose Dark
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 300)
    )

    val saveAndGoBack = {
        val tagsString = activeTagsList.distinct().joinToString(",")
        // Save if not empty
        if (title.isNotBlank() || content.isNotBlank()) {
            if (noteId != null) {
                val noteToUpdate = existingNote?.copy(
                    title = title,
                    content = content,
                    isPinned = isPinned,
                    colorHex = selectedColor,
                    tags = tagsString,
                    isLocked = isLocked,
                    lockPin = lockPin
                ) ?: NoteEntity(
                    id = noteId,
                    title = title,
                    content = content,
                    isPinned = isPinned,
                    colorHex = selectedColor,
                    tags = tagsString,
                    isLocked = isLocked,
                    lockPin = lockPin
                )
                viewModel.updateNote(noteToUpdate)
            } else {
                viewModel.addNote(
                    title = title,
                    content = content,
                    colorHex = selectedColor,
                    tags = tagsString,
                    isLocked = isLocked,
                    lockPin = lockPin
                )
            }
        }
        onNavigateBack()
    }

    // Capture hardware back behavior gracefully
    BackHandler {
        saveAndGoBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = animatedBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (existingNote != null) "Edit Note" else "New Note",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { saveAndGoBack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        CanvasCustomIcon(CanvasIconType.BACK)
                    }
                },
                actions = {
                    // Modern Minimalist Mode Switcher (Clean Icon Toggle)
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPreviewMode = false }) {
                                CanvasCustomIcon(
                                    type = CanvasIconType.EDIT,
                                    tint = if (!isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = { isPreviewMode = true }) {
                                CanvasCustomIcon(
                                    type = CanvasIconType.VISIBILITY,
                                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Open Design & Tags tuning Sheet
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        CanvasCustomIcon(CanvasIconType.TUNE, tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        CanvasCustomIcon(
                            type = if (isPinned) CanvasIconType.PIN else CanvasIconType.UNPIN,
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (existingNote != null) {
                        IconButton(
                            onClick = {
                                val tagsString = activeTagsList.distinct().joinToString(",")
                                val noteToTrash = existingNote.copy(
                                    title = title,
                                    content = content,
                                    isPinned = isPinned,
                                    colorHex = selectedColor,
                                    tags = tagsString,
                                    isLocked = isLocked,
                                    lockPin = lockPin,
                                    isTrashed = true
                                )
                                viewModel.moveToTrash(noteToTrash)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        ) {
                            CanvasCustomIcon(CanvasIconType.DELETE, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = isPreviewMode,
                    label = "EditorPreviewTransition",
                    transitionSpec = {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> if (targetState) fullWidth else -fullWidth },
                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(500))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> if (targetState) -fullWidth else fullWidth },
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(500))
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { isPreview ->
                    if (!isPreview) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 1. Distraction-free Writing Area
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 96.dp)
                            ) {
                                TextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = {
                                        Text(
                                            "Title",
                                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, textDirection = TextDirection.ContentOrLtr),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, textDirection = TextDirection.ContentOrLtr),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
        
                                TextField(
                                    value = content,
                                    onValueChange = { content = it },
                                    placeholder = {
                                        Text(
                                            "Write your thoughts here... Use markdown shortcuts below to structure your note beautifully.",
                                            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrLtr),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, textDirection = TextDirection.ContentOrLtr),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                            }

                            // 2. High-accessibility Keyboard Format Accessory Dock (Floating)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                            ) {
                                Surface(
                                    tonalElevation = 8.dp,
                                    shadowElevation = 16.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(top = 8.dp, bottom = 8.dp)
                                    ) {
                                        // Active tags quick indicators row
                                        if (activeTagsList.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                activeTagsList.forEach { tag ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            CanvasCustomIcon(
                                                                type = CanvasIconType.LABEL,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(
                                                                text = tag,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
         
                                        // Redesigned structured non-crowded bottom toolbar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Group 1: Rich Text Formatting
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.FORMAT_BOLD,
                                                    contentDescription = "Bold text",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    onClick = { content += "**" }
                                                )
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.FORMAT_ITALIC,
                                                    contentDescription = "Italic text",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    onClick = { content += "*" }
                                                )
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.BULLET_LIST,
                                                    contentDescription = "Bullet List",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    onClick = { content += "\n- " }
                                                )
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.CHECKBOX_ON,
                                                    contentDescription = "Checklist checklist item",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    onClick = { content += "\n- [ ] " }
                                                )
                                            }

                                            // Elegant subtle separator
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            )

                                            // Group 2: Media & Voice Inputs
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.MIC,
                                                    contentDescription = "Record live audio voice clip",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                                    onClick = { checkAndRequestRecordAudio() }
                                                )
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.WAVEFORM,
                                                    contentDescription = "AI Speech-to-Text Dictation",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                                    onClick = { checkAndRequestDictation() }
                                                )
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.IMAGE,
                                                    contentDescription = "Attach photo image",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                                    onClick = { checkAndRequestImageStorage() }
                                                )
                                            }

                                            // Elegant subtle separator
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            )

                                            // Group 3: Pro & Tuning Customization
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                // Glowing Premium magic tools trigger
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    MaterialTheme.colorScheme.tertiary,
                                                                    MaterialTheme.colorScheme.primary
                                                                )
                                                            ),
                                                            alpha = 0.15f
                                                        )
                                                        .clickable { showPremiumToolsDialog = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CanvasCustomIcon(
                                                        type = CanvasIconType.WAND,
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Design Settings Tuning Panel Trigger
                                                ToolbarIconButton(
                                                    iconType = CanvasIconType.TUNE,
                                                    contentDescription = "Tune Note style and tags",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    onClick = { showSettingsSheet = true }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                    // Preview Mode Visual Port
                    androidx.compose.foundation.text.selection.SelectionContainer(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                                .animateContentSize()
                        ) {
                            if (title.isNotBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            if (content.isNotBlank()) {
                                MarkdownContent(
                                    rawText = content,
                                    onCheckedChange = { lineIndex, isChecked ->
                                        val lines = content.split("\n").toMutableList()
                                        if (lineIndex >= 0 && lineIndex < lines.size) {
                                            val original = lines[lineIndex]
                                            lines[lineIndex] = if (isChecked) {
                                                original.replace("- [ ]", "- [x]").replace("* [ ]", "* [x]")
                                            } else {
                                                original.replace("- [x]", "- [ ]").replace("* [x]", "* [ ]")
                                                    .replace("- [X]", "- [ ]").replace("* [X]", "* [ ]")
                                            }
                                            content = lines.joinToString("\n")
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CanvasCustomIcon(
                                            type = CanvasIconType.VISIBILITY_OFF,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "Write description notes to preview styled markdown content here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // End of if(!isPreview) else {..}
            } // End of AnimatedContent Content Block
        } // End of Column (modifier = fillMaxSize)
    } // End of Box (padding)

    // Modern Note Customization Slider & Bottom Sheet
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(CanvasIconType.TUNE, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "Customize Note",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 1. Vibe Theme selection card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "VIBE THEME ACCENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorOptions.forEach { colorHex ->
                                    val color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
                                        ?: MaterialTheme.colorScheme.surfaceVariant

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == colorHex) 2.5.dp else 1.dp,
                                                color = if (selectedColor == colorHex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = colorHex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedColor == colorHex) {
                                            CanvasCustomIcon(
                                                type = CanvasIconType.TICK,
                                                tint = if (colorHex == "#E6E3FF" || colorHex == "#FFF0CC" || colorHex == "#FFD6EC" || colorHex == null) MaterialTheme.colorScheme.primary else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 2. Advanced Privacy & Lock Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "PRIVACY & LOCK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Lock this note",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Requires PIN to open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isLocked,
                                    onCheckedChange = { isLocked = it }
                                )
                            }
                            if (isLocked) {
                                OutlinedTextField(
                                    value = lockPin ?: "",
                                    onValueChange = { lockPin = it.take(4) },
                                    placeholder = { Text("Set 4-digit PIN") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // 3. Advanced Custom Export Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ADVANCED EXPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$content")
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export as TXT")
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    CanvasCustomIcon(CanvasIconType.ATTACH_FILE, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export TXT", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "# $title\n\n$content\n\n> Exported from OmniNote")
                                            type = "text/markdown"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export as Markdown")
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    CanvasCustomIcon(CanvasIconType.CODE, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export MD", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. Categories & Tags Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CATEGORIES & TAGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showAddTagDialog = true },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    CanvasCustomIcon(
                                        type = CanvasIconType.ADD,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            val predefinedTags = listOf("Work", "Personal", "Ideas", "Urgent", "Study", "To-Do")
                            val allAvailableTags = (predefinedTags + activeTagsList).distinct()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allAvailableTags.forEach { tag ->
                                    val isSelected = activeTagsList.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            activeTagsList = if (isSelected) {
                                                activeTagsList - tag
                                            } else {
                                                activeTagsList + tag
                                            }
                                        },
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                        leadingIcon = if (isSelected) {
                                            { CanvasCustomIcon(CanvasIconType.TICK, modifier = Modifier.size(12.dp)) }
                                        } else {
                                            { CanvasCustomIcon(CanvasIconType.LABEL, modifier = Modifier.size(12.dp)) }
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Note Science & Analytics Card
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "NOTE SCIENCE & ANALYTICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        val charCount = content.length
                        val wordCount = remember(content) { content.split(Regex("\\s+")).filter { it.isNotBlank() }.size }
                        val readTimeMinutes = maxOf(1, (wordCount / 200).toInt())
                        val containsArabic = content.contains(Regex("[\\u0600-\\u06FF]"))
                        val containsEnglish = content.contains(Regex("[a-zA-Z]"))

                        val textLanguage = when {
                            containsArabic && containsEnglish -> "Bilingual (AR/EN)"
                            containsArabic -> "Arabic"
                            containsEnglish -> "English"
                            else -> "Plain Text"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnalyticsRow(label = "Total Characters", value = "$charCount")
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                AnalyticsRow(label = "Total Words", value = "$wordCount")
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                AnalyticsRow(label = "Estimated Read Time", value = "~$readTimeMinutes min")
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                AnalyticsRow(label = "Language Detected", value = textLanguage)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Custom Tag Creation dialog
        if (showAddTagDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddTagDialog = false
                    newTagText = ""
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasCustomIcon(CanvasIconType.LABEL, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = "Create Custom Tag",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add a new category label to organize your notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("Tag Name") },
                        placeholder = { Text("e.g. Work, Ideas, Study") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddTagDialog = false; newTagText = "" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val trimmed = newTagText.trim()
                                if (trimmed.isNotEmpty() && !activeTagsList.contains(trimmed)) {
                                    activeTagsList = activeTagsList + trimmed
                                }
                                showAddTagDialog = false
                                newTagText = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }

        // Visual text formatting color picker wizard dialog
        if (showColorHighlightWizard) {
            ModalBottomSheet(
                onDismissRequest = {
                    showColorHighlightWizard = false
                    wizardTextToFormat = ""
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasCustomIcon(CanvasIconType.PALETTE, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Text("Color Highlight Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedTextField(
                        value = wizardTextToFormat,
                        onValueChange = { wizardTextToFormat = it },
                        label = { Text("Text to Style") },
                        placeholder = { Text("e.g. Important highlight, keyword...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("TEXT COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val textColors = listOf("#2D26A0" to "Indigo Dark", "#9A5500" to "Orange Dark", "#8B2060" to "Rose Dark", "#1B5E20" to "Green Dark", "#1A237E" to "Blue Dark", "#B71C1C" to "Red Dark", "#4A148C" to "Violet Dark")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        textColors.forEach { (hexCode, _) ->
                            val rgb = Color(android.graphics.Color.parseColor(hexCode))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(rgb)
                                    .border(
                                        width = if (wizardTextColorSelected == hexCode) 2.5.dp else 1.dp, 
                                        color = if (wizardTextColorSelected == hexCode) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f), 
                                        shape = CircleShape
                                    )
                                    .clickable { wizardTextColorSelected = if (wizardTextColorSelected == hexCode) null else hexCode },
                                contentAlignment = Alignment.Center
                            ) {
                                if (wizardTextColorSelected == hexCode) {
                                    CanvasCustomIcon(
                                        type = CanvasIconType.TICK,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Text("BACKGROUND ACCENT COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val bgColors = listOf("#E6E3FF" to "Indigo", "#FFF0CC" to "Orange", "#FFD6EC" to "Rose", "#E8F5E9" to "Green", "#E8EAF6" to "Blue", "#FFEBEE" to "Red", "#F3E5F5" to "Violet")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bgColors.forEach { (hexCode, _) ->
                            val rgb = Color(android.graphics.Color.parseColor(hexCode))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(rgb)
                                    .border(
                                        width = if (wizardBgColorSelected == hexCode) 2.5.dp else 1.dp, 
                                        color = if (wizardBgColorSelected == hexCode) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f), 
                                        shape = CircleShape
                                    )
                                    .clickable { wizardBgColorSelected = if (wizardBgColorSelected == hexCode) null else hexCode },
                                contentAlignment = Alignment.Center
                            ) {
                                if (wizardBgColorSelected == hexCode) {
                                    CanvasCustomIcon(
                                        type = CanvasIconType.TICK,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), 
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showColorHighlightWizard = false; wizardTextToFormat = "" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { 
                            Text("Cancel") 
                        }
                        Button(
                            onClick = {
                                val insertTxt = wizardTextToFormat.ifBlank { "styled text" }
                                var markdownToken = insertTxt
                                if (wizardTextColorSelected != null) { markdownToken = "[color:${wizardTextColorSelected}]($markdownToken)" }
                                if (wizardBgColorSelected != null) { markdownToken = "[bg:${wizardBgColorSelected}]($markdownToken)" }
                                content += (if (content.isNotEmpty() && !content.endsWith(" ")) " " else "") + markdownToken + " "
                                showColorHighlightWizard = false; wizardTextToFormat = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) { 
                            Text("Apply Format", fontWeight = FontWeight.Bold) 
                        }
                    }
                }
            }
        }

        // Built-in Dynamic Audio Voice Recorder dialog with Animated Waveform Canvas
        if (showVoiceRecorderDialog) {
            // Amplitude variables used to animate dynamic waveform bars on Canvas!
            val amplitudes = remember { mutableStateListOf(15f, 10f, 25f, 12f, 40f, 18f, 30f, 8f, 20f, 14f) }

            ModalBottomSheet(
                onDismissRequest = {
                    if (isRecordingAudio) {
                        try { mediaRecorder?.stop() } catch (e: Exception) {}
                        mediaRecorder?.release()
                        mediaRecorder = null
                        isRecordingAudio = false
                    }
                    showVoiceRecorderDialog = false
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar with close button
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(
                                    type = CanvasIconType.MIC,
                                    tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Voice Note Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    try { mediaRecorder?.stop() } catch (e: Exception) {}
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                    isRecordingAudio = false
                                }
                                showVoiceRecorderDialog = false
                                recordTimeSeconds = 0
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = if (isRecordingAudio) "Recording active... Speak now" else "Tap red button below to record voice",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ─── STUNNING REAL-TIME CANVAS WAVEFORM ANMATION ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecordingAudio) {
                            LaunchedEffect(Unit) {
                                while (isRecordingAudio) {
                                    delay(80)
                                    for (i in amplitudes.indices) { amplitudes[i] = (15..70).random().toFloat() }
                                }
                            }
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 8.dp.toPx()
                                val barSpacing = 6.dp.toPx()
                                val startX = (size.width - (amplitudes.size * (barWidth + barSpacing) - barSpacing)) / 2

                                for (i in amplitudes.indices) {
                                    val height = amplitudes[i].dp.toPx()
                                    val x = startX + i * (barWidth + barSpacing)
                                    val y = (size.height - height) / 2
                                    drawRoundRect(
                                        color = Color(0xFFD81B60),
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, height),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(10) {
                                    Box(modifier = Modifier.width(8.dp).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                                }
                            }
                        }
                    }

                    val formattedTime = remember(recordTimeSeconds) {
                        val secs = recordTimeSeconds % 60
                        val mins = recordTimeSeconds / 60
                        String.format("%02d:%02d", mins, secs)
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    if (isRecordingAudio) {
                        LaunchedEffect(Unit) {
                            while (isRecordingAudio) {
                                delay(1000)
                                recordTimeSeconds++
                            }
                        }
                    }

                    // Center trigger recording button with nice pulse background styling
                    IconButton(
                        onClick = {
                            if (isRecordingAudio) {
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                    isRecordingAudio = false
                                    tempAudioFile?.let {
                                        content += "\n[voice:Voice Recording ${System.currentTimeMillis() % 1000}](${android.net.Uri.fromFile(it)})\n"
                                    }
                                    showVoiceRecorderDialog = false
                                    recordTimeSeconds = 0
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Error saving recording: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                try {
                                    val cacheDir = context.cacheDir
                                    val file = java.io.File(cacheDir, "omni_voice_${System.currentTimeMillis()}.mp4")
                                    tempAudioFile = file
                                    recordTimeSeconds = 0
                                    val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        android.media.MediaRecorder(context)
                                    } else { android.media.MediaRecorder() }
                                    recorder.apply {
                                        setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    mediaRecorder = recorder
                                    isRecordingAudio = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Mic access required: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecordingAudio) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                            .border(width = 2.dp, color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, shape = CircleShape)
                    ) {
                        CanvasCustomIcon(
                            type = if (isRecordingAudio) CanvasIconType.STOP else CanvasIconType.RECORD,
                            tint = if (isRecordingAudio) MaterialTheme.colorScheme.onErrorContainer else Color.Red,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (isRecordingAudio) {
                                try { mediaRecorder?.stop() } catch (e: Exception) {}
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecordingAudio = false
                            }
                            showVoiceRecorderDialog = false
                            recordTimeSeconds = 0
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // AI Speech Dictation dialog
        if (showSpeechDictationDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (isListeningSpeech) {
                        try { speechRecognizer.stopListening() } catch (e: Exception) {}
                        isListeningSpeech = false
                    }
                    showSpeechDictationDialog = false
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header with close button
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(
                                    type = CanvasIconType.MIC,
                                    tint = if (isListeningSpeech) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                "AI Voice Dictation",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isListeningSpeech) {
                                    try { speechRecognizer.stopListening() } catch (e: Exception) {}
                                    isListeningSpeech = false
                                }
                                showSpeechDictationDialog = false
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Status Indicator Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isListeningSpeech) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isListeningSpeech) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                CanvasCustomIcon(CanvasIconType.INFO, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = dictationStatusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isListeningSpeech) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Transcribed Text Display Area
                    OutlinedTextField(
                        value = dictatedText,
                        onValueChange = { dictatedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        label = { Text("Transcribed Content") },
                        placeholder = { Text("Spoken words will appear here in real-time...") },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    // Control Buttons (Mic Start/Stop)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Toggle Listening button
                        Button(
                            onClick = {
                                if (isListeningSpeech) {
                                    try { speechRecognizer.stopListening() } catch (e: Exception) {}
                                    isListeningSpeech = false
                                    dictationStatusMessage = "Listening stopped."
                                } else {
                                    try {
                                        speechRecognizer.startListening(speechIntent)
                                        isListeningSpeech = true
                                        dictationStatusMessage = "Initializing Speech Recognizer..."
                                    } catch (e: Exception) {
                                        dictationStatusMessage = "Failed to start listening: ${e.localizedMessage}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListeningSpeech) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            CanvasCustomIcon(
                                type = if (isListeningSpeech) CanvasIconType.STOP else CanvasIconType.MIC,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isListeningSpeech) "Stop" else "Listen", fontWeight = FontWeight.Bold)
                        }

                        // AI Refine formatting button
                        Button(
                            onClick = {
                                if (dictatedText.isBlank()) {
                                    android.widget.Toast.makeText(context, "Transcribe some text first!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        isAiRefining = true
                                        val apiKey = com.omninote.BuildConfig.GEMINI_API_KEY
                                        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE" || apiKey.startsWith("YOUR_")) {
                                            dictationStatusMessage = "Applying Smart Local formatting (offline)..."
                                            delay(500)
                                            dictatedText = localRefineText(dictatedText)
                                            dictationStatusMessage = "Formatted offline successfully!"
                                        } else {
                                            dictationStatusMessage = "Refining text using Gemini AI..."
                                            val prompt = "Correct any transcription mistakes, speech hesitations, grammatical errors, and format the following spoken text professionally with proper punctuation. Return ONLY the refined, clean text, and do not add any conversational framing: \"$dictatedText\""
                                            val refined = refineTextWithAI(prompt)
                                            if (refined.startsWith("Error:")) {
                                                dictatedText = localRefineText(dictatedText)
                                                dictationStatusMessage = "Formatted offline (Gemini API unavailable)"
                                            } else {
                                                dictatedText = refined
                                                dictationStatusMessage = "Refined by AI successfully!"
                                            }
                                        }
                                        isAiRefining = false
                                    }
                                }
                            },
                            enabled = !isAiRefining && dictatedText.isNotBlank(),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            if (isAiRefining) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                CanvasCustomIcon(
                                    type = CanvasIconType.WAND,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Smart Format", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action buttons (Apply/Insert and Clear)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                dictatedText = ""
                                dictationStatusMessage = "Transcription cleared."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text("Clear", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (dictatedText.isNotBlank()) {
                                    content += (if (content.isNotEmpty() && !content.endsWith(" ")) " " else "") + dictatedText
                                }
                                showSpeechDictationDialog = false
                            },
                            enabled = dictatedText.isNotBlank(),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Insert Text", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Interactive Table generator dialog
        if (showTableGeneratorDialog) {
            ModalBottomSheet(
                onDismissRequest = { showTableGeneratorDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar with close
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(CanvasIconType.GRID_ON, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                "Table Generator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showTableGeneratorDialog = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text("Specify table rows and columns to generate clean Markdown table tags.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = tableColumnsCount,
                        onValueChange = { tableColumnsCount = it },
                        label = { Text("Number of Columns") },
                        placeholder = { Text("e.g. 3") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tableRowsCount,
                        onValueChange = { tableRowsCount = it },
                        label = { Text("Number of Rows") },
                        placeholder = { Text("e.g. 4") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTableGeneratorDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val cols = tableColumnsCount.toIntOrNull() ?: 2
                                val rows = tableRowsCount.toIntOrNull() ?: 3
                                val sb = java.lang.StringBuilder()
                                sb.append("\n|")
                                for (c in 1..cols) { sb.append(" Header $c |") }
                                sb.append("\n|")
                                for (c in 1..cols) { sb.append("---|") }
                                sb.append("\n")
                                for (r in 1..rows) {
                                    sb.append("|")
                                    for (c in 1..cols) { sb.append(" Cell $r,$c |") }
                                    sb.append("\n")
                                }
                                sb.append("\n")
                                content += sb.toString()
                                showTableGeneratorDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Insert Table", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Premium styling Studio overlay dialog
        if (showPremiumToolsDialog) {
            ModalBottomSheet(
                onDismissRequest = { showPremiumToolsDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CanvasCustomIcon(
                            type = CanvasIconType.WAND,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pro Formatting Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Advanced content creator suite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("TXT HEADINGS & STYLES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumToolChip(label = "H1 Title", iconType = CanvasIconType.HEADING_1) { content += "\n# "; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "H2 Subtitle", iconType = CanvasIconType.HEADING_2) { content += "\n## "; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "Italics", iconType = CanvasIconType.FORMAT_ITALIC) { content += "*"; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "Marker", iconType = CanvasIconType.HIGHLIGHT) { content += "=="; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "Quote block", iconType = CanvasIconType.QUOTE) { content += "\n> "; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "Code Block", iconType = CanvasIconType.CODE) { content += "\n```kotlin\n\n```"; showPremiumToolsDialog = false }
                            PremiumToolChip(label = "Bullet List", iconType = CanvasIconType.BULLET_LIST) { content += "\n- "; showPremiumToolsDialog = false }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                        
                        Text("ADVANCED BUILDERS & STORAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PremiumInteractiveCard(title = "AI Voice Dictation", description = "Transcribe and clean up voice notes with Gemini AI", iconType = CanvasIconType.MIC, tint = MaterialTheme.colorScheme.primary) { showPremiumToolsDialog = false; checkAndRequestDictation() }
                            PremiumInteractiveCard(title = "Text Color Vibe Customizer", description = "Format with tailored backgrounds & text highlights", iconType = CanvasIconType.PALETTE, tint = MaterialTheme.colorScheme.primary) { showPremiumToolsDialog = false; showColorHighlightWizard = true }
                            PremiumInteractiveCard(title = "Dynamic Table Builder", description = "Insert customized grid spreadsheets dynamically", iconType = CanvasIconType.GRID_ON, tint = MaterialTheme.colorScheme.secondary) { showPremiumToolsDialog = false; showTableGeneratorDialog = true }
                            PremiumInteractiveCard(title = "Upload Document Files", description = "Attach PDF, ZIP or spreadsheet documents safely", iconType = CanvasIconType.ATTACH_FILE, tint = MaterialTheme.colorScheme.tertiary) { showPremiumToolsDialog = false; checkAndRequestFileStorage() }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarIconButton(
    iconType: CanvasIconType,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CanvasCustomIcon(
            type = iconType,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun AnalyticsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumToolChip(
    label: String,
    iconType: CanvasIconType,
    onClick: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        leadingIcon = {
            CanvasCustomIcon(
                type = iconType,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = InputChipDefaults.inputChipBorder(
            selected = false,
            enabled = true,
            borderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
fun PremiumInteractiveCard(
    title: String,
    description: String,
    iconType: CanvasIconType,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                CanvasCustomIcon(
                    type = iconType,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            CanvasCustomIcon(
                type = CanvasIconType.ARROW_RIGHT,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

suspend fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri, defaultName: String): android.net.Uri {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var fileName = defaultName
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIdx)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }
        } catch (e: Exception) {}
        
        val time = System.currentTimeMillis()
        val finalFileName = "${time}_${fileName.replace(" ", "_").replace("/", "_")}"
        
        val destFile = java.io.File(context.filesDir, finalFileName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            android.net.Uri.fromFile(destFile)
        } catch (e: Exception) {
            uri 
        }
    }
}