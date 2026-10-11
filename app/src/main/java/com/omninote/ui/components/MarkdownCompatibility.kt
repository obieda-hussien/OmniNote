package com.omninote.ui.components

import org.commonmark.node.Node

/** Render-only repair: saved Markdown and checklist source indices remain untouched. */
data class PreviewDocument(val root: Node, val lines: List<String>, val sourceLines: List<Int>) {
    fun originalLine(index: Int): Int = sourceLines.getOrElse(index) { index }
}

internal data class PreviewSource(val lines: List<String>, val sourceLines: List<Int>)

internal fun compatiblePreviewSource(source: String): PreviewSource {
    val input = source.split('\n').map { it.removeSuffix("\r") }
    val lines = mutableListOf<String>()
    val indices = mutableListOf<Int>()
    var fenceChar: Char? = null
    var fenceLength = 0
    var index = 0
    fun append(line: String, original: Int) { lines.add(line); indices.add(original) }
    while (index < input.size) {
        val line = input[index]
        val fence = Regex("""^ {0,3}(`{3,}|~{3,})(.*)$""").matchEntire(line)
        if (fenceChar != null) {
            append(line, index)
            if (fence != null && fence.groupValues[1][0] == fenceChar &&
                fence.groupValues[1].length >= fenceLength && fence.groupValues[2].isBlank()) fenceChar = null
            index++
            continue
        }
        if (fence != null) {
            fenceChar = fence.groupValues[1][0]; fenceLength = fence.groupValues[1].length
            append(line, index); index++; continue
        }
        val header = splitMarkdownRow(line)
        val separator = input.getOrNull(index + 1)?.let(::splitMarkdownRow)
        // Only recognise an unmistakable table separator, never ordinary prose or a code line.
        val table = !line.startsWith("    ") && !line.startsWith('\t') && header != null && header.size >= 2 &&
            separator != null && separator.isNotEmpty() && separator.all { Regex(":?-+:?").matches(it.trim()) }
        if (table) {
            if (lines.lastOrNull()?.isNotBlank() == true) append("", index)
            append(escapeTableCodePipes(line), index)
            val cells = List(header!!.size) { column ->
                val cell = separator!!.getOrNull(column)?.trim().orEmpty()
                (if (cell.startsWith(':')) ":" else "") + "---" + (if (cell.endsWith(':')) ":" else "")
            }
            append("| " + cells.joinToString(" | ") + " |", index + 1)
            index += 2
            while (index < input.size && splitMarkdownRow(input[index]) != null && input[index].isNotBlank()) {
                append(escapeTableCodePipes(input[index]), index); index++
            }
            // Separate following list/task blocks from the final row, without changing the note.
            if (index < input.size && input[index].isNotBlank()) append("", index)
        } else { append(line, index); index++ }
    }
    return PreviewSource(lines, indices)
}

/** Pipes inside inline code or escaped pipes belong to a cell. */
internal fun splitMarkdownRow(line: String): List<String>? {
    val text = line.trim()
    if (text.isEmpty()) return null
    val cells = mutableListOf<String>()
    var cell = StringBuilder()
    var codeTicks = 0
    var separators = 0
    var index = 0
    while (index < text.length) {
        val char = text[index]
        if (char == '\\' && index + 1 < text.length) {
            cell.append(char).append(text[index + 1]); index += 2; continue
        }
        if (char == '`') {
            var end = index
            while (end < text.length && text[end] == '`') end++
            val count = end - index
            if (codeTicks == 0) codeTicks = count else if (codeTicks == count) codeTicks = 0
            cell.append(text.substring(index, end)); index = end; continue
        }
        if (char == '|' && codeTicks == 0) {
            cells.add(cell.toString().trim()); cell = StringBuilder(); separators++
        } else cell.append(char)
        index++
    }
    cells.add(cell.toString().trim())
    if (separators == 0) return null
    if (text.startsWith('|')) cells.removeAt(0)
    if (text.endsWith('|') && cells.lastOrNull().isNullOrEmpty()) cells.removeAt(cells.lastIndex)
    return cells
}

/** GFM requires pipes inside code cells to be escaped; normalise only the rendered table. */
private fun escapeTableCodePipes(line: String): String = buildString {
    var ticks = 0
    var index = 0
    while (index < line.length) {
        val char = line[index]
        if (char == '\\' && index + 1 < line.length) {
            append(char); append(line[index + 1]); index += 2; continue
        }
        if (char == '`') {
            var end = index
            while (end < line.length && line[end] == '`') end++
            val count = end - index
            if (ticks == 0) ticks = count else if (ticks == count) ticks = 0
            append(line.substring(index, end)); index = end; continue
        }
        if (char == '|' && ticks != 0) append('\\')
        append(char); index++
    }
}
