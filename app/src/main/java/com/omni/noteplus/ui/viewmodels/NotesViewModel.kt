package com.omni.noteplus.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omni.noteplus.data.*
import com.omni.noteplus.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import com.omni.noteplus.ui.editor.EditorDraft

sealed class NoteEvent {
    data class ActionCompleted(val receipt: NoteActionReceipt) : NoteEvent()
    data class Failed(val message: String, val retry: (() -> Unit)? = null) : NoteEvent()
    data object UndoCompleted : NoteEvent()
    data object TrashEmptied : NoteEvent()
    data class Trashed(val note: NoteEntity, val message: String = "Moved to Trash") : NoteEvent()
    data class Archived(val note: NoteEntity, val message: String = "Note Archived") : NoteEvent()
    data class Restored(val note: NoteEntity, val message: String = "Note Restored") : NoteEvent()
    data class Unarchived(val note: NoteEntity, val message: String = "Note Unarchived") : NoteEvent()
}

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val productivity = repository.productivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.omni.noteplus.ui.components.ProductivitySnapshot())
    private val _productivityError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val productivityError: StateFlow<String?> = _productivityError
    init { initializeProductivity() }
    fun initializeProductivity() {
        viewModelScope.launch {
            _productivityError.value = null
            try { repository.initializeHistory() }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _productivityError.value = "Could not load productivity history"
            }
        }
    }

    private var openDraft: EditorDraft? = null
    fun editorDraft(id: Int?): EditorDraft = openDraft?.takeIf { it.requestedId == id && !it.discarded }
        ?: EditorDraft(id).also { openDraft = it }

    suspend fun loadDraft(draft: EditorDraft) {
        if (draft.loaded) return
        try {
            val note = repository.getNoteById(requireNotNull(draft.requestedId))
                ?: error("This note is no longer available")
            draft.initialize(note)
            draft.error = null
        } catch (e: Exception) {
            draft.error = e.message ?: "Could not open this note"
        }
    }

    // NonCancellable keeps an in-flight database write alive if the screen rotates.
    suspend fun saveDraft(draft: EditorDraft): NoteEntity? = withContext(NonCancellable) {
        draft.saveMutex.withLock {
            if (!draft.loaded || draft.discarded || !draft.dirty) return@withLock draft.base
            draft.saving = true
            try {
                val snapshot = draft.snapshot().copy(timestamp = System.currentTimeMillis())
                val id = repository.insert(snapshot).toInt()
                val saved = snapshot.copy(id = id)
                draft.base = saved
                draft.saved = saved
                draft.error = null
                saved
            } catch (e: Exception) {
                draft.error = "Couldn't save. Your changes are still here. Try again."
                throw e
            } finally { draft.saving = false }
        }
    }

    fun persistDraft(draft: EditorDraft) {
        viewModelScope.launch { runCatching { saveDraft(draft) } }
    }

    suspend fun trashDraft(draft: EditorDraft) = withContext(NonCancellable) {
        draft.saveMutex.withLock {
            val note = draft.snapshot().copy(isTrashed = true, timestamp = System.currentTimeMillis())
            repository.insert(note)
            draft.discarded = true
            _noteEvent.emit(NoteEvent.Trashed(note))
        }
    }

    fun closeDraft(draft: EditorDraft) {
        draft.discarded = true
        if (openDraft === draft) openDraft = null
    }

    private val _noteEvent = MutableSharedFlow<NoteEvent>()
    val noteEvent: SharedFlow<NoteEvent> = _noteEvent.asSharedFlow()

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = allNotes
        .map { notes -> notes.filter { !it.isArchived && !it.isTrashed }.sortedByDescending { it.isPinned } }
        .distinctUntilChanged().flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = allNotes
        .map { notes -> notes.filter { it.isArchived && !it.isTrashed } }
        .distinctUntilChanged().flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = allNotes
        .map { notes -> notes.filter { it.isTrashed } }
        .distinctUntilChanged().flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createQuickNote(content: String): NoteEntity = withContext(NonCancellable) {
        val note = NoteEntity(title = "", content = content)
        note.copy(id = repository.insert(note).toInt())
    }

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
    
    private val _pendingActions = kotlinx.coroutines.flow.MutableStateFlow<Set<Int>>(emptySet())
    val pendingActions: StateFlow<Set<Int>> = _pendingActions
    private var emptyingTrash = false

    fun performLibraryAction(note: NoteEntity, action: NoteLibraryAction, pin: String? = null) {
        if (note.id in _pendingActions.value || emptyingTrash) return
        _pendingActions.value += note.id
        viewModelScope.launch {
            try {
                val receipt = repository.applyLibraryAction(note.id, action, note.libraryState(), pin)
                _noteEvent.emit(NoteEvent.ActionCompleted(receipt))
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val retry = if (error is NoteActionConflictException || error is NoteActionLockedException) null
                    else ({ performLibraryAction(note, action, pin) })
                _noteEvent.emit(NoteEvent.Failed(when (error) {
                    is NoteActionConflictException -> "This note changed. Please try again from its current section."
                    is NoteActionLockedException -> "This note is locked. Unlock it and try again."
                    else -> "Could not move or delete this note. Your note is still saved."
                }, retry))
            } finally { _pendingActions.value -= note.id }
        }
    }
    fun undoLibraryAction(receipt: NoteActionReceipt) {
        if (receipt.noteId in _pendingActions.value || emptyingTrash) return
        _pendingActions.value += receipt.noteId
        viewModelScope.launch {
            try { repository.undoLibraryAction(receipt); _noteEvent.emit(NoteEvent.UndoCompleted) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _noteEvent.emit(NoteEvent.Failed("Could not undo. The note may have changed or been deleted."))
            } finally { _pendingActions.value -= receipt.noteId }
        }
    }
    fun moveToTrash(note: NoteEntity) = performLibraryAction(note, NoteLibraryAction.TRASH)
    fun restoreFromTrash(note: NoteEntity) = performLibraryAction(note, NoteLibraryAction.RESTORE)
    fun archiveNote(note: NoteEntity) = performLibraryAction(note, NoteLibraryAction.ARCHIVE)
    fun unarchiveNote(note: NoteEntity) = performLibraryAction(note, NoteLibraryAction.UNARCHIVE)
    fun deleteNotePermanent(note: NoteEntity, pin: String? = null) = performLibraryAction(note, NoteLibraryAction.DELETE, pin)
    fun emptyTrash() {
        if (emptyingTrash || _pendingActions.value.isNotEmpty()) return
        emptyingTrash = true
        viewModelScope.launch {
            try { repository.emptyTrash(); _noteEvent.emit(NoteEvent.TrashEmptied) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _noteEvent.emit(NoteEvent.Failed("Could not empty trash. Your notes are still saved.", { emptyTrash() }))
            } finally { emptyingTrash = false }
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

