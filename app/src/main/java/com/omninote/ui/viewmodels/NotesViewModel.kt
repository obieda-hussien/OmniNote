package com.omninote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omninote.data.NoteEntity
import com.omninote.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class NoteEvent {
    data class Trashed(val note: NoteEntity, val message: String = "Moved to Trash") : NoteEvent()
    data class Archived(val note: NoteEntity, val message: String = "Note Archived") : NoteEvent()
    data class Restored(val note: NoteEntity, val message: String = "Note Restored") : NoteEvent()
    data class Unarchived(val note: NoteEntity, val message: String = "Note Unarchived") : NoteEvent()
}

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _noteEvent = MutableSharedFlow<NoteEvent>()
    val noteEvent: SharedFlow<NoteEvent> = _noteEvent.asSharedFlow()

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String, content: String, colorHex: String?, tags: String, isLocked: Boolean, lockPin: String?) {
        viewModelScope.launch {
            repository.insert(NoteEntity(title = title, content = content, colorHex = colorHex, tags = tags, isLocked = isLocked, lockPin = lockPin))
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note)
        }
    }
    
    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            val noteWithTrash = note.copy(isTrashed = true)
            repository.insert(noteWithTrash)
            _noteEvent.emit(NoteEvent.Trashed(noteWithTrash))
        }
    }
    
    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            val restoredNote = note.copy(isTrashed = false)
            repository.insert(restoredNote)
            _noteEvent.emit(NoteEvent.Restored(restoredNote))
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            val archivedNote = note.copy(isArchived = true)
            repository.insert(archivedNote)
            _noteEvent.emit(NoteEvent.Archived(archivedNote))
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            val unarchivedNote = note.copy(isArchived = false)
            repository.insert(unarchivedNote)
            _noteEvent.emit(NoteEvent.Unarchived(unarchivedNote))
        }
    }

    fun deleteNotePermanent(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note.id, !note.isPinned)
        }
    }
    
    fun lockNote(note: NoteEntity, pin: String) {
        viewModelScope.launch {
            repository.insert(note.copy(isLocked = true, lockPin = pin))
        }
    }
    
    fun unlockNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note.copy(isLocked = false, lockPin = null))
        }
    }

    var sharedText: String? = null
    var sharedUris: List<android.net.Uri>? = null

    fun consumeSharedContent() {
        sharedText = null
        sharedUris = null
    }
}
