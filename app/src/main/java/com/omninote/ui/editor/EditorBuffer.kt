package com.omninote.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** One source of truth for text, selection and immediate undo/redo. */
class EditorBuffer(initial: String = "") {
    private var current by mutableStateOf(TextFieldValue(initial))
    private var past by mutableStateOf(emptyList<TextFieldValue>())
    private var future by mutableStateOf(emptyList<TextFieldValue>())
    var value: TextFieldValue
        get() = current
        set(next) {
            if (next.text != current.text) {
                past = (past + current.copy(composition = null)).takeLast(100)
                future = emptyList()
            }
            current = next
        }
    var text: String
        get() = current.text
        set(next) { value = TextFieldValue(next, TextRange(current.selection.start.coerceAtMost(next.length), current.selection.end.coerceAtMost(next.length))) }
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()
    fun reset(text: String) {
        current = TextFieldValue(text)
        past = emptyList()
        future = emptyList()
    }
    fun undo() {
        if (!canUndo) return
        future = future + current.copy(composition = null)
        current = past.last()
        past = past.dropLast(1)
    }
    fun redo() {
        if (!canRedo) return
        past = past + current.copy(composition = null)
        current = future.last()
        future = future.dropLast(1)
    }
    fun insert(text: String) {
        val start = current.selection.min
        val end = current.selection.max
        value = TextFieldValue(current.text.replaceRange(start, end, text), TextRange(start + text.length))
    }
    fun wrap(prefix: String, suffix: String = prefix) {
        val start = current.selection.min
        val end = current.selection.max
        val selected = current.text.substring(start, end)
        // A second press removes the surrounding markup.
        if (start >= prefix.length && current.text.substring(start - prefix.length, start) == prefix &&
            current.text.substring(end).startsWith(suffix)) {
            value = TextFieldValue(current.text.removeRange(end, end + suffix.length).removeRange(start - prefix.length, start),
                TextRange(start - prefix.length, end - prefix.length))
        } else {
            value = TextFieldValue(current.text.replaceRange(start, end, prefix + selected + suffix),
                TextRange(start + prefix.length, end + prefix.length))
        }
    }
    fun prefixLines(prefix: String) {
        val start = current.text.lastIndexOf('\n', (current.selection.min - 1).coerceAtLeast(-1)) + 1
        val selectionEnd = if (current.selection.max > current.selection.min && current.selection.max > 0 &&
            current.text[current.selection.max - 1] == '\n') current.selection.max - 1 else current.selection.max
        val end = current.text.indexOf('\n', selectionEnd).let { if (it < 0) current.text.length else it }
        val lines = current.text.substring(start, end).split('\n')
        val remove = lines.all { it.startsWith(prefix) }
        val replacement = lines.joinToString("\n") { if (remove) it.removePrefix(prefix) else prefix + it }
        val offset = if (remove) -prefix.length else prefix.length
        val newText = current.text.replaceRange(start, end, replacement)
        val newStart = (current.selection.min + offset).coerceIn(start, newText.length)
        val newEnd = (current.selection.max + replacement.length - (end - start)).coerceIn(newStart, newText.length)
        value = TextFieldValue(newText, TextRange(newStart, newEnd))
    }
    fun setChecked(lineIndex: Int, checked: Boolean) {
        val lines = text.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return
        val marker = Regex("""^(\s*(?:[-*+]|\d+[.)])\s+\[)[ xX](\])""").find(lines[lineIndex]) ?: return
        val position = marker.groups[1]!!.range.last + 1
        lines[lineIndex] = lines[lineIndex].replaceRange(position, position + 1, if (checked) "x" else " ")
        text = lines.joinToString("\n")
    }
}
