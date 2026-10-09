package com.omninote.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.commonmark.node.*
import org.commonmark.ext.gfm.strikethrough.Strikethrough

internal fun markdownInline(node: Node, primary: Color, secondary: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    fun render(current: Node) {
        val style = when (current) {
            is StrongEmphasis -> SpanStyle(fontWeight = FontWeight.Bold)
            is Emphasis -> SpanStyle(fontStyle = FontStyle.Italic)
            is Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            is Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = secondary.copy(alpha = 0.12f))
            is Link -> SpanStyle(color = primary, textDecoration = TextDecoration.Underline)
            else -> null
        }
        if (style != null) builder.pushStyle(style)
        val safeLink = (current as? Link)?.destination?.takeIf {
            val scheme = android.net.Uri.parse(it).scheme?.lowercase()
            scheme == null || scheme in listOf("https", "http", "mailto", "tel")
        }
        if (safeLink != null) builder.pushStringAnnotation("URL", safeLink)
        when (current) {
            is org.commonmark.node.Text -> appendAnnotatedPlainSlice(builder, current.literal, primary)
            is Code -> builder.append(current.literal)
            is SoftLineBreak, is HardLineBreak -> builder.append("\n")
            is HtmlInline -> builder.append(current.literal)
            else -> current.children().forEach { render(it) }
        }
        if (safeLink != null) builder.pop()
        if (style != null) builder.pop()
    }
    node.children().forEach { render(it) }
    return builder.toAnnotatedString()
}

/** Keeps Omni's existing colour/highlight syntax, alongside standard CommonMark. */
fun parseInlineStyles(text: String, primaryColor: Color, onSurfaceVariant: Color): AnnotatedString {
    val marker = Regex("""\[(color|bg):(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\]\(|==|`+""")
    val builder = AnnotatedString.Builder()
    fun standard(source: String) {
        if (source.isEmpty()) return
        val leading = source.takeWhile { it.isWhitespace() }
        val core = source.trim()
        builder.append(leading)
        if (core.isNotEmpty()) {
            builder.append(markdownInline(NoteMarkdownParser.parse(core), primaryColor, onSurfaceVariant))
            builder.append(source.takeLastWhile { it.isWhitespace() })
        }
    }
    var cursor = 0
    var plainStart = 0
    while (cursor < text.length) {
        val match = marker.find(text, cursor) ?: break
        val start = match.range.first
        if (match.value.startsWith("`")) {
            val end = text.indexOf(match.value, match.range.last + 1)
            cursor = if (end < 0) text.length else end + match.value.length
            continue // Code contents are literal, including Omni colour/highlight syntax.
        }
        val openEnd = match.range.last + 1
        var close = -1
        var after = -1
        if (match.value == "==") {
            close = text.indexOf("==", openEnd)
            if (close >= 0) after = close + 2
        } else {
            var depth = 1
            var index = openEnd
            while (index < text.length && depth > 0) {
                if (text[index] == '\\') { index += 2; continue }
                if (text[index] == '(') depth++
                if (text[index] == ')') depth--
                if (depth == 0) { close = index; after = index + 1 }
                index++
            }
        }
        if (close < 0) { cursor = openEnd; continue }
        standard(text.substring(plainStart, start))
        val hex = match.groups[2]?.value
        val color = hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        builder.pushStyle(when {
            match.value == "==" -> SpanStyle(background = primaryColor.copy(alpha = 0.22f))
            match.groups[1]?.value == "bg" -> SpanStyle(background = (color ?: primaryColor).copy(alpha = 0.25f))
            else -> SpanStyle(color = color ?: primaryColor)
        })
        builder.append(parseInlineStyles(text.substring(openEnd, close), primaryColor, onSurfaceVariant))
        builder.pop()
        cursor = after
        plainStart = after
    }
    standard(text.substring(plainStart))
    return builder.toAnnotatedString()
}
