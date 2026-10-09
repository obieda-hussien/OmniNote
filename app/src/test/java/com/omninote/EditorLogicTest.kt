package com.omninote

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Color
import com.omninote.ui.editor.EditorBuffer
import com.omninote.ui.components.NoteMarkdownParser
import com.omninote.ui.components.parseInlineStyles
import com.omninote.data.*
import com.omninote.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.commonmark.node.*
import org.commonmark.ext.gfm.tables.TableBlock
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditorLogicTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun formattingUsesSelectionAndUndoRestoresItImmediately() {
        val buffer = EditorBuffer("hello world")
        buffer.value = TextFieldValue(buffer.text, TextRange(6, 11))
        buffer.wrap("**")
        assertEquals("hello **world**", buffer.text)
        assertEquals(TextRange(8, 13), buffer.value.selection)
        buffer.undo()
        assertEquals("hello world", buffer.text)
        assertEquals(TextRange(6, 11), buffer.value.selection)
        buffer.redo()
        buffer.wrap("**")
        assertEquals("hello world", buffer.text)
        buffer.insert("earth")
        assertEquals("hello earth", buffer.text)
        assertFalse(buffer.canRedo)
    }
    @Test fun emptySelectionLeavesCaretInsideMarkupAndLineToolsRespectSelection() {
        val buffer = EditorBuffer("first\nsecond\nthird")
        buffer.value = TextFieldValue(buffer.text, TextRange(6, 12))
        buffer.prefixLines("- [ ] ")
        assertEquals("first\n- [ ] second\nthird", buffer.text)
        buffer.prefixLines("- [ ] ")
        assertEquals("first\nsecond\nthird", buffer.text)
        buffer.value = TextFieldValue(buffer.text, TextRange(3))
        buffer.wrap("*")
        assertEquals("fir**st\nsecond\nthird", buffer.text)
        assertEquals(TextRange(4), buffer.value.selection)
    }
    @Test fun checkingATaskChangesOnlyItsLeadingMarker() {
        val buffer = EditorBuffer("+ [X] keep literal - [x]\n3. [ ] ordered")
        buffer.setChecked(0, false)
        buffer.setChecked(1, true)
        assertEquals("+ [ ] keep literal - [x]\n3. [x] ordered", buffer.text)
        buffer.undo()
        assertEquals("+ [ ] keep literal - [x]\n3. [ ] ordered", buffer.text)
    }
    @Test fun markdownSupportsNestedStylesEscapesCodeAndBalancedLinks() {
        val inline = parseInlineStyles("***bold italic*** \\*literal\\* `**code**` [site](https://example.com/a_(b))", Color.Blue, Color.Gray)
        assertEquals("bold italic *literal* **code** site", inline.text)
        assertTrue(inline.spanStyles.any { it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold })
        assertTrue(inline.spanStyles.any { it.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic })
        assertEquals("https://example.com/a_(b)", inline.getStringAnnotations("URL", 0, inline.length).single().item)
        assertEquals("marked red words [color:#ff0000](literal)",
            parseInlineStyles("==marked== [color:#ff0000](red **words**) `[color:#ff0000](literal)`", Color.Blue, Color.Gray).text)
    }
    @Test fun documentParserHandlesNestedListsTablesAndTildeFences() {
        val document = NoteMarkdownParser.parse("#### Small heading\n\n1. first\n   - nested\n\n~~~text\n- [ ] literal code\n~~~\n\n| A | B |\n| --- | --- |\n| one | two |")
        val nodes = mutableListOf<Node>()
        fun visit(node: Node) { nodes.add(node); var child = node.firstChild; while (child != null) { visit(child); child = child.next } }
        visit(document)
        assertTrue(nodes.any { it is Heading && it.level == 4 })
        assertTrue(nodes.any { it is OrderedList })
        assertTrue(nodes.any { it is BulletList })
        assertTrue(nodes.any { it is TableBlock })
        assertEquals("- [ ] literal code\n", nodes.filterIsInstance<FencedCodeBlock>().single().literal)
    }
    @Test fun concurrentSavesCreateOneDraftAndRetainPinAndMetadata() = runTest {
        val dao = MemoryDao()
        val vm = NotesViewModel(NoteRepository(dao))
        val draft = vm.editorDraft(null)
        draft.title = "Draft"
        draft.isPinned = true
        val first = async { vm.saveDraft(draft) }
        val second = async { vm.saveDraft(draft) }
        first.await(); second.await()
        assertEquals(1, dao.rows.value.size)
        assertTrue(dao.rows.value.single().isPinned)
        draft.buffer.text = "Updated"
        vm.saveDraft(draft)
        assertEquals(1, dao.rows.value.size)
        assertEquals("Updated", dao.rows.value.single().content)
        assertFalse(draft.dirty)
    }
    @Test fun clearingAnExistingNotePersistsAndTrashCannotBeResurrectedByAutosave() = runTest {
        val dao = MemoryDao()
        val original = NoteEntity(id = 7, title = "Old", content = "Body", isArchived = true)
        dao.insertNote(original)
        val vm = NotesViewModel(NoteRepository(dao))
        val draft = vm.editorDraft(7)
        vm.loadDraft(draft)
        draft.title = ""; draft.buffer.text = ""
        vm.saveDraft(draft)
        assertEquals("", dao.rows.value.single().content)
        assertTrue(dao.rows.value.single().isArchived)
        vm.trashDraft(draft)
        draft.buffer.text = "late update"
        vm.saveDraft(draft)
        assertTrue(dao.rows.value.single().isTrashed)
        assertEquals("", dao.rows.value.single().content)
    }
    @Test fun failedSaveKeepsDraftAndCanBeRetried() = runTest {
        val dao = MemoryDao()
        val vm = NotesViewModel(NoteRepository(dao))
        val draft = vm.editorDraft(null)
        draft.buffer.text = "Keep me"
        dao.fail = true
        assertTrue(runCatching { vm.saveDraft(draft) }.isFailure)
        assertEquals("Keep me", draft.buffer.text)
        assertTrue(draft.dirty)
        assertNotNull(draft.error)
        dao.fail = false
        vm.saveDraft(draft)
        assertNull(draft.error)
        assertFalse(draft.dirty)
    }
    private class MemoryDao : NoteDao {
        val rows = MutableStateFlow(emptyList<NoteEntity>())
        var fail = false
        override fun getAllNotes() = rows
        override fun getActiveNotes() = rows.map { it.filter { n -> !n.isArchived && !n.isTrashed } }
        override fun getArchivedNotes() = rows.map { it.filter { n -> n.isArchived && !n.isTrashed } }
        override fun getTrashedNotes() = rows.map { it.filter { n -> n.isTrashed } }
        override suspend fun getNoteById(id: Int) = rows.value.find { it.id == id }
        override suspend fun insertNote(note: NoteEntity): Long {
            delay(1)
            if (fail) error("disk unavailable")
            val id = note.id.takeIf { it != 0 } ?: ((rows.value.maxOfOrNull { it.id } ?: 0) + 1)
            rows.value = rows.value.filterNot { it.id == id } + note.copy(id = id)
            return id.toLong()
        }
        override suspend fun deleteNote(note: NoteEntity) { rows.value = rows.value.filterNot { it.id == note.id } }
        override suspend fun emptyTrash() { rows.value = rows.value.filterNot { it.isTrashed } }
        override suspend fun updatePinnedStatus(id: Int, isPinned: Boolean) { rows.value = rows.value.map { if (it.id == id) it.copy(isPinned = isPinned) else it } }
    }
}
