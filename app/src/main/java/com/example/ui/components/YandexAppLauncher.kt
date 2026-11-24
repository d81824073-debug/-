package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.service.YandexMediaHub
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.YandexAmber

object YandexNavigator {
    fun openYandexMusic(context: Context): Boolean {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(YandexMediaHub.YANDEX_MUSIC_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun openDeepLink(context: Context, uriString: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage(YandexMediaHub.YANDEX_MUSIC_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openYandexMusic(context)
        }
    }

    fun shareTrack(context: Context, artist: String, title: String) {
        val trackText = if (artist.isNotBlank()) "$artist — $title" else title
        val shareBody = "Слушаю в Яндекс Музыке: $trackText"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.toast_share_chooser)
        )
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun NeonVinylDisc(
    albumArt: android.graphics.Bitmap?,
    isPlaying: Boolean,
    accentColor: Color,
    size: Dp = 240.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Rotating vinyl platter
        Box(
            modifier = Modifier
                .size(size - 14.dp)
                .rotate(if (isPlaying) angle else 0f)
                .clip(CircleShape)
                .background(Color(0xFF101016))
                .border(2.5.dp, accentColor.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_album_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_vinyl_placeholder),
                    contentDescription = stringResource(R.string.cd_album_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Concentric vinyl grooves overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxR = this.size.minDimension / 2f
                listOf(0.92f, 0.84f, 0.76f, 0.42f).forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = maxR * factor,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // Center label ring & spindle hole
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AmoledBlack)
                    .border(2.dp, accentColor.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
}

@Composable
fun PulseVisualizerBars(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "bars")
    val barMultipliers = listOf(0.45f, 0.9f, 0.65f, 1.0f, 0.55f, 0.8f, 0.4f).mapIndexed { idx, base ->
        val anim by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = base,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 360 + idx * 75),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$idx"
        )
        if (isPlaying) anim else 0.2f
    }

    Row(
        modifier = modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barMultipliers.forEach { level ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((22f * level).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isPlaying) accentColor else YandexAmber.copy(alpha = 0.35f))
            )
        }
    }
}
