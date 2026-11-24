package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ArtistStatTuple
import com.example.data.local.TrackBookmarkEntity
import com.example.data.local.TrackHistoryEntity
import com.example.service.YandexMediaHub
import com.example.ui.components.YandexNavigator
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CardGlass
import com.example.ui.theme.CardGlassBorder
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.YandexAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryStatsScreen(
    totalPlays: Int,
    uniqueArtists: Int,
    topArtists: List<ArtistStatTuple>,
    recentTracks: List<TrackHistoryEntity>,
    bookmarks: List<TrackBookmarkEntity>,
    currentTrackTitle: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history")
                ) {
                    Text(stringResource(R.string.history_clear_confirm), color = DangerCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.history_clear_cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("История и статистика", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                if (totalPlays > 0 || bookmarks.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text("Очистить", color = DangerCoral)
                    }
                }
            }
        }

        // Summary KPI Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard(
                    label = "Треков перехвачено",
                    value = totalPlays.toString(),
                    accent = YandexAmber,
                    modifier = Modifier.weight(1f)
                )
                StatKpiCard(
                    label = "Артистов",
                    value = uniqueArtists.toString(),
                    accent = EmeraldGlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Artists Breakdown
        if (topArtists.isNotEmpty()) {
            item {
                Text("Топ артистов", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            }
            val maxPlays = topArtists.maxOf { it.playCount }.coerceAtLeast(1)
            items(topArtists, key = { it.artist }) { stat ->
                val ratio = stat.playCount.toFloat() / maxPlays.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardGlass)
                        .border(1.dp, CardGlassBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stat.playCount}x", style = MaterialTheme.typography.labelMedium, color = YandexAmber)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CardGlassBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.08f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(YandexAmber)
                        )
                    }
                }
            }
        }

        // Saved Timecode Bookmarks
        if (bookmarks.isNotEmpty()) {
            item {
                Text("Закладки моментов", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            }
            items(bookmarks, key = { "bm_${it.id}" }) { bm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardGlass)
                        .border(1.dp, CardGlassBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (bm.title == currentTrackTitle) {
                                YandexMediaHub.seekTo(bm.positionMs)
                                onShowMessage("Перемотка к ${formatDurationMs(bm.positionMs)}")
                            } else {
                                onShowMessage("Таймкод ${formatDurationMs(bm.positionMs)} для «${bm.title}»")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = YandexAmber)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bm.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${bm.artist.ifBlank { "Трек" }} • ${formatDurationMs(bm.positionMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = YandexAmber
                        )
                    }
                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить закладку", tint = MutedSlate)
                    }
                }
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Поиск в истории треков...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить поиск")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YandexAmber,
                    unfocusedBorderColor = CardGlassBorder,
                    focusedContainerColor = CardGlass,
                    unfocusedContainerColor = CardGlass
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_field")
            )
        }

        item {
            Text("Недавние треки", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
        }

        if (recentTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGlass)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("История пока пуста", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                    Text(
                        "Как только вы включите музыку в Яндекс Музыке, YaPulse автоматически сохранит треки здесь.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                }
            }
        } else {
            items(recentTracks, key = { it.id }) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardGlass.copy(alpha = 0.8f))
                        .border(1.dp, CardGlassBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (!YandexNavigator.openYandexMusic(context)) {
                                onShowMessage(context.getString(R.string.toast_yandex_not_installed))
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = YandexAmber)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist.ifBlank { "Неизвестный исполнитель" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatTimestamp(track.playedAtMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlass),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, CardGlassBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.displaySmall, color = accent)
        }
    }
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
