package com.omninote

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omninote.data.*
import com.omninote.ui.screens.NotesListScreen
import com.omninote.ui.theme.MyApplicationTheme
import com.omninote.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProductivityScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AppDatabase
    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    }
    @After fun cleanup() { database.close(); Dispatchers.resetMain() }

    @Test fun dashboardUsesGlobalHistoryAcrossSectionsAndSearchAfterPermanentDeletion() {
        val repository = NoteRepository(database.noteDao(), database)
        runBlocking {
            repository.insert(NoteEntity(title = "Active example", content = ""))
            repository.insert(NoteEntity(title = "Archived example", content = "", isArchived = true))
            repository.insert(NoteEntity(title = "Trashed example", content = "", isTrashed = true))
            val id = repository.insert(NoteEntity(title = "Deleted example", content = "")).toInt()
            repository.delete(repository.getNoteById(id)!!)
        }
        val viewModel = NotesViewModel(repository)
        lateinit var host: android.view.View
        compose.setContent { MyApplicationTheme {
            host = LocalView.current
            NotesListScreen(viewModel, onNavigateToAddNote = {}, onNavigateToEditNote = {})
        } }
        compose.runOnIdle {
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(host,
                androidx.core.view.WindowInsetsCompat.Builder()
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                    .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), false).build())
        }
        compose.mainClock.advanceTimeBy(500)
        fun checkDashboard() {
            compose.onNodeWithContentDescription("Your productivity — all notes").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Notes recorded").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("4") and hasAnySibling(hasText("Notes recorded")), useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("All notes · every section · saved history").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close productivity").performClick()
            compose.mainClock.advanceTimeBy(500)
        }
        checkDashboard()
        compose.onNode(hasText("Archive") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(500)
        checkDashboard()
        compose.onNode(hasText("Trash") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(500)
        checkDashboard()
        compose.onNodeWithContentDescription("Search notes").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("no matching note")
        compose.mainClock.advanceTimeBy(500)
        checkDashboard()
    }
}
