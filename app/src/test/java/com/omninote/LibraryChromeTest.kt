package com.omninote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omninote.ui.components.*
import com.omninote.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.flow.flowOf
import com.omninote.data.*
import com.omninote.ui.viewmodels.NotesViewModel
import com.omninote.ui.screens.NotesListScreen
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryChromeTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @get:Rule val compose = createComposeRule()
    @Test fun directionThresholdPreventsFlickerAndShowsChromeAtTheTop() {
        val state = LibraryChromeState()
        state.scroll(-5f, false, 32f)
        assertTrue(state.expanded)
        state.scroll(-30f, false, 32f)
        assertFalse(state.expanded)
        state.scroll(5f, false, 32f)
        assertFalse(state.expanded)
        state.scroll(30f, false, 32f)
        assertTrue(state.expanded)
        state.scroll(-40f, false, 32f)
        state.scroll(1f, true, 32f)
        assertTrue(state.expanded)
    }
    @Test fun scrollingHidesChromeWhileSearchFilterCaptureAndNewNoteStayReachable() {
        val dao = FixedDao()
        val viewModel = NotesViewModel(NoteRepository(dao))
        var newNotes = 0
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme {
                    NotesListScreen(viewModel, onNavigateToAddNote = { newNotes++ }, onNavigateToEditNote = {})
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("note_library_grid").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("note_library_grid").performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Omni Note").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Omni Note").assertDoesNotExist()
        compose.onNodeWithText("Active").assertDoesNotExist()
        compose.onNodeWithContentDescription("Search notes").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sort & filter").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Quick note").assertIsDisplayed()
        compose.onNodeWithContentDescription("New note").performClick()
        compose.runOnIdle { assertEquals(1, newNotes) }
        compose.onNodeWithTag("note_library_grid").performTouchInput { swipeDown() }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Omni Note").assertIsDisplayed()
        compose.onNodeWithText("Active").assertIsDisplayed()
        compose.onNodeWithTag("note_library_grid").performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Show library navigation").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Omni Note").assertIsDisplayed()
        compose.onNodeWithText("Active").assertIsDisplayed()
    }
    private class FixedDao : NoteDao {
        private val notes = List(50) { NoteEntity(id = it + 1, title = "Regression note $it", content = "Body\nDetails", timestamp = (50 - it).toLong()) }
        override fun getAllNotes() = flowOf(notes)
        override fun getActiveNotes() = flowOf(notes)
        override fun getArchivedNotes() = flowOf(emptyList<NoteEntity>())
        override fun getTrashedNotes() = flowOf(emptyList<NoteEntity>())
        override suspend fun getNoteById(id: Int) = notes.find { it.id == id }
        override suspend fun insertNote(note: NoteEntity) = 1L
        override suspend fun deleteNote(note: NoteEntity) {}
        override suspend fun emptyTrash() {}
        override suspend fun updatePinnedStatus(id: Int, isPinned: Boolean) {}

    }
}
