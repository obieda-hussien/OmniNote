package com.omninote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omninote.R

@Composable
fun NoteCaptureBar(text: String, onTextChange: (String) -> Unit, saving: Boolean, onSave: () -> Unit, onNewNote: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), maxLines = 3,
                placeholder = { Text(stringResource(R.string.quick_note), style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent))
            if (text.isNotBlank() || saving) IconButton(onClick = onSave, enabled = text.isNotBlank() && !saving) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Check, stringResource(R.string.save_quick_note), tint = MaterialTheme.colorScheme.primary)
            }
            FilledTonalIconButton(onClick = onNewNote, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Add, stringResource(R.string.new_note))
            }
        }
    }
}
