package com.omninote.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omninote.data.NoteEntity
import kotlinx.coroutines.sync.Mutex

/** Retained by the ViewModel while this editor is open, including configuration changes. */
class EditorDraft(val requestedId: Int?) {
    val buffer = EditorBuffer()
    var title by mutableStateOf("")
    var isPinned by mutableStateOf(false)
    var colorHex by mutableStateOf<String?>(null)
    var tags by mutableStateOf(emptyList<String>())
    var isLocked by mutableStateOf(false)
    var lockPin by mutableStateOf<String?>(null)
    var isPreview by mutableStateOf(false)
    var base by mutableStateOf<NoteEntity?>(null)
    var loaded by mutableStateOf(requestedId == null)
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var closing by mutableStateOf(false)
    var discarded = false
    var sharedConsumed = false
    internal val saveMutex = Mutex()
    internal var saved: NoteEntity? = null
    fun initialize(note: NoteEntity) {
        if (loaded) return
        base = note
        title = note.title
        buffer.reset(note.content)
        isPinned = note.isPinned
        colorHex = note.colorHex
        tags = note.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        isLocked = note.isLocked
        lockPin = note.lockPin
        loaded = true
        saved = snapshot()
    }
    fun snapshot(): NoteEntity = (base ?: NoteEntity(title = "", content = "", timestamp = 0)).copy(
        title = title, content = buffer.text, isPinned = isPinned, colorHex = colorHex,
        tags = tags.distinct().joinToString(","), isLocked = isLocked, lockPin = lockPin
    )
    val dirty get() = loaded && !discarded && snapshot() != saved &&
        (base != null || title.isNotBlank() || buffer.text.isNotBlank())
}
