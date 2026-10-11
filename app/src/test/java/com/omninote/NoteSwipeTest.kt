package com.omninote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.omninote.data.*
import com.omninote.ui.components.*
import com.omninote.ui.theme.MyApplicationTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp")
class NoteSwipeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun physicalDirectionsUseTheNoteStateInLtrAndRtl() {
        var direction by mutableStateOf(LayoutDirection.Ltr)
        var note by mutableStateOf(NoteEntity(id = 1, title = "Note", content = ""))
        val actions = mutableListOf<NoteLibraryAction>()
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MyApplicationTheme { NoteSwipeCard(note, true, { actions.add(it) }) { Text("Note", Modifier.fillMaxWidth().height(100.dp)) } }
            }
        }
        for (layout in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            for (section in NoteSection.entries) {
                compose.runOnIdle { direction = layout; note = note.copy(isArchived = section == NoteSection.ARCHIVED, isTrashed = section == NoteSection.TRASHED) }
                for (right in listOf(true, false)) {
                    val before = actions.size
                    compose.onNodeWithTag("note_swipe_1").performTouchInput {
                        swipe(Offset(width * if (right) 0.1f else 0.9f, height / 2f), Offset(width * if (right) 0.9f else 0.1f, height / 2f), 600)
                    }
                    compose.mainClock.advanceTimeBy(1000)
                    compose.runOnIdle {
                        assertEquals(before + 1, actions.size)
                        assertEquals(swipeAction(note, right), actions.last())
                    }
                }
            }
        }
    }
    @Test fun shortFastVerticalAndDisabledGesturesNeverDispatchActions() {
        var enabled by mutableStateOf(true)
        var actions = 0
        compose.setContent { MyApplicationTheme {
            NoteSwipeCard(NoteEntity(id = 1, title = "Note", content = ""), enabled, { actions++ }) { Text("Note", Modifier.fillMaxWidth().height(150.dp)) }
        } }
        compose.onNodeWithTag("note_swipe_1").performTouchInput { swipe(Offset(width * 0.4f, height / 2f), Offset(width * 0.55f, height / 2f), 50) }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("note_swipe_1").performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("note_swipe_1").performTouchInput {
            down(Offset(width * 0.1f, height / 2f))
            moveTo(Offset(width * 0.9f, height / 2f))
            cancel()
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { enabled = false }
        compose.onNodeWithTag("note_swipe_1").performTouchInput { swipeRight() }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(0, actions) }
    }
}
