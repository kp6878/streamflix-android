package com.example.streamfilx_androidtv.features.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.services.AvatarGradients
import com.example.streamfilx_androidtv.services.Profile

private val DARK_BG   = Color(0xFF0A0A0A)
private val ACCENT    = Color(0xFFE50914)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onProfileSelected: (Profile) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profiles  by viewModel.profiles.collectAsState()
    val canAddMore by viewModel.canAddMore.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }

    // Focus the first card automatically
    val firstCardFocusRequester = remember { FocusRequester() }
    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            firstCardFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DARK_BG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isEditMode) "Manage Profiles" else "Who's Watching?",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
                TextButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Profile cards in a horizontally scrollable TvLazyRow
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(profiles) { index, profile ->
                    ProfileCard(
                        profile = profile,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) onEditProfile(profile.id)
                            else onProfileSelected(profile)
                        },
                        modifier = if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier,
                    )
                }

                // "Add Profile" card at the end
                if (!isEditMode && canAddMore) {
                    item {
                        AddProfileCard(onClick = onCreateProfile)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    isEditMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val gradientColors = AvatarGradients.colors(profile.avatarGradient)

    // Animate scale: 1.12f when focused, 1.0f otherwise
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "profile-card-scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(width = 120.dp, height = 140.dp)
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(CircleShape),
            colors = CardDefaults.colors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 2dp red border on focus, transparent otherwise
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        color = if (isFocused) ACCENT else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(gradientColors.start),
                                Color(gradientColors.end),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.initial,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "✏", fontSize = 28.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = profile.name,
            color = if (isFocused) Color.White else Color(0xFFB3B3B3),
            fontSize = 16.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (profile.isKidsProfile) {
            Spacer(Modifier.height(2.dp))
            Text(text = "Kids", color = ACCENT, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "add-card-scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(width = 120.dp, height = 140.dp)
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(CircleShape),
            colors = CardDefaults.colors(containerColor = Color(0xFF1F1F1F)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) ACCENT else Color(0xFF444444),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = if (isFocused) Color.White else Color(0xFFB3B3B3),
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Add Profile",
            color = if (isFocused) Color.White else Color(0xFFB3B3B3),
            fontSize = 16.sp,
        )
    }
}
