package com.omni.noteplus.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class OmniIconType {
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
    INSIGHTS,
    NOTES,
    TABLE,
    WORDS,
    TASKS,
    HISTORY
}

/** A shared vector system: a caller's semantics must never remove the glyph's size. */
@Composable
fun OmniIcon(
    type: OmniIconType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(omniIconVector(type), contentDescription = null, tint = tint, modifier = modifier.size(24.dp))
}

fun omniIconVector(type: OmniIconType): ImageVector = when (type) {
    OmniIconType.BACK -> Icons.AutoMirrored.Rounded.ArrowBack
    OmniIconType.DELETE -> Icons.Rounded.Delete
    OmniIconType.RESTORE -> Icons.Rounded.Restore
    OmniIconType.ARCHIVE -> Icons.Rounded.Archive
    OmniIconType.UNARCHIVE -> Icons.Rounded.Unarchive
    OmniIconType.SEARCH -> Icons.Rounded.Search
    OmniIconType.CLOSE -> Icons.Rounded.Close
    OmniIconType.TUNE -> Icons.Rounded.Tune
    OmniIconType.PIN -> Icons.Rounded.PushPin
    OmniIconType.UNPIN -> Icons.Rounded.PushPin
    OmniIconType.LOCK -> Icons.Rounded.Lock
    OmniIconType.UNLOCK -> Icons.Rounded.LockOpen
    OmniIconType.LABEL -> Icons.Rounded.Label
    OmniIconType.MIC -> Icons.Rounded.Mic
    OmniIconType.WAVEFORM -> Icons.Rounded.GraphicEq
    OmniIconType.CHECKBOX_ON -> Icons.Rounded.CheckBox
    OmniIconType.CHECKBOX_OFF -> Icons.Rounded.CheckBoxOutlineBlank
    OmniIconType.GRID_ON -> Icons.Rounded.GridView
    OmniIconType.GRID_OFF -> Icons.Rounded.ViewList
    OmniIconType.CODE -> Icons.Rounded.Code
    OmniIconType.IMAGE -> Icons.Rounded.Image
    OmniIconType.ATTACH_FILE -> Icons.Rounded.AttachFile
    OmniIconType.MENU_BOOK -> Icons.AutoMirrored.Rounded.MenuBook
    OmniIconType.MORE_VERT -> Icons.Rounded.MoreVert
    OmniIconType.TICK -> Icons.Rounded.Check
    OmniIconType.ADD -> Icons.Rounded.Add
    OmniIconType.SETTINGS -> Icons.Rounded.Settings
    OmniIconType.FORMAT_BOLD -> Icons.Rounded.FormatBold
    OmniIconType.FORMAT_ITALIC -> Icons.Rounded.FormatItalic
    OmniIconType.PALETTE -> Icons.Rounded.Palette
    OmniIconType.WAND -> Icons.Rounded.AutoAwesome
    OmniIconType.SHARE -> Icons.Rounded.Share
    OmniIconType.PLUS -> Icons.Rounded.Add
    OmniIconType.BULLET_LIST -> Icons.AutoMirrored.Rounded.FormatListBulleted
    OmniIconType.INFO -> Icons.Rounded.Info
    OmniIconType.ARROW_UP -> Icons.Rounded.KeyboardArrowUp
    OmniIconType.ARROW_DOWN -> Icons.Rounded.KeyboardArrowDown
    OmniIconType.EDIT -> Icons.Rounded.Edit
    OmniIconType.VISIBILITY -> Icons.Rounded.Visibility
    OmniIconType.VISIBILITY_OFF -> Icons.Rounded.VisibilityOff
    OmniIconType.STOP -> Icons.Rounded.Stop
    OmniIconType.RECORD -> Icons.Rounded.FiberManualRecord
    OmniIconType.HEADING_1 -> Icons.Rounded.Title
    OmniIconType.HEADING_2 -> Icons.Rounded.TextFields
    OmniIconType.HIGHLIGHT -> Icons.Rounded.Highlight
    OmniIconType.QUOTE -> Icons.Rounded.FormatQuote
    OmniIconType.ARROW_RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
    OmniIconType.UNDO -> Icons.AutoMirrored.Rounded.Undo
    OmniIconType.REDO -> Icons.AutoMirrored.Rounded.Redo
    OmniIconType.INSIGHTS -> Icons.Rounded.BarChart
    OmniIconType.NOTES -> Icons.Rounded.Description
    OmniIconType.TABLE -> Icons.Rounded.TableChart
    OmniIconType.WORDS -> Icons.Rounded.TextFields
    OmniIconType.TASKS -> Icons.Rounded.TaskAlt
    OmniIconType.HISTORY -> Icons.Rounded.History
}

