package com.example.streamfilx_androidtv.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private val BG = Color(0xFF141414)
private val CARD_BG = Color(0xFF1A1A1A)
private val ACCENT_RED = Color(0xFFE50914)
private val ACCENT_BLUE = Color(0xFF3B82F6)
private val ACCENT_GREEN = Color(0xFF22C55E)
private val ACCENT_ORANGE = Color(0xFFF97316)
private val ACCENT_YELLOW = Color(0xFFEAB308)
private val WHITE70 = Color.White.copy(0.7f)
private val WHITE40 = Color.White.copy(0.4f)

private enum class SettingsSection {
    ACCOUNTS, PLAYBACK, STORAGE, ADDONS, ABOUT
}

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }

    BackHandler(enabled = currentSection != null) { currentSection = null }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        when (currentSection) {
            null -> SettingsHub(
            onSection = { currentSection = it },
            onSignOut = { viewModel.signOut(onSignedOut) },
            userEmail = state.supabaseEmail,
        )
            SettingsSection.ACCOUNTS -> AccountsSection(
                state = state,
                vm = viewModel,
                onBack = { currentSection = null },
                onSignedOut = onSignedOut,
            )
            SettingsSection.PLAYBACK -> PlaybackSection(
                state = state,
                vm = viewModel,
                onBack = { currentSection = null },
            )
            SettingsSection.STORAGE -> StorageSection(
                state = state,
                vm = viewModel,
                onBack = { currentSection = null },
            )
            SettingsSection.ADDONS -> AddonsSection(
                state = state,
                vm = viewModel,
                onBack = { currentSection = null },
            )
            SettingsSection.ABOUT -> AboutSection(
                state = state,
                vm = viewModel,
                onBack = { currentSection = null },
                onSignedOut = onSignedOut,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hub
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsHub(onSection: (SettingsSection) -> Unit, onSignOut: () -> Unit, userEmail: String?) {
    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionPageTitle("Settings") }
        // Quick sign-out row at the top
        userEmail?.let { email ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CARD_BG)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Signed in as", color = WHITE70, fontSize = 12.sp)
                        Text(email, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.colors(containerColor = ACCENT_RED.copy(0.15f)),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    ) { Text("Sign Out", color = ACCENT_RED, fontSize = 14.sp) }
                }
            }
        }
        item {
            SectionLink("👤  Accounts", "Real-Debrid, OpenSubtitles, StreamFlix") { onSection(SettingsSection.ACCOUNTS) }
        }
        item {
            SectionLink("▶️  Playback", "Audio language, subtitles, quality") { onSection(SettingsSection.PLAYBACK) }
        }
        item {
            SectionLink("💾  Storage", "Cache size and management") { onSection(SettingsSection.STORAGE) }
        }
        item {
            SectionLink("🧩  Addons", "Installed addons and custom sources") { onSection(SettingsSection.ADDONS) }
        }
        item {
            SectionLink("ℹ️  About", "App info and factory reset") { onSection(SettingsSection.ABOUT) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionLink(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CARD_BG)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = WHITE70, fontSize = 13.sp)
        }
        Text("›", color = WHITE40, fontSize = 22.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accounts
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccountsSection(
    state: SettingsUiState,
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Accounts", onBack) }

        // StreamFlix Account
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("StreamFlix Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Signed in as", color = WHITE70, fontSize = 13.sp)
                        Text(state.supabaseEmail ?: "—", color = Color.White, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { vm.signOut(onSignedOut) },
                        colors = ButtonDefaults.colors(containerColor = ACCENT_RED.copy(0.15f)),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    ) {
                        if (state.isSigningOut) {
                            CircularProgressIndicator(color = ACCENT_RED, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Sign Out", color = ACCENT_RED, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Real-Debrid
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("☁️  Real-Debrid", color = ACCENT_GREEN, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Provides high-speed cached downloads for instant streaming.", color = WHITE70, fontSize = 13.sp)
                    SecretField(
                        label = "API Key",
                        value = state.rdApiKey,
                        visible = state.rdKeyVisible,
                        onValueChange = vm::setRdApiKey,
                        onToggleVisible = vm::toggleRdKeyVisible,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = vm::validateRdKey,
                            enabled = state.rdApiKey.isNotBlank() && !state.rdValidating,
                            colors = ButtonDefaults.colors(containerColor = ACCENT_BLUE.copy(0.2f)),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        ) {
                            if (state.rdValidating) CircularProgressIndicator(color = ACCENT_BLUE, modifier = Modifier.size(14.dp))
                            else Text("Validate Key", color = ACCENT_BLUE, fontSize = 14.sp)
                        }
                        state.rdUser?.let { Text("✅ ${it.username}", color = ACCENT_GREEN, fontSize = 14.sp) }
                        state.rdError?.let { Text("❌ $it", color = ACCENT_RED, fontSize = 13.sp) }
                    }
                }
            }
        }

        // OpenSubtitles
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💬  OpenSubtitles", color = ACCENT_YELLOW, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Automatic subtitle downloading for movies and TV shows.", color = WHITE70, fontSize = 13.sp)
                    SecretField(
                        label = "API Key",
                        value = state.osApiKey,
                        visible = state.osApiKeyVisible,
                        onValueChange = vm::setOsApiKey,
                        onToggleVisible = vm::toggleOsApiKeyVisible,
                    )
                    SettingsTextField("Username (Optional)", state.osUsername, vm::setOsUsername)
                    SecretField(
                        label = "Password (Optional)",
                        value = state.osPassword,
                        visible = state.osPasswordVisible,
                        onValueChange = vm::setOsPassword,
                        onToggleVisible = vm::toggleOsPasswordVisible,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = vm::validateOs,
                            enabled = state.osApiKey.isNotBlank() && !state.osValidating,
                            colors = ButtonDefaults.colors(containerColor = ACCENT_YELLOW.copy(0.2f)),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        ) {
                            if (state.osValidating) CircularProgressIndicator(color = ACCENT_YELLOW, modifier = Modifier.size(14.dp))
                            else Text("Validate", color = ACCENT_YELLOW, fontSize = 14.sp)
                        }
                        if (state.osLoggedIn) Text("✅ Logged in as ${state.osUsername}", color = ACCENT_GREEN, fontSize = 13.sp)
                        state.osError?.let { Text("❌ $it", color = ACCENT_RED, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playback
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackSection(state: SettingsUiState, vm: SettingsViewModel, onBack: () -> Unit) {
    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Playback", onBack) }

        // Video Quality
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Video Quality", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    SegmentedPicker(
                        options = QUALITY_OPTIONS.map { it.second },
                        selected = QUALITY_OPTIONS.indexOfFirst { it.first == state.defaultQuality }.coerceAtLeast(0),
                        onSelect = { vm.setDefaultQuality(QUALITY_OPTIONS[it].first) },
                    )
                }
            }
        }

        // Audio
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Preferred audio track language", color = WHITE70, fontSize = 13.sp)
                    LangPicker("Primary Language", state.primaryAudioLang, vm::setPrimaryAudioLang)
                    LangPicker("Secondary Language (Fallback)", state.secondaryAudioLang, vm::setSecondaryAudioLang)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LangBadge(state.primaryAudioLang)
                        Text("→", color = WHITE40, fontSize = 16.sp)
                        LangBadge(state.secondaryAudioLang)
                    }
                }
            }
        }

        // Behavior
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Behavior", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    ToggleRow("Auto-play next episode", state.autoPlayNext, vm::setAutoPlayNext)
                }
            }
        }

        // Subtitles
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subtitles", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    ToggleRow("Enable Subtitles", state.subtitlesEnabled, vm::setSubtitlesEnabled)
                    if (state.subtitlesEnabled) {
                        LangPicker("Preferred Language", state.subtitleLang, vm::setSubtitleLang)
                        Text("💬 ${langName(state.subtitleLang)} subtitles enabled", color = ACCENT_GREEN, fontSize = 13.sp)
                    }
                    Text("Text Size", color = WHITE70, fontSize = 13.sp)
                    SegmentedPicker(
                        options = SUBTITLE_TEXT_SIZES.map { it.second },
                        selected = SUBTITLE_TEXT_SIZES.indexOfFirst { it.first == state.subtitleTextSize }.coerceAtLeast(0),
                        onSelect = { vm.setSubtitleTextSize(SUBTITLE_TEXT_SIZES[it].first) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Storage
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StorageSection(state: SettingsUiState, vm: SettingsViewModel, onBack: () -> Unit) {
    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Storage", onBack) }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💾  Image Cache Size", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Maximum disk space for caching posters and thumbnails.", color = WHITE70, fontSize = 13.sp)
                    SegmentedPicker(
                        options = listOf("2 GB", "4 GB", "6 GB", "8 GB"),
                        selected = listOf(2, 4, 6, 8).indexOf(state.imageCacheGb).coerceAtLeast(0),
                        onSelect = { vm.setImageCacheGb(listOf(2, 4, 6, 8)[it]) },
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Usage", color = WHITE70, fontSize = 13.sp)
                        Text("${state.cacheDiskUsedMb} MB of ${state.imageCacheGb} GB", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🗑  Clear Cache", color = ACCENT_ORANGE, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Remove all cached images and API responses.", color = WHITE70, fontSize = 13.sp)
                    Button(
                        onClick = vm::clearCaches,
                        enabled = !state.isClearingCache,
                        colors = ButtonDefaults.colors(containerColor = ACCENT_ORANGE.copy(0.2f)),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    ) {
                        if (state.isClearingCache) {
                            CircularProgressIndicator(color = ACCENT_ORANGE, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clearing…", color = ACCENT_ORANGE, fontSize = 14.sp)
                        } else {
                            Text("Clear All Caches", color = ACCENT_ORANGE, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Addons
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddonsSection(state: SettingsUiState, vm: SettingsViewModel, onBack: () -> Unit) {
    // Auto-dismiss addon add result after 3s
    LaunchedEffect(state.addonAddResult) {
        if (state.addonAddResult != null) {
            delay(3_000)
            vm.clearAddonResult()
        }
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Addons", onBack) }

        // Installed list
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Installed Addons", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    state.installedAddons.forEachIndexed { i, addon ->
                        if (i > 0) Spacer(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(0.07f)))
                        AddonRow(addon = addon, onToggle = { vm.toggleAddon(addon.baseUrl) }, onRemove = { vm.removeAddon(addon.baseUrl) })
                    }
                }
            }
        }

        // Add custom addon
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Custom Addon", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.addonUrlInput,
                            onValueChange = vm::setAddonUrlInput,
                            label = { androidx.compose.material3.Text("Addon manifest URL", color = WHITE70, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = settingsTextFieldColors(),
                        )
                        Button(
                            onClick = vm::addAddon,
                            enabled = state.addonUrlInput.isNotBlank() && !state.isAddingAddon,
                            colors = ButtonDefaults.colors(containerColor = ACCENT_BLUE.copy(0.2f)),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        ) {
                            if (state.isAddingAddon) CircularProgressIndicator(color = ACCENT_BLUE, modifier = Modifier.size(14.dp))
                            else Text("Add", color = ACCENT_BLUE, fontSize = 14.sp)
                        }
                    }
                    state.addonAddResult?.let {
                        Text(it, color = if (it.startsWith("✅")) ACCENT_GREEN else ACCENT_RED, fontSize = 13.sp)
                    }
                    Text("Enter the full URL to an addon's manifest", color = WHITE40, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddonRow(addon: InstalledAddon, onToggle: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (addon.enabled) ACCENT_GREEN else WHITE40, shape = RoundedCornerShape(50)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(addon.manifest.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (addon.isBuiltIn) {
                    Text("Built-in", color = WHITE40, fontSize = 11.sp,
                        modifier = Modifier.background(WHITE40.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(addon.manifest.description, color = WHITE70, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!addon.isBuiltIn) {
            Switch(
                checked = addon.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = ACCENT_GREEN, checkedTrackColor = ACCENT_GREEN.copy(0.3f)),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = ACCENT_RED.copy(0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutSection(
    state: SettingsUiState,
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    if (state.showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissFactoryResetDialog,
            title = { androidx.compose.material3.Text("Factory Reset", color = Color.White) },
            text = {
                androidx.compose.material3.Text(
                    "This will delete all your data including profiles, watch history, favorites, downloaded content, API keys, and all settings. This action cannot be undone.",
                    color = WHITE70,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.performFactoryReset(onSignedOut) }) {
                    androidx.compose.material3.Text("Reset Everything", color = ACCENT_RED)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissFactoryResetDialog) {
                    androidx.compose.material3.Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1F1F1F),
        )
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("About", onBack) }

        // App info
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("▶️  StreamFlix", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0", color = WHITE70, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("A native streaming app with Stremio addon support and Real-Debrid integration.", color = WHITE70, fontSize = 13.sp)
                }
            }
        }

        // Features
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Features", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    listOf(
                        "🎬 Browse movies & TV shows from Cinemeta",
                        "⚡ Instant playback via Real-Debrid",
                        "🔊 TrueHD & DTS-HD audio support",
                        "💬 OpenSubtitles integration",
                        "⬇️ Offline downloads",
                    ).forEach { Text(it, color = WHITE70, fontSize = 13.sp) }
                }
            }
        }

        // Danger zone
        item {
            SettingsCard(bgColor = ACCENT_RED.copy(0.08f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danger Zone", color = ACCENT_RED, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("These actions cannot be undone.", color = WHITE70, fontSize = 13.sp)
                    Button(
                        onClick = vm::showFactoryResetDialog,
                        colors = ButtonDefaults.colors(containerColor = ACCENT_RED),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    ) {
                        Text("Factory Reset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Removes all profiles, watch history, favorites, API keys, downloads, and settings.", color = WHITE40, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI components
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
        SectionPageTitle(title)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionPageTitle(title: String) {
    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsCard(bgColor: Color = CARD_BG, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(label, color = Color.White, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = ACCENT_GREEN, checkedTrackColor = ACCENT_GREEN.copy(0.3f)),
        )
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { androidx.compose.material3.Text(label, color = WHITE70, fontSize = 12.sp) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = WHITE70,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = settingsTextFieldColors(),
    )
}

@Composable
private fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { androidx.compose.material3.Text(label, color = WHITE70, fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = settingsTextFieldColors(),
    )
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = ACCENT_BLUE,
    unfocusedBorderColor = WHITE40,
    cursorColor = ACCENT_BLUE,
)

@Composable
private fun SegmentedPicker(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ACCENT_BLUE else Color.White.copy(0.1f))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (isSelected) Color.White else WHITE70,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun LangPicker(label: String, selectedCode: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = langName(selectedCode)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.Text(label, color = WHITE70, fontSize = 12.sp)
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.1f))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text(name, color = Color.White, fontSize = 14.sp)
                androidx.compose.material3.Text("▾", color = WHITE40, fontSize = 14.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AUDIO_LANGUAGES.forEach { (code, langName) ->
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(langName) },
                        onClick = { onSelect(code); expanded = false },
                        trailingIcon = {
                            if (code == selectedCode) Icon(Icons.Default.Check, null, tint = ACCENT_BLUE, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LangBadge(code: String) {
    Box(
        modifier = Modifier
            .background(ACCENT_BLUE.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        androidx.compose.material3.Text(langName(code), color = ACCENT_BLUE, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun langName(code: String) = AUDIO_LANGUAGES.firstOrNull { it.first == code }?.second ?: code
