package com.omninote.ui.components

import org.commonmark.node.*

sealed interface ParagraphPart {
    data class TextRun(val nodes: List<Node>) : ParagraphPart
    data class Photo(val uri: String, val description: String) : ParagraphPart
    data class Audio(val uri: String, val label: String) : ParagraphPart
    data class File(val uri: String, val label: String) : ParagraphPart
}

internal fun plainInline(node: Node): String = when (node) {
    is Text -> node.literal
    is Code -> node.literal
    is SoftLineBreak, is HardLineBreak -> "\n"
    else -> node.children().joinToString("") { plainInline(it) }
}

/** Media nodes can appear anywhere in a paragraph, including adjacent attachments. */
fun paragraphParts(paragraph: Node): List<ParagraphPart> = buildList {
    val text = mutableListOf<Node>()
    fun flush() {
        if (text.any { it !is SoftLineBreak && it !is HardLineBreak }) add(ParagraphPart.TextRun(text.toList()))
        text.clear()
    }
    for (node in paragraph.children()) {
        val attachment = when (node) {
            is Image -> ParagraphPart.Photo(node.destination, plainInline(node).ifBlank { "Image" })
            is Link -> {
                val label = plainInline(node)
                val type = label.substringBefore(':').lowercase()
                val name = label.substringAfter(':', "").ifBlank { if (type == "file") "Attachment" else "Voice recording" }
                when (type) {
                    "voice", "audio" -> ParagraphPart.Audio(node.destination,
                        if (name.startsWith("file:") || name.startsWith("content:")) "Voice recording" else name)
                    "file" -> ParagraphPart.File(node.destination, name)
                    else -> null
                }
            }
            else -> null
        }
        if (attachment != null) { flush(); add(attachment) } else text.add(node)
    }
    flush()
}
