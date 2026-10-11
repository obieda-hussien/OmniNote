package com.omninote.data

import androidx.room.withTransaction
import com.omninote.ui.components.ProductivitySnapshot
import com.omninote.ui.components.addedWords
import com.omninote.ui.components.noteWordTokens
import com.omninote.ui.components.productivityDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val database: AppDatabase? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dayKey: (Long) -> String = ::productivityDay
) {
    val allNotes = noteDao.getAllNotes()
    val activeNotes = noteDao.getActiveNotes()
    val archivedNotes = noteDao.getArchivedNotes()
    val trashedNotes = noteDao.getTrashedNotes()
    private val history = database?.productivityDao()
    private val undoTokens = java.util.concurrent.ConcurrentHashMap<Int, String>()
    private val actionMutex = kotlinx.coroutines.sync.Mutex()
    val productivity: Flow<ProductivitySnapshot> = history?.let { dao ->
        combine(dao.observeTracking(), dao.observeNotes(), dao.observeDays()) { tracking, records, days ->
            ProductivitySnapshot(tracking, records, days)
        }.flowOn(Dispatchers.Default)
    } ?: flowOf(ProductivitySnapshot()) // In-memory DAO test doubles have no persistent history.

    suspend fun initializeHistory() { database?.withTransaction { initializeInTransaction() } }
    private suspend fun initializeInTransaction() {
        val dao = history ?: return
        if (dao.tracking() != null) return
        val notes = dao.currentNotes()
        for (note in notes) {
            val words = noteWordTokens(note).size
            dao.putNote(ProductivityNote(UUID.randomUUID().toString(), note.id, state(note), words.toLong(), words, null, null))
        }
        dao.insertTracking(ProductivityTracking(startedAt = clock(), baselineNotes = notes.size))
    }
    private fun state(note: NoteEntity) = when {
        note.isTrashed -> "TRASHED"
        note.isArchived -> "ARCHIVED"
        else -> "ACTIVE"
    }
    private suspend fun record(id: String, kind: String, words: Long = 0, now: Long) {
        val dao = history ?: return
        val day = dayKey(now)
        val previous = dao.activity(day, id, kind)
        dao.putActivity(ProductivityActivity(day, id, kind, (previous?.wordsAdded ?: 0) + words, now))
    }
    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insert(note: NoteEntity): Long {
        val db = database ?: return noteDao.insertNote(note)
        return db.withTransaction { initializeInTransaction(); insertTracked(note) }
    }
    private suspend fun insertTracked(note: NoteEntity): Long {
        val dao = requireNotNull(history)
        val previous = if (note.id != 0) noteDao.getNoteById(note.id) else null
        check(note.id == 0 || previous != null) { "This note is no longer available" }
        val before = previous?.let { dao.note(it.id) }
        val changed = previous == null || previous.title != note.title || previous.content != note.content
        val tokens = if (changed) noteWordTokens(note) else emptyList()
        val words = if (changed) tokens.size else before?.currentWords ?: 0
        val additions = when {
            previous == null -> words.toLong()
            changed -> addedWords(noteWordTokens(previous), tokens)
            else -> 0L
        }
        val noteId = noteDao.insertNote(note)
        if (previous == null || previous.libraryState() != note.libraryState() || previous.lockPin != note.lockPin || previous.isLocked != note.isLocked) undoTokens.remove(noteId.toInt())
        val now = clock()
        val identity = before?.historyId ?: UUID.randomUUID().toString()
        dao.putNote(ProductivityNote(identity, noteId.toInt(), state(note), (before?.wordsAdded ?: 0) + additions,
            words, before?.createdAt ?: if (previous == null) now else null, null))
        if (previous == null) record(identity, "CREATED", additions, now)
        else {
            if (changed) record(identity, "EDITED", additions, now)
            if (state(previous) != state(note)) record(identity, when {
                previous.isTrashed -> "RESTORED"
                note.isTrashed -> "TRASHED"
                note.isArchived -> "ARCHIVED"
                else -> "UNARCHIVED"
            }, now = now)
        }
        return noteId
    }
    private suspend fun recordDeletion(note: NoteEntity) {
        val dao = requireNotNull(history)
        val entry = requireNotNull(dao.note(note.id))
        val now = clock()
        dao.putNote(entry.copy(liveNoteId = null, state = "DELETED", currentWords = 0, deletedAt = now))
        record(entry.historyId, "DELETED", now = now)
    }
    suspend fun delete(note: NoteEntity) {
        val db = database
        if (db == null) { noteDao.deleteNote(note); return }
        db.withTransaction {
            initializeInTransaction()
            val existing = noteDao.getNoteById(note.id) ?: return@withTransaction
            recordDeletion(existing)
            noteDao.deleteNote(existing)
            undoTokens.remove(existing.id)
        }
    }
    suspend fun emptyTrash() {
        val db = database
        if (db == null) { noteDao.emptyTrash(); return }
        db.withTransaction {
            initializeInTransaction()
            history!!.currentNotes().filter { it.isTrashed }.forEach { recordDeletion(it); undoTokens.remove(it.id) }
            noteDao.emptyTrash()
        }
    }
    suspend fun togglePin(id: Int, isPinned: Boolean) {
        val db = database
        if (db == null) { noteDao.updatePinnedStatus(id, isPinned); return }
        db.withTransaction {
            initializeInTransaction()
            noteDao.getNoteById(id)?.let { insertTracked(it.copy(isPinned = isPinned)) }
        }
    }

    private suspend fun <T> actionTransaction(block: suspend () -> T): T {
        actionMutex.lock()
        try { return if (database == null) block() else database.withTransaction { initializeInTransaction(); block() } }
        finally { actionMutex.unlock() }
    }
    suspend fun applyLibraryAction(id: Int, action: NoteLibraryAction, expected: LibraryState, pin: String? = null): NoteActionReceipt = actionTransaction {
        val latest = noteDao.getNoteById(id) ?: throw NoteActionConflictException()
        if (latest.libraryState() != expected) throw NoteActionConflictException()
        if (latest.isLocked && (pin == null || pin != latest.lockPin)) throw NoteActionLockedException()
        val next = when (action) {
            NoteLibraryAction.ARCHIVE -> latest.takeIf { expected.section == NoteSection.ACTIVE }?.copy(isArchived = true)
            NoteLibraryAction.UNARCHIVE -> latest.takeIf { expected.section == NoteSection.ARCHIVED }?.copy(isArchived = false)
            NoteLibraryAction.TRASH -> latest.takeIf { expected.section != NoteSection.TRASHED }?.copy(isTrashed = true)
            NoteLibraryAction.RESTORE -> latest.takeIf { expected.section == NoteSection.TRASHED }?.copy(isTrashed = false)
            NoteLibraryAction.DELETE -> null
        }
        if (action == NoteLibraryAction.DELETE) {
            if (expected.section != NoteSection.TRASHED) throw NoteActionConflictException()
            if (database != null) recordDeletion(latest)
            noteDao.deleteNote(latest)
            undoTokens.remove(id)
            NoteActionReceipt(id, latest.title, action, expected, null, null, null)
        } else {
            val updated = next ?: throw NoteActionConflictException()
            if (database == null) noteDao.insertNote(updated) else insertTracked(updated)
            val token = UUID.randomUUID().toString()
            undoTokens[id] = token
            NoteActionReceipt(id, latest.title, action, expected, updated.libraryState(), token, pin)
        }
    }
    suspend fun undoLibraryAction(receipt: NoteActionReceipt): Unit = actionTransaction {
        val latest = noteDao.getNoteById(receipt.noteId) ?: throw NoteActionConflictException()
        if (receipt.undoToken == null || undoTokens[receipt.noteId] != receipt.undoToken || latest.libraryState() != receipt.after) throw NoteActionConflictException()
        if (latest.isLocked && (receipt.authorizedPin == null || latest.lockPin != receipt.authorizedPin)) throw NoteActionLockedException()
        val restored = latest.copy(isArchived = receipt.before.archived, isTrashed = receipt.before.trashed)
        if (database == null) noteDao.insertNote(restored) else insertTracked(restored)
        undoTokens.remove(receipt.noteId)
    }
}
