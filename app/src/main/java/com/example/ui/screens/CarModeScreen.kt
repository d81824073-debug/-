package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.YandexMediaHub
import com.example.service.YandexPlaybackUiState
import com.example.ui.theme.CardGlass
import com.example.ui.theme.CardGlassBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.YandexAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CarModeScreen(
    mediaState: YandexPlaybackUiState,
    eqEnabled: Boolean,
    onToggleEq: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var clockText by remember { mutableStateOf(formatCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockText = formatCurrentTime()
            delay(15_000L)
        }
    }

    val title = mediaState.title.ifBlank { stringResource(R.string.default_track_title) }
    val artist = mediaState.artist.ifBlank { "Яндекс Музыка" }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Car Cockpit Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = YandexAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, YandexAmber.copy(alpha = 0.45f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = stringResource(R.string.car_mode_badge),
                        color = YandexAmber,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = clockText,
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftWhite
                )
            }

            // Track Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = if (isWide) 34.sp else 28.sp,
                    lineHeight = if (isWide) 40.sp else 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = artist,
                    fontSize = 18.sp,
                    color = MutedSlate,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                FilterChip(
                    selected = eqEnabled,
                    onClick = { onToggleEq(!eqEnabled) },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(if (eqEnabled) "Авто-EQ включён" else "Включить Авто-EQ") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YandexAmber,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    ),
                    modifier = Modifier.testTag("car_eq_chip")
                )
            }

            // Oversized Tactile Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { YandexMediaHub.playPause() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YandexAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWide) 88.dp else 104.dp)
                        .testTag("car_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { YandexMediaHub.previous() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardGlass, contentColor = SoftWhite),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .testTag("car_prev_button")
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous_track),
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Button(
                        onClick = { YandexMediaHub.next() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardGlass, contentColor = SoftWhite),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .testTag("car_next_button")
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.cd_next_track),
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { YandexMediaHub.skipBy(-15_000L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14141E), contentColor = MutedSlate),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("car_rewind_button")
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("-15 сек", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { YandexMediaHub.skipBy(15_000L) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14141E), contentColor = MutedSlate),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("car_forward_button")
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("+15 сек", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatCurrentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
