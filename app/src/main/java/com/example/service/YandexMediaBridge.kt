package com.example.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.YaPulseDatabase
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomPlayerAction(
    val action: String,
    val name: String
)

data class YandexPlaybackUiState(
    val isConnected: Boolean = false,
    val hasPermission: Boolean = false,
    val onlyYandexMusic: Boolean = true,
    val sourcePackage: String = "",
    val isYandexSource: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastUpdatedMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val albumArt: Bitmap? = null,
    val accentColorArgb: Int = 0xFFFFCC00.toInt(),
    val customActions: List<CustomPlayerAction> = emptyList()
)

object YandexMediaHub {
    const val YANDEX_MUSIC_PACKAGE = "ru.yandex.music"
    private const val DEFAULT_AMBER = 0xFFFFCC00.toInt()

    private val hubScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(YandexPlaybackUiState())
    val state: StateFlow<YandexPlaybackUiState> = _state.asStateFlow()

    var activeController: MediaController? = null
        private set

    private var repository: HistoryRepository? = null
    private var bridgeListener: (() -> Unit)? = null

    fun ensureRepository(context: Context) {
        if (repository == null) {
            val db = YaPulseDatabase.getInstance(context.applicationContext)
            repository = HistoryRepository(db.trackHistoryDao())
        }
    }

    fun registerSessionRefreshHook(hook: (() -> Unit)?) {
        bridgeListener = hook
    }

    fun setOnlyYandexMusic(onlyYandex: Boolean) {
        _state.value = _state.value.copy(onlyYandexMusic = onlyYandex)
        bridgeListener?.invoke()
    }

    fun hasNotificationAccess(context: Context): Boolean {
        return NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun updatePermission(hasPerm: Boolean) {
        _state.value = _state.value.copy(hasPermission = hasPerm)
    }

    fun attachController(controller: MediaController?) {
        activeController = controller
        if (controller == null) {
            _state.value = _state.value.copy(
                isConnected = false,
                sourcePackage = "",
                isYandexSource = false,
                isPlaying = false,
                customActions = emptyList()
            )
            return
        }
        val pkg = controller.packageName ?: ""
        _state.value = _state.value.copy(
            isConnected = true,
            sourcePackage = pkg,
            isYandexSource = pkg == YANDEX_MUSIC_PACKAGE
        )
        updateFromMetadata(controller.metadata, pkg)
        updateFromPlaybackState(controller.playbackState)
    }

    fun updateFromMetadata(metadata: MediaMetadata?, sourcePkg: String = activeController?.packageName ?: "") {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        if (title.isBlank()) return

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        _state.value = _state.value.copy(
            isConnected = true,
            sourcePackage = sourcePkg,
            isYandexSource = sourcePkg == YANDEX_MUSIC_PACKAGE,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            albumArt = art,
            accentColorArgb = if (art == null) DEFAULT_AMBER else _state.value.accentColorArgb
        )

        if (art != null) {
            hubScope.launch(Dispatchers.Default) {
                val extracted = extractVibrantAccent(art)
                _state.value = _state.value.copy(accentColorArgb = extracted)
            }
        }

        val repo = repository
        if (repo != null) {
            hubScope.launch {
                repo.recordTrackIfChanged(
                    title = title,
                    artist = artist,
                    album = album,
                    sourcePackage = sourcePkg,
                    durationMs = duration
                )
            }
        }
    }

    fun updateFromPlaybackState(playbackState: PlaybackState?) {
        if (playbackState == null) return
        val playing = playbackState.state == PlaybackState.STATE_PLAYING
        val actions = playbackState.customActions.map {
            CustomPlayerAction(action = it.action, name = it.name.toString())
        }
        _state.value = _state.value.copy(
            isConnected = true,
            isPlaying = playing,
            positionMs = playbackState.position.coerceAtLeast(0L),
            lastUpdatedMs = System.currentTimeMillis(),
            playbackSpeed = playbackState.playbackSpeed.takeIf { it > 0f } ?: 1f,
            customActions = actions
        )
    }

    fun playPause() {
        val controller = activeController ?: return
        if (_state.value.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun pause() {
        activeController?.transportControls?.pause()
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        activeController?.transportControls?.seekTo(target)
        _state.value = _state.value.copy(
            positionMs = target,
            lastUpdatedMs = System.currentTimeMillis()
        )
    }

    fun skipBy(deltaMs: Long) {
        val stateNow = _state.value
        val elapsed = if (stateNow.isPlaying) {
            ((System.currentTimeMillis() - stateNow.lastUpdatedMs) * stateNow.playbackSpeed).toLong()
        } else 0L
        val current = stateNow.positionMs + elapsed
        val maxDur = if (stateNow.durationMs > 0L) stateNow.durationMs else Long.MAX_VALUE
        seekTo((current + deltaMs).coerceIn(0L, maxDur))
    }

    fun sendCustomAction(action: String) {
        activeController?.transportControls?.sendCustomAction(action, null)
    }

    private fun extractVibrantAccent(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val hsv = FloatArray(3)
        var bestScore = -1f
        var bestColor = DEFAULT_AMBER
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                if (AndroidColor.alpha(pixel) < 128) continue
                AndroidColor.colorToHSV(pixel, hsv)
                val saturation = hsv[1]
                val brightness = hsv[2]
                if (brightness < 0.25f || saturation < 0.2f) continue
                val score = saturation * 2f + brightness
                if (score > bestScore) {
                    bestScore = score
                    bestColor = pixel
                }
            }
        }
        if (scaled != bitmap) scaled.recycle()
        return bestColor
    }
}

class YandexMediaBridge : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            YandexMediaHub.updateFromMetadata(metadata, currentController?.packageName ?: "")
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            YandexMediaHub.updateFromPlaybackState(state)
        }

        override fun onSessionDestroyed() {
            detachCurrent()
            findTargetSession()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            pickTargetController(controllers)
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        YandexMediaHub.ensureRepository(this)
        YandexMediaHub.updatePermission(true)
        YandexMediaHub.registerSessionRefreshHook { findTargetSession() }

        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(this, YandexMediaBridge::class.java)
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            findTargetSession()
        } catch (_: SecurityException) {
            YandexMediaHub.updatePermission(false)
        }
    }

    override fun onListenerDisconnected() {
        YandexMediaHub.registerSessionRefreshHook(null)
        detachCurrent()
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {
        }
        super.onListenerDisconnected()
    }

    private fun findTargetSession() {
        val component = ComponentName(this, YandexMediaBridge::class.java)
        val controllers = try {
            sessionManager?.getActiveSessions(component)
        } catch (_: SecurityException) {
            null
        }
        pickTargetController(controllers)
    }

    private fun pickTargetController(controllers: List<MediaController>?) {
        val onlyYandex = YandexMediaHub.state.value.onlyYandexMusic
        val yandexController = controllers?.firstOrNull {
            it.packageName == YandexMediaHub.YANDEX_MUSIC_PACKAGE
        }
        val targetController = if (onlyYandex) {
            yandexController
        } else {
            yandexController ?: controllers?.firstOrNull()
        }

        if (targetController?.sessionToken == currentController?.sessionToken) return

        detachCurrent()
        currentController = targetController
        currentController?.registerCallback(controllerCallback)
        YandexMediaHub.attachController(currentController)
    }

    private fun detachCurrent() {
        currentController?.unregisterCallback(controllerCallback)
        currentController = null
        YandexMediaHub.attachController(null)
    }
}
