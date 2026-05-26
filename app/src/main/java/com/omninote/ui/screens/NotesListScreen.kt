package com.omninote.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omninote.data.NoteEntity
import com.omninote.ui.viewmodels.NotesViewModel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToEditNote: (Int) -> Unit
) {
    var currentTab by remember { mutableStateOf("ACTIVE") }
    
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()

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

    // Snachbar state for undo
    val snackbarHostState = remember { SnackbarHostState() }
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

    // Retrieve unique tags from all stored notes
    val allTags = remember(notes) {
        notes.flatMap { note ->
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
    val checklistItemsCount = notes.sumOf { note ->
        note.content.split("\n").count { it.trim().startsWith("- [ ]") || it.trim().startsWith("- [x]") || it.trim().startsWith("* [ ") }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Custom professional micro gem icon with styling
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = com.omninote.R.drawable.ic_gem),
                                    contentDescription = "Premium OmniNote Gem",
                                    tint = Color.Unspecified, 
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "OmniNote",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Smart Workspace",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Layout Actions Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            AnimatedContent(targetState = isGridView, label = "LayoutToggle") { grid ->
                                Icon(
                                    imageVector = if (grid) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle layout",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showSortBottomSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort Filters",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                var isSearchActive by remember { mutableStateOf(false) }

                AnimatedVisibility(
                    visible = !isSearchActive, 
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(400)), 
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(tween(400))
                ) {
                    // Modern Statistics Widget Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.4f), MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.4f))))
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBadge(icon = Icons.Default.StickyNote2, label = "Notes", value = "$totalNotesCount", color = MaterialTheme.colorScheme.primary)
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                        StatBadge(icon = Icons.Default.PushPin, label = "Pinned", value = "$pinnedNotesCount", color = MaterialTheme.colorScheme.tertiary)
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                        StatBadge(icon = Icons.Default.Mic, label = "Audio", value = "$audioNotesCount", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                // Premium Search input bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        if (it.isNotEmpty()) isSearchActive = true
                    },
                    shape = RoundedCornerShape(24.dp),
                    placeholder = {
                        Text("Search in notes...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; isSearchActive = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state -> 
                            if (state.isFocused) isSearchActive = true 
                            else if (searchQuery.isEmpty()) isSearchActive = false 
                        }
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.height(72.dp)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.StickyNote2, contentDescription = "Active Notes") },
                        label = { Text("Active", fontWeight = FontWeight.Bold) },
                        selected = currentTab == "ACTIVE",
                        onClick = { currentTab = "ACTIVE" },
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
                        icon = { Icon(Icons.Default.Archive, contentDescription = "Archived Notes") },
                        label = { Text("Archive", fontWeight = FontWeight.Bold) },
                        selected = currentTab == "ARCHIVED",
                        onClick = { currentTab = "ARCHIVED" },
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
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Trashed Notes") },
                        label = { Text("Trash", fontWeight = FontWeight.Bold) },
                        selected = currentTab == "TRASHED",
                        onClick = { currentTab = "TRASHED" },
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
        },
        floatingActionButton = {
            if (currentTab == "ACTIVE") {
                ExtendedFloatingActionButton(
                    text = { 
                        Text(
                            text = "New Note",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Add New Note"
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
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Empty Trash"
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
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
                                    Icon(
                                        imageVector = if (selected) Icons.Default.Check else Icons.Default.Label,
                                        contentDescription = null,
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

            // Cards Grid or List
            if (filteredAndSortedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank() || selectedTag != null || filterPinnedOnly) Icons.Default.SearchOff else Icons.Default.NoteAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTag != null || filterPinnedOnly) {
                                "No notes match your filters"
                            } else {
                                "Your workspace is pristine.\nTap the button to write down your thoughts!"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        if (searchQuery.isNotBlank() || selectedTag != null || filterPinnedOnly) {
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
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 165.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalItemSpacing = 16.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredAndSortedNotes, key = { _, note -> note.id }) { index, note ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                        if (note.isLocked) {
                                            noteToUnlock = note
                                            unlockPin = ""
                                            unlockError = false
                                            unlockActionType = if (dismissValue == SwipeToDismissBoxValue.EndToStart) "delete" else "restore"
                                            return@rememberSwipeToDismissBoxState false
                                        }

                                        when (currentTab) {
                                            "TRASHED" -> {
                                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                    viewModel.deleteNotePermanent(note)
                                                } else {
                                                    viewModel.restoreFromTrash(note)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Note restored from Trash")
                                                    }
                                                }
                                            }
                                            "ARCHIVED" -> {
                                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                    viewModel.moveToTrash(note)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Archived to Trash",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreFromTrash(note)
                                                        }
                                                    }
                                                } else {
                                                    viewModel.unarchiveNote(note)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Note unarchived")
                                                    }
                                                }
                                            }
                                            else -> { // ACTIVE
                                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                    viewModel.moveToTrash(note)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Moved to Trash",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreFromTrash(note)
                                                        }
                                                    }
                                                } else {
                                                    viewModel.archiveNote(note)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Note Archived",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.unarchiveNote(note)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

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
                                    val icon = when (activeDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> when (currentTab) {
                                            "TRASHED" -> Icons.Default.Restore
                                            "ARCHIVED" -> Icons.Default.Unarchive
                                            else -> Icons.Default.Archive
                                        }
                                        else -> Icons.Default.Delete
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
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "Swipe Action",
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
                                            if (note.isLocked) {
                                                noteToUnlock = note
                                                unlockPin = ""
                                                unlockError = false
                                                unlockActionType = "toggle_pin"
                                            } else {
                                                viewModel.togglePin(note) 
                                            }
                                        }
                                    )
                                }
                            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sort & Filters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showSortBottomSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    "Sorting Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                        )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
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
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                Text(
                    "Advanced Filters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = filterPinnedOnly,
                        onClick = { filterPinnedOnly = !filterPinnedOnly },
                        label = { Text("Pinned Only") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (filterPinnedOnly) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showSortBottomSheet = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Configuration")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Customize Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = { quickTitle = it },
                    label = { Text("Quick Edit Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                    Text("Pin note to top", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = quickPinned, onCheckedChange = { quickPinned = it })
                }

                val currentIndex = filteredAndSortedNotes.indexOfFirst { it.id == note.id }
                if (currentIndex != -1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Manual Reordering", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = { moveNoteUp(currentIndex, filteredAndSortedNotes); showQuickActionsForNote = null },
                                enabled = currentIndex > 0,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Up", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = { moveNoteDown(currentIndex, filteredAndSortedNotes); showQuickActionsForNote = null },
                                enabled = currentIndex < filteredAndSortedNotes.size - 1,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Down", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Change Vibe accent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val quickColors = listOf(null, "#E6E3FF", "#2D26A0", "#FFF0CC", "#9A5500", "#FFD6EC", "#8B2060")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickColors.forEach { colorHex ->
                            val colorBg = colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorBg)
                                    .border(width = if (quickColor == colorHex) 2.5.dp else 1.dp, color = if (quickColor == colorHex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f), shape = CircleShape)
                                    .clickable { quickColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (quickColor == colorHex) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = if (colorHex == "#E6E3FF" || colorHex == "#FFF0CC" || colorHex == "#FFD6EC" || colorHex == null) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }

                    IconButton(
                        onClick = {
                            if (currentTab == "TRASHED") { viewModel.deleteNotePermanent(note) } else { viewModel.moveToTrash(note) }
                            showQuickActionsForNote = null
                            scope.launch {
                                if (currentTab != "TRASHED") {
                                    val result = snackbarHostState.showSnackbar(message = "Moved to trash", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                    if (result == SnackbarResult.ActionPerformed) { viewModel.restoreFromTrash(note) }
                                }
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }

                    if (currentTab != "ARCHIVED" && currentTab != "TRASHED") {
                        IconButton(
                            onClick = { viewModel.archiveNote(note); showQuickActionsForNote = null },
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                        ) { Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                    } else if (currentTab == "ARCHIVED") {
                        IconButton(
                            onClick = { viewModel.unarchiveNote(note); showQuickActionsForNote = null },
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                        ) { Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            viewModel.updateNote(note.copy(title = quickTitle, isPinned = quickPinned, colorHex = quickColor))
                            showQuickActionsForNote = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Vibe")
                    }
                }
            }
        }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Note is Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Enter the 4-digit PIN to open this note.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = unlockPin,
                    onValueChange = { 
                        unlockPin = it.take(4) 
                        unlockError = false
                    },
                    label = { Text("PIN") },
                    isError = unlockError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (unlockError) {
                    Text(
                        "Incorrect PIN.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { noteToUnlock = null }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                                            viewModel.deleteNotePermanent(unlockedNote)
                                        } else {
                                            viewModel.moveToTrash(unlockedNote)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Archived to Trash",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreFromTrash(unlockedNote)
                                                }
                                            }
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
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
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Note",
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
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned Note Indicator Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(45f)
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
                            IconBadge(icon = Icons.Default.Mic, desc = "Voice Recording Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasImage) {
                            IconBadge(icon = Icons.Default.Image, desc = "Image Attachment Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasFile) {
                            IconBadge(icon = Icons.Default.AttachFile, desc = "File Document Indicator", color = MaterialTheme.colorScheme.secondary)
                        }
                        if (hasChecklist) {
                            IconBadge(icon = Icons.Default.CheckBox, desc = "Checklist Tasks Indicator", color = MaterialTheme.colorScheme.primary)
                        }
                        if (hasTable) {
                            IconBadge(icon = Icons.Default.GridOn, desc = "Table Block Indicator", color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasCode) {
                            IconBadge(icon = Icons.Default.Code, desc = "Code Block Indicator", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
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
fun IconBadge(icon: ImageVector, desc: String, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}
