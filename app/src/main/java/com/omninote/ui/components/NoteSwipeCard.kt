package com.omninote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omninote.R
import com.omninote.data.*
import kotlin.math.abs

fun swipeThreshold(width: Float, minimum: Float) = maxOf(minimum, width * 0.38f).coerceAtMost(width * 0.65f)
@OptIn(ExperimentalMaterial3Api::class)
private class SwipeStateHolder {
    var state: SwipeToDismissBoxState? = null
    var released = false
}

/** Physical right/left actions stay predictable in RTL; the note content keeps its own direction. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSwipeCard(note: NoteEntity, enabled: Boolean, onAction: (NoteLibraryAction) -> Unit,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val direction = LocalLayoutDirection.current
    val minimum = with(LocalDensity.current) { 72.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    var width by remember { mutableFloatStateOf(0f) }
    var requested by remember { mutableStateOf<NoteLibraryAction?>(null) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentNote by rememberUpdatedState(note)
    val callback by rememberUpdatedState(onAction)
    val holder = remember { SwipeStateHolder() }
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { swipeThreshold(it, minimum) },
        confirmValueChange = { target ->
            if (target != SwipeToDismissBoxValue.Settled && holder.released && currentEnabled && requested == null && width > 0f &&
                abs(runCatching { holder.state?.requireOffset() ?: 0f }.getOrDefault(0f)) >= swipeThreshold(width, minimum)) {
                holder.released = false
                requested = swipeAction(currentNote, target == SwipeToDismissBoxValue.StartToEnd)
            }
            false // Keep the card until the database commits; cancellation/failure always settles it.
        })
    holder.state = state
    LaunchedEffect(requested) {
        requested?.let { action ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            callback(action)
            requested = null
        }
    }
    val right = swipeAction(note, true)
    val left = swipeAction(note, false)
    val rightLabel = stringResource(actionLabel(right))
    val leftLabel = stringResource(actionLabel(left))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        SwipeToDismissBox(state, modifier = modifier.onSizeChanged { width = it.width.toFloat() }
            .pointerInput(enabled, note.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    holder.released = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) {
                            holder.released = event.type == PointerEventType.Release
                            break
                        }
                    }
                }
            }
            .testTag("note_swipe_${note.id}").semantics {
                if (enabled) customActions = listOf(
                    CustomAccessibilityAction(rightLabel) { callback(right); true },
                    CustomAccessibilityAction(leftLabel) { callback(left); true })
            }, enableDismissFromStartToEnd = enabled, enableDismissFromEndToStart = enabled,
            backgroundContent = {
                val rightward = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                val active = state.dismissDirection != SwipeToDismissBoxValue.Settled
                val color by animateColorAsState(if (!active) androidx.compose.ui.graphics.Color.Transparent else
                    if (rightward) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 16.dp),
                    contentAlignment = if (rightward) Alignment.CenterStart else Alignment.CenterEnd) {
                    if (active) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OmniIcon(actionIcon(if (rightward) right else left), tint = if (rightward)
                            MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer)
                        Text(if (rightward) rightLabel else leftLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }) {
            CompositionLocalProvider(LocalLayoutDirection provides direction) { content() }
        }
    }
}

fun actionLabel(action: NoteLibraryAction): Int = when (action) {
    NoteLibraryAction.ARCHIVE -> R.string.action_archive
    NoteLibraryAction.UNARCHIVE -> R.string.action_unarchive
    NoteLibraryAction.TRASH -> R.string.action_trash
    NoteLibraryAction.RESTORE -> R.string.action_restore
    NoteLibraryAction.DELETE -> R.string.action_delete_permanently
}
private fun actionIcon(action: NoteLibraryAction) = when (action) {
    NoteLibraryAction.ARCHIVE -> OmniIconType.ARCHIVE
    NoteLibraryAction.UNARCHIVE -> OmniIconType.UNARCHIVE
    NoteLibraryAction.RESTORE -> OmniIconType.RESTORE
    else -> OmniIconType.DELETE
}
