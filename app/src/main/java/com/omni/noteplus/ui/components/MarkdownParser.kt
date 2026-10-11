package com.omni.noteplus.ui.components

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension

data class NoteCardPreview(val text: String, val media: Set<AttachmentKind>)

/** Parse once per document; source spans keep preview tasks tied to the correct source line. */
object NoteMarkdownParser {
    private val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()
    fun parse(source: String): Node = parser.parse(source)
    fun parseDocument(source: String): PreviewDocument {
        val compatible = compatiblePreviewSource(source)
        return PreviewDocument(parser.parse(compatible.lines.joinToString("\n")), compatible.lines, compatible.sourceLines)
    }
    fun preview(source: String): String = cardPreview(source).text
    fun cardPreview(source: String): NoteCardPreview {
        val media = mutableSetOf<AttachmentKind>()
        val result = StringBuilder()
        fun append(node: Node) {
            when (node) {
                is org.commonmark.node.Image -> { media.add(AttachmentKind.IMAGE); result.append("[Image]") }
                is org.commonmark.node.Link -> {
                    val label = node.children().filterIsInstance<org.commonmark.node.Text>().joinToString("") { it.literal }
                    when {
                        label.startsWith("voice") || label.startsWith("audio") -> { media.add(AttachmentKind.AUDIO); result.append("[Voice clip]") }
                        label.startsWith("file:") -> {
                            val kind = attachmentKind(node.destination, label.substringAfter(':'))
                            media.add(kind)
                            result.append(when (kind) {
                            AttachmentKind.IMAGE -> "[Image]"
                            AttachmentKind.AUDIO -> "[Voice clip]"
                            AttachmentKind.FILE -> "[Attachment]"
                        })
                        }
                        else -> node.children().forEach { append(it) }
                    }
                }
                is org.commonmark.node.Text -> result.append(node.literal)
                is org.commonmark.node.Code -> result.append(node.literal)
                is org.commonmark.node.FencedCodeBlock -> result.append(node.literal)
                is org.commonmark.node.IndentedCodeBlock -> result.append(node.literal)
                is org.commonmark.node.SoftLineBreak, is org.commonmark.node.HardLineBreak -> result.append('\n')
                else -> node.children().forEach { append(it) }
            }
            if (node is org.commonmark.node.Paragraph || node is org.commonmark.node.Heading) result.append('\n')
        }
        append(parseDocument(source.take(8000)).root)
        return NoteCardPreview(result.toString().trim().take(1000), media)
    }
}

internal fun Node.children(): List<Node> = buildList {
    var node = firstChild
    while (node != null) { add(node); node = node.next }
}

internal fun Node.sourceText(lines: List<String>): String = sourceSpans.joinToString("\n") { span ->
    lines.getOrNull(span.lineIndex)?.let { line ->
        line.substring(span.columnIndex.coerceAtMost(line.length), (span.columnIndex + span.length).coerceAtMost(line.length))
    } ?: ""
}
