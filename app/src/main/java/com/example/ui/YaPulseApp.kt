package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.CarModeScreen
import com.example.ui.screens.HistoryStatsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.StudioToolsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.YandexAmber
import kotlinx.coroutines.launch

private data class NavDestination(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(0, R.string.nav_player, Icons.Rounded.Album, Icons.Outlined.Album, "nav_player"),
    NavDestination(1, R.string.nav_studio, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_studio"),
    NavDestination(2, R.string.nav_car, Icons.Rounded.DirectionsCar, Icons.Outlined.DirectionsCar, "nav_car"),
    NavDestination(3, R.string.nav_history, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_history")
)

@Composable
fun YaPulseApp(
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val featuresState by viewModel.featuresState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val totalPlays by viewModel.totalPlays.collectAsStateWithLifecycle()
    val uniqueArtists by viewModel.uniqueArtists.collectAsStateWithLifecycle()
    val topArtists by viewModel.topArtists.collectAsStateWithLifecycle()
    val recentTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedTab != 0) {
        onSelectTab(0)
    }

    val accentColor = Color(mediaState.accentColorArgb)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.28f),
            AmoledBlack,
            AmoledBlack
        )
    )

    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = ElevatedObsidian) {
                        Destinations.forEach { dest ->
                            val selected = selectedTab == dest.index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(dest.index) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = YandexAmber,
                                    indicatorColor = YandexAmber,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ElevatedObsidian,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Destinations.forEach { dest ->
                            val selected = selectedTab == dest.index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(dest.index) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = YandexAmber,
                                    indicatorColor = YandexAmber,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (!mediaState.hasPermission) {
                        NotificationAccessBanner(
                            onGrantClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> PlayerScreen(
                                mediaState = mediaState,
                                sleepSecondsRemaining = featuresState.sleepSecondsRemaining,
                                onBookmarkMoment = {
                                    if (viewModel.saveCurrentBookmark()) {
                                        showMessage(context.getString(R.string.toast_bookmark_saved))
                                    } else {
                                        showMessage("Включите трек, чтобы сохранить таймкод")
                                    }
                                },
                                onShowMessage = showMessage
                            )

                            1 -> StudioToolsScreen(
                                featuresState = featuresState,
                                audioState = audioState,
                                onlyYandexMusic = settingsState.onlyYandexMusic,
                                onSetOnlyYandex = viewModel::setOnlyYandexMusic,
                                onStartSleepTimer = viewModel::startSleepTimer,
                                onStopAtTrackEnd = viewModel::startStopAtEndOfTrack,
                                onCancelSleepTimer = viewModel::cancelSleepTimer,
                                onToggleShakeToSkip = viewModel::setShakeToSkip,
                                onChangeShakeSensitivity = viewModel::setShakeSensitivity,
                                onToggleHaptic = viewModel::setHapticFeedback,
                                onToggleEq = viewModel::toggleEqEnabled,
                                onSelectPreset = viewModel::selectEqPreset,
                                onChangeBass = viewModel::setBassStrength,
                                onChangeVirtualizer = viewModel::setVirtualizerStrength,
                                onToggleLoudness = viewModel::setLoudnessBoost
                            )

                            2 -> CarModeScreen(
                                mediaState = mediaState,
                                eqEnabled = audioState.isEnabled,
                                onToggleEq = viewModel::toggleEqEnabled
                            )

                            3 -> HistoryStatsScreen(
                                totalPlays = totalPlays,
                                uniqueArtists = uniqueArtists,
                                topArtists = topArtists,
                                recentTracks = recentTracks,
                                bookmarks = bookmarks,
                                currentTrackTitle = mediaState.title,
                                searchQuery = searchQuery,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onDeleteBookmark = viewModel::deleteBookmark,
                                onClearHistory = viewModel::clearHistory,
                                onShowMessage = showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAccessBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, YandexAmber.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261F09)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = YandexAmber)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
                Text(
                    text = stringResource(R.string.permission_banner_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YandexAmber,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("grant_notification_access_button")
            ) {
                Text(stringResource(R.string.permission_banner_btn))
            }
        }
    }
}
