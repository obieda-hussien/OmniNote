package com.omninote

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.omninote.ui.components.NoteLibraryHeader
import com.omninote.ui.components.OmniSheet
import com.omninote.ui.components.noteSurface
import com.omninote.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class NoteDesignTest {
    @get:Rule val compose = createComposeRule()

    @Test fun semanticModifiersKeepEveryToolbarGlyphAtItsIntendedSize() {
        compose.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Row {
                    com.omninote.ui.screens.CanvasCustomIcon(com.omninote.ui.screens.CanvasIconType.BACK,
                        androidx.compose.ui.Modifier.semantics { contentDescription = "Back glyph" })
                    com.omninote.ui.screens.CanvasCustomIcon(com.omninote.ui.screens.CanvasIconType.VISIBILITY,
                        androidx.compose.ui.Modifier.semantics { contentDescription = "Preview glyph" })
                }
            }
        }
        compose.onNodeWithContentDescription("Back glyph").assertWidthIsEqualTo(24.dp).assertHeightIsEqualTo(24.dp)
        compose.onNodeWithContentDescription("Preview glyph").assertWidthIsEqualTo(24.dp).assertHeightIsEqualTo(24.dp)
    }

    @Test fun customNoteColoursKeepReadableTextInBothThemes() {
        val contrasts = mutableListOf<Float>()
        compose.setContent {
            for (dark in listOf(false, true)) {
                MyApplicationTheme(darkTheme = dark, dynamicColor = false) {
                    for (hex in listOf(null, "#FFFFFF", "#000000", "#2D26A0", "#FFF0CC", "#8B2060")) {
                        val text = MaterialTheme.colorScheme.onSurface.luminance()
                        val background = noteSurface(hex).luminance()
                        contrasts.add((maxOf(text, background) + 0.05f) / (minOf(text, background) + 0.05f))
                    }
                }
            }
        }
        compose.runOnIdle { assertTrue(contrasts.all { it >= 4.5f }) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun longToolSheetKeepsLastActionReachable() {
        compose.setContent {
            MyApplicationTheme {
                OmniSheet(onDismissRequest = {}) {
                    Column {
                        repeat(40) { Text("Tool $it", style = MaterialTheme.typography.headlineLarge) }
                        Button(onClick = {}) { Text("Finish") }
                    }
                }
            }
        }
        compose.onNodeWithText("Finish").performScrollTo().assertIsDisplayed().assertHasClickAction()
    }

    @Test fun previewCheckboxKeepsItsOriginalSourceLine() {
        var clickedLine = -1
        var checkedValue = false
        compose.setContent {
            MyApplicationTheme {
                com.omninote.ui.components.MarkdownDocumentPreview(
                    title = "Workout",
                    rawText = "# Plan\n\nSome text\n\n+ [ ] first task\n+ [X] finished task",
                    onCheckedChange = { line, checked -> clickedLine = line; checkedValue = checked }
                )
            }
        }
        compose.waitUntil(10000) { compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size == 2 }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.runOnIdle { assertTrue(clickedLine == 4 && checkedValue) }
    }

    @Test fun searchLayoutAndFilterActionsAreDiscoverable() {
        var cleared = false
        var switched = false
        var filtered = false
        compose.setContent {
            MyApplicationTheme {
                NoteLibraryHeader("Omni Note", "12 notes", "meeting",
                    onQueryChange = { cleared = it.isEmpty() },
                    grid = true, onLayoutChange = { switched = true },
                    onFilter = { filtered = true }, onStats = {})
            }
        }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("List view").performClick()
        compose.onNodeWithText("Sort & filter").performClick()
        compose.runOnIdle { assertTrue(cleared && switched && filtered) }
    }
}
