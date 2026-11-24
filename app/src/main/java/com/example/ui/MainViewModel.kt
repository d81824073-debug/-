package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEnhancer
import com.example.audio.AudioUiState
import com.example.audio.EqPreset
import com.example.data.local.ArtistStatTuple
import com.example.data.local.TrackBookmarkEntity
import com.example.data.local.TrackHistoryEntity
import com.example.data.local.YaPulseDatabase
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsRepository
import com.example.data.repository.HistoryRepository
import com.example.service.BackgroundFeaturesState
import com.example.service.PulseBackgroundService
import com.example.service.PulseFeaturesHub
import com.example.service.YandexMediaHub
import com.example.service.YandexPlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = YaPulseDatabase.getInstance(context)
    private val historyRepo = HistoryRepository(database.trackHistoryDao())
    private val settingsRepo = UserSettingsRepository(context)
    private val audioEnhancer = AudioEnhancer()

    val mediaState: StateFlow<YandexPlaybackUiState> = YandexMediaHub.state
    val featuresState: StateFlow<BackgroundFeaturesState> = PulseFeaturesHub.state

    val settingsState: StateFlow<UserSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    private val _audioState = MutableStateFlow(AudioUiState())
    val audioState: StateFlow<AudioUiState> = _audioState.asStateFlow()

    val totalPlays: StateFlow<Int> = historyRepo.totalPlays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val uniqueArtists: StateFlow<Int> = historyRepo.uniqueArtists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val topArtists: StateFlow<List<ArtistStatTuple>> = historyRepo.topArtists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookmarks: StateFlow<List<TrackBookmarkEntity>> = historyRepo.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTracks: StateFlow<List<TrackHistoryEntity>> = combine(
        historyRepo.recentTracks,
        _searchQuery
    ) { tracks, query ->
        if (query.isBlank()) tracks
        else {
            val q = query.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        YandexMediaHub.ensureRepository(context)
        viewModelScope.launch {
            val saved = settingsRepo.settingsFlow.first()
            YandexMediaHub.setOnlyYandexMusic(saved.onlyYandexMusic)
            val preset = EqPreset.fromName(saved.eqPresetName)
            val bands = audioEnhancer.applyPreset(preset)
            _audioState.value = AudioUiState(
                isEnabled = false,
                hardwareAvailable = audioEnhancer.hardwareAvailable,
                selectedPreset = preset,
                bassStrength = saved.bassStrength,
                virtualizerStrength = saved.virtualizerStrength,
                loudnessBoostEnabled = saved.loudnessGainMb > 0,
                bandLevelsNormalized = bands
            )
            PulseFeaturesHub.update {
                it.copy(
                    shakeSensitivity = saved.shakeSensitivity,
                    hapticFeedbackEnabled = saved.hapticFeedbackEnabled
                )
            }
        }
    }

    fun refreshNotificationPermission() {
        YandexMediaHub.updatePermission(YandexMediaHub.hasNotificationAccess(context))
    }

    fun setOnlyYandexMusic(onlyYandex: Boolean) {
        YandexMediaHub.setOnlyYandexMusic(onlyYandex)
        viewModelScope.launch { settingsRepo.setOnlyYandexMusic(onlyYandex) }
    }

    fun toggleEqEnabled(enabled: Boolean) {
        val applied = audioEnhancer.setEnabled(enabled) && enabled
        if (applied) {
            val current = _audioState.value
            val bands = audioEnhancer.applyPreset(current.selectedPreset)
            audioEnhancer.setBassStrength(current.bassStrength)
            audioEnhancer.setVirtualizerStrength(current.virtualizerStrength)
            audioEnhancer.setLoudnessBoost(current.loudnessBoostEnabled)
            _audioState.value = current.copy(
                isEnabled = true,
                hardwareAvailable = true,
                bandLevelsNormalized = bands
            )
        } else {
            _audioState.value = _audioState.value.copy(
                isEnabled = false,
                hardwareAvailable = audioEnhancer.hardwareAvailable
            )
        }
        viewModelScope.launch { settingsRepo.setEqEnabled(applied) }
    }

    fun selectEqPreset(preset: EqPreset) {
        val bands = audioEnhancer.applyPreset(preset)
        _audioState.value = _audioState.value.copy(
            selectedPreset = preset,
            bandLevelsNormalized = bands
        )
        viewModelScope.launch { settingsRepo.setEqPreset(preset.name) }
    }

    fun setBassStrength(value: Float) {
        audioEnhancer.setBassStrength(value)
        _audioState.value = _audioState.value.copy(bassStrength = value)
        viewModelScope.launch { settingsRepo.setBassStrength(value) }
    }

    fun setVirtualizerStrength(value: Float) {
        audioEnhancer.setVirtualizerStrength(value)
        _audioState.value = _audioState.value.copy(virtualizerStrength = value)
        viewModelScope.launch { settingsRepo.setVirtualizerStrength(value) }
    }

    fun setLoudnessBoost(enabled: Boolean) {
        audioEnhancer.setLoudnessBoost(enabled)
        _audioState.value = _audioState.value.copy(loudnessBoostEnabled = enabled)
        viewModelScope.launch { settingsRepo.setLoudnessGainMb(if (enabled) 450 else 0) }
    }

    fun startSleepTimer(minutes: Int) {
        val intent = Intent(context, PulseBackgroundService::class.java).apply {
            action = PulseBackgroundService.ACTION_START_SLEEP_TIMER
            putExtra(PulseBackgroundService.EXTRA_MINUTES, minutes)
        }
        startBgService(intent)
    }

    fun startStopAtEndOfTrack() {
        val intent = Intent(context, PulseBackgroundService::class.java).apply {
            action = PulseBackgroundService.ACTION_STOP_AT_TRACK_END
        }
        startBgService(intent)
    }

    fun cancelSleepTimer() {
        val intent = Intent(context, PulseBackgroundService::class.java).apply {
            action = PulseBackgroundService.ACTION_CANCEL_SLEEP
        }
        context.startService(intent)
    }

    fun setShakeToSkip(enabled: Boolean) {
        val current = featuresState.value
        val intent = Intent(context, PulseBackgroundService::class.java).apply {
            action = PulseBackgroundService.ACTION_SET_SHAKE
            putExtra(PulseBackgroundService.EXTRA_SHAKE_ENABLED, enabled)
            putExtra(PulseBackgroundService.EXTRA_SHAKE_SENSITIVITY, current.shakeSensitivity)
            putExtra(PulseBackgroundService.EXTRA_HAPTIC_ENABLED, current.hapticFeedbackEnabled)
        }
        if (enabled) startBgService(intent) else context.startService(intent)
    }

    fun setShakeSensitivity(sensitivity: Float) {
        PulseFeaturesHub.update { it.copy(shakeSensitivity = sensitivity) }
        viewModelScope.launch { settingsRepo.setShakeSensitivity(sensitivity) }
        if (featuresState.value.shakeToSkipEnabled) {
            setShakeToSkip(true)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        PulseFeaturesHub.update { it.copy(hapticFeedbackEnabled = enabled) }
        viewModelScope.launch { settingsRepo.setHapticFeedback(enabled) }
    }

    fun saveCurrentBookmark(): Boolean {
        val state = mediaState.value
        if (state.title.isBlank()) return false
        val elapsed = if (state.isPlaying) {
            ((System.currentTimeMillis() - state.lastUpdatedMs) * state.playbackSpeed).toLong()
        } else 0L
        val currentPos = (state.positionMs + elapsed).coerceAtLeast(0L)
        viewModelScope.launch {
            historyRepo.addBookmark(
                title = state.title,
                artist = state.artist,
                positionMs = currentPos
            )
        }
        return true
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { historyRepo.deleteBookmark(id) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearAll() }
    }

    private fun startBgService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun onCleared() {
        audioEnhancer.release()
        super.onCleared()
    }
}
