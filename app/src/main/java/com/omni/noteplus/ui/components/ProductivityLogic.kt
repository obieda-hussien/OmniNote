package com.omni.noteplus.ui.components

import com.omni.noteplus.data.*
import org.commonmark.node.*
import java.text.SimpleDateFormat
import java.util.*

data class ProductivitySnapshot(
    val tracking: ProductivityTracking? = null,
    val records: List<ProductivityNote> = emptyList(),
    val days: List<ProductivityDay> = emptyList()
) {
    val ready get() = tracking != null
    val totalNotes get() = records.size
    val recordedWords get() = records.sumOf { it.wordsAdded }
    val deletedNotes get() = records.count { it.state == "DELETED" }
    val activeNotes get() = records.count { it.state == "ACTIVE" }
    val archivedNotes get() = records.count { it.state == "ARCHIVED" }
    val trashedNotes get() = records.count { it.state == "TRASHED" }
    val currentWords get() = records.filter { it.state != "DELETED" }.sumOf { it.currentWords.toLong() }
    val writingDays get() = days.count { it.workedOn > 0 }
}

private val wordPattern = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")
private val urlPattern = Regex("""(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

/** Count written text, excluding Markdown syntax, code, media labels and destinations. */
fun noteWordTokens(note: NoteEntity): List<String> {
    fun text(node: Node): String = when (node) {
        is Image, is Code, is FencedCodeBlock, is IndentedCodeBlock, is HtmlBlock -> ""
        is Text -> node.literal
        is SoftLineBreak, is HardLineBreak -> " "
        is Link -> if (plainInline(node).substringBefore(':').lowercase() in setOf("voice", "audio", "file")) ""
            else node.children().joinToString("") { text(it) }
        else -> node.children().joinToString("") { text(it) } +
            if (node is Paragraph || node is Heading || node is org.commonmark.ext.gfm.tables.TableCell) "\n" else ""
    }
    val source = note.content.replace(Regex("""\[(?:color|bg):#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?\]\("""), "(")
    val body = text(NoteMarkdownParser.parseDocument(source).root)
        .replace(Regex("""(?m)^\[([ xX])\]\s*"""), "")
    return wordPattern.findAll(urlPattern.replace(note.title + "\n" + body, ""))
        .map { it.value.lowercase(Locale.ROOT) }.toList()
}

/** Reorders, metadata changes and removals add no words; replacements count their new words. */
fun addedWords(previous: List<String>, next: List<String>): Long {
    val counts = previous.groupingBy { it }.eachCount().toMutableMap()
    var additions = 0L
    for (word in next) {
        val remaining = counts[word] ?: 0
        if (remaining > 0) counts[word] = remaining - 1 else additions++
    }
    return additions
}

fun productivityDay(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))

/** Yesterday keeps the streak alive until today's writing session begins. */
fun writingStreak(days: Set<String>, today: String): Int {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    val date = runCatching { format.parse(today) }.getOrNull() ?: return 0
    val cursor = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
    if (today !in days) cursor.add(Calendar.DATE, -1)
    var count = 0
    while (format.format(cursor.time) in days) { count++; cursor.add(Calendar.DATE, -1) }
    return count
}
