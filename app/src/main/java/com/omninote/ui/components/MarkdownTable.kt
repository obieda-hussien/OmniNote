package com.omninote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.omninote.R
import org.commonmark.ext.gfm.tables.*

/** One horizontal scroll owner, equal row heights and widths sized for readable cell content. */
@Composable
internal fun MarkdownTable(table: TableBlock) {
    val rows = remember(table) { table.children().flatMap { it.children() }.map { it.children().filterIsInstance<TableCell>() } }
    val count = rows.maxOfOrNull { it.size } ?: 0
    if (count == 0) return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val body = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, textDirection = TextDirection.ContentOrLtr)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.markdown_table)
    val summary = stringResource(R.string.table_dimensions, (rows.size - 1).coerceAtLeast(0), count)
    val scroll = rememberScrollState()
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.TableChart, null, tint = primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(summary, style = MaterialTheme.typography.labelSmall, color = secondary, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val available = maxWidth
                val widths = remember(table, available, body, density.fontScale) {
                    val measured = List(count) { column ->
                        val samples = rows.take(40).mapNotNull { it.getOrNull(column) }
                        val longest = samples.maxOfOrNull { cell ->
                            // A compact width lets long sentences wrap, while short values stay on one line.
                            measurer.measure(plainInline(cell).take(48), style = body, maxLines = 1, softWrap = false).size.width
                        } ?: 0
                        with(density) { longest.toDp() + 28.dp }.coerceIn(132.dp, 248.dp)
                    }
                    val total = measured.fold(0.dp) { sum, width -> sum + width }
                    val extra = ((available - (count - 1).dp - total) / count).coerceAtLeast(0.dp)
                    measured.map { it + extra }
                }
                Column {
                Column(Modifier.horizontalScroll(scroll).semantics { contentDescription = title }) {
                    rows.forEachIndexed { rowIndex, cells ->
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            repeat(count) { column ->
                                val cell = cells.getOrNull(column)
                                val header = cell?.isHeader == true
                                Surface(
                                    modifier = Modifier.width(widths[column]).heightIn(min = 48.dp).fillMaxHeight(),
                                    color = when {
                                        header -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        rowIndex % 2 == 0 -> MaterialTheme.colorScheme.surfaceContainerLow
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ) {
                                    InteractiveText(cell?.let { markdownInline(it, primary, secondary) } ?: androidx.compose.ui.text.AnnotatedString(""),
                                        body.copy(fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                                            textAlign = when (cell?.alignment?.name) {
                                                "CENTER" -> TextAlign.Center
                                                "RIGHT" -> TextAlign.Right
                                                "LEFT" -> TextAlign.Left
                                                else -> TextAlign.Start
                                            }), Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
                                }
                                if (column < count - 1) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            }
                        }
                        if (rowIndex < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                }
                if (widths.fold(0.dp) { sum, width -> sum + width } + (count - 1).dp > available) {
                    Text(stringResource(R.string.table_scroll_hint), color = secondary,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                }
            }
        }
    }
}
