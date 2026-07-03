package com.omninote.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun CanvasCustomIcon(
    type: CanvasIconType,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = LocalContentColor.current
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = kotlin.math.min(w, h)
        
        when (type) {
            CanvasIconType.BACK -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.5f),
                    end = Offset(w * 0.85f, h * 0.5f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.5f),
                    end = Offset(w * 0.45f, h * 0.25f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.5f),
                    end = Offset(w * 0.45f, h * 0.75f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.DELETE -> {
                // Lid
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.25f),
                    end = Offset(w * 0.85f, h * 0.25f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Handle
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.4f, h * 0.12f),
                    size = Size(w * 0.2f, h * 0.13f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Bin Body
                val bodyPath = Path().apply {
                    moveTo(w * 0.25f, h * 0.3f)
                    lineTo(w * 0.31f, h * 0.85f)
                    lineTo(w * 0.69f, h * 0.85f)
                    lineTo(w * 0.75f, h * 0.3f)
                    close()
                }
                drawPath(
                    path = bodyPath,
                    color = tint,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inside Lines
                drawLine(
                    color = tint,
                    start = Offset(w * 0.43f, h * 0.42f),
                    end = Offset(w * 0.43f, h * 0.73f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.57f, h * 0.42f),
                    end = Offset(w * 0.57f, h * 0.73f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.RESTORE -> {
                drawArc(
                    color = tint,
                    startAngle = 50f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Arrow tip
                val tipPath = Path().apply {
                    moveTo(w * 0.65f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.2f)
                    lineTo(w * 0.75f, h * 0.42f)
                }
                drawPath(
                    path = tipPath,
                    color = tint,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inside inner small circular path
                drawCircle(
                    color = tint.copy(alpha = 0.5f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.5f),
                    end = Offset(w * 0.5f, h * 0.35f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.5f),
                    end = Offset(w * 0.63f, h * 0.5f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.ARCHIVE -> {
                // Main box
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.25f),
                    size = Size(w * 0.7f, h * 0.6f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Drawer Lid
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.45f),
                    end = Offset(w * 0.85f, h * 0.45f),
                    strokeWidth = 2.dp.toPx()
                )
                // Drawer Handle
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.38f, h * 0.55f),
                    size = Size(w * 0.24f, h * 0.12f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            CanvasIconType.UNARCHIVE -> {
                // Drawer Box at bottom
                drawRoundRect(
                    color = tint.copy(alpha = 0.6f),
                    topLeft = Offset(w * 0.15f, h * 0.45f),
                    size = Size(w * 0.7f, h * 0.45f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Up arrow in top
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.12f),
                    end = Offset(w * 0.5f, h * 0.6f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.12f),
                    end = Offset(w * 0.3f, h * 0.32f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.12f),
                    end = Offset(w * 0.7f, h * 0.32f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.SEARCH -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.22f,
                    center = Offset(w * 0.42f, h * 0.42f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.57f, h * 0.57f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.CLOSE -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.25f, h * 0.25f),
                    end = Offset(w * 0.75f, h * 0.75f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.75f, h * 0.25f),
                    end = Offset(w * 0.25f, h * 0.75f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.TUNE -> {
                // Draw 3 slider sliders
                for (i in 0..2) {
                    val y = h * (0.25f + i * 0.25f)
                    drawLine(
                        color = tint.copy(alpha = 0.3f),
                        start = Offset(w * 0.15f, y),
                        end = Offset(w * 0.85f, y),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    val knobX = when (i) {
                        0 -> w * 0.4f
                        1 -> w * 0.7f
                        else -> w * 0.3f
                    }
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.15f, y),
                        end = Offset(knobX, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = tint,
                        radius = 2.8.dp.toPx(),
                        center = Offset(knobX, y)
                    )
                }
            }
            CanvasIconType.PIN -> {
                // Tilted pushpin
                val path = Path().apply {
                    // Head
                    moveTo(w * 0.45f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.55f)
                    // Neck
                    lineTo(w * 0.75f, h * 0.65f)
                    lineTo(w * 0.65f, h * 0.55f)
                    // Body
                    lineTo(w * 0.35f, h * 0.65f)
                    // Back head
                    lineTo(w * 0.35f, h * 0.45f)
                    close()
                }
                drawPath(path = path, color = tint)
                // Pin needle
                drawLine(
                    color = tint,
                    start = Offset(w * 0.45f, h * 0.55f),
                    end = Offset(w * 0.15f, h * 0.85f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.UNPIN -> {
                // Tilted pin with a strike line across it
                val path = Path().apply {
                    moveTo(w * 0.45f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.55f)
                    lineTo(w * 0.75f, h * 0.65f)
                    lineTo(w * 0.65f, h * 0.55f)
                    lineTo(w * 0.35f, h * 0.65f)
                    lineTo(w * 0.35f, h * 0.45f)
                    close()
                }
                drawPath(path = path, color = tint.copy(alpha = 0.5f))
                drawLine(
                    color = tint.copy(alpha = 0.5f),
                    start = Offset(w * 0.45f, h * 0.55f),
                    end = Offset(w * 0.15f, h * 0.85f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Strike line
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.15f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.LOCK -> {
                // Shackle
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.3f, h * 0.15f),
                    size = Size(w * 0.4f, h * 0.45f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Solid Body
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.45f),
                    size = Size(w * 0.6f, h * 0.43f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // Keyhole dot
                drawCircle(
                    color = surfaceColor,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.5f, h * 0.62f)
                )
            }
            CanvasIconType.UNLOCK -> {
                // Open Shackle
                val openShackle = Path().apply {
                    moveTo(w * 0.3f, h * 0.45f)
                    lineTo(w * 0.3f, h * 0.25f)
                    quadraticTo(w * 0.3f, h * 0.15f, w * 0.5f, h * 0.15f)
                    quadraticTo(w * 0.7f, h * 0.15f, w * 0.7f, h * 0.25f)
                    lineTo(w * 0.7f, h * 0.35f) // hanging open
                }
                drawPath(
                    path = openShackle,
                    color = tint,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Solid Body
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.45f),
                    size = Size(w * 0.6f, h * 0.43f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // Keyhole dot
                drawCircle(
                    color = surfaceColor,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.5f, h * 0.62f)
                )
            }
            CanvasIconType.LABEL -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.5f)
                    lineTo(w * 0.45f, h * 0.2f)
                    lineTo(w * 0.8f, h * 0.2f)
                    lineTo(w * 0.8f, h * 0.8f)
                    lineTo(w * 0.45f, h * 0.8f)
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
                )
                // eyelet
                drawCircle(
                    color = tint,
                    radius = 1.8.dp.toPx(),
                    center = Offset(w * 0.4f, h * 0.5f)
                )
            }
            CanvasIconType.MIC -> {
                // Mic Capsule
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.35f, h * 0.15f),
                    size = Size(w * 0.3f, h * 0.42f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                // Horizontal Grids inside capsule
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.35f),
                    end = Offset(w * 0.65f, h * 0.35f),
                    strokeWidth = 1.8.dp.toPx()
                )
                // Outer Cradle
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.28f),
                    size = Size(w * 0.52f, h * 0.44f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Stand support
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.72f),
                    end = Offset(w * 0.5f, h * 0.88f),
                    strokeWidth = 2.2.dp.toPx()
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.33f, h * 0.88f),
                    end = Offset(w * 0.67f, h * 0.88f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.WAVEFORM -> {
                val barCount = 5
                val barWidth = 2.2.dp.toPx()
                val spacing = 2.5.dp.toPx()
                val totalWidth = barCount * barWidth + (barCount - 1) * spacing
                val startX = (w - totalWidth) / 2f
                
                val heights = listOf(0.4f, 0.75f, 0.5f, 0.85f, 0.35f)
                for (i in heights.indices) {
                    val x = startX + i * (barWidth + spacing)
                    val barH = h * heights[i]
                    val y = (h - barH) / 2f
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
            }
            CanvasIconType.CHECKBOX_ON -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // White tick mark
                val tickPath = Path().apply {
                    moveTo(w * 0.32f, h * 0.5f)
                    lineTo(w * 0.46f, h * 0.64f)
                    lineTo(w * 0.68f, h * 0.32f)
                }
                drawPath(
                    path = tickPath,
                    color = surfaceColor,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            CanvasIconType.CHECKBOX_OFF -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            CanvasIconType.GRID_ON -> {
                val size = w * 0.33f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.55f, h * 0.12f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.55f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.55f, h * 0.55f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
            CanvasIconType.GRID_OFF -> {
                // List View style: 3 horizontal list rows
                for (i in 0..2) {
                    val y = h * (0.24f + i * 0.26f)
                    // bullet
                    drawCircle(
                        color = tint,
                        radius = 1.8.dp.toPx(),
                        center = Offset(w * 0.22f, y)
                    )
                    // text lines
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.38f, y),
                        end = Offset(w * 0.85f, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            CanvasIconType.CODE -> {
                // Left chevron `<`
                val leftPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.3f)
                    lineTo(w * 0.15f, h * 0.5f)
                    lineTo(w * 0.35f, h * 0.7f)
                }
                drawPath(path = leftPath, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                
                // Right chevron `>`
                val rightPath = Path().apply {
                    moveTo(w * 0.65f, h * 0.3f)
                    lineTo(w * 0.85f, h * 0.5f)
                    lineTo(w * 0.65f, h * 0.7f)
                }
                drawPath(path = rightPath, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                
                // Slash `/`
                drawLine(
                    color = tint,
                    start = Offset(w * 0.58f, h * 0.22f),
                    end = Offset(w * 0.42f, h * 0.78f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.IMAGE -> {
                // Photo Frame
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.18f),
                    size = Size(w * 0.7f, h * 0.64f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Mountains
                val mountPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.75f)
                    lineTo(w * 0.45f, h * 0.42f)
                    lineTo(w * 0.62f, h * 0.62f)
                    lineTo(w * 0.72f, h * 0.48f)
                    lineTo(w * 0.8f, h * 0.75f)
                }
                drawPath(path = mountPath, color = tint, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                // Sun
                drawCircle(
                    color = tint,
                    radius = 2.2.dp.toPx(),
                    center = Offset(w * 0.65f, h * 0.33f)
                )
            }
            CanvasIconType.ATTACH_FILE -> {
                // Paperclip
                val path = Path().apply {
                    moveTo(w * 0.75f, h * 0.35f)
                    lineTo(w * 0.42f, h * 0.68f)
                    quadraticTo(w * 0.32f, h * 0.78f, w * 0.25f, h * 0.68f)
                    quadraticTo(w * 0.18f, h * 0.58f, w * 0.28f, h * 0.45f)
                    lineTo(w * 0.58f, h * 0.18f)
                    quadraticTo(w * 0.68f, h * 0.08f, w * 0.78f, h * 0.18f)
                    quadraticTo(w * 0.88f, h * 0.28f, w * 0.78f, h * 0.41f)
                    lineTo(w * 0.48f, h * 0.68f)
                    quadraticTo(w * 0.42f, h * 0.73f, w * 0.36f, h * 0.65f)
                    quadraticTo(w * 0.31f, h * 0.58f, w * 0.38f, h * 0.52f)
                    lineTo(w * 0.68f, h * 0.25f)
                }
                drawPath(path = path, color = tint, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
            }
            CanvasIconType.MENU_BOOK -> {
                // Open Book pages
                val pathLeft = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    quadraticTo(w * 0.35f, h * 0.65f, w * 0.15f, h * 0.72f)
                    lineTo(w * 0.15f, h * 0.22f)
                    quadraticTo(w * 0.35f, h * 0.15f, w * 0.5f, h * 0.32f)
                    close()
                }
                val pathRight = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    quadraticTo(w * 0.65f, h * 0.65f, w * 0.85f, h * 0.72f)
                    lineTo(w * 0.85f, h * 0.22f)
                    quadraticTo(w * 0.65f, h * 0.15f, w * 0.5f, h * 0.32f)
                    close()
                }
                drawPath(path = pathLeft, color = tint, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                drawPath(path = pathRight, color = tint, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.32f),
                    end = Offset(w * 0.5f, h * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            CanvasIconType.MORE_VERT -> {
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.25f))
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.75f))
            }
            CanvasIconType.TICK -> {
                val tickPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.53f)
                    lineTo(w * 0.44f, h * 0.73f)
                    lineTo(w * 0.82f, h * 0.3f)
                }
                drawPath(
                    path = tickPath,
                    color = tint,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            CanvasIconType.ADD, CanvasIconType.PLUS -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.22f, h * 0.5f),
                    end = Offset(w * 0.78f, h * 0.5f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.22f),
                    end = Offset(w * 0.5f, h * 0.78f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.SETTINGS -> {
                // Gear wheel
                drawCircle(
                    color = tint,
                    radius = w * 0.16f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                // Draw 8 radial gear teeth
                for (angle in 0..315 step 45) {
                    val angleRad = Math.toRadians(angle.toDouble())
                    val cos = Math.cos(angleRad).toFloat()
                    val sin = Math.sin(angleRad).toFloat()
                    val start = Offset(w * 0.5f + cos * w * 0.18f, h * 0.5f + sin * h * 0.18f)
                    val end = Offset(w * 0.5f + cos * w * 0.32f, h * 0.5f + sin * h * 0.32f)
                    drawLine(
                        color = tint,
                        start = start,
                        end = end,
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            CanvasIconType.FORMAT_BOLD -> {
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.22f)
                    lineTo(w * 0.52f, h * 0.22f)
                    quadraticTo(w * 0.72f, h * 0.22f, w * 0.72f, h * 0.45f)
                    quadraticTo(w * 0.72f, h * 0.52f, w * 0.6f, h * 0.52f)
                    quadraticTo(w * 0.76f, h * 0.52f, w * 0.76f, h * 0.78f)
                    quadraticTo(w * 0.76f, h * 0.78f, w * 0.48f, h * 0.78f)
                    lineTo(w * 0.28f, h * 0.78f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.38f, h * 0.22f),
                    end = Offset(w * 0.38f, h * 0.78f),
                    strokeWidth = 3.5.dp.toPx()
                )
            }
            CanvasIconType.FORMAT_ITALIC -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.22f),
                    end = Offset(w * 0.65f, h * 0.22f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.56f, h * 0.22f),
                    end = Offset(w * 0.44f, h * 0.78f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.78f),
                    end = Offset(w * 0.65f, h * 0.78f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.PALETTE -> {
                // Artist Palette
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    quadraticTo(w * 0.85f, h * 0.15f, w * 0.85f, h * 0.52f)
                    quadraticTo(w * 0.85f, h * 0.85f, w * 0.5f, h * 0.85f)
                    quadraticTo(w * 0.32f, h * 0.85f, w * 0.22f, h * 0.72f)
                    quadraticTo(w * 0.12f, h * 0.6f, w * 0.2f, h * 0.42f)
                    quadraticTo(w * 0.25f, h * 0.3f, w * 0.38f, h * 0.3f)
                    quadraticTo(w * 0.38f, h * 0.15f, w * 0.5f, h * 0.15f)
                }
                drawPath(path = path, color = tint, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                // Paint dots
                drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.42f, h * 0.45f))
                drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.58f, h * 0.35f))
                drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.7f, h * 0.52f))
            }
            CanvasIconType.WAND -> {
                // Magic Wand
                drawLine(
                    color = tint,
                    start = Offset(w * 0.2f, h * 0.8f),
                    end = Offset(w * 0.68f, h * 0.32f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Highlight wand head
                drawCircle(
                    color = tint,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.68f, h * 0.32f)
                )
                // Sparkles (Cross stars)
                val stars = listOf(
                    Offset(w * 0.75f, h * 0.15f),
                    Offset(w * 0.82f, h * 0.32f),
                    Offset(w * 0.52f, h * 0.18f)
                )
                for (s in stars) {
                    drawLine(color = tint, start = Offset(s.x - 2.dp.toPx(), s.y), end = Offset(s.x + 2.dp.toPx(), s.y), strokeWidth = 1.dp.toPx())
                    drawLine(color = tint, start = Offset(s.x, s.y - 2.dp.toPx()), end = Offset(s.x, s.y + 2.dp.toPx()), strokeWidth = 1.dp.toPx())
                }
            }
            CanvasIconType.SHARE -> {
                // Three nodes and lines
                val n1 = Offset(w * 0.75f, h * 0.22f)
                val n2 = Offset(w * 0.28f, h * 0.5f)
                val n3 = Offset(w * 0.75f, h * 0.78f)
                
                drawLine(color = tint, start = n2, end = n1, strokeWidth = 1.8.dp.toPx())
                drawLine(color = tint, start = n2, end = n3, strokeWidth = 1.8.dp.toPx())
                
                drawCircle(color = tint, radius = 3.dp.toPx(), center = n1)
                drawCircle(color = tint, radius = 3.dp.toPx(), center = n2)
                drawCircle(color = tint, radius = 3.dp.toPx(), center = n3)
            }
            CanvasIconType.BULLET_LIST -> {
                for (i in 0..2) {
                    val y = h * (0.25f + i * 0.25f)
                    drawCircle(color = tint, radius = 1.8.dp.toPx(), center = Offset(w * 0.22f, y))
                    drawLine(color = tint, start = Offset(w * 0.38f, y), end = Offset(w * 0.85f, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            CanvasIconType.INFO -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.38f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Letter 'i'
                drawCircle(
                    color = tint,
                    radius = 1.2.dp.toPx(),
                    center = Offset(w * 0.5f, h * 0.35f)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.46f),
                    end = Offset(w * 0.5f, h * 0.68f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.ARROW_UP -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.15f),
                    end = Offset(w * 0.5f, h * 0.85f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.15f),
                    end = Offset(w * 0.25f, h * 0.4f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.15f),
                    end = Offset(w * 0.75f, h * 0.4f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.ARROW_DOWN -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.15f),
                    end = Offset(w * 0.5f, h * 0.85f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.85f),
                    end = Offset(w * 0.25f, h * 0.6f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.85f),
                    end = Offset(w * 0.75f, h * 0.6f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.EDIT -> {
                // Pencil at 45 degrees
                drawLine(
                    color = tint,
                    start = Offset(w * 0.25f, h * 0.75f),
                    end = Offset(w * 0.7f, h * 0.3f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.85f),
                    end = Offset(w * 0.8f, h * 0.4f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Eraser/top cap
                drawLine(
                    color = tint,
                    start = Offset(w * 0.7f, h * 0.3f),
                    end = Offset(w * 0.8f, h * 0.4f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Pencil tip
                val tipPath = Path().apply {
                    moveTo(w * 0.25f, h * 0.75f)
                    lineTo(w * 0.15f, h * 0.85f)
                    lineTo(w * 0.35f, h * 0.85f)
                    close()
                }
                drawPath(path = tipPath, color = tint)
            }
            CanvasIconType.VISIBILITY -> {
                // Eye lids
                val eyeTop = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.15f, w * 0.85f, h * 0.5f)
                }
                val eyeBottom = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.85f, w * 0.85f, h * 0.5f)
                }
                drawPath(path = eyeTop, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path = eyeBottom, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                // Iris
                drawCircle(
                    color = tint,
                    radius = w * 0.16f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Pupil
                drawCircle(
                    color = tint,
                    radius = w * 0.07f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            CanvasIconType.VISIBILITY_OFF -> {
                // Eye lids
                val eyeTop = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.15f, w * 0.85f, h * 0.5f)
                }
                val eyeBottom = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.85f, w * 0.85f, h * 0.5f)
                }
                drawPath(path = eyeTop, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path = eyeBottom, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                // Iris
                drawCircle(
                    color = tint,
                    radius = w * 0.16f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Pupil
                drawCircle(
                    color = tint,
                    radius = w * 0.07f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                // Diagonal slash
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.15f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.STOP -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.25f, h * 0.25f),
                    size = Size(w * 0.5f, h * 0.5f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
                )
            }
            CanvasIconType.RECORD -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.35f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            CanvasIconType.HEADING_1 -> {
                val strokeW = 2.dp.toPx()
                // Left vertical bar of H
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.2f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Right vertical bar of H
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.2f), end = Offset(w * 0.5f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Horizontal bar of H
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.5f), end = Offset(w * 0.5f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Number 1
                drawLine(color = tint, start = Offset(w * 0.72f, h * 0.3f), end = Offset(w * 0.82f, h * 0.2f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.82f, h * 0.2f), end = Offset(w * 0.82f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.7f, h * 0.8f), end = Offset(w * 0.94f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            CanvasIconType.HEADING_2 -> {
                val strokeW = 2.dp.toPx()
                // Left vertical bar of H
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.2f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Right vertical bar of H
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.2f), end = Offset(w * 0.5f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Horizontal bar of H
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.5f), end = Offset(w * 0.5f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Number 2
                val twoPath = Path().apply {
                    moveTo(w * 0.7f, h * 0.35f)
                    quadraticTo(w * 0.8f, h * 0.2f, w * 0.9f, h * 0.35f)
                    quadraticTo(w * 0.85f, h * 0.55f, w * 0.7f, h * 0.8f)
                    lineTo(w * 0.92f, h * 0.8f)
                }
                drawPath(path = twoPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            CanvasIconType.HIGHLIGHT -> {
                val strokeW = 2.dp.toPx()
                // Draw a marker highlighter tip & body
                val path = Path().apply {
                    moveTo(w * 0.3f, h * 0.8f)
                    lineTo(w * 0.2f, h * 0.7f)
                    lineTo(w * 0.6f, h * 0.3f)
                    lineTo(w * 0.7f, h * 0.4f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = strokeW, join = StrokeJoin.Round))
                // Draw tip
                val tip = Path().apply {
                    moveTo(w * 0.6f, h * 0.3f)
                    lineTo(w * 0.7f, h * 0.4f)
                    lineTo(w * 0.85f, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.15f)
                    close()
                }
                drawPath(path = tip, color = tint)
            }
            CanvasIconType.QUOTE -> {
                // Two double quote marks
                val strokeW = 2.dp.toPx()
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.3f),
                    size = Size(w * 0.2f, h * 0.25f),
                    style = Stroke(width = strokeW)
                )
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.42f), end = Offset(w * 0.12f, h * 0.65f), strokeWidth = strokeW, cap = StrokeCap.Round)

                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.55f, h * 0.3f),
                    size = Size(w * 0.2f, h * 0.25f),
                    style = Stroke(width = strokeW)
                )
                drawLine(color = tint, start = Offset(w * 0.55f, h * 0.42f), end = Offset(w * 0.47f, h * 0.65f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            CanvasIconType.ARROW_RIGHT -> {
                val strokeW = 2.5.dp.toPx()
                drawLine(
                    color = tint,
                    start = Offset(w * 0.2f, h * 0.5f),
                    end = Offset(w * 0.8f, h * 0.5f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.55f, h * 0.25f),
                    end = Offset(w * 0.8f, h * 0.5f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.55f, h * 0.75f),
                    end = Offset(w * 0.8f, h * 0.5f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            CanvasIconType.UNDO -> {
                val strokeW = 2.dp.toPx()
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.35f),
                    size = Size(w * 0.6f, h * 0.45f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                val arrowPath = Path().apply {
                    moveTo(w * 0.4f, h * 0.2f)
                    lineTo(w * 0.2f, h * 0.35f)
                    lineTo(w * 0.4f, h * 0.5f)
                }
                drawPath(path = arrowPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            CanvasIconType.REDO -> {
                val strokeW = 2.dp.toPx()
                drawArc(
                    color = tint,
                    startAngle = 90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.35f),
                    size = Size(w * 0.6f, h * 0.45f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                val arrowPath = Path().apply {
                    moveTo(w * 0.6f, h * 0.2f)
                    lineTo(w * 0.8f, h * 0.35f)
                    lineTo(w * 0.6f, h * 0.5f)
                }
                drawPath(path = arrowPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            CanvasIconType.INSIGHTS -> {
                val strokeW = 2.dp.toPx()
                // Simple Bar chart
                drawLine(
                    color = tint,
                    start = Offset(w * 0.2f, h * 0.8f),
                    end = Offset(w * 0.2f, h * 0.5f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.8f),
                    end = Offset(w * 0.5f, h * 0.2f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.8f, h * 0.8f),
                    end = Offset(w * 0.8f, h * 0.4f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
