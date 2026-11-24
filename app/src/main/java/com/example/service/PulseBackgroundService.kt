package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class BackgroundFeaturesState(
    val sleepSecondsRemaining: Int = 0,
    val stopAtEndOfTrack: Boolean = false,
    val shakeToSkipEnabled: Boolean = false,
    val shakeSensitivity: Float = 15f,
    val hapticFeedbackEnabled: Boolean = true
)

object PulseFeaturesHub {
    private val _state = MutableStateFlow(BackgroundFeaturesState())
    val state: StateFlow<BackgroundFeaturesState> = _state.asStateFlow()

    fun update(block: (BackgroundFeaturesState) -> BackgroundFeaturesState) {
        _state.value = block(_state.value)
    }
}

class PulseBackgroundService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START_SLEEP_TIMER = "com.example.action.START_SLEEP_TIMER"
        const val ACTION_STOP_AT_TRACK_END = "com.example.action.STOP_AT_TRACK_END"
        const val ACTION_CANCEL_SLEEP = "com.example.action.CANCEL_SLEEP"
        const val ACTION_SET_SHAKE = "com.example.action.SET_SHAKE"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_SHAKE_ENABLED = "extra_shake_enabled"
        const val EXTRA_SHAKE_SENSITIVITY = "extra_shake_sensitivity"
        const val EXTRA_HAPTIC_ENABLED = "extra_haptic_enabled"

        private const val CHANNEL_ID = "yapulse_bg_channel"
        private const val NOTIFICATION_ID = 4201
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sleepJob: Job? = null
    private var trackWatcherJob: Job? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely(buildNotification("Активны умные функции плеера"))

        when (intent?.action) {
            ACTION_START_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 15).coerceAtLeast(1)
                startSleepCountdown(minutes)
            }
            ACTION_STOP_AT_TRACK_END -> {
                startStopAtTrackEnd()
            }
            ACTION_CANCEL_SLEEP -> {
                cancelSleepTimers()
                stopIfIdle()
            }
            ACTION_SET_SHAKE -> {
                val enabled = intent.getBooleanExtra(EXTRA_SHAKE_ENABLED, false)
                val sens = intent.getFloatExtra(EXTRA_SHAKE_SENSITIVITY, 15f)
                val haptic = intent.getBooleanExtra(EXTRA_HAPTIC_ENABLED, true)
                configureShake(enabled, sens, haptic)
            }
        }
        return START_STICKY
    }

    private fun startForegroundSafely(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }
    }

    private fun startSleepCountdown(minutes: Int) {
        cancelSleepTimers()
        val totalSeconds = minutes * 60
        updateNotification("Таймер сна: $minutes мин")
        sleepJob = scope.launch {
            for (sec in totalSeconds downTo 1) {
                PulseFeaturesHub.update {
                    it.copy(
                        sleepSecondsRemaining = sec,
                        stopAtEndOfTrack = false
                    )
                }
                if (sec == 10) {
                    fadeOutAndPause()
                    break
                }
                delay(1000L)
            }
            PulseFeaturesHub.update { it.copy(sleepSecondsRemaining = 0) }
            stopIfIdle()
        }
    }

    private fun startStopAtTrackEnd() {
        cancelSleepTimers()
        val initialTitle = YandexMediaHub.state.value.title
        PulseFeaturesHub.update { it.copy(stopAtEndOfTrack = true) }
        updateNotification("Остановка в конце текущего трека")
        trackWatcherJob = scope.launch {
            while (PulseFeaturesHub.state.value.stopAtEndOfTrack) {
                delay(500L)
                val current = YandexMediaHub.state.value
                if (current.isConnected && current.title.isNotBlank() && current.title != initialTitle) {
                    YandexMediaHub.pause()
                    PulseFeaturesHub.update { it.copy(stopAtEndOfTrack = false) }
                    break
                }
            }
            stopIfIdle()
        }
    }

    private suspend fun fadeOutAndPause() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val originalVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (originalVol > 0) {
            val stepDelay = (8000L / originalVol).coerceAtLeast(250L)
            for (v in originalVol downTo 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                delay(stepDelay)
            }
        }
        YandexMediaHub.pause()
        delay(500L)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVol, 0)
    }

    private fun cancelSleepTimers() {
        sleepJob?.cancel()
        trackWatcherJob?.cancel()
        PulseFeaturesHub.update {
            it.copy(
                sleepSecondsRemaining = 0,
                stopAtEndOfTrack = false
            )
        }
    }

    private fun configureShake(enabled: Boolean, sensitivity: Float, haptic: Boolean) {
        PulseFeaturesHub.update {
            it.copy(
                shakeToSkipEnabled = enabled,
                shakeSensitivity = sensitivity,
                hapticFeedbackEnabled = haptic
            )
        }
        sensorManager?.unregisterListener(this)
        if (enabled && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            updateNotification("Shake-to-Skip активен")
        } else {
            stopIfIdle()
        }
    }

    private fun stopIfIdle() {
        val s = PulseFeaturesHub.state.value
        if (!s.shakeToSkipEnabled && s.sleepSecondsRemaining == 0 && !s.stopAtEndOfTrack) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        val featureState = PulseFeaturesHub.state.value
        val now = System.currentTimeMillis()
        if (acceleration > featureState.shakeSensitivity && now - lastShakeTime > 1300L) {
            lastShakeTime = now
            YandexMediaHub.next()
            if (featureState.hapticFeedbackEnabled) {
                triggerHapticPulse()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerHapticPulse() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70L)
            }
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YaPulse — фоновые функции",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YaPulse работает")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
