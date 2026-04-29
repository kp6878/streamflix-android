package com.example.streamfilx_androidtv.features.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.services.AvatarGradients

private val DARK_BG = Color(0xFF0A0A0A)
private val ACCENT  = Color(0xFFE50914)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: String?,         // null = create mode, non-null = edit mode
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val isCreateMode = profileId == null
    val editorState  by viewModel.editorState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Focus requesters for top-to-bottom D-pad traversal
    val nameFocusRequester    = remember { FocusRequester() }
    val swatchFocusRequester  = remember { FocusRequester() }
    val kidsNoFocusRequester  = remember { FocusRequester() }
    val kidsYesFocusRequester = remember { FocusRequester() }
    val saveFocusRequester    = remember { FocusRequester() }
    val cancelFocusRequester  = remember { FocusRequester() }
    val deleteFocusRequester  = remember { FocusRequester() }

    LaunchedEffect(profileId) {
        if (profileId != null) viewModel.loadProfileForEdit(profileId)
        else viewModel.resetEditor()
    }
    LaunchedEffect(editorState.isSaved)   { if (editorState.isSaved)   onSaved()   }
    LaunchedEffect(editorState.isDeleted) { if (editorState.isDeleted) onDeleted() }
    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DARK_BG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = if (isCreateMode) "Create Profile" else "Edit Profile",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(36.dp))

            // Preview avatar
            val gradientColors = AvatarGradients.colors(editorState.avatarGradient)
            Box(
                modifier = Modifier
                    .size(88.dp)
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
                    text = editorState.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(28.dp))

            // Name field — D-pad Down → first swatch
            ProfileTextField(
                value = editorState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Profile name",
                focusRequester = nameFocusRequester,
                onDone = { focusManager.moveFocus(FocusDirection.Down) },
                modifier = Modifier.focusProperties {
                    down = swatchFocusRequester
                },
            )

            if (editorState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = editorState.error!!, color = ACCENT, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Section label
            Text(
                text = "CHOOSE COLOR",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // Avatar color swatches — each is a focusable TV element
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AvatarGradients.options.forEachIndexed { index, key ->
                    val colors    = AvatarGradients.colors(key)
                    val isSelected = key == editorState.avatarGradient

                    ColorSwatch(
                        gradientStart = Color(colors.start),
                        gradientEnd   = Color(colors.end),
                        isSelected    = isSelected,
                        onClick       = { viewModel.onAvatarChange(key) },
                        // First swatch gets the focus requester so name field can jump to it
                        modifier = if (index == 0) {
                            Modifier
                                .focusRequester(swatchFocusRequester)
                                .focusProperties {
                                    up   = nameFocusRequester
                                    down = kidsNoFocusRequester
                                }
                        } else {
                            Modifier.focusProperties {
                                up   = nameFocusRequester
                                down = kidsNoFocusRequester
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Section label
            Text(
                text = "KIDS PROFILE",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Kids toggle — two focusable buttons side by side
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(false to "No", true to "Yes").forEach { (value, label) ->
                    val isSelected = editorState.isKidsProfile == value
                    val fr = if (!value) kidsNoFocusRequester else kidsYesFocusRequester

                    KidsToggleButton(
                        label = label,
                        isSelected = isSelected,
                        onClick = { viewModel.onKidsToggle(value) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(fr)
                            .focusProperties {
                                up   = swatchFocusRequester
                                down = saveFocusRequester
                            },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save button — D-pad Up goes back to kids row
            Button(
                onClick = {
                    if (isCreateMode) viewModel.saveNewProfile(onDone = { onSaved() })
                    else viewModel.updateProfile(profileId!!, onDone = { onSaved() })
                },
                enabled = !editorState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusRequester(saveFocusRequester)
                    .focusProperties {
                        up   = kidsNoFocusRequester
                        down = if (isCreateMode) cancelFocusRequester else deleteFocusRequester
                    },
                colors = ButtonDefaults.colors(
                    containerColor        = ACCENT,
                    contentColor          = Color.White,
                    focusedContainerColor = Color(0xFFFF1E2D),
                ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp)),
            ) {
                Text(
                    text = if (editorState.isLoading) "Saving…" else "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Delete button (edit mode only)
            if (!isCreateMode) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.deleteProfile(profileId!!, onDone = { onDeleted() }) },
                    enabled = !editorState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(deleteFocusRequester)
                        .focusProperties {
                            up   = saveFocusRequester
                            down = cancelFocusRequester
                        },
                    colors = ButtonDefaults.colors(
                        containerColor        = Color(0xFF2A0000),
                        contentColor          = ACCENT,
                        focusedContainerColor = Color(0xFF3D0000),
                        focusedContentColor   = Color(0xFFFF4444),
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp)),
                ) {
                    Text(
                        text = "Delete Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Cancel button — uses androidx.tv.material3.Button for D-pad reachability
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(cancelFocusRequester)
                    .focusProperties {
                        up = if (isCreateMode) saveFocusRequester else deleteFocusRequester
                    },
                colors = ButtonDefaults.colors(
                    containerColor        = Color.Transparent,
                    contentColor          = Color(0xFFB3B3B3),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    focusedContentColor   = Color.White,
                ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp)),
            ) {
                Text(text = "Cancel", fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Color swatch with focus scale + checkmark overlay
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ColorSwatch(
    gradientStart: Color,
    gradientEnd: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "swatch-scale",
    )

    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused },
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(CircleShape),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor        = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(gradientStart, gradientEnd)))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) Color(0xFFE50914) else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                // Checkmark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Kids toggle option button
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KidsToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor        = if (isSelected) Color(0xFF1F1F1F) else Color.Transparent,
            focusedContainerColor = if (isSelected) Color(0xFF2A2A2A) else Color(0xFF1A1A1A),
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = when {
                        isFocused  -> Color(0xFFE50914)
                        isSelected -> Color(0xFFE50914)
                        else       -> Color(0xFF444444)
                    },
                ),
                shape = RoundedCornerShape(6.dp),
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE50914)),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (isSelected || isFocused) Color.White else Color(0xFF888888),
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Name text field
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color(0xFFE50914)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { onDone() },
            onDone = { onDone() },
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color(0xFF1F1F1F) else Color(0xFF141414),
                RoundedCornerShape(6.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFFE50914) else Color(0xFF333333),
                shape = RoundedCornerShape(6.dp),
            ),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color(0xFF666666), fontSize = 16.sp)
                }
                inner()
            }
        },
    )
}
