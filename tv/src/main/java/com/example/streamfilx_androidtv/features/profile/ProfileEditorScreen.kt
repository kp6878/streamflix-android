package com.example.streamfilx_androidtv.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
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
    val editorState by viewModel.editorState.collectAsState()
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(profileId) {
        if (profileId != null) {
            viewModel.loadProfileForEdit(profileId)
        } else {
            viewModel.resetEditor()
        }
    }

    LaunchedEffect(editorState.isSaved) { if (editorState.isSaved) onSaved() }
    LaunchedEffect(editorState.isDeleted) { if (editorState.isDeleted) onDeleted() }

    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(horizontal = 40.dp),
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

            // Name field
            ProfileTextField(
                value = editorState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Profile name",
                focusRequester = nameFocusRequester,
                onDone = { focusManager.clearFocus() },
            )

            if (editorState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = editorState.error!!, color = Color(0xFFE50914), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Avatar color picker
            Text(
                text = "Choose Color",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AvatarGradients.options.forEach { key ->
                    val colors = AvatarGradients.colors(key)
                    val isSelected = key == editorState.avatarGradient
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape,
                            )
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(colors.start), Color(colors.end))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { viewModel.onAvatarChange(key) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Kids Profile toggle
            Text(
                text = "Kids Profile",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(false, true).forEach { value ->
                    val isSelected = editorState.isKidsProfile == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF1F1F1F) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFE50914) else Color(0xFF444444),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { viewModel.onKidsToggle(value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (value) "Yes" else "No",
                            color = if (isSelected) Color.White else Color(0xFF888888),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    if (isCreateMode) {
                        viewModel.saveNewProfile(onDone = { onSaved() })
                    } else {
                        viewModel.updateProfile(profileId!!, onDone = { onSaved() })
                    }
                },
                enabled = !editorState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFFE50914),
                    contentColor = Color.White,
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
                        .height(52.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF2A0000),
                        contentColor = Color(0xFFE50914),
                        focusedContainerColor = Color(0xFF3D0000),
                        focusedContentColor = Color(0xFFFF4444),
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

            androidx.compose.material3.TextButton(onClick = onBack) {
                Text(text = "Cancel", color = Color(0xFFB3B3B3), fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color(0xFFE50914)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color(0xFF1F1F1F) else Color(0xFF141414),
                RoundedCornerShape(6.dp),
            )
            .border(
                1.dp,
                if (isFocused) Color(0xFFE50914) else Color(0xFF333333),
                RoundedCornerShape(6.dp),
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
