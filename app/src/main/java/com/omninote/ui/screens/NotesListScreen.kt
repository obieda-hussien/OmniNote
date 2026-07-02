package com.omninote.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.omninote.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omninote.data.NoteEntity
import com.omninote.ui.viewmodels.NotesViewModel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch

/**
 * Custom helper function to perform search-result query highlighting inside Note view cards
 */
fun highlightSearchQuery(
    text: String,
    query: String?,
    primaryColor: Color
): AnnotatedString {
    if (query.isNullOrBlank()) {
        return AnnotatedString(text)
    }
    val builder = AnnotatedString.Builder()
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    var startIdx = 0
    while (true) {
        val foundIdx = lowerText.indexOf(lowerQuery, startIdx)
        if (foundIdx == -1) {
            builder.append(text.substring(startIdx))
            break
        }

        builder.append(text.substring(startIdx, foundIdx))

        builder.pushStyle(
            SpanStyle(
                background = Color(0xFFFBC02D).copy(alpha = 0.45f), // Golden Amber highlighter
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
        )
        builder.append(text.substring(foundIdx, foundIdx + query.length))
        builder.pop()

        startIdx = foundIdx + query.length
    }
    return builder.toAnnotatedString()
}

@Composable
fun BeautifulEmptyStateGraphic(
    isSearchOrFilter: Boolean,
    tabType: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.size(140.dp)) {
        val width = size.width
        val height = size.height
        
        if (isSearchOrFilter) {
            // Draw a gorgeous futuristic glowing Search Magnifying Glass
            drawCircle(
                color = primaryColor.copy(alpha = 0.08f),
                radius = width * 0.45f
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = width * 0.3f
            )
            drawLine(
                color = primaryColor,
                start = Offset(width * 0.65f, height * 0.65f),
                end = Offset(width * 0.85f, height * 0.85f),
                strokeWidth = 12f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(
                color = primaryColor,
                radius = width * 0.22f,
                center = Offset(width * 0.45f, height * 0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )
            drawCircle(
                color = secondaryColor,
                radius = 6f,
                center = Offset(width * 0.38f, height * 0.38f)
            )
        } else {
            when (tabType) {
                "ACTIVE" -> {
                    // Draw a stylized vector notebook page with rounded corners
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.08f),
                        radius = width * 0.45f
                    )
                    
                    val cardW = width * 0.45f
                    val cardH = height * 0.6f
                    val cardX = (width - cardW) / 2
                    val cardY = (height - cardH) / 2
                    
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.12f),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    
                    val startLineX = cardX + 12f
                    val endLineX = cardX + cardW - 12f
                    val firstLineY = cardY + 20f
                    val lineSpacing = 20f
                    
                    drawLine(
                        color = primaryColor,
                        start = Offset(startLineX, firstLineY),
                        end = Offset(cardX + cardW * 0.6f, firstLineY),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(startLineX, firstLineY + lineSpacing),
                        end = Offset(endLineX, firstLineY + lineSpacing),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(startLineX, firstLineY + lineSpacing * 2),
                        end = Offset(cardX + cardW * 0.8f, firstLineY + lineSpacing * 2),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    drawCircle(
                        color = secondaryColor,
                        radius = 8f,
                        center = Offset(cardX + cardW + 10f, cardY - 5f)
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.4f),
                        radius = 14f,
                        center = Offset(cardX + cardW + 10f, cardY - 5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
                "ARCHIVED" -> {
                    // Draw a gorgeous archive secure vault / safe box
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.08f),
                        radius = width * 0.45f
                    )
                    
                    val boxW = width * 0.5f
                    val boxH = height * 0.45f
                    val boxX = (width - boxW) / 2
                    val boxY = (height - boxH) / 2 + 10f
                    
                    // Box base
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.1f),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    
                    // Lid top
                    val lidW = boxW + 16f
                    val lidH = 14f
                    val lidX = boxX - 8f
                    val lidY = boxY - 14f
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(lidX, lidY),
                        size = Size(lidW, lidH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    
                    // Lock handle
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(width / 2 - 16f, boxY + boxH * 0.35f),
                        size = Size(32f, 12f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    
                    // Star decoration
                    drawCircle(
                        color = secondaryColor,
                        radius = 6f,
                        center = Offset(width / 2, boxY + boxH * 0.35f - 16f)
                    )
                }
                "TRASHED" -> {
                    // Draw a recycle trash bin
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.08f),
                        radius = width * 0.45f
                    )
                    
                    val binW = width * 0.38f
                    val binH = height * 0.48f
                    val binX = (width - binW) / 2
                    val binY = (height - binH) / 2 + 10f
                    
                    // Bin body
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.1f),
                        topLeft = Offset(binX, binY),
                        size = Size(binW, binH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(binX, binY),
                        size = Size(binW, binH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    
                    // Top lid line
                    drawLine(
                        color = primaryColor,
                        start = Offset(binX - 12f, binY),
                        end = Offset(binX + binW + 12f, binY),
                        strokeWidth = 6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    // Vertical lines on bin
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(binX + binW * 0.3f, binY + 16f),
                        end = Offset(binX + binW * 0.3f, binY + binH - 16f),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(binX + binW * 0.7f, binY + 16f),
                        end = Offset(binX + binW * 0.7f, binY + binH - 16f),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    // Cross / X badge on trash indicating deleted items
                    drawLine(
                        color = secondaryColor,
                        start = Offset(width / 2 - 10f, binY + binH / 2 - 10f),
                        end = Offset(width / 2 + 10f, binY + binH / 2 + 10f),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = secondaryColor,
                        start = Offset(width / 2 + 10f, binY + binH / 2 - 10f),
                        end = Offset(width / 2 - 10f, binY + binH / 2 + 10f),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

enum class SnackbarType {
    INFO, SUCCESS, ERROR, WARNING
}

data class CustomSnackbarData(
    val id: Long,
    val title: String,
    val message: String,
    val type: SnackbarType,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToEditNote: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var customSnackbarData by remember { mutableStateOf<CustomSnackbarData?>(null) }
    val animatableProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    var snackbarVisible by remember { mutableStateOf(false) }

    fun showCustomSnackbar(
        title: String,
        message: String,
        type: SnackbarType,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        customSnackbarData = CustomSnackbarData(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    LaunchedEffect(customSnackbarData) {
        if (customSnackbarData != null) {
            animatableProgress.snapTo(1f)
            snackbarVisible = true
            animatableProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 5000, easing = androidx.compose.animation.core.LinearEasing)
            )
            snackbarVisible = false
            kotlinx.coroutines.delay(300)
            customSnackbarData = null
        } else {
            snackbarVisible = false
        }
    }
    
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()

    // 3-tab smooth ViewPager
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val currentTab = when (pagerState.currentPage) {
        0 -> "ACTIVE"
        1 -> "ARCHIVED"
        else -> "TRASHED"
    }

    val notes = when (currentTab) {
        "ACTIVE" -> activeNotes
        "ARCHIVED" -> archivedNotes
        else -> trashedNotes
    }

    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Configuration states
    var sortBy by remember { mutableStateOf("newest") } // "newest", "oldest", "a-z", "z-a", "color", "pin"
    var filterPinnedOnly by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    // Scroll state handlers for top and bottom bar hide/show dynamics
    val gridState0 = rememberLazyStaggeredGridState()
    val listState0 = rememberLazyListState()

    val gridState1 = rememberLazyStaggeredGridState()
    val listState1 = rememberLazyListState()

    val gridState2 = rememberLazyStaggeredGridState()
    val listState2 = rememberLazyListState()

    var isTopHeaderVisible by remember { mutableStateOf(true) }
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val isCurrentlyAtTop = remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> {
                    if (isGridView) {
                        gridState0.firstVisibleItemIndex == 0 && gridState0.firstVisibleItemScrollOffset == 0
                    } else {
                        listState0.firstVisibleItemIndex == 0 && listState0.firstVisibleItemScrollOffset == 0
                    }
                }
                1 -> {
                    if (isGridView) {
                        gridState1.firstVisibleItemIndex == 0 && gridState1.firstVisibleItemScrollOffset == 0
                    } else {
                        listState1.firstVisibleItemIndex == 0 && listState1.firstVisibleItemScrollOffset == 0
                    }
                }
                else -> {
                    if (isGridView) {
                        gridState2.firstVisibleItemIndex == 0 && gridState2.firstVisibleItemScrollOffset == 0
                    } else {
                        listState2.firstVisibleItemIndex == 0 && listState2.firstVisibleItemScrollOffset == 0
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.noteEvent.collect { event ->
            when (event) {
                is com.omninote.ui.viewmodels.NoteEvent.Trashed -> {
                    showCustomSnackbar(
                        title = "Moved to Trash",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.INFO,
                        actionLabel = "UNDO",
                        onAction = {
                            viewModel.restoreFromTrash(event.note)
                        }
                    )
                }
                is com.omninote.ui.viewmodels.NoteEvent.Archived -> {
                    showCustomSnackbar(
                        title = "Note Archived",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.INFO,
                        actionLabel = "UNDO",
                        onAction = {
                            viewModel.unarchiveNote(event.note)
                        }
                    )
                }
                is com.omninote.ui.viewmodels.NoteEvent.Restored -> {
                    showCustomSnackbar(
                        title = "Note Restored",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.SUCCESS
                    )
                }
                is com.omninote.ui.viewmodels.NoteEvent.Unarchived -> {
                    showCustomSnackbar(
                        title = "Note Unarchived",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.SUCCESS
                    )
                }
            }
        }
    }

    LaunchedEffect(isCurrentlyAtTop.value) {
        if (isCurrentlyAtTop.value) {
            isTopHeaderVisible = true
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta < -12f) { // Scrolling down -> hide
                    isBottomBarVisible = false
                    isTopHeaderVisible = false
                } else if (delta > 12f) { // Scrolling up -> show bottom bar, only show header if we are close to top
                    isBottomBarVisible = true
                    if (isCurrentlyAtTop.value) {
                        isTopHeaderVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Snackbar state for undo
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Quick Actions pop-up State
    var showQuickActionsForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // Lock prompt state
    var noteToUnlock by remember { mutableStateOf<NoteEntity?>(null) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf(false) }
    var unlockActionType by remember { mutableStateOf("") }

    // Permanent deletion confirmation state
    var noteToDeletePermanently by remember { mutableStateOf<NoteEntity?>(null) }

    // Retrieve unique tags from all stored notes
    val allTags = remember(activeNotes, archivedNotes, trashedNotes) {
        val all = activeNotes + archivedNotes + trashedNotes
        all.flatMap { note ->
            note.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct()
    }

    // Helper functions for Manual Shuffling (Swapping note timestamps)
    fun moveNoteUp(currentIndex: Int, currentList: List<NoteEntity>) {
        if (currentIndex > 0) {
            val note1 = currentList[currentIndex]
            val note2 = currentList[currentIndex - 1]
            val tempTime = note1.timestamp
            viewModel.updateNote(note1.copy(timestamp = note2.timestamp))
            viewModel.updateNote(note2.copy(timestamp = tempTime))
        }
    }

    fun moveNoteDown(currentIndex: Int, currentList: List<NoteEntity>) {
        if (currentIndex < currentList.size - 1) {
            val note1 = currentList[currentIndex]
            val note2 = currentList[currentIndex + 1]
            val tempTime = note1.timestamp
            viewModel.updateNote(note1.copy(timestamp = note2.timestamp))
            viewModel.updateNote(note2.copy(timestamp = tempTime))
        }
    }

    // Filter AND Sort notes dynamically
    val filteredAndSortedNotes = remember(notes, selectedTag, searchQuery, sortBy, filterPinnedOnly) {
        var list = notes

        // 1. Filter by tag
        if (selectedTag != null) {
            list = list.filter { note ->
                note.tags.split(",")
                    .map { it.trim() }
                    .contains(selectedTag)
            }
        }

        // 2. Filter by search box
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { note ->
                note.title.lowercase().contains(q) || note.content.lowercase().contains(q)
            }
        }

        // 3. Filter by Pinned only
        if (filterPinnedOnly) {
            list = list.filter { it.isPinned }
        }

        // 4. Sort notes list
        list = when (sortBy) {
            "oldest" -> list.sortedBy { it.timestamp }
            "a-z" -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "zzz" } })
            "z-a" -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "aaa" } })
            "color" -> list.sortedBy { it.colorHex ?: "" }
            "pin" -> list.sortedByDescending { it.isPinned }
            else -> list.sortedByDescending { it.timestamp } // "newest"
        }

        // Keep pinned notes always at the top by default unless custom alphabetical sorts are activated
        if (sortBy != "oldest" && sortBy != "a-z" && sortBy != "z-a") {
            list = list.sortedByDescending { it.isPinned }
        }

        list
    }

    // Statistical variables for dashboard metrics
    val totalNotesCount = notes.size
    val pinnedNotesCount = notes.count { it.isPinned }
    val audioNotesCount = notes.count { it.content.contains("[voice:") || it.content.contains("[audio:") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(
                                    type = if (snackbarData.visuals.message.contains("Trash", ignoreCase = true) || snackbarData.visuals.message.contains("trash", ignoreCase = true)) {
                                        CanvasIconType.DELETE
                                    } else {
                                        CanvasIconType.TICK
                                    },
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = snackbarData.visuals.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        snackbarData.visuals.actionLabel?.let { actionLabel ->
                            TextButton(
                                onClick = { snackbarData.performAction() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(500, easing = FastOutSlowInEasing))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = isTopHeaderVisible,
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(400)),
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(tween(400))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Top header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val headerTitle = when (currentTab) {
                                        "ACTIVE" -> "OmniNote"
                                        "ARCHIVED" -> "Vault"
                                        else -> "Trash Bin"
                                    }
                                    val headerSub = when (currentTab) {
                                        "ACTIVE" -> "Workspace · $totalNotesCount Note" + (if(totalNotesCount != 1) "s" else "") + (if(pinnedNotesCount > 0) " ($pinnedNotesCount Pinned)" else "")
                                        "ARCHIVED" -> "Vault · $totalNotesCount Archived"
                                        else -> "Trash · $totalNotesCount Deleted"
                                    }
                                    Text(
                                        text = headerTitle,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = headerSub,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when (currentTab) {
                                            "ACTIVE" -> MaterialTheme.colorScheme.primary
                                            "ARCHIVED" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Premium Search input bar combined with Layout and Filter buttons (Always stays visible!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        shape = RoundedCornerShape(24.dp),
                        placeholder = {
                            Text(
                                text = when (currentTab) {
                                    "ACTIVE" -> "Search active notes..."
                                    "ARCHIVED" -> "Search in vault..."
                                    else -> "Search in trash..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            CanvasCustomIcon(CanvasIconType.SEARCH, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Layout Action Button
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        AnimatedContent(targetState = isGridView, label = "LayoutToggle") { grid ->
                            CanvasCustomIcon(
                                type = if (grid) CanvasIconType.GRID_OFF else CanvasIconType.GRID_ON,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sort & Filter Action Button
                    IconButton(
                        onClick = { showSortBottomSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        CanvasFilterSortIcon(modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)) // gorgeous iOS glassy bubble dock
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(64.dp)
                    ) {
                        NavigationBarItem(
                            icon = { CanvasActiveTabIcon(isSelected = currentTab == "ACTIVE") },
                            label = { Text("Active", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "ACTIVE",
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                        NavigationBarItem(
                            icon = { CanvasArchiveTabIcon(isSelected = currentTab == "ARCHIVED") },
                            label = { Text("Archive", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "ARCHIVED",
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                        NavigationBarItem(
                            icon = { CanvasTrashTabIcon(isSelected = currentTab == "TRASHED") },
                            label = { Text("Trash", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "TRASHED",
                            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.errorContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onErrorContainer,
                                selectedTextColor = MaterialTheme.colorScheme.error,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (currentTab == "ACTIVE") {
                    ExtendedFloatingActionButton(
                        text = { 
                            Text(
                                text = "New Note",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            ) 
                        },
                        icon = { 
                            CanvasCustomIcon(
                                type = CanvasIconType.EDIT
                            ) 
                        },
                        onClick = onNavigateToAddNote,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp,
                            hoveredElevation = 8.dp
                        ),
                        modifier = Modifier.padding(bottom = 0.dp, end = 0.dp)
                    )
                } else if (currentTab == "TRASHED" && notes.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        text = { 
                            Text(
                                text = "Empty",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            ) 
                        },
                        icon = { 
                            CanvasCustomIcon(
                                type = CanvasIconType.DELETE
                            ) 
                        },
                        onClick = { viewModel.emptyTrash() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        modifier = Modifier.padding(bottom = 0.dp, end = 0.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            // Dynamic Tag / Category filter carousel
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllNotes = selectedTag == null
                    FilterChip(
                        selected = isAllNotes,
                        onClick = { selectedTag = null },
                        label = { Text("All Notes", fontWeight = if (isAllNotes) FontWeight.ExtraBold else FontWeight.SemiBold) },
                        leadingIcon = {
                            CanvasCustomIcon(
                                type = CanvasIconType.MENU_BOOK,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAllNotes,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            selectedBorderWidth = 0.dp
                        )
                    )

                    allTags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = if (isSelected) null else tag },
                            label = { Text(tag, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold) },
                            leadingIcon = {
                                AnimatedContent(targetState = isSelected, label = "TagIcon") { selected ->
                                    CanvasCustomIcon(
                                        type = if (selected) CanvasIconType.TICK else CanvasIconType.LABEL,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderWidth = 0.dp
                            )
                        )
                    }
                }
            }

            // Cards Swipable Pager (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absoluteOffset = if (pageOffset < 0f) -pageOffset else pageOffset
                val scale = 1f - (absoluteOffset * 0.08f).coerceIn(0f, 0.08f)
                val alpha = 1f - (absoluteOffset * 0.35f).coerceIn(0f, 0.35f)
                val translationX = pageOffset * 60f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.translationX = translationX
                        }
                ) {
                    val pageNotes = when (page) {
                        0 -> activeNotes
                        1 -> archivedNotes
                        else -> trashedNotes
                    }

                // Filter AND Sort notes dynamically per page
                val pageFilteredAndSortedNotes = remember(pageNotes, selectedTag, searchQuery, sortBy, filterPinnedOnly) {
                    var list = pageNotes

                    // 1. Filter by tag (Only apply to Active and Archived tabs, ignore for Trash)
                    if (selectedTag != null && page != 2) {
                        list = list.filter { note ->
                            note.tags.split(",")
                                .map { it.trim() }
                                .contains(selectedTag)
                        }
                    }

                    // 2. Filter by search box
                    if (searchQuery.isNotBlank()) {
                        val q = searchQuery.trim().lowercase()
                        list = list.filter { note ->
                            note.title.lowercase().contains(q) || note.content.lowercase().contains(q)
                        }
                    }

                    // 3. Filter by Pinned only (Only apply to Active and Archived tabs, ignore for Trash)
                    if (filterPinnedOnly && page != 2) {
                        list = list.filter { it.isPinned }
                    }

                    // 4. Sort notes list
                    list = when (sortBy) {
                        "oldest" -> list.sortedBy { it.timestamp }
                        "a-z" -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "zzz" } })
                        "z-a" -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "aaa" } })
                        "color" -> list.sortedBy { it.colorHex ?: "" }
                        "pin" -> list.sortedByDescending { it.isPinned }
                        else -> list.sortedByDescending { it.timestamp } // "newest"
                    }

                    // Keep pinned notes always at the top by default unless custom alphabetical sorts are activated
                    if (sortBy != "oldest" && sortBy != "a-z" && sortBy != "z-a") {
                        list = list.sortedByDescending { it.isPinned }
                    }

                    list
                }

                if (pageFilteredAndSortedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val isSearchOrFilter = searchQuery.isNotBlank() || selectedTag != null || filterPinnedOnly
                            val tabType = when (page) {
                                0 -> "ACTIVE"
                                1 -> "ARCHIVED"
                                else -> "TRASHED"
                            }
                            BeautifulEmptyStateGraphic(
                                isSearchOrFilter = isSearchOrFilter,
                                tabType = tabType,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.tertiary
                            )

                            val emptyTitle = if (isSearchOrFilter) {
                                "No notes match your filters"
                            } else {
                                when (page) {
                                    0 -> "Your workspace is pristine"
                                    1 -> "Archive Vault is empty"
                                    else -> "Trash is clean"
                                }
                            }

                            val emptyDesc = if (isSearchOrFilter) {
                                "Try adjusting your search query or reset filters."
                            } else {
                                when (page) {
                                    0 -> "Tap the button below to write down your thoughts, voice notes, and more!"
                                    1 -> "Archive notes you want to keep secure but out of your main workspace."
                                    else -> "Notes you delete will stay here for safekeeping. Tap 'Empty' to permanently delete them."
                                }
                            }

                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = emptyDesc,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            if (isSearchOrFilter) {
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedTag = null
                                        filterPinnedOnly = false
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Reset Filters")
                                }
                            } else if (page == 0) {
                                Button(
                                    onClick = onNavigateToAddNote,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Create Note")
                                }
                            }
                        }
                    }
                } else {
                    if (isGridView) {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidthDp = configuration.screenWidthDp
                        val gridColumns = when {
                            screenWidthDp < 360 -> StaggeredGridCells.Fixed(1)
                            screenWidthDp < 600 -> StaggeredGridCells.Fixed(2)
                            screenWidthDp < 900 -> StaggeredGridCells.Fixed(3)
                            else -> StaggeredGridCells.Fixed(4)
                        }
                        val gridContentPadding = when {
                            screenWidthDp < 360 -> PaddingValues(top = 12.dp, bottom = 100.dp, start = 12.dp, end = 12.dp)
                            else -> PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp)
                        }
                        val gridSpacing = if (screenWidthDp < 360) 12.dp else 16.dp

                        LazyVerticalStaggeredGrid(
                            columns = gridColumns,
                            state = when (page) {
                                0 -> gridState0
                                1 -> gridState1
                                else -> gridState2
                            },
                            contentPadding = gridContentPadding,
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                            verticalItemSpacing = gridSpacing,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(pageFilteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
                                NoteCard(
                                    note = note,
                                    searchQuery = searchQuery,
                                    onClick = { 
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = "edit"
                                        } else {
                                            onNavigateToEditNote(note.id)
                                        }
                                    },
                                    onLongClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = "quick_actions"
                                        } else {
                                            showQuickActionsForNote = note 
                                        }
                                    },
                                    onDoubleTap = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = "toggle_pin"
                                        } else {
                                            viewModel.togglePin(note) 
                                        }
                                    },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(500), 
                                        fadeOutSpec = tween(500), 
                                        placementSpec = tween(500, easing = FastOutSlowInEasing)
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = when (page) {
                                0 -> listState0
                                1 -> listState1
                                else -> listState2
                            },
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(pageFilteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd
                                    }
                                )

                                var showDeleteDialogLocal by remember { mutableStateOf(false) }

                                LaunchedEffect(dismissState.currentValue) {
                                    val currentVal = dismissState.currentValue
                                    if (currentVal == SwipeToDismissBoxValue.EndToStart) {
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = "delete"
                                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                        } else {
                                            when (currentTab) {
                                                "TRASHED" -> {
                                                    showDeleteDialogLocal = true
                                                }
                                                "ARCHIVED" -> {
                                                    viewModel.moveToTrash(note)
                                                }
                                                else -> { // ACTIVE
                                                     viewModel.moveToTrash(note)
                                                }
                                            }
                                        }
                                    } else if (currentVal == SwipeToDismissBoxValue.StartToEnd) {
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = "restore"
                                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                        } else {
                                            when (currentTab) {
                                                "TRASHED" -> {
                                                    viewModel.restoreFromTrash(note)
                                                }
                                                "ARCHIVED" -> {
                                                    viewModel.unarchiveNote(note)
                                                }
                                                else -> { // ACTIVE
                                                    viewModel.archiveNote(note)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showDeleteDialogLocal) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showDeleteDialogLocal = false
                                            scope.launch {
                                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                            }
                                        },
                                        title = { Text(text = stringResource(R.string.delete_permanent_title)) },
                                        text = { Text(text = stringResource(R.string.delete_permanent_msg)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteNotePermanent(note)
                                                    showDeleteDialogLocal = false
                                                    showCustomSnackbar(
                                                        title = "Deleted Permanently",
                                                        message = note.title.ifBlank { "Untitled Note" },
                                                        type = SnackbarType.ERROR,
                                                        actionLabel = "UNDO",
                                                        onAction = {
                                                            viewModel.updateNote(note)
                                                        }
                                                    )
                                                }
                                            ) {
                                                Text(text = stringResource(R.string.delete_permanent_confirm), color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = {
                                                showDeleteDialogLocal = false
                                                scope.launch {
                                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                }
                                            }) {
                                                Text(text = stringResource(R.string.delete_permanent_cancel))
                                            }
                                        }
                                    )
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(500), 
                                        fadeOutSpec = tween(500), 
                                        placementSpec = tween(500, easing = FastOutSlowInEasing)
                                    ),
                                    backgroundContent = {
                                        val activeDirection = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                            dismissState.targetValue
                                        } else {
                                            dismissState.dismissDirection
                                        }
                                        
                                        val color by animateColorAsState(
                                            when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                                SwipeToDismissBoxValue.StartToEnd -> when (currentTab) {
                                                    "TRASHED" -> MaterialTheme.colorScheme.primaryContainer
                                                    "ARCHIVED" -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                                }
                                            }
                                        )
                                        val alignment = when (activeDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.CenterStart
                                        }
                                        val iconType = when (activeDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> when (currentTab) {
                                                "TRASHED" -> CanvasIconType.UNARCHIVE
                                                "ARCHIVED" -> CanvasIconType.UNARCHIVE
                                                else -> CanvasIconType.ARCHIVE
                                            }
                                            else -> CanvasIconType.DELETE
                                        }
                                        val iconTint = when (activeDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> when (currentTab) {
                                                "TRASHED" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "ARCHIVED" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                                            }
                                            else -> MaterialTheme.colorScheme.onErrorContainer
                                        }

                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = alignment
                                        ) {
                                            if (activeDirection != SwipeToDismissBoxValue.Settled) {
                                                CanvasCustomIcon(
                                                    type = iconType,
                                                    tint = iconTint
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        NoteCard(
                                            note = note,
                                            searchQuery = searchQuery,
                                            onClick = { 
                                                if (note.isLocked) {
                                                    noteToUnlock = note
                                                    unlockPin = ""
                                                    unlockError = false
                                                    unlockActionType = "edit"
                                                } else {
                                                    onNavigateToEditNote(note.id)
                                                }
                                            },
                                            onLongClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (note.isLocked) {
                                                    noteToUnlock = note
                                                    unlockPin = ""
                                                    unlockError = false
                                                    unlockActionType = "quick_actions"
                                                } else {
                                                    showQuickActionsForNote = note 
                                                }
                                            },
                                            onDoubleTap = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (note.isLocked) {
                                                    noteToUnlock = note
                                                    unlockPin = ""
                                                    unlockError = false
                                                    unlockActionType = "toggle_pin"
                                                } else {
                                                    viewModel.togglePin(note) 
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // Sort & Filter Dynamic Bottom Sheet Panel
    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = "Sort & Filters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showSortBottomSheet = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                         CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "SORT BY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Grid/Flow of elegant Sort Options
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortOptions = listOf(
                        "newest" to "Newest First",
                        "oldest" to "Oldest First",
                        "a-z" to "Alphabetical A-Z",
                        "z-a" to "Alphabetical Z-A",
                        "color" to "Vibe Accent",
                        "pin" to "Pinned Status"
                    )

                    sortOptions.forEach { (optionKey, optionLabel) ->
                        val isSelected = sortBy == optionKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { sortBy = optionKey }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                CanvasCustomIcon(
                                    type = CanvasIconType.TICK,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                Text(
                    text = "FILTER OPTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = filterPinnedOnly,
                        onClick = { filterPinnedOnly = !filterPinnedOnly },
                        label = { Text("Pinned Only", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            CanvasCustomIcon(
                                type = if (filterPinnedOnly) CanvasIconType.PIN else CanvasIconType.UNPIN,
                                modifier = Modifier.size(16.dp),
                                tint = if (filterPinnedOnly) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { showSortBottomSheet = false },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Apply Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Dynamic Quick Actions Pop-Up Dialogue
    if (showQuickActionsForNote != null) {
        val note = showQuickActionsForNote!!
        var quickTitle by remember(note.id) { mutableStateOf(note.title) }
        var quickPinned by remember(note.id) { mutableStateOf(note.isPinned) }
        var quickColor by remember(note.id) { mutableStateOf(note.colorHex) }

        ModalBottomSheet(
            onDismissRequest = { showQuickActionsForNote = null },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Row
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
                        Text("Customize Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    IconButton(
                        onClick = { showQuickActionsForNote = null },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        CanvasCustomIcon(CanvasIconType.CLOSE, modifier = Modifier.size(16.dp))
                    }
                }

                // Title and Pin State Card Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = quickTitle,
                            onValueChange = { quickTitle = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CanvasCustomIcon(
                                    type = if (quickPinned) CanvasIconType.PIN else CanvasIconType.UNPIN,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Pin note to top", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Switch(checked = quickPinned, onCheckedChange = { quickPinned = it })
                        }
                    }
                }

                // Reordering Section (if visible)
                val currentIndex = filteredAndSortedNotes.indexOfFirst { it.id == note.id }
                if (currentIndex != -1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MANUAL REORDERING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = { moveNoteUp(currentIndex, filteredAndSortedNotes); showQuickActionsForNote = null },
                                enabled = currentIndex > 0,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CanvasCustomIcon(CanvasIconType.ARROW_UP, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Move Up", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { moveNoteDown(currentIndex, filteredAndSortedNotes); showQuickActionsForNote = null },
                                enabled = currentIndex < filteredAndSortedNotes.size - 1,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CanvasCustomIcon(CanvasIconType.ARROW_DOWN, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Move Down", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Change Vibe Accent color bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VIBE ACCENT COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val quickColors = listOf(null, "#E6E3FF", "#2D26A0", "#FFF0CC", "#9A5500", "#FFD6EC", "#8B2060")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickColors.forEach { colorHex ->
                            val colorBg = colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colorBg)
                                    .border(
                                        width = if (quickColor == colorHex) 2.5.dp else 1.dp,
                                        color = if (quickColor == colorHex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { quickColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (quickColor == colorHex) {
                                    CanvasCustomIcon(
                                        type = CanvasIconType.TICK,
                                        tint = if (colorHex == "#E6E3FF" || colorHex == "#FFF0CC" || colorHex == "#FFD6EC" || colorHex == null) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Quick Badges & Save Trigger
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                            showQuickActionsForNote = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        CanvasCustomIcon(CanvasIconType.SHARE, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (currentTab == "TRASHED") {
                                noteToDeletePermanently = note
                            } else {
                                viewModel.moveToTrash(note)
                            }
                            showQuickActionsForNote = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        CanvasCustomIcon(CanvasIconType.DELETE, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    }

                    if (currentTab != "ARCHIVED" && currentTab != "TRASHED") {
                        IconButton(
                            onClick = { viewModel.archiveNote(note); showQuickActionsForNote = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            CanvasCustomIcon(CanvasIconType.ARCHIVE, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    } else if (currentTab == "ARCHIVED") {
                        IconButton(
                            onClick = { viewModel.unarchiveNote(note); showQuickActionsForNote = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            CanvasCustomIcon(CanvasIconType.UNARCHIVE, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            viewModel.updateNote(note.copy(title = quickTitle, isPinned = quickPinned, colorHex = quickColor))
                            showQuickActionsForNote = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Save Vibe", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (noteToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text(text = stringResource(R.string.delete_permanent_title)) },
            text = { Text(text = stringResource(R.string.delete_permanent_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDeletePermanently?.let { note ->
                            viewModel.deleteNotePermanent(note)
                            showCustomSnackbar(
                                title = "Deleted Permanently",
                                message = note.title.ifBlank { "Untitled Note" },
                                type = SnackbarType.ERROR,
                                actionLabel = "UNDO",
                                onAction = {
                                    viewModel.updateNote(note)
                                }
                            )
                        }
                        noteToDeletePermanently = null
                    }
                ) {
                    Text(text = stringResource(R.string.delete_permanent_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeletePermanently = null }) {
                    Text(text = stringResource(R.string.delete_permanent_cancel))
                }
            }
        )
    }

    if (noteToUnlock != null) {
        ModalBottomSheet(
            onDismissRequest = { noteToUnlock = null },
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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CanvasCustomIcon(CanvasIconType.LOCK, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                
                Text(
                    text = "Decrypt Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Enter the 4-digit PIN to access this private note.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                OutlinedTextField(
                    value = unlockPin,
                    onValueChange = { 
                        unlockPin = it.take(4) 
                        unlockError = false
                    },
                    label = { Text("4-Digit PIN") },
                    isError = unlockError,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (unlockError) {
                    Text(
                        "Incorrect PIN. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { noteToUnlock = null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val pinString = noteToUnlock?.lockPin ?: ""
                            if (unlockPin == pinString) {
                                val unlockedNote = noteToUnlock!!
                                noteToUnlock = null
                                
                                when (unlockActionType) {
                                    "edit" -> onNavigateToEditNote(unlockedNote.id)
                                    "quick_actions" -> showQuickActionsForNote = unlockedNote
                                    "toggle_pin" -> viewModel.togglePin(unlockedNote)
                                    "delete" -> {
                                        if (currentTab == "TRASHED") {
                                            noteToDeletePermanently = unlockedNote
                                        } else {
                                            viewModel.moveToTrash(unlockedNote)
                                        }
                                    }
                                    "restore" -> {
                                        when (currentTab) {
                                            "TRASHED" -> viewModel.restoreFromTrash(unlockedNote)
                                            "ARCHIVED" -> viewModel.unarchiveNote(unlockedNote)
                                            else -> viewModel.archiveNote(unlockedNote)
                                        }
                                    }
                                }
                            } else {
                                unlockError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decrypt")
                    }
                }
            }
        }
    }

    // --- Custom iOS-style Snackbar Overlay ---
    AnimatedVisibility(
        visible = snackbarVisible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
            .navigationBarsPadding(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut() + shrinkVertically()
    ) {
        customSnackbarData?.let { data ->
            val iconContainerColor: Color
            val iconTint: Color
            val titleTextColor: Color
            val progressBarColor: Color
            val iconType: CanvasIconType

            when (data.type) {
                SnackbarType.ERROR -> {
                    iconContainerColor = Color(0x26EF4444)
                    iconTint = Color(0xFFF87171)
                    titleTextColor = Color(0xFFFCA5A5)
                    progressBarColor = Color(0xFFEF4444)
                    iconType = CanvasIconType.DELETE
                }
                SnackbarType.SUCCESS -> {
                    iconContainerColor = Color(0x2610B981)
                    iconTint = Color(0xFF34D399)
                    titleTextColor = Color(0xFF6EE7B7)
                    progressBarColor = Color(0xFF10B981)
                    iconType = CanvasIconType.TICK
                }
                SnackbarType.INFO -> {
                    iconContainerColor = Color(0x263B82F6)
                    iconTint = Color(0xFF60A5FA)
                    titleTextColor = Color(0xFF93C5FD)
                    progressBarColor = Color(0xFF3B82F6)
                    iconType = CanvasIconType.TICK
                }
                SnackbarType.WARNING -> {
                    iconContainerColor = Color(0x26F59E0B)
                    iconTint = Color(0xFFFBBF24)
                    titleTextColor = Color(0xFFFCD34D)
                    progressBarColor = Color(0xFFF59E0B)
                    iconType = CanvasIconType.TICK
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEA1A1B20)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(iconContainerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                CanvasCustomIcon(
                                    type = iconType,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = data.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = data.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            data.actionLabel?.let { actionLabel ->
                                TextButton(
                                    onClick = {
                                        data.onAction?.invoke()
                                        snackbarVisible = false
                                        scope.launch {
                                            kotlinx.coroutines.delay(300)
                                            customSnackbarData = null
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFFFFB300)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    snackbarVisible = false
                                    scope.launch {
                                        kotlinx.coroutines.delay(300)
                                        customSnackbarData = null
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Notification",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatableProgress.value)
                                .height(3.dp)
                                .background(progressBarColor)
                        )
                    }
                }
            }
        }
    }
    } // closes Box
}

@Composable
fun StatBadge(
    iconType: CanvasIconType,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CanvasCustomIcon(
                type = iconType,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    searchQuery: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val targetColor = note.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.surfaceVariant

    val backgroundColor by animateColorAsState(targetValue = targetColor)

    // Calculate reading time
    val words = note.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val readTimeChars = if (words > 10) "${words / 200 + 1} min read" else "$words words"

    // Analyze attachments/metadata inside note's content to decorate card beautifully
    val hasVoice = note.content.contains("[voice:") || note.content.contains("[audio:")
    val hasFile = note.content.contains("[file:")
    val hasImage = note.content.contains("![Image](")
    val hasTable = note.content.contains("|") && note.content.contains("---|")
    val hasCode = note.content.contains("```")
    val hasChecklist = note.content.contains("- [ ]") || note.content.contains("- [x]")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(note.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            if (note.isLocked) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvasCustomIcon(
                        type = CanvasIconType.LOCK,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Locked Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Title block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = highlightSearchQuery(note.title, searchQuery, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "Untitled Note",
                            style = MaterialTheme.typography.titleMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
    
                    if (note.isPinned) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CanvasCustomIcon(
                                type = CanvasIconType.PIN,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
    
                Spacer(modifier = Modifier.height(12.dp))
    
                // Content Preview block
                if (note.content.isNotBlank()) {
                    // Strips all markdown image tokens/voice links for clean preview
                    val cleanContent = remember(note.content) {
                        note.content
                            .replace(Regex("""!\[.*?\]\(.*?\)"""), "[Image]")
                            .replace(Regex("""\[voice.*?\]\(.*?\)"""), "[Voice clip]")
                            .replace(Regex("""\[file.*?\]\(.*?\)"""), "[Attachment]")
                            .replace(Regex("""\[color.*?\]\((.*?)\)"""), "$1")
    
                            .replace(Regex("""\[bg.*?\]\((.*?)\)"""), "$1")
                            .replace(Regex("""[#*`>]"""), "")
                            .trim()
                    }
    
                    Text(
                        text = highlightSearchQuery(cleanContent, searchQuery, MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
    
                // Interactive Dynamic Attachment badges
                if (hasVoice || hasFile || hasChecklist || hasImage || hasTable || hasCode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasVoice) {
                            IconBadge(type = CanvasIconType.MIC, desc = "Voice Recording Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasImage) {
                            IconBadge(type = CanvasIconType.IMAGE, desc = "Image Attachment Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasFile) {
                            IconBadge(type = CanvasIconType.ATTACH_FILE, desc = "File Document Indicator", color = MaterialTheme.colorScheme.secondary)
                        }
                        if (hasChecklist) {
                            IconBadge(type = CanvasIconType.CHECKBOX_ON, desc = "Checklist Tasks Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasTable) {
                            IconBadge(type = CanvasIconType.GRID_ON, desc = "Table Block Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasCode) {
                            IconBadge(type = CanvasIconType.CODE, desc = "Code Block Indicator", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
    
                // Categories Tag labels Row
                val noteTags = remember(note.tags) {
                    note.tags.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
    
                if (noteTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        noteTags.forEach { tag ->
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
                                    CanvasCustomIcon(
                                        type = CanvasIconType.LABEL,
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
                }
            } // End of else block for not locked

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    CanvasCustomIcon(
                        type = CanvasIconType.MENU_BOOK,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = readTimeChars,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IconBadge(type: CanvasIconType, desc: String, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        CanvasCustomIcon(
            type = type,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun CanvasPremiumGemIcon(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val pathLeft = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.5f, h)
            close()
        }
        val pathRight = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.5f, h)
            close()
        }
        val pathCenter = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.35f)
            close()
        }
        
        drawPath(
            path = pathLeft,
            brush = Brush.linearGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.4f)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
        drawPath(
            path = pathRight,
            brush = Brush.linearGradient(
                colors = listOf(tertiaryColor.copy(alpha = 0.8f), tertiaryColor.copy(alpha = 0.4f)),
                start = Offset(w, 0f),
                end = Offset(0f, h)
            )
        )
        drawPath(
            path = pathCenter,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.9f)),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h)
            )
        )
    }
}

@Composable
fun CanvasActiveTabIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Draw document page background
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.1f),
            size = Size(w * 0.7f, h * 0.8f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Draw lines inside
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(w * 0.3f, h * 0.3f),
            end = Offset(w * 0.7f, h * 0.3f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(w * 0.3f, h * 0.5f),
            end = Offset(w * 0.7f, h * 0.5f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(w * 0.3f, h * 0.7f),
            end = Offset(w * 0.6f, h * 0.7f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun CanvasArchiveTabIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    val tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Draw outer box
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.7f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Draw vault lock wheel
        drawCircle(
            color = tint,
            radius = w * 0.18f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Draw lock dial ticks
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.22f),
            end = Offset(w * 0.5f, h * 0.32f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.68f),
            end = Offset(w * 0.5f, h * 0.78f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.5f),
            end = Offset(w * 0.32f, h * 0.5f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.68f, h * 0.5f),
            end = Offset(w * 0.78f, h * 0.5f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun CanvasTrashTabIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    val tint = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Draw can body
        val pathBin = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.3f)
            lineTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.68f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.3f)
            close()
        }
        drawPath(
            path = pathBin,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        // Draw lid
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.25f),
            end = Offset(w * 0.85f, h * 0.25f),
            strokeWidth = 2.dp.toPx()
        )
        // Draw lid handle
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.4f, h * 0.15f),
            size = Size(w * 0.2f, h * 0.1f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        // Draw internal vertical ridges
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(w * 0.45f, h * 0.4f),
            end = Offset(w * 0.45f, h * 0.75f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(w * 0.55f, h * 0.4f),
            end = Offset(w * 0.55f, h * 0.75f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun CanvasFilterSortIcon(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Horizontal lines thickness
        val lineThickness = 2.dp.toPx()
        val knobRadius = 3.dp.toPx()
        
        // Slider 1 (Top)
        val y1 = h * 0.25f
        drawLine(
            color = tint.copy(alpha = 0.3f),
            start = Offset(w * 0.15f, y1),
            end = Offset(w * 0.85f, y1),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, y1),
            end = Offset(w * 0.45f, y1),
            strokeWidth = lineThickness
        )
        drawCircle(
            color = tint,
            radius = knobRadius,
            center = Offset(w * 0.45f, y1)
        )
        
        // Slider 2 (Middle)
        val y2 = h * 0.5f
        drawLine(
            color = tint.copy(alpha = 0.3f),
            start = Offset(w * 0.15f, y2),
            end = Offset(w * 0.85f, y2),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, y2),
            end = Offset(w * 0.7f, y2),
            strokeWidth = lineThickness
        )
        drawCircle(
            color = tint,
            radius = knobRadius,
            center = Offset(w * 0.7f, y2)
        )
        
        // Slider 3 (Bottom)
        val y3 = h * 0.75f
        drawLine(
            color = tint.copy(alpha = 0.3f),
            start = Offset(w * 0.15f, y3),
            end = Offset(w * 0.85f, y3),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, y3),
            end = Offset(w * 0.3f, y3),
            strokeWidth = lineThickness
        )
        drawCircle(
            color = tint,
            radius = knobRadius,
            center = Offset(w * 0.3f, y3)
        )
    }
}
