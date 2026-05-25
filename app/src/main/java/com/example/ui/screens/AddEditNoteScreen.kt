package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.data.NoteEntity
import com.example.ui.components.MarkdownContent
import com.example.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: Int?,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val existingNote = remember(noteId, allNotes) {
        allNotes.find { it.id == noteId }
    }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var activeTagsList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Tab state: false = Edit Mode, true = Preview Mode
    var isPreviewMode by remember { mutableStateOf(false) }

    // Bottom Sheet State for Note Settings (Vibe, Tags, Analytics)
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Initialize states once when existingNote is loaded
    LaunchedEffect(existingNote) {
        if (existingNote != null) {
            title = existingNote.title
            content = existingNote.content
            isPinned = existingNote.isPinned
            selectedColor = existingNote.colorHex
            activeTagsList = existingNote.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    // State for the redesigned Premium / More formatting tools popup window
    var showPremiumToolsDialog by remember { mutableStateOf(false) }

    // Text color highlight wizard variables
    var showColorHighlightWizard by remember { mutableStateOf(false) }
    var wizardTextToFormat by remember { mutableStateOf("") }
    var wizardIsBackgroundHighlight by remember { mutableStateOf(true) }
    var wizardColorSelected by remember { mutableStateOf("#E6E3FF") }

    // Media attachment pickers & launchers state
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            content += "\n![Image]($it)\n"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            var fileName = "Attached_File"
            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIdx)
                    }
                }
            } catch (e: Exception) {}
            content += "\n[file:$fileName]($it)\n"
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

    val targetColor = selectedColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.background

    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300)
    )

    val saveAndGoBack = {
        val tagsString = activeTagsList.distinct().joinToString(",")
        // Save if not empty
        if (title.isNotBlank() || content.isNotBlank()) {
            if (existingNote != null) {
                viewModel.updateNote(
                    id = existingNote.id,
                    title = title,
                    content = content,
                    isPinned = isPinned,
                    colorHex = selectedColor,
                    timestamp = existingNote.timestamp,
                    tags = tagsString
                )
            } else {
                viewModel.addNote(
                    title = title,
                    content = content,
                    colorHex = selectedColor,
                    tags = tagsString
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
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_gem),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (existingNote != null) "Edit Note" else "New Note",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Mode",
                                    tint = if (!isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = { isPreviewMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Preview Mode",
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
                        Icon(Icons.Default.Tune, contentDescription = "Note Options", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (isPinned) Modifier.rotate(45f) else Modifier
                        )
                    }

                    if (existingNote != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteNote(existingNote)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.onErrorContainer)
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
                if (!isPreviewMode) {
                    // 1. Distraction-free Writing Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "Title",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = {
                                Text(
                                    "Write your thoughts here... Use markdown shortcuts below to structure beautifully.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
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
                        )
                    }

                    // 2. High-accessibility Keyboard Format Accessory Dock
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(vertical = 8.dp)
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
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Label,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(10.dp)
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

                            // Highly optimized minimalist format bar with pro-level tools
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShortcutButton(text = "Bold", icon = Icons.Default.FormatBold, desc = "Bold text") { content += "**" }
                                ShortcutButton(text = "Task", icon = Icons.Default.CheckBox, desc = "Checklist checklist item") { content += "\n- [ ] " }
                                ShortcutButton(text = "Voice", icon = Icons.Default.Mic, desc = "Record live audio voice clip") { checkAndRequestRecordAudio() }
                                ShortcutButton(text = "Photo", icon = Icons.Default.Image, desc = "Attach photo image") { checkAndRequestImageStorage() }
                                
                                // Glowing Premium trigger button
                                InputChip(
                                    selected = false,
                                    onClick = { showPremiumToolsDialog = true },
                                    label = {
                                        Text(
                                            text = "✨ Pro Tools",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Unlock Advanced formatting Tools",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        selected = false,
                                        enabled = true,
                                        borderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Preview Mode Visual Port
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
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
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
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
            }
        }

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
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Note",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showSettingsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. Vibe Theme selection card
                    Text(
                        text = "Vibe Theme Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (colorHex == "#E6E3FF" || colorHex == "#FFF0CC" || colorHex == "#FFD6EC" || colorHex == null) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Categories selection card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories & Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { showAddTagDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom category tag",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

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
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else {
                                    { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Document details & stats
                    Text(
                        text = "Note Science & Analytics",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Custom Tag Creation dialog
        if (showAddTagDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddTagDialog = false
                    newTagText = ""
                },
                shape = RoundedCornerShape(20.dp),
                title = { Text("Create Custom Tag", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = newTagText.trim()
                            if (trimmed.isNotEmpty()) {
                                if (!activeTagsList.contains(trimmed)) {
                                    activeTagsList = activeTagsList + trimmed
                                }
                            }
                            showAddTagDialog = false
                            newTagText = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddTagDialog = false
                            newTagText = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Visual text formatting color picker wizard dialog
        if (showColorHighlightWizard) {
            AlertDialog(
                onDismissRequest = {
                    showColorHighlightWizard = false
                    wizardTextToFormat = ""
                },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        "Color Customizer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = wizardTextToFormat,
                            onValueChange = { wizardTextToFormat = it },
                            label = { Text("Text to style") },
                            placeholder = { Text("e.g. Highlighted point...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mode toggle: background vs foreground text color
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Style Mode:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = wizardIsBackgroundHighlight,
                                onClick = { wizardIsBackgroundHighlight = true },
                                label = { Text("Background") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = !wizardIsBackgroundHighlight,
                                onClick = { wizardIsBackgroundHighlight = false },
                                label = { Text("Text Color") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Vibrant color palette choice
                        Text(
                            "Choose Styling Color:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        val wizardColors = listOf(
                            "#E6E3FF" to "Indigo",
                            "#FFF0CC" to "Orange",
                            "#FFD6EC" to "Rose",
                            "#E8F5E9" to "Green",
                            "#E8EAF6" to "Blue",
                            "#FFEBEE" to "Red",
                            "#F3E5F5" to "Violet"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            wizardColors.forEach { (hexCode, labelName) ->
                                val rgb = Color(android.graphics.Color.parseColor(hexCode))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(rgb)
                                        .border(
                                            width = if (wizardColorSelected == hexCode) 2.5.dp else 1.dp,
                                            color = if (wizardColorSelected == hexCode) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                        .clickable { wizardColorSelected = hexCode }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val insertTxt = wizardTextToFormat.ifBlank { "styled text" }
                            val markdownToken = if (wizardIsBackgroundHighlight) {
                                "[bg:$wizardColorSelected]($insertTxt)"
                            } else {
                                "[color:$wizardColorSelected]($insertTxt)"
                            }
                            content += markdownToken
                            showColorHighlightWizard = false
                            wizardTextToFormat = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Format")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showColorHighlightWizard = false
                            wizardTextToFormat = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Built-in Dynamic Audio Voice Recorder dialog with Animated Waveform Canvas
        if (showVoiceRecorderDialog) {
            // Amplitude variables used to animate dynamic waveform bars on Canvas!
            val amplitudes = remember { mutableStateListOf(15f, 10f, 25f, 12f, 40f, 18f, 30f, 8f, 20f, 14f) }

            AlertDialog(
                onDismissRequest = {
                    if (isRecordingAudio) {
                        try {
                            mediaRecorder?.stop()
                        } catch (e: Exception) {}
                        mediaRecorder?.release()
                        mediaRecorder = null
                        isRecordingAudio = false
                    }
                    showVoiceRecorderDialog = false
                },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Voice Note",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
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
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecordingAudio) {
                                // Launch high-frequency timer to undulating amplitudes
                                LaunchedEffect(Unit) {
                                    while (isRecordingAudio) {
                                        delay(80)
                                        for (i in amplitudes.indices) {
                                            amplitudes[i] = (15..70).random().toFloat()
                                        }
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
                                // Static flat quiet waveforms representation
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(10) {
                                        Box(
                                            modifier = Modifier
                                                .width(8.dp)
                                                .height(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                        )
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

                        // Center trigger recording button
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
                                        } else {
                                            android.media.MediaRecorder()
                                        }

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
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecordingAudio) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecordingAudio) "Stop Recording" else "Start Recording",
                                tint = if (isRecordingAudio) MaterialTheme.colorScheme.onErrorContainer else Color.Red,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (isRecordingAudio) {
                                try {
                                    mediaRecorder?.stop()
                                } catch (e: Exception) {}
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecordingAudio = false
                            }
                            showVoiceRecorderDialog = false
                            recordTimeSeconds = 0
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Interactive Table generator dialog
        if (showTableGeneratorDialog) {
            AlertDialog(
                onDismissRequest = { showTableGeneratorDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Table Generator",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Specify table rows and columns to generate clean Markdown table tags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = tableColumnsCount,
                            onValueChange = { tableColumnsCount = it },
                            label = { Text("Number of Columns") },
                            placeholder = { Text("e.g. 3") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tableRowsCount,
                            onValueChange = { tableRowsCount = it },
                            label = { Text("Number of Rows") },
                            placeholder = { Text("e.g. 4") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cols = tableColumnsCount.toIntOrNull() ?: 2
                            val rows = tableRowsCount.toIntOrNull() ?: 3

                            val sb = java.lang.StringBuilder()
                            sb.append("\n")
                            sb.append("|")
                            for (c in 1..cols) {
                                sb.append(" Header $c |")
                            }
                            sb.append("\n|")
                            for (c in 1..cols) {
                                sb.append("---|")
                            }
                            sb.append("\n")
                            for (r in 1..rows) {
                                sb.append("|")
                                for (c in 1..cols) {
                                    sb.append(" Cell $r,$c |")
                                }
                                sb.append("\n")
                            }
                            sb.append("\n")

                            content += sb.toString()
                            showTableGeneratorDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Insert Table")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTableGeneratorDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom Premium styling Studio overlay dialog
        if (showPremiumToolsDialog) {
            AlertDialog(
                onDismissRequest = { showPremiumToolsDialog = false },
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pro Formatting Studio",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Advanced content creator suite",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Section 1: Typography & Text Styles
                        Text(
                            text = "TXT HEADINGS & STYLES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumToolChip(label = "H1 Title", icon = Icons.Default.Title) {
                                content += "\n# "
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "H2 Subtitle", icon = Icons.Default.Subtitles) {
                                content += "\n## "
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "Italics", icon = Icons.Default.FormatItalic) {
                                content += "*"
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "Marker", icon = Icons.Default.Highlight) {
                                content += "=="
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "Quote block", icon = Icons.Default.FormatQuote) {
                                content += "\n> "
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "Code Block", icon = Icons.Default.Code) {
                                content += "\n```kotlin\n\n```"
                                showPremiumToolsDialog = false
                            }
                            PremiumToolChip(label = "Bullet List", icon = Icons.Default.FormatListBulleted) {
                                content += "\n- "
                                showPremiumToolsDialog = false
                            }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                        
                        // Section 2: Premium Builders & Widgets
                        Text(
                            text = "ADVANCED BUILDERS & STORAGE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Text highlight wizard
                            PremiumInteractiveCard(
                                title = "Text Color Vibe Customizer",
                                description = "Format with tailored backgrounds & text highlights",
                                icon = Icons.Default.Palette,
                                tint = MaterialTheme.colorScheme.primary
                            ) {
                                showPremiumToolsDialog = false
                                showColorHighlightWizard = true
                            }
                            
                            // Markdown table generator
                            PremiumInteractiveCard(
                                title = "Dynamic Table Builder",
                                description = "Insert customized grid spreadsheets dynamically",
                                icon = Icons.Default.GridOn,
                                tint = MaterialTheme.colorScheme.secondary
                            ) {
                                showPremiumToolsDialog = false
                                showTableGeneratorDialog = true
                            }
                            
                            // Upload custom files support
                            PremiumInteractiveCard(
                                title = "Upload Document Files",
                                description = "Attach PDF, ZIP or spreadsheet documents safely",
                                icon = Icons.Default.AttachFile,
                                tint = MaterialTheme.colorScheme.tertiary
                            ) {
                                showPremiumToolsDialog = false
                                checkAndRequestFileStorage()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showPremiumToolsDialog = false }
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutButton(
    text: String,
    icon: ImageVector? = null,
    desc: String,
    onClick: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = desc,
                    modifier = Modifier.size(14.dp)
                )
            }
        },
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = InputChipDefaults.inputChipBorder(
            selected = false,
            enabled = true,
            borderColor = Color.Transparent
        )
    )
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
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
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
    icon: ImageVector,
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
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
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
