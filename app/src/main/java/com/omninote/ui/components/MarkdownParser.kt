package com.omninote.ui.components

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension

/** Parse once per document; source spans keep preview tasks tied to the correct source line. */
object NoteMarkdownParser {
    private val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()
    fun parse(source: String): Node = parser.parse(source)
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
