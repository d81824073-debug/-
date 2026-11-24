package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.service.YandexMediaHub
import com.example.service.YandexPlaybackUiState
import com.example.ui.components.NeonVinylDisc
import com.example.ui.components.PulseVisualizerBars
import com.example.ui.components.YandexNavigator
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CardGlass
import com.example.ui.theme.CardGlassBorder
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.YandexAmber
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    mediaState: YandexPlaybackUiState,
    sleepSecondsRemaining: Int,
    onBookmarkMoment: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = Color(mediaState.accentColorArgb)

    var currentPos by remember(mediaState.positionMs, mediaState.isPlaying, mediaState.lastUpdatedMs) {
        mutableLongStateOf(mediaState.positionMs)
    }

    LaunchedEffect(mediaState.isPlaying, mediaState.positionMs, mediaState.lastUpdatedMs, mediaState.durationMs) {
        while (mediaState.isPlaying && mediaState.durationMs > 0L) {
            val elapsed = System.currentTimeMillis() - mediaState.lastUpdatedMs
            currentPos = (mediaState.positionMs + (elapsed * mediaState.playbackSpeed).toLong())
                .coerceIn(0L, mediaState.durationMs)
            delay(400L)
        }
    }

    val displayTitle = mediaState.title.ifBlank { stringResource(R.string.default_track_title) }
    val displayArtist = when {
        mediaState.artist.isNotBlank() && mediaState.album.isNotBlank() ->
            "${mediaState.artist} • ${mediaState.album}"
        mediaState.artist.isNotBlank() -> mediaState.artist
        else -> stringResource(R.string.default_track_artist)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp
        val vinylSize = if (isWide) 260.dp else (maxWidth * 0.62f).coerceIn(190.dp, 260.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header & Source Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "YaPulse",
                        style = MaterialTheme.typography.headlineMedium,
                        color = YandexAmber
                    )
                    SourceBadge(
                        isConnected = mediaState.isConnected,
                        isYandex = mediaState.isYandexSource,
                        sleepSeconds = sleepSecondsRemaining
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = {
                            if (mediaState.title.isNotBlank()) {
                                YandexNavigator.shareTrack(context, mediaState.artist, mediaState.title)
                            } else {
                                onShowMessage("Включите трек, чтобы поделиться им")
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_track_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CardGlass,
                            contentColor = SoftWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.cd_share_track)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (!YandexNavigator.openYandexMusic(context)) {
                                onShowMessage(context.getString(R.string.toast_yandex_not_installed))
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_yandex_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = YandexAmber,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = stringResource(R.string.cd_open_yandex)
                        )
                    }
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonVinylDisc(
                            albumArt = mediaState.albumArt,
                            isPlaying = mediaState.isPlaying,
                            accentColor = accentColor,
                            size = vinylSize
                        )
                        PulseVisualizerBars(isPlaying = mediaState.isPlaying, accentColor = accentColor)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TrackMetadataBlock(displayTitle, displayArtist, centered = false)
                        PlaybackControlsSection(
                            mediaState = mediaState,
                            currentPos = currentPos,
                            accentColor = accentColor,
                            onBookmarkMoment = onBookmarkMoment
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NeonVinylDisc(
                        albumArt = mediaState.albumArt,
                        isPlaying = mediaState.isPlaying,
                        accentColor = accentColor,
                        size = vinylSize
                    )
                    PulseVisualizerBars(isPlaying = mediaState.isPlaying, accentColor = accentColor)
                    TrackMetadataBlock(displayTitle, displayArtist, centered = true)
                    PlaybackControlsSection(
                        mediaState = mediaState,
                        currentPos = currentPos,
                        accentColor = accentColor,
                        onBookmarkMoment = onBookmarkMoment
                    )
                }
            }

            // Dynamic Custom Actions from Yandex Music (e.g. Like / Dislike)
            if (mediaState.customActions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaState.customActions.take(4).forEach { act ->
                        AssistChip(
                            onClick = { YandexMediaHub.sendCustomAction(act.action) },
                            label = { Text(act.name, maxLines = 1) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = CardGlass,
                                labelColor = SoftWhite
                            ),
                            modifier = Modifier.testTag("custom_action_${act.action}")
                        )
                    }
                }
            }

            // Yandex Music Quick Shortcuts Card
            Surface(
                color = CardGlass.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardGlassBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Быстрый запуск Яндекс Музыки",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlate
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                if (!YandexNavigator.openDeepLink(context, "yandexmusic://radio/user/onyourwave")) {
                                    onShowMessage(context.getString(R.string.toast_yandex_not_installed))
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Radio, contentDescription = null, tint = YandexAmber, modifier = Modifier.size(18.dp))
                            },
                            label = { Text("Моя волна") },
                            modifier = Modifier.testTag("shortcut_my_wave")
                        )
                        AssistChip(
                            onClick = {
                                if (!YandexNavigator.openDeepLink(context, "yandexmusic://search")) {
                                    onShowMessage(context.getString(R.string.toast_yandex_not_installed))
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(18.dp))
                            },
                            label = { Text("Поиск трека") },
                            modifier = Modifier.testTag("shortcut_search")
                        )
                        AssistChip(
                            onClick = {
                                if (!YandexNavigator.openDeepLink(context, "yandexmusic://users/me/playlists")) {
                                    onShowMessage(context.getString(R.string.toast_yandex_not_installed))
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(18.dp))
                            },
                            label = { Text("Коллекция") },
                            modifier = Modifier.testTag("shortcut_collection")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(
    isConnected: Boolean,
    isYandex: Boolean,
    sleepSeconds: Int
) {
    val label = when {
        sleepSeconds > 0 -> "Сон ${sleepSeconds / 60}:${"%02d".format(sleepSeconds % 60)}"
        isConnected && isYandex -> stringResource(R.string.status_yandex_connected)
        isConnected -> stringResource(R.string.status_universal_connected)
        else -> stringResource(R.string.status_disconnected)
    }
    val dotColor = when {
        sleepSeconds > 0 -> YandexAmber
        isConnected && isYandex -> EmeraldGlow
        isConnected -> YandexAmber
        else -> MutedSlate
    }

    Surface(
        color = CardGlass,
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, CardGlassBorder, RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SoftWhite
            )
        }
    }
}

@Composable
private fun TrackMetadataBlock(
    title: String,
    subtitle: String,
    centered: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = SoftWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSlate,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
    }
}

@Composable
private fun PlaybackControlsSection(
    mediaState: YandexPlaybackUiState,
    currentPos: Long,
    accentColor: Color,
    onBookmarkMoment: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val maxDur = mediaState.durationMs.coerceAtLeast(1L).toFloat()
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentPos.toFloat().coerceIn(0f, maxDur),
                onValueChange = { YandexMediaHub.seekTo(it.toLong()) },
                valueRange = 0f..maxDur,
                enabled = mediaState.isConnected && mediaState.durationMs > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = YandexAmber,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = CardGlassBorder
                ),
                modifier = Modifier.testTag("player_seek_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDurationMs(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
                AssistChip(
                    onClick = onBookmarkMoment,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = stringResource(R.string.cd_bookmark_moment),
                            tint = YandexAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Закладка") },
                    modifier = Modifier.testTag("bookmark_moment_button")
                )
                Text(
                    text = formatDurationMs(mediaState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { YandexMediaHub.skipBy(-15_000L) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("rewind_15_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CardGlass,
                    contentColor = MutedSlate
                )
            ) {
                Icon(Icons.Rounded.Replay10, contentDescription = stringResource(R.string.cd_rewind_15))
            }

            FilledTonalIconButton(
                onClick = { YandexMediaHub.previous() },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("prev_track_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CardGlass,
                    contentColor = SoftWhite
                )
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_track),
                    modifier = Modifier.size(32.dp)
                )
            }

            FloatingActionButton(
                onClick = { YandexMediaHub.playPause() },
                containerColor = YandexAmber,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    modifier = Modifier.size(38.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { YandexMediaHub.next() },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("next_track_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CardGlass,
                    contentColor = SoftWhite
                )
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    modifier = Modifier.size(32.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { YandexMediaHub.skipBy(15_000L) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("forward_15_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CardGlass,
                    contentColor = MutedSlate
                )
            ) {
                Icon(Icons.Rounded.Forward10, contentDescription = stringResource(R.string.cd_forward_15))
            }
        }
    }
}
