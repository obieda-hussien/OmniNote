package com.omninote

import com.omninote.data.*
import com.omninote.ui.viewmodels.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class NoteActionEventsTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun duplicateActionsAreIgnoredAndFailuresReportRetryWithoutFalseSuccess() = runTest {
        val dao = ControlledDao()
        val vm = NotesViewModel(NoteRepository(dao))
        var event = async(UnconfinedTestDispatcher(testScheduler)) { vm.noteEvent.first() }
        val original = dao.note
        vm.archiveNote(original)
        vm.archiveNote(original)
        assertTrue(original.id in vm.pendingActions.value)
        dao.gate.complete(Unit)
        assertTrue(event.await() is NoteEvent.ActionCompleted)
        advanceUntilIdle()
        assertEquals(1, dao.writes)
        assertTrue(dao.note.isArchived)
        assertTrue(vm.pendingActions.value.isEmpty())
        dao.fail = true
        event = async(UnconfinedTestDispatcher(testScheduler)) { vm.noteEvent.first() }
        vm.moveToTrash(dao.note)
        val failed = event.await() as NoteEvent.Failed
        advanceUntilIdle()
        assertFalse(dao.note.isTrashed)
        assertTrue(vm.pendingActions.value.isEmpty())
        assertNotNull(failed.retry)
        dao.fail = false
        event = async(UnconfinedTestDispatcher(testScheduler)) { vm.noteEvent.first() }
        failed.retry!!()
        assertTrue(event.await() is NoteEvent.ActionCompleted)
        advanceUntilIdle()
        assertTrue(dao.note.isTrashed)
        assertEquals(3, dao.writes)
    }
    private class ControlledDao : NoteDao {
        var note = NoteEntity(id = 1, title = "Keep", content = "Body")
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        var fail = false
        override fun getAllNotes() = flowOf(listOf(note))
        override fun getActiveNotes() = flowOf(listOf(note))
        override fun getArchivedNotes() = flowOf(emptyList<NoteEntity>())
        override fun getTrashedNotes() = flowOf(emptyList<NoteEntity>())
        override suspend fun getNoteById(id: Int): NoteEntity { gate.await(); return note }
        override suspend fun insertNote(note: NoteEntity): Long {
            writes++
            if (fail) throw java.io.IOException("Storage unavailable")
            this.note = note
            return note.id.toLong()
        }
        override suspend fun deleteNote(note: NoteEntity) {}
        override suspend fun emptyTrash() {}
        override suspend fun updatePinnedStatus(id: Int, isPinned: Boolean) {}
    }
}
