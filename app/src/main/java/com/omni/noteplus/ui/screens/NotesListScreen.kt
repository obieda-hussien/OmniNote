package com.omni.noteplus.ui.screens

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.omni.noteplus.ui.components.OmniIcon
import com.omni.noteplus.ui.components.OmniIconType
import com.omni.noteplus.ui.components.NoteLibraryHeader
import com.omni.noteplus.ui.components.NoteCaptureBar
import com.omni.noteplus.ui.components.LibraryChromeState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.omni.noteplus.ui.components.OmniSheet
import com.omni.noteplus.ui.components.OmniConfirmDialog
import com.omni.noteplus.ui.components.noteSurface
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import com.omni.noteplus.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omni.noteplus.data.*
import com.omni.noteplus.ui.components.NoteSwipeCard
import com.omni.noteplus.ui.components.actionLabel
import com.omni.noteplus.ui.viewmodels.NotesViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import com.omni.noteplus.ui.components.libraryNotes
import com.omni.noteplus.ui.components.NoteMarkdownParser

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
                fontWeight = FontWeight.SemiBold,
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
    Box(Modifier.size(120.dp).background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center) {
        OmniIcon(when {
            isSearchOrFilter -> OmniIconType.SEARCH
            tabType == "TRASHED" -> OmniIconType.DELETE
            tabType == "ARCHIVED" -> OmniIconType.ARCHIVE
            else -> OmniIconType.NOTES
        }, Modifier.size(56.dp), primaryColor)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToEditNote: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarQueue = remember { androidx.compose.runtime.mutableStateListOf<CustomSnackbarData>() }
    val currentSnackbar = snackbarQueue.firstOrNull()
    val animatableProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    var snackbarVisible by remember { mutableStateOf(false) }

    fun showCustomSnackbar(
        title: String,
        message: String,
        type: SnackbarType,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        snackbarQueue.add(
            CustomSnackbarData(
                id = System.currentTimeMillis(),
                title = title,
                message = message,
                type = type,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    LaunchedEffect(currentSnackbar) {
        if (currentSnackbar != null) {
            animatableProgress.snapTo(1f)
            snackbarVisible = true
            animatableProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 5000, easing = androidx.compose.animation.core.LinearEasing)
            )
            snackbarVisible = false
            kotlinx.coroutines.delay(300)
            if (snackbarQueue.isNotEmpty()) {
                snackbarQueue.removeAt(0)
            }
        } else {
            snackbarVisible = false
        }
    }

    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val pendingActions by viewModel.pendingActions.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("OmniNotePrefs", android.content.Context.MODE_PRIVATE) }

    // 3-tab smooth ViewPager
    val initialPage = remember { sharedPrefs.getInt("lastTab", 0).coerceIn(0, 2) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) {
        sharedPrefs.edit().putInt("lastTab", pagerState.currentPage).apply()
    }

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

    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Configuration states (Persisted)
    var sortBy by remember { mutableStateOf(sharedPrefs.getString("sortBy", "newest") ?: "newest") }
    var filterPinnedOnly by remember { mutableStateOf(sharedPrefs.getBoolean("filterPinnedOnly", false)) }
    var isGridView by remember { mutableStateOf(sharedPrefs.getBoolean("isGridView", true)) }

    LaunchedEffect(sortBy, filterPinnedOnly, isGridView) {
        sharedPrefs.edit()
            .putString("sortBy", sortBy)
            .putBoolean("filterPinnedOnly", filterPinnedOnly)
            .putBoolean("isGridView", isGridView)
            .apply()
    }

    // Scroll state handlers for top and bottom bar hide/show dynamics
    val gridState0 = rememberLazyStaggeredGridState()
    val listState0 = rememberLazyListState()

    val gridState1 = rememberLazyStaggeredGridState()
    val listState1 = rememberLazyListState()

    val gridState2 = rememberLazyStaggeredGridState()
    val listState2 = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.noteEvent.collect { event ->
            when (event) {
                is com.omni.noteplus.ui.viewmodels.NoteEvent.ActionCompleted -> {
                    val receipt = event.receipt
                    showCustomSnackbar(context.getString(actionLabel(receipt.action)), receipt.title.ifBlank { "Untitled Note" },
                        if (receipt.action == NoteLibraryAction.DELETE) SnackbarType.ERROR else SnackbarType.SUCCESS,
                        actionLabel = if (receipt.undoToken != null) "UNDO" else null,
                        onAction = receipt.undoToken?.let { { viewModel.undoLibraryAction(receipt) } })
                }
                is com.omni.noteplus.ui.viewmodels.NoteEvent.Failed -> showCustomSnackbar("Could not complete action", event.message,
                    SnackbarType.ERROR, if (event.retry != null) "RETRY" else null, event.retry)
                com.omni.noteplus.ui.viewmodels.NoteEvent.UndoCompleted -> showCustomSnackbar("Action undone", "The previous section was restored.", SnackbarType.SUCCESS)
                com.omni.noteplus.ui.viewmodels.NoteEvent.TrashEmptied -> showCustomSnackbar("Trash emptied", "Trashed notes were permanently deleted.", SnackbarType.INFO)
                is com.omni.noteplus.ui.viewmodels.NoteEvent.Trashed -> {
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
                is com.omni.noteplus.ui.viewmodels.NoteEvent.Archived -> {
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
                is com.omni.noteplus.ui.viewmodels.NoteEvent.Restored -> {
                    showCustomSnackbar(
                        title = "Note Restored",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.SUCCESS
                    )
                }
                is com.omni.noteplus.ui.viewmodels.NoteEvent.Unarchived -> {
                    showCustomSnackbar(
                        title = "Note Unarchived",
                        message = event.note.title.ifBlank { "Untitled Note" },
                        type = SnackbarType.SUCCESS
                    )
                }
            }
        }
    }



    // Snackbar state for undo
    val scope = rememberCoroutineScope()

    // Quick Actions pop-up State
    var showQuickActionsForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showStatsDashboard by remember { mutableStateOf(false) }
    var quickCaptureText by rememberSaveable { mutableStateOf("") }
    var quickCaptureSaving by remember { mutableStateOf(false) }

    // Lock prompt state
    var noteToUnlock by remember { mutableStateOf<NoteEntity?>(null) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf(false) }
    var unlockActionType by remember { mutableStateOf("") }

    // Permanent deletion confirmation state
    var noteToDeletePermanently by remember { mutableStateOf<NoteEntity?>(null) }
    var authorizedNote by remember { mutableStateOf<Pair<Int, String>?>(null) }
    fun requestNoteAction(note: NoteEntity, action: NoteLibraryAction) {
        val pin = authorizedNote?.takeIf { it.first == note.id }?.second
        if (note.isLocked && pin != note.lockPin) {
            noteToUnlock = note
            unlockPin = ""
            unlockError = false
            unlockActionType = "library:${action.name}"
        } else if (action == NoteLibraryAction.DELETE) noteToDeletePermanently = note
        else viewModel.performLibraryAction(note, action, pin)
    }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

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
        libraryNotes(notes, selectedTag, searchQuery, sortBy, filterPinnedOnly)
    }

    // Statistical variables for dashboard metrics
    val totalNotesCount = notes.size

    val isImeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val chrome = remember { LibraryChromeState() }
    var footerHeight by remember { mutableStateOf(0.dp) }
    val atTop by remember(isGridView, pagerState.currentPage) {
        derivedStateOf {
            if (isGridView) {
                val state = when (pagerState.currentPage) { 0 -> gridState0; 1 -> gridState1; else -> gridState2 }
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            } else {
                val state = when (pagerState.currentPage) { 0 -> listState0; 1 -> listState1; else -> listState2 }
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            }
        }
    }
    LaunchedEffect(pagerState.currentPage, isGridView) { chrome.show() }
    val topNow by rememberUpdatedState(atTop)
    val imeNow by rememberUpdatedState(isImeVisible)
    val scrollConnection = remember(chrome, density) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Follow the gesture, not layout/fling corrections when the bars resize the list.
                if (!imeNow && source == NestedScrollSource.UserInput)
                    chrome.scroll(available.y, topNow, with(density) { 32.dp.toPx() })
                return Offset.Zero
            }
        }
    }
    fun saveQuickCapture() {
        if (quickCaptureText.isBlank() || quickCaptureSaving) return
        val captured = quickCaptureText.trim()
        quickCaptureSaving = true
        scope.launch {
            try {
                viewModel.createQuickNote(captured)
                if (quickCaptureText.trim() == captured) quickCaptureText = ""
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (e: Exception) { snackbarHostState.showSnackbar("Couldn't save. Try again.") }
            finally { quickCaptureSaving = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                OmniIcon(
                                    type = if (snackbarData.visuals.message.contains("Trash", ignoreCase = true) || snackbarData.visuals.message.contains("trash", ignoreCase = true)) {
                                        OmniIconType.DELETE
                                    } else {
                                        OmniIconType.TICK
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
            NoteLibraryHeader(
                title = when (currentTab) {
                    "ACTIVE" -> stringResource(R.string.notes_title)
                    "ARCHIVED" -> stringResource(R.string.archive_title)
                    else -> stringResource(R.string.trash_title)
                },
                summary = "$totalNotesCount " + stringResource(R.string.notes_count_label),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                grid = isGridView,
                onLayoutChange = { isGridView = !isGridView },
                onFilter = { showSortBottomSheet = true },
                onStats = { showStatsDashboard = true },
                expanded = chrome.expanded,
                onExpand = { chrome.show() },
                filtersActive = selectedTag != null || filterPinnedOnly || sortBy != "newest"
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().onSizeChanged { footerHeight = with(density) { it.height.toDp() } }) {
                if (currentTab == "ACTIVE") NoteCaptureBar(quickCaptureText, { quickCaptureText = it },
                    quickCaptureSaving, ::saveQuickCapture, onNavigateToAddNote)
                else if (currentTab == "TRASHED" && notes.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            OmniIcon(OmniIconType.DELETE, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.empty_trash_title))
                        }
                    }
                }
            AnimatedVisibility(
                visible = chrome.expanded && !isImeVisible,
                enter = expandVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface) // gorgeous iOS glassy bubble dock
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(64.dp)
                    ) {
                        NavigationBarItem(
                            icon = { OmniIcon(OmniIconType.NOTES) },
                            label = { Text("Active", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "ACTIVE",
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                        NavigationBarItem(
                            icon = {
                                OmniIcon(
                                    type = OmniIconType.ARCHIVE,
                                    tint = if (currentTab == "ARCHIVED") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            },
                            label = { Text("Archive", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "ARCHIVED",
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                        NavigationBarItem(
                            icon = { OmniIcon(OmniIconType.DELETE) },
                            label = { Text("Trash", fontWeight = FontWeight.Bold) },
                            selected = currentTab == "TRASHED",
                            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                            alwaysShowLabel = true,
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
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            // Dynamic Tag / Category filter carousel
            AnimatedVisibility(visible = allTags.isNotEmpty() && chrome.expanded,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))) {
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
                        label = { Text("All Notes", fontWeight = if (isAllNotes) FontWeight.SemiBold else FontWeight.SemiBold) },
                        leadingIcon = {
                            OmniIcon(
                                type = OmniIconType.MENU_BOOK,
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
                            label = { Text(tag, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.SemiBold) },
                            leadingIcon = {
                                AnimatedContent(targetState = isSelected, label = "TagIcon") { selected ->
                                    OmniIcon(
                                        type = if (selected) OmniIconType.TICK else OmniIconType.LABEL,
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
                userScrollEnabled = false, // Horizontal card gestures own archive/trash; tabs navigate sections.
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
                    libraryNotes(pageNotes, selectedTag, searchQuery, sortBy, filterPinnedOnly)
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
                            screenWidthDp < 360 -> PaddingValues(top = 12.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
                            else -> PaddingValues(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
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
                            modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection).testTag("note_library_grid")
                        ) {
                            itemsIndexed(pageFilteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
                                NoteSwipeCard(note, enabled = note.id !in pendingActions && noteToUnlock == null && noteToDeletePermanently == null && pagerState.settledPage == page,
                                    onAction = { requestNoteAction(note, it) },
                                    modifier = Modifier.animateItem(fadeInSpec = tween(220), fadeOutSpec = tween(220), placementSpec = tween(220, easing = FastOutSlowInEasing))) {
                                NoteCard(
                                    note = note,
                                    searchQuery = searchQuery,
                                    onClick = {
                                        if (note.id in pendingActions) return@NoteCard
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
                                        if (note.id in pendingActions) return@NoteCard
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
                                        if (note.id in pendingActions) return@NoteCard
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
                            }
                        }
                    } else {
                        LazyColumn(
                            state = when (page) {
                                0 -> listState0
                                1 -> listState1
                                else -> listState2
                            },
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection).testTag("note_library_list")
                        ) {
                            itemsIndexed(pageFilteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
                                NoteSwipeCard(note, enabled = note.id !in pendingActions && noteToUnlock == null && noteToDeletePermanently == null && pagerState.settledPage == page,
                                    onAction = { requestNoteAction(note, it) },
                                    modifier = Modifier.animateItem(fadeInSpec = tween(220), fadeOutSpec = tween(220), placementSpec = tween(220, easing = FastOutSlowInEasing))) {
                                        NoteCard(
                                            note = note,
                                            searchQuery = searchQuery,
                                            onClick = {
                                                if (note.id in pendingActions) return@NoteCard
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
                                                if (note.id in pendingActions) return@NoteCard
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
                                                if (note.id in pendingActions) return@NoteCard
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
                            }
                        }
                    }
                }
                }
            } // HorizontalPager
        }
    }

    // Sort & Filter Dynamic Bottom Sheet Panel
    if (showSortBottomSheet) {
        OmniSheet(
            onDismissRequest = { showSortBottomSheet = false },
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
                            OmniIcon(OmniIconType.TUNE, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "Sort & Filters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showSortBottomSheet = false },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                         OmniIcon(OmniIconType.CLOSE, modifier = Modifier.size(16.dp))
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
                        "color" to "Note color",
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
                                OmniIcon(
                                    type = OmniIconType.TICK,
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
                            OmniIcon(
                                type = if (filterPinnedOnly) OmniIconType.PIN else OmniIconType.UNPIN,
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

    if (showStatsDashboard) {
        val productivity by viewModel.productivity.collectAsStateWithLifecycle()
        val productivityError by viewModel.productivityError.collectAsStateWithLifecycle()
        com.omni.noteplus.ui.components.ProductivitySheet(productivity, productivityError,
            onRetry = viewModel::initializeProductivity, onDismiss = { showStatsDashboard = false })
    }

    // Dynamic Quick Actions Pop-Up Dialogue
    if (showQuickActionsForNote != null) {
        val note = showQuickActionsForNote!!
        var quickTitle by remember(note.id) { mutableStateOf(note.title) }
        var quickPinned by remember(note.id) { mutableStateOf(note.isPinned) }
        var quickColor by remember(note.id) { mutableStateOf(note.colorHex) }

        OmniSheet(
            onDismissRequest = { showQuickActionsForNote = null },
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
                            OmniIcon(OmniIconType.TUNE, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text("Note options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = { showQuickActionsForNote = null },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        OmniIcon(OmniIconType.CLOSE, modifier = Modifier.size(16.dp))
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
                                OmniIcon(
                                    type = if (quickPinned) OmniIconType.PIN else OmniIconType.UNPIN,
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
                val currentIndex = if (sortBy == "newest") filteredAndSortedNotes.indexOfFirst { it.id == note.id } else -1
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
                                OmniIcon(OmniIconType.ARROW_UP, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Move Up", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { moveNoteDown(currentIndex, filteredAndSortedNotes); showQuickActionsForNote = null },
                                enabled = currentIndex < filteredAndSortedNotes.size - 1,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                OmniIcon(OmniIconType.ARROW_DOWN, modifier = Modifier.size(16.dp))
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
                                    OmniIcon(
                                        type = OmniIconType.TICK,
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
                        OmniIcon(OmniIconType.SHARE, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            requestNoteAction(note, swipeAction(note, false))
                            showQuickActionsForNote = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        OmniIcon(OmniIconType.DELETE, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    }

                    if (!note.isArchived && !note.isTrashed) {
                        IconButton(
                            onClick = { requestNoteAction(note, NoteLibraryAction.ARCHIVE); showQuickActionsForNote = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            OmniIcon(OmniIconType.ARCHIVE, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    } else if (note.isArchived && !note.isTrashed) {
                        IconButton(
                            onClick = { requestNoteAction(note, NoteLibraryAction.UNARCHIVE); showQuickActionsForNote = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            OmniIcon(OmniIconType.UNARCHIVE, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
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
                        Text("Save changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (noteToDeletePermanently != null) {
        OmniConfirmDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text(text = stringResource(R.string.delete_permanent_title)) },
            text = { Text(text = stringResource(R.string.delete_permanent_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDeletePermanently?.let { note ->
                            viewModel.deleteNotePermanent(note, authorizedNote?.takeIf { it.first == note.id }?.second)

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

    if (showEmptyTrashDialog) {
        OmniConfirmDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text(text = stringResource(R.string.empty_trash_title)) },
            text = { Text(text = stringResource(R.string.empty_trash_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false

                    }
                ) {
                    Text(text = stringResource(R.string.delete_permanent_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text(text = stringResource(R.string.delete_permanent_cancel))
                }
            }
        )
    }

    if (noteToUnlock != null) {
        OmniSheet(
            onDismissRequest = { noteToUnlock = null },
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
                    OmniIcon(OmniIconType.LOCK, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "Unlock note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
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
                                authorizedNote = unlockedNote.id to unlockPin
                                noteToUnlock = null

                                when {
                                    unlockActionType.startsWith("library:") -> requestNoteAction(unlockedNote, NoteLibraryAction.valueOf(unlockActionType.substringAfter(":")))
                                    unlockActionType == "edit" -> onNavigateToEditNote(unlockedNote.id)
                                    unlockActionType == "quick_actions" -> showQuickActionsForNote = unlockedNote
                                    unlockActionType == "toggle_pin" -> viewModel.togglePin(unlockedNote)

                                }
                            } else {
                                unlockError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlock")
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
            .padding(bottom = footerHeight + 8.dp, start = 16.dp, end = 16.dp),
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
        currentSnackbar?.let { data ->
            val iconContainerColor: Color
            val iconTint: Color
            val titleTextColor: Color
            val progressBarColor: Color
            val iconType: OmniIconType

            when (data.type) {
                SnackbarType.ERROR -> {
                    iconContainerColor = Color(0x26EF4444)
                    iconTint = MaterialTheme.colorScheme.error
                    titleTextColor = Color(0xFFFCA5A5)
                    progressBarColor = Color(0xFFEF4444)
                    iconType = OmniIconType.DELETE
                }
                SnackbarType.SUCCESS -> {
                    iconContainerColor = Color(0x2610B981)
                    iconTint = MaterialTheme.colorScheme.tertiary
                    titleTextColor = Color(0xFF6EE7B7)
                    progressBarColor = Color(0xFF10B981)
                    iconType = OmniIconType.TICK
                }
                SnackbarType.INFO -> {
                    iconContainerColor = Color(0x263B82F6)
                    iconTint = MaterialTheme.colorScheme.primary
                    titleTextColor = Color(0xFF93C5FD)
                    progressBarColor = Color(0xFF3B82F6)
                    iconType = OmniIconType.TICK
                }
                SnackbarType.WARNING -> {
                    iconContainerColor = Color(0x26F59E0B)
                    iconTint = MaterialTheme.colorScheme.secondary
                    titleTextColor = Color(0xFFFCD34D)
                    progressBarColor = Color(0xFFF59E0B)
                    iconType = OmniIconType.TICK
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                OmniIcon(
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = data.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                            if (snackbarQueue.isNotEmpty()) {
                                                snackbarQueue.removeAt(0)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
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
                                        if (snackbarQueue.isNotEmpty()) {
                                            snackbarQueue.removeAt(0)
                                        }
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Notification",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
    iconType: OmniIconType,
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
            OmniIcon(
                type = iconType,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
    val backgroundColor by animateColorAsState(
        targetValue = noteSurface(note.colorHex),
        animationSpec = tween(180), label = "NoteCardColor"
    )

    // Calculate reading time
    val words = remember(note.content) { note.content.split(Regex("\\s+")).count { it.isNotBlank() } }
    val readTimeChars = if (words > 10) "${words / 200 + 1} min read" else "$words words"

    // Analyze attachments/metadata inside note's content to decorate card beautifully
    val cardPreview by produceState(com.omni.noteplus.ui.components.NoteCardPreview("", emptySet()), note.content, note.isLocked) {
        value = if (note.isLocked) com.omni.noteplus.ui.components.NoteCardPreview("", emptySet())
            else withContext(Dispatchers.Default) { NoteMarkdownParser.cardPreview(note.content) }
    }
    val hasVoice = com.omni.noteplus.ui.components.AttachmentKind.AUDIO in cardPreview.media
    val hasFile = com.omni.noteplus.ui.components.AttachmentKind.FILE in cardPreview.media
    val hasImage = com.omni.noteplus.ui.components.AttachmentKind.IMAGE in cardPreview.media
    val hasTable = note.content.contains("|") && note.content.contains("---|")
    val hasCode = note.content.contains("```")
    val hasChecklist = remember(note.content) { Regex("""(?m)^\s*(?:[-*+]|\d+[.)])\s+\[[ xX]\]""").containsMatchIn(note.content) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleTap
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                    OmniIcon(
                        type = OmniIconType.LOCK,
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
                            fontWeight = FontWeight.SemiBold,
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
                            OmniIcon(
                                type = OmniIconType.PIN,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Preview block
                if (note.content.isNotBlank()) {
                    val cleanContent = cardPreview.text

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
                            IconBadge(type = OmniIconType.MIC, desc = "Voice Recording Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasImage) {
                            IconBadge(type = OmniIconType.IMAGE, desc = "Image Attachment Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasFile) {
                            IconBadge(type = OmniIconType.ATTACH_FILE, desc = "File Document Indicator", color = MaterialTheme.colorScheme.secondary)
                        }
                        if (hasChecklist) {
                            IconBadge(type = OmniIconType.CHECKBOX_ON, desc = "Checklist Tasks Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasTable) {
                            IconBadge(type = OmniIconType.GRID_ON, desc = "Table Block Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasCode) {
                            IconBadge(type = OmniIconType.CODE, desc = "Code Block Indicator", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    OmniIcon(
                                        type = OmniIconType.LABEL,
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
                    OmniIcon(
                        type = OmniIconType.MENU_BOOK,
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
fun IconBadge(type: OmniIconType, desc: String, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        OmniIcon(
            type = type,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}

