package com.omninote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import android.net.Uri
import android.media.MediaPlayer
import android.content.Intent

// Define visual inline tokens
private sealed class MarkdownToken {
    data class Text(val content: String) : MarkdownToken()
    data class Bold(val content: String) : MarkdownToken()
    data class Italic(val content: String) : MarkdownToken()
    data class Highlight(val content: String) : MarkdownToken()
    data class InlineCode(val content: String) : MarkdownToken()
    data class CustomColor(val colorHex: String, val content: String) : MarkdownToken()
    data class CustomBg(val bgHex: String, val content: String) : MarkdownToken()
}

/**
 * Appends text while detecting and annotating links, emails, and phone numbers.
 */
fun appendAnnotatedPlainSlice(
    builder: AnnotatedString.Builder,
    text: String,
    primaryColor: Color
) {
    val urlRegex = Regex("""\b(https?://[^\s()<>]+(?:\([\w\d]+\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))""")
    val wwwRegex = Regex("""\b(www\.[^\s()<>]+(?:\([\w\d]+\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))""")
    val emailRegex = Regex("""\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b""")
    val phoneRegex = Regex("""\+?[0-9][0-9\- ]{6,14}[0-9]""")

    val matches = mutableListOf<Triple<Int, Int, Pair<String, String>>>() // start, end, <tag, value>

    urlRegex.findAll(text).forEach {
        matches.add(Triple(it.range.first, it.range.last + 1, "URL" to it.value))
    }
    wwwRegex.findAll(text).forEach {
        matches.add(Triple(it.range.first, it.range.last + 1, "URL" to it.value))
    }
    emailRegex.findAll(text).forEach {
        matches.add(Triple(it.range.first, it.range.last + 1, "EMAIL" to it.value))
    }
    phoneRegex.findAll(text).forEach {
        val start = it.range.first
        val end = it.range.last + 1
        val phoneStr = it.value.trim()
        val digitCount = phoneStr.count { c -> c.isDigit() }
        if (digitCount >= 8) {
            matches.add(Triple(start, end, "PHONE" to phoneStr))
        }
    }

    val sortedMatches = matches.sortedBy { it.first }
    var lastIndex = 0
    
    for (match in sortedMatches) {
        val start = match.first
        val end = match.second
        val (tag, value) = match.third

        if (start < lastIndex) continue // Overlap safety

        if (start > lastIndex) {
            builder.append(text.substring(lastIndex, start))
        }

        builder.pushStringAnnotation(tag = tag, annotation = value)
        builder.pushStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
        builder.append(text.substring(start, end))
        builder.pop()
        builder.pop()

        lastIndex = end
    }

    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }
}

/**
 * Parses markdown inline styles recursively/flat-wise to make AnnotatedString robust.
 */
fun parseInlineStyles(
    text: String,
    primaryColor: Color,
    onSurfaceVariant: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    
    // We will use a reliable sequential checking algorithm.
    // Order of priority: 
    // 1. [color:#HEX](text) or [bg:#HEX](text)
    // 2. Standard Markdown Link [Label](url)
    // 3. Inline Code `code`
    // 4. Highlight ==text==
    // 5. Bold **text**
    // 6. Italic *text* or _text_
    
    var remainingText = text
    while (remainingText.isNotEmpty()) {
        // Parse Color & Background tags with balanced parenthesis counting
        val colorPrefixMatch = Regex("""^\[color:(#[0-9a-fA-F]{6,8})\]\(""").find(remainingText)
        val bgPrefixMatch = Regex("""^\[bg:(#[0-9a-fA-F]{6,8})\]\(""").find(remainingText)
        
        val linkRegex = Regex("""^\[([^\]]+)\]\(([^)]+)\)""")
        val codeRegex = Regex("""^`(.*?)`""")
        val highlightRegex = Regex("""^==(.*?)==""")
        val boldRegex = Regex("""^\*\*(.*?)\*\*""")
        val italicRegex = Regex("""^\*(.*?)\*""")
        val italicUnderlineRegex = Regex("""^_(.*?)_""")

        val linkMatch = linkRegex.find(remainingText)
        val codeMatch = codeRegex.find(remainingText)
        val highlightMatch = highlightRegex.find(remainingText)
        val boldMatch = boldRegex.find(remainingText)
        val italicMatch = italicRegex.find(remainingText) ?: italicUnderlineRegex.find(remainingText)

        when {
            colorPrefixMatch != null -> {
                val hex = colorPrefixMatch.groupValues[1]
                val prefix = colorPrefixMatch.value
                var depth = 1
                var endIndex = -1
                for (i in prefix.length until remainingText.length) {
                    if (remainingText[i] == '(') depth++
                    else if (remainingText[i] == ')') {
                        depth--
                        if (depth == 0) {
                            endIndex = i
                            break
                        }
                    }
                }
                
                if (endIndex != -1) {
                    val insideValue = remainingText.substring(prefix.length, endIndex)
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { primaryColor }
                    
                    builder.pushStyle(SpanStyle(color = color))
                    builder.append(parseInlineStyles(insideValue, primaryColor, onSurfaceVariant))
                    builder.pop()
                    
                    remainingText = remainingText.substring(endIndex + 1)
                } else {
                    // Fallback to treat as plain text if no matching closing bracket was found
                    builder.append(remainingText.first())
                    remainingText = remainingText.drop(1)
                }
            }
            bgPrefixMatch != null -> {
                val hex = bgPrefixMatch.groupValues[1]
                val prefix = bgPrefixMatch.value
                var depth = 1
                var endIndex = -1
                for (i in prefix.length until remainingText.length) {
                    if (remainingText[i] == '(') depth++
                    else if (remainingText[i] == ')') {
                        depth--
                        if (depth == 0) {
                            endIndex = i
                            break
                        }
                    }
                }
                
                if (endIndex != -1) {
                    val insideValue = remainingText.substring(prefix.length, endIndex)
                    val bgColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { primaryColor.copy(alpha = 0.2f) }
                    
                    builder.pushStyle(SpanStyle(background = bgColor))
                    builder.append(parseInlineStyles(insideValue, primaryColor, onSurfaceVariant))
                    builder.pop()
                    
                    remainingText = remainingText.substring(endIndex + 1)
                } else {
                    // Fallback to treat as plain text if no matching closing bracket was found
                    builder.append(remainingText.first())
                    remainingText = remainingText.drop(1)
                }
            }
            linkMatch != null -> {
                val label = linkMatch.groupValues[1]
                val url = linkMatch.groupValues[2]
                
                builder.pushStringAnnotation(tag = "URL", annotation = url)
                builder.pushStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                builder.append(parseInlineStyles(label, primaryColor, onSurfaceVariant))
                builder.pop()
                builder.pop()
                
                remainingText = remainingText.substring(linkMatch.value.length)
            }
            codeMatch != null -> {
                val insideValue = codeMatch.groupValues[1]
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = onSurfaceVariant.copy(alpha = 0.1f),
                        color = primaryColor
                    )
                )
                builder.append(insideValue)
                builder.pop()
                remainingText = remainingText.substring(codeMatch.value.length)
            }
            highlightMatch != null -> {
                val insideValue = highlightMatch.groupValues[1]
                builder.pushStyle(SpanStyle(background = Color(0xFFFBC02D).copy(alpha = 0.4f), fontWeight = FontWeight.Medium))
                builder.append(parseInlineStyles(insideValue, primaryColor, onSurfaceVariant))
                builder.pop()
                remainingText = remainingText.substring(highlightMatch.value.length)
            }
            boldMatch != null -> {
                val insideValue = boldMatch.groupValues[1]
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(parseInlineStyles(insideValue, primaryColor, onSurfaceVariant))
                builder.pop()
                remainingText = remainingText.substring(boldMatch.value.length)
            }
            italicMatch != null -> {
                val insideValue = italicMatch.groupValues[1]
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(parseInlineStyles(insideValue, primaryColor, onSurfaceVariant))
                builder.pop()
                remainingText = remainingText.substring(italicMatch.value.length)
            }
            else -> {
                // Find next token boundary
                val nextSpecialIndex = remainingText.indexOfAny(listOf("[color:", "[bg:", "`", "==", "**", "*", "_", "["), 1)
                val plainTextSlice = if (nextSpecialIndex == -1) {
                    remainingText
                } else {
                    remainingText.substring(0, nextSpecialIndex)
                }
                appendAnnotatedPlainSlice(builder, plainTextSlice, primaryColor)
                remainingText = remainingText.substring(plainTextSlice.length)
            }
        }
    }
    return builder.toAnnotatedString()
}

/**
 * Renders complete notes with rich Markdown layout structure.
 */
@Composable
fun MarkdownContent(
    modifier: Modifier = Modifier,
    rawText: String,
    onCheckedChange: ((lineIndex: Int, isChecked: Boolean) -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val lines = remember(rawText) { rawText.split("\n") }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var index = 0
            while (index < lines.size) {
                val line = lines[index]
                val trimmedLine = line.trim()

                // 1. Code block handling:
                if (trimmedLine.startsWith("```")) {
                    val language = trimmedLine.substring(3).trim().ifEmpty { "code" }
                    val codeBuilder = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        codeBuilder.append(lines[index]).append("\n")
                        index++
                    }
                    CodeBlockLayout(code = codeBuilder.toString(), language = language)
                    if (index < lines.size) index++ // Consume ending ```
                    continue
                }

                // 2. Table handling:
                if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|") && index + 1 < lines.size && lines[index + 1].trim().startsWith("|")) {
                    val tableLines = mutableListOf<String>()
                    while (index < lines.size && lines[index].trim().startsWith("|") && lines[index].trim().endsWith("|")) {
                        tableLines.add(lines[index].trim())
                        index++
                    }
                    TableLayout(tableLines = tableLines, primaryColor = primaryColor, onSurfaceVariant = onSurfaceVariant)
                    continue
                }

                // 3. Inline Images handling: ![desc](uriString)
                val imageRegex = Regex("""^!\[(.*?)\]\((.*?)\)$""")
                val imageMatch = imageRegex.matchEntire(trimmedLine)
                if (imageMatch != null) {
                    val desc = imageMatch.groupValues[1]
                    val uriStr = imageMatch.groupValues[2]
                    ImageLayout(uriString = uriStr, description = desc)
                    index++
                    continue
                }

                // 4. Inline Voice Note / Audio handling: [audio:Label](uriString) or [voice:Label](uriString)
                val audioRegex = Regex("""^\[(audio|voice)(?::(.*?))?\]\((.*?)\)$""")
                val audioMatch = audioRegex.matchEntire(trimmedLine)
                if (audioMatch != null) {
                    val label = audioMatch.groupValues[2].ifEmpty { "Voice Recording" }
                    val uriStr = audioMatch.groupValues[3]
                    AudioPlayerLayout(uriString = uriStr, label = label)
                    index++
                    continue
                }

                // 5. Generic File attachment: [file:Filename](uriString)
                val fileRegex = Regex("""^\[file(?::(.*?))?\]\((.*?)\)$""")
                val fileMatch = fileRegex.matchEntire(trimmedLine)
                if (fileMatch != null) {
                    val filename = fileMatch.groupValues[1].ifEmpty { "File Attachment" }
                    val uriStr = fileMatch.groupValues[2]
                    FileAttachmentLayout(uriString = uriStr, filename = filename)
                    index++
                    continue
                }

                // 6. Headers
                if (trimmedLine.startsWith("# ")) {
                    val headerText = trimmedLine.substring(2)
                    InteractiveText(
                        annotatedString = parseInlineStyles(headerText, primaryColor, onSurfaceVariant),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), thickness = 2.dp)
                    index++
                    continue
                }

                if (trimmedLine.startsWith("## ")) {
                    val headerText = trimmedLine.substring(3)
                    InteractiveText(
                        annotatedString = parseInlineStyles(headerText, primaryColor, onSurfaceVariant),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                    index++
                    continue
                }

                if (trimmedLine.startsWith("### ")) {
                    val headerText = trimmedLine.substring(4)
                    InteractiveText(
                        annotatedString = parseInlineStyles(headerText, primaryColor, onSurfaceVariant),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    index++
                    continue
                }

                // 7. Checklist Items: - [ ] or - [x]
                val isUncheckedBox = trimmedLine.startsWith("- [ ]") || trimmedLine.startsWith("* [ ]")
                val isCheckedBox = trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("* [x]") || trimmedLine.startsWith("- [X]") || trimmedLine.startsWith("* [X]")
                if (isUncheckedBox || isCheckedBox) {
                    val checked = isCheckedBox
                    val checkboxTxt = trimmedLine.substring(5).trim()
                    val lineIndex = index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onCheckedChange != null) {
                                onCheckedChange?.invoke(lineIndex, !checked)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Checklist Toggle",
                            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineStyles(checkboxTxt, primaryColor, onSurfaceVariant),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                                textDirection = TextDirection.ContentOrLtr
                            ),
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    index++
                    continue
                }

                // 8. Bullet lists
                if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                    val bulletTxt = trimmedLine.substring(2)
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 14.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        InteractiveText(
                            annotatedString = parseInlineStyles(bulletTxt, primaryColor, onSurfaceVariant),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textDirection = TextDirection.ContentOrLtr
                            )
                        )
                    }
                    index++
                    continue
                }

                // 9. Blockquotes
                if (trimmedLine.startsWith("> ")) {
                    val quoteTxt = trimmedLine.substring(2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp)
                            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .drawBehindBorderLeft(color = MaterialTheme.colorScheme.primary, width = 4.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                            InteractiveText(
                                annotatedString = parseInlineStyles(quoteTxt, primaryColor, onSurfaceVariant),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    textDirection = TextDirection.ContentOrLtr
                                )
                            )
                        }
                    }
                    index++
                    continue
                }

                // 10. Horizontal Rules
                if (trimmedLine == "---" || trimmedLine == "***" || trimmedLine == "___") {
                    Divider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    index++
                    continue
                }

                // 11. Default regular line
                if (line.isNotEmpty()) {
                    InteractiveText(
                        annotatedString = parseInlineStyles(line, primaryColor, onSurfaceVariant),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.ContentOrLtr
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                index++
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageLayout(uriString: String, description: String) {
    var showDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { showDialog = true },
                onLongClick = { showOptionsDialog = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column {
            coil.compose.AsyncImage(
                model = uriString,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (showDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDialog = false }) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = description.ifBlank { "Image Preview" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 450.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = uriString,
                            contentDescription = description,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }
            }
        }
    }

    if (showOptionsDialog) {
        AttachmentOptionsDialog(
            uriString = uriString,
            filename = description.ifBlank { "Image.jpg" },
            onDismiss = { showOptionsDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerLayout(uriString: String, label: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(0) }

    // Auto-dispose player
    DisposableEffect(uriString) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    // Launch progress updater
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    val pos = mediaPlayer?.currentPosition ?: 0
                    currentPos = pos.toFloat()
                    if (pos >= totalDuration - 250 && totalDuration > 0) {
                        isPlaying = false
                        currentPos = 0f
                        mediaPlayer?.seekTo(0)
                        mediaPlayer?.pause()
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    fun initPlayer() {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, android.net.Uri.parse(uriString))
                    setOnPreparedListener { mp ->
                        totalDuration = mp.duration
                        mp.start()
                        isPlaying = true
                    }
                    setOnErrorListener { _, _, _ ->
                        isPlaying = false
                        android.widget.Toast.makeText(context, "Cannot play audio. Permission denied or file missing.", android.widget.Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Cannot play audio. Permission denied or file missing.", android.widget.Toast.LENGTH_SHORT).show()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false
            }
        } else {
            mediaPlayer?.let { player ->
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.start()
                    isPlaying = true
                }
            }
        }
    }

    var showOptionsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { /* Play/Pause button is clicked separately, but clicking the card itself can do nothing or show the same option dialog */ },
                onLongClick = { showOptionsDialog = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    try {
                        initPlayer()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val formattedDuration = remember(totalDuration) {
                        val secs = (totalDuration / 1000) % 60
                        val mins = (totalDuration / 1000) / 60
                        String.format("%02d:%02d", mins, secs)
                    }
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = currentPos,
                    onValueChange = { newVal ->
                        currentPos = newVal
                        mediaPlayer?.seekTo(newVal.toInt())
                    },
                    valueRange = 0f..(if (totalDuration > 0) totalDuration.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.height(18.dp)
                )
            }
        }
    }

    if (showOptionsDialog) {
        AttachmentOptionsDialog(
            uriString = uriString,
            filename = label.ifBlank { "VoiceNote.mp3" },
            onDismiss = { showOptionsDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileAttachmentLayout(uriString: String, filename: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }
    val mimeType = remember(uriString, filename) { getMimeType(uriString, filename) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    try {
                        val parsedUri = android.net.Uri.parse(uriString)
                        val shareUri = if (parsedUri.scheme == "file") {
                            val file = java.io.File(parsedUri.path ?: "")
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            parsedUri
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(shareUri, mimeType)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Open File With"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Cannot open this file format: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { showOptionsDialog = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = "File attachment",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to open, Long press for options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open file",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showOptionsDialog) {
        AttachmentOptionsDialog(
            uriString = uriString,
            filename = filename.ifBlank { "Attachment" },
            onDismiss = { showOptionsDialog = false }
        )
    }
}

@Composable
fun TableLayout(tableLines: List<String>, primaryColor: Color, onSurfaceVariant: Color) {
    val parsedRows = remember(tableLines) {
        tableLines.map { line ->
            line.trim().split("|").map { it.trim() }.filterIndexed { index, _ ->
                index > 0 && index < line.trim().split("|").size - 1
            }
        }.filter { it.isNotEmpty() }
    }

    if (parsedRows.size < 2) return

    val headers = parsedRows[0]
    val startRowIndex = if (parsedRows[1].any { cell -> cell.contains("---") || cell.contains("-") }) 2 else 1
    val dataRows = if (startRowIndex < parsedRows.size) parsedRows.subList(startRowIndex, parsedRows.size) else emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEach { headerCell ->
                    Box(
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 160.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = parseInlineStyles(headerCell, primaryColor, onSurfaceVariant),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            dataRows.forEachIndexed { rowIndex, rowCells ->
                val bgFactor = if (rowIndex % 2 == 0) 0.02f else 0.06f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgFactor), RoundedCornerShape(4.dp))
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (colIndex in headers.indices) {
                        val cellText = rowCells.getOrNull(colIndex) ?: ""
                        Box(
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 160.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = parseInlineStyles(cellText, primaryColor, onSurfaceVariant),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

fun highlightCodeSyntax(
    code: String,
    primaryColor: Color,
    tertiaryColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    // Simple fast tokenizer for programming syntax highlighting
    val tokens = code.split(Regex("(?<=\\b)|(?=\\b)|(?<=\\W)|(?=\\W)"))
    val keywords = setOf(
        "val", "var", "fun", "class", "import", "package", "return", "if", "else", 
        "when", "for", "while", "interface", "null", "true", "false", "override", 
        "private", "public", "protected", "const", "infix", "suspend", "object"
    )
    val types = setOf(
        "String", "Int", "Boolean", "Float", "Double", "Long", "Char", "Byte", "Short",
        "Modifier", "Color", "Composable", "Alignment", "Text", "Card", "Row", "Column", "Box"
    )

    var isInsideLineComment = false

    var index = 0
    while (index < tokens.size) {
        val token = tokens[index]
        
        when {
            token == "//" || isInsideLineComment -> {
                isInsideLineComment = true
                builder.pushStyle(SpanStyle(color = Color(0xFF78909C), fontStyle = FontStyle.Italic))
                builder.append(token)
                builder.pop()
                if (token.contains("\n")) {
                    isInsideLineComment = false
                }
            }
            keywords.contains(token) -> {
                builder.pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold))
                builder.append(token)
                builder.pop()
            }
            types.contains(token) -> {
                builder.pushStyle(SpanStyle(color = tertiaryColor, fontWeight = FontWeight.Medium))
                builder.append(token)
                builder.pop()
            }
            token.startsWith("\"") && token.endsWith("\"") -> {
                builder.pushStyle(SpanStyle(color = Color(0xFF43A047)))
                builder.append(token)
                builder.pop()
            }
            token.all { it.isDigit() } -> {
                builder.pushStyle(SpanStyle(color = Color(0xFFD81B60)))
                builder.append(token)
                builder.pop()
            }
            else -> {
                builder.append(token)
            }
        }
        index++
    }
    return builder.toAnnotatedString()
}

@Composable
fun CodeBlockLayout(code: String, language: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = highlightCodeSyntax(code.trim(), primaryColor, tertiaryColor),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun Modifier.drawBehindBorderLeft(color: Color, width: androidx.compose.ui.unit.Dp) = this.drawBehind {
    val strokeWidthPx = width.toPx()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidthPx
    )
}

@Composable
fun InteractiveText(
    annotatedString: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasAnnotations = remember(annotatedString) {
        annotatedString.getStringAnnotations(tag = "URL", start = 0, end = annotatedString.length).isNotEmpty() ||
                annotatedString.getStringAnnotations(tag = "EMAIL", start = 0, end = annotatedString.length).isNotEmpty() ||
                annotatedString.getStringAnnotations(tag = "PHONE", start = 0, end = annotatedString.length).isNotEmpty()
    }
    
    if (hasAnnotations) {
        androidx.compose.foundation.text.ClickableText(
            text = annotatedString,
            style = style.copy(textDirection = TextDirection.ContentOrLtr),
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            val uriStr = annotation.item
                            val intent = if (uriStr.startsWith("tel:") || uriStr.startsWith("mailto:") || uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                                Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                            } else {
                                val webUri = if (!uriStr.contains("://")) "https://$uriStr" else uriStr
                                Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No app found to handle this link", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        return@ClickableText
                    }
                annotatedString.getStringAnnotations(tag = "EMAIL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            val email = annotation.item
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No app found to send email", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        return@ClickableText
                    }
                annotatedString.getStringAnnotations(tag = "PHONE", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            val phone = annotation.item
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No app found to make phone calls", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        return@ClickableText
                    }
            }
        )
    } else {
        Text(
            text = annotatedString,
            style = style.copy(textDirection = TextDirection.ContentOrLtr),
            modifier = modifier
        )
    }
}

fun getMimeType(uriString: String, filename: String): String {
    val extension = java.io.File(filename).extension.lowercase()
    return when (extension) {
        "apk" -> "application/vnd.android.package-archive"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "m4a", "aac" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "mkv" -> "video/x-matroska"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "md" -> "text/markdown"
        "zip" -> "application/zip"
        else -> {
            val contentResolver = android.webkit.MimeTypeMap.getSingleton()
            val mime = contentResolver.getMimeTypeFromExtension(extension)
            mime ?: "*/*"
        }
    }
}

@Composable
fun AttachmentOptionsDialog(
    uriString: String,
    filename: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mimeType = remember(uriString, filename) { getMimeType(uriString, filename) }
    val isApk = remember(filename) { filename.lowercase().endsWith(".apk") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (mimeType.startsWith("image/")) Icons.Default.Image
                              else if (mimeType.startsWith("audio/")) Icons.Default.PlayArrow
                              else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = filename.ifEmpty { "Attachment Options" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Choose how you want to open or interact with this attachment.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isApk) {
                    Button(
                        onClick = {
                            onDismiss()
                            try {
                                val parsedUri = android.net.Uri.parse(uriString)
                                val shareUri = if (parsedUri.scheme == "file") {
                                    val file = java.io.File(parsedUri.path ?: "")
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } else {
                                    parsedUri
                                }
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(shareUri, "application/vnd.android.package-archive")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Installer failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install APK", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        try {
                            val parsedUri = android.net.Uri.parse(uriString)
                            val shareUri = if (parsedUri.scheme == "file") {
                                val file = java.io.File(parsedUri.path ?: "")
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                parsedUri
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(shareUri, mimeType)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Open with external application"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isApk) "Open as File (View Contents)" else "Open in External App", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        try {
                            val parsedUri = android.net.Uri.parse(uriString)
                            val shareUri = if (parsedUri.scheme == "file") {
                                val file = java.io.File(parsedUri.path ?: "")
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                parsedUri
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = mimeType
                                putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Attachment"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot share: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share/Send to App", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
