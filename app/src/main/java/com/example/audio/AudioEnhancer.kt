package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

enum class EqPreset(val title: String, val bandsMb: List<Short>) {
    FLAT("Ровный", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", listOf(700, 450, 50, 150, 250)),
    NIGHT_DRIVE("Ночная трасса", listOf(500, 250, -100, 300, 450)),
    VOCAL("Вокал / Подкаст", listOf(-250, -50, 500, 400, 150)),
    CLUB("Клуб", listOf(550, 350, 100, 350, 500));

    companion object {
        fun fromName(name: String): EqPreset =
            entries.firstOrNull { it.name == name } ?: FLAT
    }
}

data class AudioUiState(
    val isEnabled: Boolean = false,
    val hardwareAvailable: Boolean = true,
    val selectedPreset: EqPreset = EqPreset.FLAT,
    val bassStrength: Float = 0.35f,
    val virtualizerStrength: Float = 0.2f,
    val loudnessBoostEnabled: Boolean = false,
    val bandLevelsNormalized: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)
)

class AudioEnhancer {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isEnabled: Boolean = false
        private set

    var hardwareAvailable: Boolean = true
        private set

    fun setEnabled(enabled: Boolean): Boolean {
        isEnabled = enabled
        if (!enabled) {
            try {
                equalizer?.enabled = false
                bassBoost?.enabled = false
                virtualizer?.enabled = false
                loudnessEnhancer?.enabled = false
            } catch (_: Exception) {
            }
            return true
        }

        return try {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0)
            }
            if (bassBoost == null) {
                bassBoost = try { BassBoost(0, 0) } catch (_: Exception) { null }
            }
            if (virtualizer == null) {
                virtualizer = try { Virtualizer(0, 0) } catch (_: Exception) { null }
            }
            if (loudnessEnhancer == null) {
                loudnessEnhancer = try { LoudnessEnhancer(0) } catch (_: Exception) { null }
            }
            equalizer?.enabled = true
            bassBoost?.enabled = true
            virtualizer?.enabled = true
            hardwareAvailable = true
            true
        } catch (_: Exception) {
            isEnabled = false
            hardwareAvailable = false
            false
        }
    }

    fun applyPreset(preset: EqPreset): List<Float> {
        val normalized = preset.bandsMb.map { (it / 750f).coerceIn(-1f, 1f) }
        val eq = equalizer ?: return normalized
        try {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val min = range[0]
            val max = range[1]
            for (i in 0 until bands) {
                val target = preset.bandsMb.getOrElse(i) { 0 }
                eq.setBandLevel(i.toShort(), target.coerceIn(min, max))
            }
        } catch (_: Exception) {
        }
        return normalized
    }

    fun setBassStrength(percent: Float) {
        try {
            val strength = (percent.coerceIn(0f, 1f) * 1000).toInt().toShort()
            bassBoost?.setStrength(strength)
        } catch (_: Exception) {
        }
    }

    fun setVirtualizerStrength(percent: Float) {
        try {
            val strength = (percent.coerceIn(0f, 1f) * 1000).toInt().toShort()
            virtualizer?.setStrength(strength)
        } catch (_: Exception) {
        }
    }

    fun setLoudnessBoost(enabled: Boolean) {
        try {
            if (loudnessEnhancer == null && isEnabled) {
                loudnessEnhancer = LoudnessEnhancer(0)
            }
            loudnessEnhancer?.setTargetGain(if (enabled) 450 else 0)
            loudnessEnhancer?.enabled = isEnabled && enabled
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
