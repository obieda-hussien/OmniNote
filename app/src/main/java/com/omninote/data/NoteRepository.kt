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
        }
    }
    suspend fun emptyTrash() {
        val db = database
        if (db == null) { noteDao.emptyTrash(); return }
        db.withTransaction {
            initializeInTransaction()
            history!!.currentNotes().filter { it.isTrashed }.forEach { recordDeletion(it) }
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
}
