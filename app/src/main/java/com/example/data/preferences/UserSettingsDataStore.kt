package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yapulse_settings")

data class UserSettings(
    val onlyYandexMusic: Boolean = true,
    val eqEnabled: Boolean = false,
    val eqPresetName: String = "FLAT",
    val bassStrength: Float = 0.35f,
    val virtualizerStrength: Float = 0.2f,
    val loudnessGainMb: Int = 0,
    val shakeSensitivity: Float = 15f,
    val hapticFeedbackEnabled: Boolean = true
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val ONLY_YANDEX_MUSIC = booleanPreferencesKey("only_yandex_music")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val BASS_STRENGTH = floatPreferencesKey("bass_strength")
        val VIRTUALIZER_STRENGTH = floatPreferencesKey("virtualizer_strength")
        val LOUDNESS_GAIN_MB = intPreferencesKey("loudness_gain_mb")
        val SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onlyYandexMusic = prefs[Keys.ONLY_YANDEX_MUSIC] ?: true,
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            eqPresetName = prefs[Keys.EQ_PRESET] ?: "FLAT",
            bassStrength = prefs[Keys.BASS_STRENGTH] ?: 0.35f,
            virtualizerStrength = prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0.2f,
            loudnessGainMb = prefs[Keys.LOUDNESS_GAIN_MB] ?: 0,
            shakeSensitivity = prefs[Keys.SHAKE_SENSITIVITY] ?: 15f,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true
        )
    }

    suspend fun setOnlyYandexMusic(onlyYandex: Boolean) {
        context.dataStore.edit { it[Keys.ONLY_YANDEX_MUSIC] = onlyYandex }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_ENABLED] = enabled }
    }

    suspend fun setEqPreset(presetName: String) {
        context.dataStore.edit { it[Keys.EQ_PRESET] = presetName }
    }

    suspend fun setBassStrength(value: Float) {
        context.dataStore.edit { it[Keys.BASS_STRENGTH] = value }
    }

    suspend fun setVirtualizerStrength(value: Float) {
        context.dataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = value }
    }

    suspend fun setLoudnessGainMb(gainMb: Int) {
        context.dataStore.edit { it[Keys.LOUDNESS_GAIN_MB] = gainMb }
    }

    suspend fun setShakeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[Keys.SHAKE_SENSITIVITY] = sensitivity }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }
}
