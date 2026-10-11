package com.omninote

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omninote.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NoteLibraryActionTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: NoteRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = NoteRepository(db.noteDao(), db)
    }
    @After fun cleanup() { db.close() }
    private suspend fun create(archived: Boolean = false, trashed: Boolean = false, locked: Boolean = false): NoteEntity {
        val note = NoteEntity(title = "Title", content = "Original text", isArchived = archived, isTrashed = trashed,
            isLocked = locked, lockPin = if (locked) "1234" else null)
        return note.copy(id = repo.insert(note).toInt())
    }
    @Test fun actionAndUndoPreserveNewerTextAndMetadata() = runBlocking {
        val stale = create()
        repo.insert(stale.copy(title = "Latest", content = "New text", tags = "Work", isPinned = true))
        val receipt = repo.applyLibraryAction(stale.id, NoteLibraryAction.ARCHIVE, stale.libraryState())
        var actual = repo.getNoteById(stale.id)!!
        assertEquals("New text", actual.content)
        assertTrue(actual.isPinned && actual.isArchived)
        repo.insert(actual.copy(content = "Even newer text", colorHex = "#8E8DE5"))
        repo.undoLibraryAction(receipt)
        actual = repo.getNoteById(stale.id)!!
        assertEquals("Even newer text", actual.content)
        assertEquals("Work", actual.tags)
        assertEquals("#8E8DE5", actual.colorHex)
        assertFalse(actual.isArchived)
        assertEquals(1, repo.productivity.first { it.ready }.totalNotes)
    }
    @Test fun restoreAndUndoReturnToTheOriginalArchiveAndTrashStates() = runBlocking {
        val note = create(archived = true)
        repo.applyLibraryAction(note.id, NoteLibraryAction.TRASH, note.libraryState())
        val trashed = repo.getNoteById(note.id)!!
        val receipt = repo.applyLibraryAction(note.id, NoteLibraryAction.RESTORE, trashed.libraryState())
        assertTrue(repo.getNoteById(note.id)!!.isArchived)
        assertFalse(repo.getNoteById(note.id)!!.isTrashed)
        repo.undoLibraryAction(receipt)
        assertTrue(repo.getNoteById(note.id)!!.isArchived && repo.getNoteById(note.id)!!.isTrashed)
    }
    @Test fun oldUndoCannotChangeNewerActionsEvenWhenStatesMatchAgain() = runBlocking {
        val note = create()
        val old = repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState())
        repo.undoLibraryAction(old)
        repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState())
        assertTrue(runCatching { repo.undoLibraryAction(old) }.exceptionOrNull() is NoteActionConflictException)
        assertTrue(repo.getNoteById(note.id)!!.isArchived)
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.TRASH, note.libraryState()) }.exceptionOrNull() is NoteActionConflictException)
    }
    @Test fun lockedActionsRecheckTheCurrentPinAndInvalidateOldAuthorization() = runBlocking {
        val note = create(locked = true)
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState()) }.exceptionOrNull() is NoteActionLockedException)
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState(), "0000") }.exceptionOrNull() is NoteActionLockedException)
        val receipt = repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState(), "1234")
        val actual = repo.getNoteById(note.id)!!
        repo.insert(actual.copy(lockPin = "4321"))
        assertTrue(runCatching { repo.undoLibraryAction(receipt) }.isFailure)
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.TRASH, actual.libraryState(), "1234") }.exceptionOrNull() is NoteActionLockedException)
    }
    @Test fun failedHistoryWriteRollsBackTheMoveAndDoesNotInventAnActivity() = runBlocking {
        val note = create()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_activity BEFORE INSERT ON productivity_activity BEGIN SELECT RAISE(ABORT, 'unavailable'); END")
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.ARCHIVE, note.libraryState()) }.isFailure)
        assertFalse(repo.getNoteById(note.id)!!.isArchived)
        val result = repo.productivity.first { it.ready }
        assertEquals(1, result.activeNotes)
        assertEquals(0, result.days.sumOf { it.archived })
    }
    @Test fun permanentDeletionRequiresTrashAndCannotBeUndoneOrRepeated() = runBlocking {
        var note = create()
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.DELETE, note.libraryState()) }.isFailure)
        repo.applyLibraryAction(note.id, NoteLibraryAction.TRASH, note.libraryState())
        note = repo.getNoteById(note.id)!!
        val receipt = repo.applyLibraryAction(note.id, NoteLibraryAction.DELETE, note.libraryState())
        assertNull(receipt.undoToken)
        assertTrue(runCatching { repo.undoLibraryAction(receipt) }.isFailure)
        assertTrue(runCatching { repo.applyLibraryAction(note.id, NoteLibraryAction.DELETE, note.libraryState()) }.isFailure)
        assertNull(repo.getNoteById(note.id))
        val result = repo.productivity.first { it.ready }
        assertEquals(1, result.totalNotes)
        assertEquals(1, result.deletedNotes)
    }
}
