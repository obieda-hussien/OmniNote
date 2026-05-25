package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.NoteEntity
import com.example.ui.viewmodels.NotesViewModel

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
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Configuration states
    var sortBy by remember { mutableStateOf("newest") } // "newest", "oldest", "a-z", "z-a", "color", "pin"
    var filterPinnedOnly by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    // Quick Actions pop-up State
    var showQuickActionsForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

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
            viewModel.updateNote(
                id = note1.id,
                title = note1.title,
                content = note1.content,
                isPinned = note1.isPinned,
                colorHex = note1.colorHex,
                timestamp = note2.timestamp,
                tags = note1.tags
            )
            viewModel.updateNote(
                id = note2.id,
                title = note2.title,
                content = note2.content,
                isPinned = note2.isPinned,
                colorHex = note2.colorHex,
                timestamp = tempTime,
                tags = note2.tags
            )
        }
    }

    fun moveNoteDown(currentIndex: Int, currentList: List<NoteEntity>) {
        if (currentIndex < currentList.size - 1) {
            val note1 = currentList[currentIndex]
            val note2 = currentList[currentIndex + 1]
            val tempTime = note1.timestamp
            viewModel.updateNote(
                id = note1.id,
                title = note1.title,
                content = note1.content,
                isPinned = note1.isPinned,
                colorHex = note1.colorHex,
                timestamp = note2.timestamp,
                tags = note1.tags
            )
            viewModel.updateNote(
                id = note2.id,
                title = note2.title,
                content = note2.content,
                isPinned = note2.isPinned,
                colorHex = note2.colorHex,
                timestamp = tempTime,
                tags = note2.tags
            )
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
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Custom professional micro gem icon with subtle styling
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_gem),
                            contentDescription = "Premium OmniNote Gem",
                            tint = Color.Unspecified, // Keep original vibrant path colors intact
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(4.dp)
                        )
                        Column {
                            Text(
                                text = "OmniNote",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Smart Workspace",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Layout Actions Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle screen list/grid layout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showSortBottomSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Open Sort & Filter configurations",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Statistics Widget Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(
                        icon = Icons.Default.StickyNote2,
                        label = "Notes",
                        value = "$totalNotesCount",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                    StatBadge(
                        icon = Icons.Default.PushPin,
                        label = "Pinned",
                        value = "$pinnedNotesCount",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                    StatBadge(
                        icon = Icons.Default.Mic,
                        label = "Audio",
                        value = "$audioNotesCount",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Search input bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    shape = RoundedCornerShape(28.dp),
                    placeholder = {
                        Text(
                            "Search notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon indicator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNavigateToAddNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Add New Note",
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
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
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("All Notes", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    allTags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = if (isSelected) null else tag },
                            label = { Text(tag, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
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
                                onClick = { onNavigateToEditNote(note.id) },
                                onLongClick = { showQuickActionsForNote = note }
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
                            NoteCard(
                                note = note,
                                searchQuery = searchQuery,
                                onClick = { onNavigateToEditNote(note.id) },
                                onLongClick = { showQuickActionsForNote = note }
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

        AlertDialog(
            onDismissRequest = { showQuickActionsForNote = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Customize Note",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                        Column {
                            Text(
                                "Pin note to top",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = quickPinned,
                            onCheckedChange = { quickPinned = it }
                        )
                    }

                    // Move Up / Move Down buttons for Manual order swaps
                    val currentIndex = filteredAndSortedNotes.indexOfFirst { it.id == note.id }
                    if (currentIndex != -1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Manual Reordering",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        moveNoteUp(currentIndex, filteredAndSortedNotes)
                                        showQuickActionsForNote = null
                                    },
                                    enabled = currentIndex > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Move Up", fontSize = 12.sp)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        moveNoteDown(currentIndex, filteredAndSortedNotes)
                                        showQuickActionsForNote = null
                                    },
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
                        Text(
                            "Change Vibe accent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val quickColors = listOf(
                            null,
                            "#E6E3FF", // Electric Indigo Light
                            "#2D26A0", // Electric Indigo Dark
                            "#FFF0CC", // Warm Amber Light
                            "#9A5500", // Warm Amber Dark
                            "#FFD6EC", // Dusty Rose Light
                            "#8B2060"  // Dusty Rose Dark
                        )
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colorBg)
                                        .border(
                                            width = if (quickColor == colorHex) 2.5.dp else 1.dp,
                                            color = if (quickColor == colorHex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                        .clickable { quickColor = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (quickColor == colorHex) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (colorHex == "#E6E3FF" || colorHex == "#FFF0CC" || colorHex == "#FFD6EC" || colorHex == null) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.deleteNote(note)
                            showQuickActionsForNote = null
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete this note",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateNote(
                                id = note.id,
                                title = quickTitle,
                                content = note.content,
                                isPinned = quickPinned,
                                colorHex = quickColor,
                                timestamp = note.timestamp,
                                tags = note.tags
                            )
                            showQuickActionsForNote = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Vibe")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickActionsForNote = null }) {
                    Text("Close")
                }
            }
        )
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
    onLongClick: () -> Unit
) {
    val targetColor = note.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.surfaceVariant

    val backgroundColor by animateColorAsState(targetValue = targetColor)

    // Analyze attachments/metadata inside note's content to decorate card beautifully
    val hasVoice = note.content.contains("[voice:") || note.content.contains("[audio:")
    val hasFile = note.content.contains("[file:")
    val hasImage = note.content.contains("![Image](")
    val hasTable = note.content.contains("|") && note.content.contains("---|")
    val hasCode = note.content.contains("```")
    val hasChecklist = note.content.contains("- [ ]") || note.content.contains("- [x]")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        fontWeight = FontWeight.Bold,
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
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned Note Indicator Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(45f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Preview block
            if (note.content.isNotBlank()) {
                // Strips all markdown image tokens/voice links for clean preview
                val cleanContent = remember(note.content) {
                    note.content
                        .replace(Regex("""!\[.*?\]\(.*?\)"""), "[Image 🖼️]")
                        .replace(Regex("""\[voice.*?\]\(.*?\)"""), "[Voice clip 🎙️]")
                        .replace(Regex("""\[file.*?\]\(.*?\)"""), "[Attachment 📁]")
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
