package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.YaPulseApp
import com.example.ui.theme.YaPulseTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
    }

    private val viewModel: MainViewModel by viewModels()
    private val pendingTabState = mutableIntStateOf(0)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntentTab(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            YaPulseTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(pendingTabState.intValue) }

                val requestedFromIntent = pendingTabState.intValue
                if (requestedFromIntent != selectedTab && requestedFromIntent in 0..3) {
                    DisposableEffect(requestedFromIntent) {
                        selectedTab = requestedFromIntent
                        onDispose { }
                    }
                }

                DisposableEffect(selectedTab) {
                    if (selectedTab == 2) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                YaPulseApp(
                    viewModel = viewModel,
                    selectedTab = selectedTab,
                    onSelectTab = { newTab ->
                        pendingTabState.intValue = newTab
                        selectedTab = newTab
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentTab(intent)
    }

    private fun handleIntentTab(intent: Intent?) {
        val tab = intent?.getIntExtra(EXTRA_OPEN_TAB, -1) ?: -1
        if (tab in 0..3) {
            pendingTabState.intValue = tab
        }
    }
}
