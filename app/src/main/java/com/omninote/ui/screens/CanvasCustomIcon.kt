package com.omninote.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class CanvasIconType {
    BACK,
    DELETE,
    RESTORE,
    ARCHIVE,
    UNARCHIVE,
    SEARCH,
    CLOSE,
    TUNE,
    PIN,
    UNPIN,
    LOCK,
    UNLOCK,
    LABEL,
    MIC,
    WAVEFORM,
    CHECKBOX_ON,
    CHECKBOX_OFF,
    GRID_ON,
    GRID_OFF,
    CODE,
    IMAGE,
    ATTACH_FILE,
    MENU_BOOK,
    MORE_VERT,
    TICK,
    ADD,
    SETTINGS,
    FORMAT_BOLD,
    FORMAT_ITALIC,
    PALETTE,
    WAND,
    SHARE,
    PLUS,
    BULLET_LIST,
    INFO,
    ARROW_UP,
    ARROW_DOWN,
    EDIT,
    VISIBILITY,
    VISIBILITY_OFF,
    STOP,
    RECORD,
    HEADING_1,
    HEADING_2,
    HIGHLIGHT,
    QUOTE,
    ARROW_RIGHT,
    UNDO,
    REDO,
    INSIGHTS
}

/** A shared vector system: a caller's semantics must never remove the glyph's size. */
@Composable
fun CanvasCustomIcon(
    type: CanvasIconType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val vector = when (type) {
        CanvasIconType.BACK -> Icons.AutoMirrored.Outlined.ArrowBack
        CanvasIconType.DELETE -> Icons.Outlined.Delete
        CanvasIconType.RESTORE -> Icons.Outlined.Restore
        CanvasIconType.ARCHIVE -> Icons.Outlined.Archive
        CanvasIconType.UNARCHIVE -> Icons.Outlined.Unarchive
        CanvasIconType.SEARCH -> Icons.Outlined.Search
        CanvasIconType.CLOSE -> Icons.Outlined.Close
        CanvasIconType.TUNE -> Icons.Outlined.Tune
        CanvasIconType.PIN -> Icons.Outlined.PushPin
        CanvasIconType.UNPIN -> Icons.Filled.PushPin
        CanvasIconType.LOCK -> Icons.Outlined.Lock
        CanvasIconType.UNLOCK -> Icons.Outlined.LockOpen
        CanvasIconType.LABEL -> Icons.Outlined.Label
        CanvasIconType.MIC -> Icons.Outlined.Mic
        CanvasIconType.WAVEFORM -> Icons.Outlined.GraphicEq
        CanvasIconType.CHECKBOX_ON -> Icons.Outlined.CheckBox
        CanvasIconType.CHECKBOX_OFF -> Icons.Outlined.CheckBoxOutlineBlank
        CanvasIconType.GRID_ON -> Icons.Outlined.GridView
        CanvasIconType.GRID_OFF -> Icons.Outlined.ViewList
        CanvasIconType.CODE -> Icons.Outlined.Code
        CanvasIconType.IMAGE -> Icons.Outlined.Image
        CanvasIconType.ATTACH_FILE -> Icons.Outlined.AttachFile
        CanvasIconType.MENU_BOOK -> Icons.AutoMirrored.Outlined.MenuBook
        CanvasIconType.MORE_VERT -> Icons.Outlined.MoreVert
        CanvasIconType.TICK -> Icons.Outlined.Check
        CanvasIconType.ADD -> Icons.Outlined.Add
        CanvasIconType.SETTINGS -> Icons.Outlined.Settings
        CanvasIconType.FORMAT_BOLD -> Icons.Outlined.FormatBold
        CanvasIconType.FORMAT_ITALIC -> Icons.Outlined.FormatItalic
        CanvasIconType.PALETTE -> Icons.Outlined.Palette
        CanvasIconType.WAND -> Icons.Outlined.AutoAwesome
        CanvasIconType.SHARE -> Icons.Outlined.Share
        CanvasIconType.PLUS -> Icons.Outlined.Add
        CanvasIconType.BULLET_LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
        CanvasIconType.INFO -> Icons.Outlined.Info
        CanvasIconType.ARROW_UP -> Icons.Outlined.KeyboardArrowUp
        CanvasIconType.ARROW_DOWN -> Icons.Outlined.KeyboardArrowDown
        CanvasIconType.EDIT -> Icons.Outlined.Edit
        CanvasIconType.VISIBILITY -> Icons.Outlined.Visibility
        CanvasIconType.VISIBILITY_OFF -> Icons.Outlined.VisibilityOff
        CanvasIconType.STOP -> Icons.Outlined.Stop
        CanvasIconType.RECORD -> Icons.Outlined.FiberManualRecord
        CanvasIconType.HEADING_1 -> Icons.Outlined.Title
        CanvasIconType.HEADING_2 -> Icons.Outlined.TextFields
        CanvasIconType.HIGHLIGHT -> Icons.Outlined.Highlight
        CanvasIconType.QUOTE -> Icons.Outlined.FormatQuote
        CanvasIconType.ARROW_RIGHT -> Icons.AutoMirrored.Outlined.ArrowForward
        CanvasIconType.UNDO -> Icons.AutoMirrored.Outlined.Undo
        CanvasIconType.REDO -> Icons.AutoMirrored.Outlined.Redo
        CanvasIconType.INSIGHTS -> Icons.Outlined.Insights
    }
    Icon(vector, contentDescription = null, tint = tint, modifier = modifier.size(24.dp))
}
