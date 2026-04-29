package com.example.streamfilx_androidtv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.AppViewModel
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.features.auth.LoginScreen
import com.example.streamfilx_androidtv.features.browse.BrowseScreen
import com.example.streamfilx_androidtv.features.detail.ContentDetailScreen
import com.example.streamfilx_androidtv.features.detail.StreamSelectionSheet
import com.example.streamfilx_androidtv.features.downloads.DownloadsScreen
import com.example.streamfilx_androidtv.features.home.HomeScreen
import com.example.streamfilx_androidtv.features.settings.SettingsScreen
import com.example.streamfilx_androidtv.features.library.LibraryScreen
import com.example.streamfilx_androidtv.features.main.TopNavBar
import com.example.streamfilx_androidtv.features.search.SearchScreen
import com.example.streamfilx_androidtv.features.main.TOP_NAV_TABS
import com.example.streamfilx_androidtv.features.profile.ProfileEditorScreen
import com.example.streamfilx_androidtv.features.profile.ProfilePickerScreen

// ── Top-level routes that show the nav bar ────────────────────────────────────

private val TOP_LEVEL_ROUTES = TOP_NAV_TABS.map { it.route }.toSet()

// ── Profile section nav state ─────────────────────────────────────────────────

private sealed class ProfileNav {
    object Picker : ProfileNav()
    object Create : ProfileNav()
    data class Edit(val profileId: String) : ProfileNav()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppNavigation(appViewModel: AppViewModel) {
    val isBootstrapping by appViewModel.isBootstrapping.collectAsState()
    val isSignedIn by appViewModel.isSignedIn.collectAsState()
    val selectedProfile by appViewModel.selectedProfile.collectAsState()

    when {
        isBootstrapping -> SplashScreen()
        !isSignedIn -> LoginScreen(onSignedIn = { appViewModel.onSignedIn() })
        selectedProfile == null -> ProfileSection(appViewModel = appViewModel)
        else -> MainNavHost(appViewModel = appViewModel)
    }
}

// ── Splash ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "STREAMFLIX",
            color = Color(0xFFE50914),
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
        )
    }
}

// ── Profile section ───────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(appViewModel: AppViewModel) {
    var profileNav by remember { mutableStateOf<ProfileNav>(ProfileNav.Picker) }

    when (val nav = profileNav) {
        is ProfileNav.Picker -> ProfilePickerScreen(
            onProfileSelected = { appViewModel.onProfileSelected(it) },
            onCreateProfile = { profileNav = ProfileNav.Create },
            onEditProfile = { profileNav = ProfileNav.Edit(it) },
        )
        is ProfileNav.Create -> ProfileEditorScreen(
            profileId = null,
            onSaved = { profileNav = ProfileNav.Picker },
            onDeleted = { profileNav = ProfileNav.Picker },
            onBack = { profileNav = ProfileNav.Picker },
        )
        is ProfileNav.Edit -> ProfileEditorScreen(
            profileId = nav.profileId,
            onSaved = { profileNav = ProfileNav.Picker },
            onDeleted = { profileNav = ProfileNav.Picker },
            onBack = { profileNav = ProfileNav.Picker },
        )
    }
}

// ── Main nav host with persistent top nav bar ─────────────────────────────────

@Composable
fun MainNavHost(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Hero Play button: show StreamSelectionSheet as a full-screen overlay
    var heroPlayTarget by remember { mutableStateOf<MetaPreview?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Show top nav bar only on top-level tab screens
            if (currentRoute in TOP_LEVEL_ROUTES || currentRoute == null) {
                TopNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Screen.Home.route) { saveState = true }
                        }
                    },
                )
            }

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                // ── Tab screens ───────────────────────────────────────────────────
                composable(Screen.Home.route) {
                    HomeScreen(
                        navController = navController,
                        onPlayClick = { meta -> heroPlayTarget = meta },
                    )
                }
            composable(Screen.Browse.route) {
                BrowseScreen(navController = navController)
            }
            composable(Screen.Search.route) {
                SearchScreen(navController = navController)
            }
            composable(Screen.Library.route) {
                LibraryScreen(navController = navController)
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onPlayDownload = { item -> appViewModel.startPlayback(item) },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onSignedOut = { appViewModel.onSignedOut() })
            }

            // ── Detail screen ─────────────────────────────────────────────────
            composable(Screen.Detail.ROUTE) {
                ContentDetailScreen(
                    navController = navController,
                    onStartPlayback = { item -> appViewModel.startPlayback(item) },
                )
            }

            // ── Profile screens (accessible from within main) ─────────────────
            composable(Screen.ProfilePicker.route) {
                ProfilePickerScreen(
                    onProfileSelected = {
                        appViewModel.onProfileSelected(it)
                        navController.popBackStack()
                    },
                    onCreateProfile = { navController.navigate(Screen.ProfileEditor.route(null)) },
                    onEditProfile = { navController.navigate(Screen.ProfileEditor.route(it)) },
                )
            }
            composable(Screen.ProfileEditor.ROUTE) { back ->
                val profileId = back.arguments?.getString("profileId")?.takeIf { it != "null" }
                ProfileEditorScreen(
                    profileId = profileId,
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }

        // Stream sheet overlay for hero Play button
        heroPlayTarget?.let { meta ->
            StreamSelectionSheet(
                type = meta.type,
                id = meta.id,
                onStreamResolved = { url, infoHash, fileIdx, quality ->
                    heroPlayTarget = null
                    appViewModel.startPlayback(
                        com.example.streamfilx_androidtv.core.models.NowPlayingItem(
                            id = meta.id,
                            type = meta.type,
                            title = meta.name,
                            streamUrl = url,
                            posterUrl = meta.poster,
                            year = meta.year,
                            genres = meta.genres,
                            imdbRating = meta.imdbRating,
                            streamQuality = quality,
                            streamInfoHash = infoHash,
                            streamFileIdx = fileIdx,
                        )
                    )
                },
                onDismiss = { heroPlayTarget = null },
            )
        }
    }
}

