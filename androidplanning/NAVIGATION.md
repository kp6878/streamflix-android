# Navigation

## Structure

Android uses **Navigation Compose** with a single `NavHost` in `MainActivity`. The player runs as a full-screen composable overlay (same pattern as iOS ZIndex overlay).

```
MainActivity
└── NavHost
    ├── LoginScreen                    (route: "login")
    ├── ProfilePickerScreen            (route: "profile_picker")
    ├── MainScreen                     (Scaffold with BottomBar)
    │   ├── HomeScreen                 (route: "home")
    │   ├── BrowseScreen               (route: "browse")
    │   ├── SearchScreen               (route: "search")
    │   ├── LibraryScreen              (route: "library")
    │   ├── DownloadsScreen            (route: "downloads")
    │   └── SettingsScreen             (route: "settings")
    ├── ContentDetailScreen            (route: "detail/{type}/{id}")
    ├── ProfileEditorScreen            (route: "profile_editor/{profileId?}")
    └── PlayerScreen                   (route: "player") ← fullscreen Dialog/overlay
```

---

## Screen Sealed Class

```kotlin
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProfilePicker : Screen("profile_picker")
    object Home : Screen("home")
    object Browse : Screen("browse")
    object Search : Screen("search")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object Player : Screen("player")

    data class Detail(val type: String, val id: String) : Screen("detail/{type}/{id}") {
        companion object {
            fun route(type: String, id: String) = "detail/$type/$id"
        }
    }

    data class ProfileEditor(val profileId: String? = null) : Screen("profile_editor/{profileId}") {
        companion object {
            fun route(profileId: String? = null) = "profile_editor/$profileId"
        }
    }
}
```

---

## Bottom Navigation Tabs

```kotlin
val bottomNavItems = listOf(
    BottomNavItem("home",      "Home",      Icons.Default.Home),
    BottomNavItem("browse",    "Browse",    Icons.Default.Explore),
    BottomNavItem("search",    "Search",    Icons.Default.Search),
    BottomNavItem("library",   "My List",   Icons.Default.FavoriteBorder),
    BottomNavItem("downloads", "Downloads", Icons.Default.Download),
    BottomNavItem("settings",  "Settings",  Icons.Default.Settings),
)
```

The bottom bar is hidden when `PlayerScreen` is active (full-screen).

---

## Auth Gate (mirrors iOS RootView)

```kotlin
@Composable
fun AppNavigation(appViewModel: AppViewModel) {
    val isSignedIn by appViewModel.isSignedIn.collectAsState()
    val isBootstrapping by appViewModel.isBootstrapping.collectAsState()

    when {
        isBootstrapping -> SplashScreen()
        !isSignedIn     -> LoginScreen()
        else            -> MainNavHost()
    }
}
```

---

## Player Navigation

The player is launched via `AppViewModel.startPlayback(item)` — same pattern as iOS `appState.startPlayback`. The PlayerScreen composable sits at the top of the `ContentView` ZStack equivalent using `Box`:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    MainNavHost(...)  // background content

    val nowPlaying by appViewModel.nowPlaying.collectAsState()
    if (nowPlaying != null) {
        PlayerScreen(
            item = nowPlaying!!,
            onDismiss = { appViewModel.stopPlayback() }
        )
    }
}
```

---

## Sheet Navigation

- `StreamSelectionSheet` → `ModalBottomSheet` composable (not a separate NavHost destination)
- `ProfileEditorScreen` → navigated via NavController (full screen, matches iOS modal)

---

## Back Stack Behavior

- Home, Browse, Search, Library, Downloads, Settings use `launchSingleTop = true` and `popUpTo(Screen.Home.route) { saveState = true }` to preserve tab state on switch (standard Android tab behavior)
- Detail screen pushed on top of current tab, back pops it
- Player overlay dismisses without NavController pop (controlled by `AppViewModel.stopPlayback()`)
