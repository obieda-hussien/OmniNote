package com.omni.noteplus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omni.noteplus.data.*
import com.omni.noteplus.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ProductivityHistoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepository
    private var time = 1000L
    private var day = "2026-10-11"
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = NoteRepository(database.noteDao(), database, { time }, { day })
    }
    @After fun cleanup() { database.close() }
    private suspend fun snapshot() = repository.productivity.first { it.ready }
    private suspend fun create(content: String = "one two") = repository.insert(NoteEntity(title = "", content = content)).toInt()

    @Test fun savesMetadataArchiveRestoreAndDuplicateDeletionNeverInflateNoteCount() = runBlocking {
        val id = create()
        var note = repository.getNoteById(id)!!
        repeat(3) { repository.insert(note) }
        repository.togglePin(id, true)
        note = repository.getNoteById(id)!!
        repository.insert(note.copy(isArchived = true))
        repository.insert(note.copy(isArchived = true, isTrashed = true))
        repository.insert(note.copy(isArchived = true, isTrashed = false))
        repository.insert(note.copy(isArchived = false, isTrashed = false))
        assertEquals(1, snapshot().totalNotes)
        assertEquals(2L, snapshot().recordedWords)
        assertEquals(1, snapshot().activeNotes)
        repository.delete(note)
        repository.delete(note)
        val result = snapshot()
        assertEquals(1, result.totalNotes)
        assertEquals(1, result.deletedNotes)
        assertEquals(2L, result.recordedWords)
        assertEquals(0, result.activeNotes)
        assertEquals(1, result.days.single().deleted)
        assertEquals(1, result.days.single().restored)
        assertEquals(1, result.days.single().trashed)
        assertTrue(runCatching { repository.insert(note) }.isFailure)
    }
    @Test fun baselineIsSeededOnceWithoutInventingCreationDatesOrActivity() = runBlocking {
        database.noteDao().insertNote(NoteEntity(title = "Old", content = "note", isArchived = true))
        database.noteDao().insertNote(NoteEntity(title = "Locked", content = "note", isLocked = true))
        repository.initializeHistory()
        repository.initializeHistory()
        val result = snapshot()
        assertEquals(2, result.totalNotes)
        assertEquals(2, result.tracking!!.baselineNotes)
        assertEquals(4L, result.recordedWords)
        assertTrue(result.records.all { it.createdAt == null })
        assertTrue(result.days.isEmpty())
    }
    @Test fun emptyTrashPreservesCountersAndActiveArchiveNotes() = runBlocking {
        val active = create()
        val archive = create("three four five")
        repository.insert(repository.getNoteById(archive)!!.copy(isArchived = true))
        val trashed = create("six")
        repository.insert(repository.getNoteById(trashed)!!.copy(isTrashed = true))
        repository.emptyTrash()
        repository.emptyTrash()
        val result = snapshot()
        assertEquals(3, result.totalNotes)
        assertEquals(1, result.deletedNotes)
        assertEquals(6L, result.recordedWords)
        assertEquals(1, result.activeNotes)
        assertEquals(1, result.archivedNotes)
        assertEquals(0, result.trashedNotes)
        assertNotNull(repository.getNoteById(active))
        assertNotNull(repository.getNoteById(archive))
    }
    @Test fun writingHistoryCoalescesAutosavesAndRetainsRemovedWords() = runBlocking {
        val id = create()
        var note = repository.getNoteById(id)!!
        repository.insert(note.copy(content = "one two three"))
        note = repository.getNoteById(id)!!
        repository.insert(note.copy(content = "one two three four"))
        repository.insert(note.copy(content = "one two three four"))
        repository.insert(note.copy(content = "one"))
        var result = snapshot()
        assertEquals(4L, result.recordedWords)
        assertEquals(1L, result.currentWords)
        assertEquals(1, result.days.single().workedOn)
        assertEquals(4L, result.days.single().wordsAdded)
        day = "2026-10-12"; time += 86400000L
        repository.insert(repository.getNoteById(id)!!.copy(content = "one five"))
        result = snapshot()
        assertEquals(5L, result.recordedWords)
        assertEquals(2, result.writingDays)
        assertEquals(2, writingStreak(result.days.map { it.day }.toSet(), day))
    }
    @Test fun transactionFailureRollsBackBothNoteAndHistory() = runBlocking {
        repository.initializeHistory()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_history BEFORE INSERT ON productivity_notes BEGIN SELECT RAISE(ABORT, 'unavailable'); END")
        assertTrue(runCatching { create() }.isFailure)
        assertTrue(database.productivityDao().currentNotes().isEmpty())
        assertEquals(0, snapshot().totalNotes)
        assertTrue(snapshot().days.isEmpty())
    }
    @Test fun deletedHistorySurvivesDatabaseReopenWithoutKeepingNoteText() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "history-reopen-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*DatabaseMigrations.all).build()
        var db = open()
        try {
            var repo = NoteRepository(db.noteDao(), db)
            val id = repo.insert(NoteEntity(title = "Private title", content = "private content")).toInt()
            repo.delete(repo.getNoteById(id)!!)
            db.close()
            db = open(); repo = NoteRepository(db.noteDao(), db)
            repo.initializeHistory()
            val result = repo.productivity.first { it.ready }
            assertEquals(1, result.totalNotes)
            assertEquals(1, result.deletedNotes)
            assertTrue(db.productivityDao().currentNotes().isEmpty())
            assertFalse(result.records.toString().contains("Private title"))
            assertFalse(result.records.toString().contains("private content"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun migrationFromEveryOldVersionPreservesNotesAndStartsFreshHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (version in 1..3) {
            val name = "migration-$version.db"
            context.deleteDatabase(name)
            val raw = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null)
            raw.execSQL("""CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL,
                content TEXT NOT NULL, timestamp INTEGER NOT NULL, isPinned INTEGER NOT NULL, colorHex TEXT,
                tags TEXT NOT NULL, isArchived INTEGER NOT NULL, isTrashed INTEGER NOT NULL, isLocked INTEGER NOT NULL, lockPin TEXT)""")
            raw.execSQL("INSERT INTO notes VALUES (7, 'Keep title', 'Keep content', 99, 1, '#8E8DE5', 'Work', 1, 0, 1, '1234')")
            raw.version = version; raw.close()
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*DatabaseMigrations.all).build()
            try {
                val note = migrated.noteDao().getNoteById(7)!!
                assertEquals("Keep content", note.content)
                assertTrue(note.isArchived && note.isPinned && note.isLocked)
                assertEquals("1234", note.lockPin)
                val repo = NoteRepository(migrated.noteDao(), migrated)
                repo.initializeHistory()
                assertEquals(1, repo.productivity.first { it.ready }.totalNotes)
            } finally { migrated.close(); context.deleteDatabase(name) }
        }
    }
    @Test fun migrationSuppliesMissingLegacyColumnsWithoutDroppingContent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "minimal-legacy.db"
        context.deleteDatabase(name)
        val raw = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null)
        raw.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        raw.execSQL("INSERT INTO notes VALUES (12, 'Legacy', 'Still here', 321)")
        raw.version = 1; raw.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*DatabaseMigrations.all).build()
        try {
            val note = migrated.noteDao().getNoteById(12)!!
            assertEquals("Still here", note.content)
            assertEquals(321L, note.timestamp)
            assertFalse(note.isArchived || note.isTrashed || note.isLocked)
            assertEquals("", note.tags)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
    @Test fun markdownWordCountingAndStreaksAvoidMarkupAndGaps() {
        val note = NoteEntity(title = "عنوان", content = "# Hello **world**\n\n- [x] مهم\n\n![Photo](file:///a.jpg) [voice:Recording](file:///a.mp3)\n\n```\ncode words\n```\n\nhttps://example.com")
        assertEquals(listOf("عنوان", "hello", "world", "مهم"), noteWordTokens(note))
        assertEquals(listOf("hello", "world"), noteWordTokens(note.copy(title = "", content = "[color:#FFFFFF](Hello) [bg:#000000](world)")))
        assertEquals(0L, addedWords(listOf("one", "two"), listOf("two", "one")))
        assertEquals(1L, addedWords(listOf("one"), listOf("one", "one")))
        assertEquals(2, writingStreak(setOf("2026-10-09", "2026-10-10"), "2026-10-11"))
        assertEquals(0, writingStreak(setOf("2026-10-08"), "2026-10-11"))
    }
}
