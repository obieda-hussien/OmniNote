package com.omni.noteplus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omni.noteplus.R
import com.omni.noteplus.data.ProductivityDay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivitySheet(snapshot: ProductivitySnapshot, error: String?, onRetry: () -> Unit, onDismiss: () -> Unit) {
    var period by remember { mutableStateOf(0) }
    var visibleDays by remember(period) { mutableStateOf(30) }
    val today = productivityDay(System.currentTimeMillis())
    val days = remember(snapshot.days, period, today) {
        val limit = when (period) { 1 -> 7; 2 -> 30; else -> null }
        if (limit == null) snapshot.days else {
            val calendar = Calendar.getInstance().apply { add(Calendar.DATE, -(limit - 1)) }
            val first = productivityDay(calendar.timeInMillis)
            snapshot.days.filter { it.day >= first && it.day <= today }
        }
    }
    OmniSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    OmniIcon(OmniIconType.INSIGHTS, Modifier.padding(14.dp).size(28.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.productivity_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.productivity_global), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(omniIconVector(OmniIconType.CLOSE), stringResource(R.string.close_dashboard)) }
            }
            when {
                error != null -> {
                    Text(stringResource(R.string.productivity_error), color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                !snapshot.ready -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.productivity_loading))
                }
                else -> {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(R.string.period_all_time, R.string.period_week, R.string.period_month).forEachIndexed { index, label ->
                            FilterChip(selected = period == index, onClick = { period = index }, label = { Text(stringResource(label)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(Modifier.weight(1f), if (period == 0) R.string.notes_recorded else R.string.notes_created,
                            if (period == 0) snapshot.totalNotes.toLong() else days.sumOf { it.created.toLong() }, OmniIconType.NOTES)
                        MetricCard(Modifier.weight(1f), if (period == 0) R.string.words_recorded else R.string.words_added,
                            if (period == 0) snapshot.recordedWords else days.sumOf { it.wordsAdded }, OmniIconType.WORDS)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(Modifier.weight(1f), R.string.permanently_deleted,
                            if (period == 0) snapshot.deletedNotes.toLong() else days.sumOf { it.deleted.toLong() }, OmniIconType.DELETE)
                        MetricCard(Modifier.weight(1f), R.string.writing_days,
                            days.count { it.workedOn > 0 }.toLong(), OmniIconType.HISTORY)
                    }
                    Text(stringResource(R.string.words_recorded_help), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.current_library), style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LibraryCount(R.string.active_section, snapshot.activeNotes, OmniIconType.NOTES)
                                LibraryCount(R.string.archive_title, snapshot.archivedNotes, OmniIconType.ARCHIVE)
                                LibraryCount(R.string.trash_title, snapshot.trashedNotes, OmniIconType.DELETE)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(stringResource(R.string.current_word_count, snapshot.currentWords), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.current_streak,
                                writingStreak(snapshot.days.filter { it.workedOn > 0 }.map { it.day }.toSet(), today)),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(stringResource(R.string.activity_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (days.isEmpty()) Text(stringResource(R.string.no_activity_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        days.take(visibleDays).forEach { ActivityDayCard(it) }
                        if (days.size > visibleDays) OutlinedButton(onClick = { visibleDays += 30 }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.show_older_activity))
                        }
                    }
                    val tracking = snapshot.tracking!!
                    Text(stringResource(R.string.tracking_baseline, localDateLabel(productivityDay(tracking.startedAt)), tracking.baselineNotes),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.history_privacy), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, label: Int, value: Long, icon: OmniIconType) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OmniIcon(icon, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
            Text(String.format(Locale.getDefault(), "%,d", value), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibraryCount(label: Int, count: Int, icon: OmniIconType) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OmniIcon(icon, Modifier.size(20.dp), MaterialTheme.colorScheme.onSecondaryContainer)
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ActivityDayCard(day: ProductivityDay) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(localDateLabel(day.day), style = MaterialTheme.typography.titleSmall)
            if (day.workedOn > 0) Text(stringResource(R.string.activity_writing, day.workedOn, day.wordsAdded), style = MaterialTheme.typography.bodyMedium)
            val changes = listOf(R.string.activity_created to day.created, R.string.activity_archived to day.archived, R.string.activity_trashed to day.trashed,
                R.string.activity_restored to day.restored, R.string.activity_deleted to day.deleted)
                .filter { it.second > 0 }.map { stringResource(it.first, it.second) }
            if (changes.isNotEmpty()) Text(changes.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (day.workedOn == 0 && changes.isEmpty()) Text(stringResource(R.string.library_organized), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun localDateLabel(day: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parser.parse(day)!!)
}.getOrDefault(day)
