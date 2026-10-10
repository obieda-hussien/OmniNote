package com.omninote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.node.*
import org.commonmark.ext.gfm.tables.*

@Composable
fun MarkdownDocumentPreview(
    title: String,
    rawText: String,
    listState: LazyListState = rememberLazyListState(),
    onCheckedChange: ((Int, Boolean) -> Unit)? = null
) {
    val document by produceState<PreviewDocument?>(null, rawText) {
        value = null
        value = withContext(Dispatchers.Default) { NoteMarkdownParser.parseDocument(rawText) }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (title.isNotBlank()) item {
            Text(title, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (rawText.isBlank()) item {
            Text("Your preview will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 48.dp))
        } else if (document == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(document?.root?.children() ?: emptyList()) { block ->
            document?.let { parsed ->
                MarkdownBlock(block, parsed.lines, onCheckedChange?.let { callback ->
                    { line, checked -> callback(parsed.originalLine(line), checked) }
                })
            }
        }
    }
}

@Composable
internal fun MarkdownBlock(node: Node, lines: List<String>, onCheckedChange: ((Int, Boolean) -> Unit)?) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface,
        textDirection = TextDirection.ContentOrLtr)
    when (node) {
        is Heading -> {
            val style = when (node.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                3 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            InteractiveText(markdownInline(node, primary, secondary), style.copy(fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface), Modifier.padding(top = 8.dp))
        }
        is Paragraph -> MarkdownParagraph(node, lines, bodyStyle)
        is FencedCodeBlock -> CodeBlockLayout(node.literal, node.info.ifBlank { "code" })
        is IndentedCodeBlock -> CodeBlockLayout(node.literal, "code")
        is ThematicBreak -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
        is BlockQuote -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.children().forEach { MarkdownBlock(it, lines, onCheckedChange) }
            }
        }
        is BulletList, is OrderedList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            node.children().forEachIndexed { index, item ->
                val paragraph = item.firstChild as? Paragraph
                val source = paragraph?.sourceText(lines).orEmpty()
                val task = Regex("""^\[([ xX])\]\s+(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(source)
                val lineIndex = paragraph?.sourceSpans?.firstOrNull()?.lineIndex
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (task != null && lineIndex != null) {
                        Checkbox(checked = task.groupValues[1] != " ",
                            onCheckedChange = onCheckedChange?.let { callback -> { checked -> callback(lineIndex, checked) } })
                        Column(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InteractiveText(parseInlineStyles(task.groupValues[2], primary, secondary), bodyStyle)
                            item.children().drop(1).forEach { MarkdownBlock(it, lines, onCheckedChange) }
                        }
                    } else {
                        Text(if (node is OrderedList) "${node.startNumber + index}." else "•",
                            modifier = Modifier.widthIn(min = 24.dp).padding(end = 8.dp), color = primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.children().forEach { MarkdownBlock(it, lines, onCheckedChange) }
                        }
                    }
                }
            }
        }
        is TableBlock -> MarkdownTable(node)
        is HtmlBlock -> Text(node.literal, style = bodyStyle) // Raw HTML stays visible, never executable.
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.children().forEach { MarkdownBlock(it, lines, onCheckedChange) }
        }
    }
}

@Composable
private fun MarkdownParagraph(node: Paragraph, lines: List<String>, style: androidx.compose.ui.text.TextStyle) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val parts = remember(node) { paragraphParts(node) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        for (part in parts) {
            when (part) {
                is ParagraphPart.Photo -> ImageLayout(part.uri, part.description)
                is ParagraphPart.Audio -> AudioPlayerLayout(part.uri, part.label)
                is ParagraphPart.File -> FileAttachmentLayout(part.uri, part.label)
                is ParagraphPart.TextRun -> {
                    val source = node.sourceText(lines)
                    InteractiveText(
                        if (parts.size == 1 && (source.contains("[color:") || source.contains("[bg:") || source.contains("==")))
                            parseInlineStyles(source, primary, secondary)
                        else markdownInlineNodes(part.nodes, primary, secondary), style)
                }
            }
        }
    }
}
