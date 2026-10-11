package com.omninote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omninote.ui.components.*
import com.omninote.ui.theme.MyApplicationTheme
import org.commonmark.node.*
import org.commonmark.ext.gfm.tables.TableBlock
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp")
class MarkdownRegressionTest {
    @get:Rule val compose = createComposeRule()
    private fun descendants(node: Node): List<Node> = buildList {
        add(node)
        var child = node.firstChild
        while (child != null) { addAll(descendants(child)); child = child.next }
    }
    @Test fun adjacentImagesAndVoiceInAParagraphRemainMediaWithSurroundingText() {
        val source = "Before ![First](file:///one.jpg)\n![Second](content://images/2)\n[voice:Recording](file:///clip.m4a) after"
        val paragraph = NoteMarkdownParser.parse(source).firstChild
        val parts = paragraphParts(paragraph)
        assertEquals(2, parts.filterIsInstance<ParagraphPart.Photo>().size)
        assertEquals("file:///clip.m4a", parts.filterIsInstance<ParagraphPart.Audio>().single().uri)
        assertEquals(2, parts.filterIsInstance<ParagraphPart.TextRun>().size)
        assertEquals("First", parts.filterIsInstance<ParagraphPart.Photo>().first().description)
    }
    @Test fun allLegacyAudioLabelsAndFilesRenderButCodeStaysLiteral() {
        val source = "[audio](file:///one.mp3)\n[voice:Voice Recording](file:///two.m4a)\n[file:Report.pdf](content://files/1)\n`![literal](file:///x.jpg)`"
        val parts = paragraphParts(NoteMarkdownParser.parse(source).firstChild)
        assertEquals(2, parts.filterIsInstance<ParagraphPart.Audio>().size)
        assertEquals("Report.pdf", parts.filterIsInstance<ParagraphPart.File>().single().label)
        assertTrue(parts.none { it is ParagraphPart.Photo })
    }
    @Test fun pastedArabicTableAfterListAndShortSeparatorKeepsTaskSourceMapping() {
        val source = "1. خريطة المكملات\n| المكمل | الجرعة | الموعد |\n|--|--|\n| كرياتين | 5 جرام | يومياً |\n- [ ] التمرين"
        val document = NoteMarkdownParser.parseDocument(source)
        val nodes = descendants(document.root)
        val table = nodes.filterIsInstance<TableBlock>().single()
        assertEquals(3, table.firstChild.firstChild.childrenCount())
        val task = nodes.filterIsInstance<Paragraph>().first { it.sourceSpans.firstOrNull()?.let { span -> document.lines[span.lineIndex].contains("[ ]") } == true }
        assertEquals(4, document.originalLine(task.sourceSpans.first().lineIndex))
        assertEquals("|--|--|", source.split('\n')[2])
    }
    @Test fun validThreeColumnPastedTableAlsoWorksWithoutBlankLine() {
        val source = "1. Supplement map\n| A | B | C |\n|---|---|---|\n| one | two | three |"
        assertEquals(1, descendants(NoteMarkdownParser.parseDocument(source).root).filterIsInstance<TableBlock>().size)
    }
    @Test fun tableRepairNeverChangesCodeAndEscapedOrInlineCodePipesAreNotColumns() {
        val source = "~~~md\n| A | B | C |\n|--|--|\n~~~\n\n| A\\|B | `C|D` |\n| --- | --- |\n| left | right |"
        val document = NoteMarkdownParser.parseDocument(source)
        val nodes = descendants(document.root)
        assertEquals("| A | B | C |\n|--|--|\n", nodes.filterIsInstance<FencedCodeBlock>().single().literal)
        assertEquals(2, nodes.filterIsInstance<TableBlock>().single().firstChild.firstChild.childrenCount())
    }
    @Test fun previewActuallyDisplaysMixedPhotoAudioAndFileCards() {
        compose.setContent {
            MyApplicationTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    MarkdownContent(rawText = "Before ![Photo](file:///one.jpg)\n[voice:Voice clip](file:///clip.m4a)\n[file:Report.pdf](content://files/1) after")
                }
            }
        }
        compose.onNodeWithContentDescription("Photo").assertIsDisplayed()
        compose.onNodeWithText("Voice clip").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").assertHasClickAction()
        compose.onNodeWithText("Report.pdf").performScrollTo().assertIsDisplayed()
    }
    @Test fun repairedTableAndTaskAreInteractiveInActualRtlPreview() {
        var line = -1
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme {
                    MarkdownDocumentPreview("", "1. خطة\n| المكمل | الجرعة | الموعد |\n|--|--|\n| كرياتين | 5 جرام | يومياً |\n- [ ] تمرين", onCheckedChange = { index, _ -> line = index })
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Table").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Table").assertIsDisplayed()
        compose.onNodeWithText("1 rows · 3 columns").assertIsDisplayed()
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.runOnIdle { assertEquals(4, line) }
    }
    private fun Node.childrenCount(): Int {
        var count = 0; var child = firstChild
        while (child != null) { count++; child = child.next }
        return count
    }
    @Test fun genericFileLinksDetectImagesAndAudioUsingRealPathsOrNames() {
        val source = "[file:IMG_2026-10-11-08-17-23-559.jpg](file:///data/user/0/com.aistudio.omninote.premium/files/1791718548206_IMG_2026-10-11-08-17-23-559.jpg)\n[file:Track.M4A](content://files/21)\n[file:Archive.pdf](file:///archive.pdf)\n`[file:literal.jpg](file:///x.jpg)`"
        val parts = paragraphParts(NoteMarkdownParser.parse(source).firstChild)
        assertEquals(1, parts.filterIsInstance<ParagraphPart.Photo>().size)
        assertEquals(1, parts.filterIsInstance<ParagraphPart.Audio>().size)
        assertEquals(1, parts.filterIsInstance<ParagraphPart.File>().size)
        assertEquals(setOf(AttachmentKind.IMAGE, AttachmentKind.AUDIO, AttachmentKind.FILE), NoteMarkdownParser.cardPreview(source).media)
        assertEquals(AttachmentKind.AUDIO, attachmentKind("content://opaque/1", "unknown", "audio/mpeg"))
        assertEquals(AttachmentKind.FILE, attachmentKind("file:///real.pdf", "wrong.jpg"))
        assertEquals(AttachmentKind.FILE, attachmentKind("content://opaque/1", "wrong.jpg", "application/pdf"))
        assertEquals(AttachmentKind.IMAGE, attachmentKind("file:///photo%20one.PNG?token=1", "unknown"))
    }
    @Test fun genericPhotoAndAudioFileLinksActuallyRenderMediaControls() {
        compose.setContent { MyApplicationTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MarkdownContent(rawText = "[file:Photo.jpg](file:///photo.jpg)\n[file:Song.mp3](file:///song.mp3)")
            }
        } }
        compose.onNodeWithContentDescription("Photo.jpg").assertIsDisplayed()
        compose.onNodeWithText("Song.mp3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").assertHasClickAction()
        compose.onNodeWithContentDescription("File attachment").assertDoesNotExist()
    }
    @Test fun importedLabelsAndDestinationsKeepBracketsSpacesAndParenthesesIntact() {
        val markdown = attachmentMarkdown("file:///a (b).jpg", "Photo [one].jpg", "image/jpeg")
        val photo = paragraphParts(NoteMarkdownParser.parse(markdown).firstChild).filterIsInstance<ParagraphPart.Photo>().single()
        assertEquals("Photo [one].jpg", photo.description)
        assertEquals("file:///a%20(b).jpg", photo.uri)
        val audio = attachmentMarkdown("file:///a.m4a", "Recording", "audio/mp4")
        assertEquals("Recording", paragraphParts(NoteMarkdownParser.parse(audio).firstChild).filterIsInstance<ParagraphPart.Audio>().single().label)
    }
}
