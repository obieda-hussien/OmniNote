package com.omninote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omninote.R

/** One persistent icon row replaces separate search, layout and filter rows. */
@Composable
fun NoteLibraryHeader(
    title: String, summary: String, query: String, onQueryChange: (String) -> Unit,
    grid: Boolean, onLayoutChange: () -> Unit, onFilter: () -> Unit, onStats: () -> Unit,
    expanded: Boolean = true, onExpand: () -> Unit = {}, filtersActive: Boolean = false
) {
    var searching by remember { mutableStateOf(query.isNotEmpty()) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp)) {
            AnimatedVisibility(visible = expanded,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onStats) { Icon(Icons.Outlined.Insights, stringResource(R.string.note_statistics)) }
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (searching || query.isNotEmpty()) {
                    OutlinedTextField(value = query, onValueChange = onQueryChange, singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focus), shape = MaterialTheme.shapes.large,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text(stringResource(R.string.search_notes)) },
                        trailingIcon = { IconButton(onClick = { onQueryChange(""); searching = false; focusManager.clearFocus() }) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.clear_search))
                        } })
                } else {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, stringResource(R.string.search_notes)) }
                    if (!expanded) IconButton(onClick = onExpand) {
                        Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.show_library_navigation))
                    }
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = onLayoutChange) {
                    Icon(if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Outlined.GridView,
                        stringResource(if (grid) R.string.list_view else R.string.grid_view))
                }
                IconButton(onClick = onFilter) {
                    BadgedBox(badge = { if (filtersActive) Badge() }) {
                        Icon(Icons.Outlined.Tune, stringResource(R.string.sort_filter))
                    }
                }
            }
        }
    }
}
