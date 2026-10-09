package com.omninote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omninote.R

@Composable
fun NoteLibraryHeader(
    title: String,
    summary: String,
    query: String,
    onQueryChange: (String) -> Unit,
    grid: Boolean,
    onLayoutChange: () -> Unit,
    onFilter: () -> Unit,
    onStats: () -> Unit
) {
    val compact = LocalConfiguration.current.screenHeightDp < 500
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = if (compact) 6.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                    if (!compact) Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = onStats, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Insights, contentDescription = stringResource(R.string.note_statistics))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text(stringResource(R.string.search_notes)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            if (compact) {
                FilledTonalIconButton(onClick = onLayoutChange) {
                    Icon(if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Outlined.GridView,
                        contentDescription = stringResource(if (grid) R.string.list_view else R.string.grid_view))
                }
                FilledTonalIconButton(onClick = onFilter) {
                    Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.sort_filter))
                }
            }
            }
            if (!compact) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLayoutChange, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Icon(
                        if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Outlined.GridView,
                        contentDescription = null, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (grid) R.string.list_view else R.string.grid_view))
                }
                OutlinedButton(onClick = onFilter, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sort_filter))
                }
            }
        }
    }
}
