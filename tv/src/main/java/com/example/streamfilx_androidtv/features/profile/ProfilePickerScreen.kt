package com.example.streamfilx_androidtv.features.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.services.AvatarGradients
import com.example.streamfilx_androidtv.services.Profile

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onProfileSelected: (Profile) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsState()
    val canAddMore by viewModel.canAddMore.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header row
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

            // Profile cards row — use TV Card so D-pad + OK button work
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                profiles.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) onEditProfile(profile.id)
                            else onProfileSelected(profile)
                        },
                    )
                }
                if (!isEditMode && canAddMore) {
                    AddProfileCard(onClick = onCreateProfile)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(profile: Profile, isEditMode: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val gradientColors = AvatarGradients.colors(profile.avatarGradient)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // TV Card handles D-pad focus + OK button natively
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(112.dp)
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(CircleShape),
            colors = CardDefaults.colors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        color = if (isFocused) Color.White else Color.Transparent,
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
            Text(text = "Kids", color = Color(0xFFE50914), fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF444444),
        label = "border",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(112.dp)
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(CircleShape),
            colors = CardDefaults.colors(containerColor = Color(0xFF1F1F1F)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    color = Color(0xFFB3B3B3),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
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
