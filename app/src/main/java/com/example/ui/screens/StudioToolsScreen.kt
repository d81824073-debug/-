package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioUiState
import com.example.audio.EqPreset
import com.example.service.BackgroundFeaturesState
import com.example.ui.theme.CardGlass
import com.example.ui.theme.CardGlassBorder
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.YandexAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioToolsScreen(
    featuresState: BackgroundFeaturesState,
    audioState: AudioUiState,
    onlyYandexMusic: Boolean,
    onSetOnlyYandex: (Boolean) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onStopAtTrackEnd: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleShakeToSkip: (Boolean) -> Unit,
    onChangeShakeSensitivity: (Float) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onChangeBass: (Float) -> Unit,
    onChangeVirtualizer: (Float) -> Unit,
    onToggleLoudness: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Звук и фишки",
                style = MaterialTheme.typography.headlineMedium,
                color = SoftWhite
            )
            Text(
                text = "Умная автоматизация, DSP-эквалайзер и настройка моста",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
        }

        // 1. Sleep Timer with Smooth Fade-Out
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = YandexAmber)
                    Spacer(Modifier.width(10.dp))
                    Text("Таймер сна с Fade-Out", style = MaterialTheme.typography.titleMedium)
                }

                val statusText = when {
                    featuresState.sleepSecondsRemaining > 0 -> {
                        val m = featuresState.sleepSecondsRemaining / 60
                        val s = featuresState.sleepSecondsRemaining % 60
                        "Плавное затухание и пауза через $m мин ${"%02d".format(s)} сек"
                    }
                    featuresState.stopAtEndOfTrack -> "Пауза сработает сразу после текущего трека"
                    else -> "За последние 10 секунд плавно снизит громкость и вернёт её после паузы"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (featuresState.sleepSecondsRemaining > 0 || featuresState.stopAtEndOfTrack) YandexAmber else MutedSlate
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 45, 60).forEach { minutes ->
                        FilledTonalButton(
                            onClick = { onStartSleepTimer(minutes) },
                            modifier = Modifier.testTag("sleep_timer_${minutes}m")
                        ) {
                            Text("${minutes} мин")
                        }
                    }
                    FilledTonalButton(
                        onClick = onStopAtTrackEnd,
                        modifier = Modifier.testTag("sleep_timer_track_end")
                    ) {
                        Text("До конца трека")
                    }
                }

                if (featuresState.sleepSecondsRemaining > 0 || featuresState.stopAtEndOfTrack) {
                    TextButton(
                        onClick = onCancelSleepTimer,
                        modifier = Modifier.testTag("cancel_sleep_timer_button")
                    ) {
                        Text("Отменить таймер", color = DangerCoral)
                    }
                }
            }
        }

        // 2. Audio Enhancer & Equalizer
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = YandexAmber)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("DSP-Улучшайзер звука", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (audioState.hardwareAvailable) "Глобальный микс устройства" else "DSP недоступен на этом устройстве",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (audioState.hardwareAvailable) MutedSlate else DangerCoral
                            )
                        }
                    }
                    Switch(
                        checked = audioState.isEnabled,
                        onCheckedChange = onToggleEq,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = YandexAmber),
                        modifier = Modifier.testTag("eq_enable_switch")
                    )
                }

                // 5-Band EQ visual readout
                EqBandVisualizer(
                    levels = audioState.bandLevelsNormalized,
                    enabled = audioState.isEnabled
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EqPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = audioState.selectedPreset == preset,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YandexAmber,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("eq_preset_${preset.name}")
                        )
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Усиление баса (Bass Boost)", style = MaterialTheme.typography.bodyMedium)
                        Text("${(audioState.bassStrength * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = YandexAmber)
                    }
                    Slider(
                        value = audioState.bassStrength,
                        onValueChange = onChangeBass,
                        colors = SliderDefaults.colors(thumbColor = YandexAmber, activeTrackColor = YandexAmber),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3D-сцена (Virtualizer)", style = MaterialTheme.typography.bodyMedium)
                        Text("${(audioState.virtualizerStrength * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = CoralPulse)
                    }
                    Slider(
                        value = audioState.virtualizerStrength,
                        onValueChange = onChangeVirtualizer,
                        colors = SliderDefaults.colors(thumbColor = CoralPulse, activeTrackColor = CoralPulse),
                        modifier = Modifier.testTag("virtualizer_slider")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Усилитель громкости (+4.5 дБ)", style = MaterialTheme.typography.bodyMedium)
                        Text("Полезно для тихих треков и подкастов", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }
                    Switch(
                        checked = audioState.loudnessBoostEnabled,
                        onCheckedChange = onToggleLoudness,
                        modifier = Modifier.testTag("loudness_switch")
                    )
                }
            }
        }

        // 3. Shake-to-Skip
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Vibration, contentDescription = null, tint = YandexAmber)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Shake-to-Skip", style = MaterialTheme.typography.titleMedium)
                            Text("Встряхните телефон для переключения трека", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                        }
                    }
                    Switch(
                        checked = featuresState.shakeToSkipEnabled,
                        onCheckedChange = onToggleShakeToSkip,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = YandexAmber),
                        modifier = Modifier.testTag("shake_to_skip_switch")
                    )
                }

                Column {
                    val sensLabel = when {
                        featuresState.shakeSensitivity < 12.5f -> "Высокая (лёгкий взмах)"
                        featuresState.shakeSensitivity < 17f -> "Средняя"
                        else -> "Низкая (сильный рывок)"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Чувствительность", style = MaterialTheme.typography.bodyMedium)
                        Text(sensLabel, style = MaterialTheme.typography.labelSmall, color = EmeraldGlow)
                    }
                    Slider(
                        value = featuresState.shakeSensitivity,
                        onValueChange = onChangeShakeSensitivity,
                        valueRange = 9f..22f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldGlow, activeTrackColor = EmeraldGlow),
                        modifier = Modifier.testTag("shake_sensitivity_slider")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Виброотклик при встряхивании", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = featuresState.hapticFeedbackEnabled,
                        onCheckedChange = onToggleHaptic,
                        modifier = Modifier.testTag("shake_haptic_switch")
                    )
                }
            }
        }

        // 4. Bridge Source Filter
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = YandexAmber)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Фильтр: только Яндекс Музыка", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (onlyYandexMusic)
                                    "Отслеживается только пакет ru.yandex.music"
                                else
                                    "Приоритет Яндекс Музыке, но работают и другие плееры",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }
                    Switch(
                        checked = onlyYandexMusic,
                        onCheckedChange = onSetOnlyYandex,
                        modifier = Modifier.testTag("only_yandex_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlass),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGlassBorder, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun EqBandVisualizer(
    levels: List<Float>,
    enabled: Boolean
) {
    val freqLabels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101018))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        freqLabels.forEachIndexed { idx, freq ->
            val norm = levels.getOrElse(idx) { 0f }
            val heightFraction = ((norm + 1f) / 2f).coerceIn(0.12f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardGlass),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((54f * heightFraction).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (enabled) YandexAmber else MutedSlate.copy(alpha = 0.45f))
                    )
                }
                Text(freq, style = MaterialTheme.typography.labelSmall, color = MutedSlate, fontWeight = FontWeight.Medium)
            }
        }
    }
}
