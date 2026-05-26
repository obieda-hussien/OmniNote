package com.omninote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omninote.data.NoteEntity
import com.omninote.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

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
            repository.insert(note.copy(isTrashed = true))
        }
    }
    
    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note.copy(isTrashed = false))
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note.copy(isArchived = true))
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note.copy(isArchived = false))
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
