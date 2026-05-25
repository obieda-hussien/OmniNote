package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addNote(title: String, content: String, colorHex: String?, tags: String) {
        viewModelScope.launch {
            repository.insert(NoteEntity(title = title, content = content, colorHex = colorHex, tags = tags))
        }
    }

    fun updateNote(id: Int, title: String, content: String, isPinned: Boolean, colorHex: String?, timestamp: Long, tags: String) {
        viewModelScope.launch {
            repository.insert(NoteEntity(id = id, title = title, content = content, timestamp = timestamp, isPinned = isPinned, colorHex = colorHex, tags = tags))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun togglePin(id: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(id, isPinned)
        }
    }
}
