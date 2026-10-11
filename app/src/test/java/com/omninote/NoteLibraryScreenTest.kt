package com.omninote

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omninote.data.*
import com.omninote.ui.screens.NotesListScreen
import com.omninote.ui.theme.MyApplicationTheme
import com.omninote.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp")
@OptIn(ExperimentalCoroutinesApi::class)
class NoteLibraryScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("OmniNotePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }
    @After fun cleanup() { db.close(); Dispatchers.resetMain() }
    @Test fun gridAndListSwipesCommitCorrectSectionsAndCancelledDeletionKeepsTheNote() {
        val repo = NoteRepository(db.noteDao(), db)
        val id = runBlocking { repo.insert(NoteEntity(title = "Swipe me", content = "Body")).toInt() }
        val vm = NotesViewModel(repo)
        lateinit var host: android.view.View
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MyApplicationTheme {
                host = LocalView.current
                NotesListScreen(vm, {}, {})
            } }
        }
        compose.runOnIdle { androidx.core.view.ViewCompat.dispatchApplyWindowInsets(host,
            androidx.core.view.WindowInsetsCompat.Builder().setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), false).build()) }
        fun swipe(right: Boolean) {
            compose.waitUntil(10000) { compose.onAllNodesWithTag("note_swipe_$id").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("note_swipe_$id").performTouchInput {
                swipe(Offset(width * if (right) 0.1f else 0.9f, height / 2f), Offset(width * if (right) 0.9f else 0.1f, height / 2f), 600)
            }
            compose.mainClock.advanceTimeBy(1000)
        }
        swipe(true)
        compose.waitUntil(10000) { runBlocking { repo.getNoteById(id)!!.isArchived } }
        compose.onNode(hasText("Archive") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("List view").performClick()
        swipe(false)
        compose.waitUntil(10000) { runBlocking { repo.getNoteById(id)!!.isTrashed } }
        compose.onNode(hasText("Trash") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(500)
        swipe(false)
        compose.onNodeWithText("Delete Permanently").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertNotNull(runBlocking { repo.getNoteById(id) })
        swipe(true)
        compose.waitUntil(10000) { runBlocking { !repo.getNoteById(id)!!.isTrashed } }
        val restored = runBlocking { repo.getNoteById(id)!! }
        assertTrue(restored.isArchived)
        assertEquals("Body", restored.content)
    }
}
