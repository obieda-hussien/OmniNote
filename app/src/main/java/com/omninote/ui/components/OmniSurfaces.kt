package com.omninote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Custom note colours are accents; reading surfaces retain theme contrast. */
@Composable
fun noteSurface(hex: String?, editor: Boolean = false): Color {
    val base = if (editor) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    val accent = hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    return if (accent == null) base else lerp(base, accent, if (editor) 0.05f else 0.09f)
}

/** One scroll owner keeps every tool reachable on small screens and with the keyboard open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.40f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
fun OmniConfirmDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        icon = icon,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    )
}
