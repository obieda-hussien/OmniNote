package com.omninote.ui.components

import com.omninote.data.NoteEntity

/** One filtering policy shared by the pager and its contextual actions. */
fun libraryNotes(notes: List<NoteEntity>, tag: String?, query: String, sort: String, pinnedOnly: Boolean): List<NoteEntity> {
    val filtered = notes.filter { note ->
        (tag == null || tag in note.tags.split(',').map { it.trim() }) &&
            (query.isBlank() || note.title.contains(query.trim(), ignoreCase = true) || note.content.contains(query.trim(), ignoreCase = true)) &&
            (!pinnedOnly || note.isPinned)
    }
    val sorted = when (sort) {
        "oldest" -> filtered.sortedBy { it.timestamp }
        "a-z" -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        "z-a" -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        "color" -> filtered.sortedBy { it.colorHex ?: "" }
        else -> filtered.sortedByDescending { it.timestamp }
    }
    return if (sort in listOf("oldest", "a-z", "z-a")) sorted else sorted.sortedByDescending { it.isPinned }
}
