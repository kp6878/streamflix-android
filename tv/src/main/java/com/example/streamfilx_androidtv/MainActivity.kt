package com.example.streamfilx_androidtv

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.streamfilx_androidtv.features.player.PlayerScreen
import com.example.streamfilx_androidtv.navigation.AppNavigation
import com.example.streamfilx_androidtv.ui.theme.StreamFlixTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lock to landscape when player is open, restore when closed
        lifecycleScope.launch {
            appViewModel.nowPlaying.collect { nowPlaying ->
                requestedOrientation = if (nowPlaying != null) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        setContent {
            StreamFlixTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(appViewModel = appViewModel)

                        val nowPlaying by appViewModel.nowPlaying.collectAsState()
                        nowPlaying?.let { item ->
                            PlayerScreen(
                                nowPlaying = item,
                                onClose = { appViewModel.stopPlayback() },
                                onNextEpisode = { appViewModel.stopPlayback() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(100f),
                            )
                        }
                    }
                }
            }
        }
    }
}
