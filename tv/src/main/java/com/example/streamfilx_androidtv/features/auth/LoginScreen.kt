package com.example.streamfilx_androidtv.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.material3.TextButton

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Focus requesters for each interactive element
    val emailFocusRequester    = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester   = remember { FocusRequester() }

    // Auto-focus the email field when the screen first appears
    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    // Dark gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF111111), Color(0xFF0A0A0A)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Centered card panel
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .padding(horizontal = 48.dp, vertical = 40.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Logo
                Text(
                    text = "STREAMFLIX",
                    color = Color(0xFFE50914),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (uiState.isSignUpMode) "Create your account" else "Sign in to continue",
                    color = Color(0xFFB3B3B3),
                    fontSize = 16.sp,
                )

                Spacer(Modifier.height(40.dp))

                // Email field — D-pad Down moves to password
                AuthTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.clearError()
                    },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    focusRequester = emailFocusRequester,
                    modifier = Modifier.focusProperties {
                        down = passwordFocusRequester
                    },
                )

                Spacer(Modifier.height(16.dp))

                // Password field — D-pad Down moves to sign-in button
                AuthTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    placeholder = "Password",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isPassword = true,
                    focusRequester = passwordFocusRequester,
                    modifier = Modifier.focusProperties {
                        up   = emailFocusRequester
                        down = buttonFocusRequester
                    },
                )

                // Animated error message
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn(),
                    exit  = fadeOut(),
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = Color(0xFFE50914),
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Primary action button — uses androidx.tv.material3.Button
                Button(
                    onClick = {
                        if (uiState.isSignUpMode) {
                            viewModel.signUp(email, password, onSuccess = onSignedIn)
                        } else {
                            viewModel.signIn(email, password, onSuccess = onSignedIn)
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(buttonFocusRequester)
                        .focusProperties { up = passwordFocusRequester },
                    colors = ButtonDefaults.colors(
                        containerColor        = Color(0xFFE50914),
                        contentColor          = Color.White,
                        focusedContainerColor = Color(0xFFFF1E2D),
                        focusedContentColor   = Color.White,
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp)),
                ) {
                    if (uiState.isLoading) {
                        // Show spinner inside button while loading
                        CircularProgressIndicator(
                            color     = Color.White,
                            modifier  = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Text(
                            text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Toggle sign in / sign up
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.isSignUpMode) "Already have an account?" else "New to StreamFlix?",
                        color = Color(0xFFB3B3B3),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = viewModel::toggleMode) {
                        Text(
                            text = if (uiState.isSignUpMode) "Sign In" else "Sign Up",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    isPassword: Boolean = false,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = if (isFocused) Color(0xFFE50914) else Color(0xFF333333)
    val bgColor     = if (isFocused) Color(0xFF1F1F1F) else Color(0xFF141414)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
        ),
        cursorBrush = SolidColor(Color(0xFFE50914)),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp),
            ),
        decorationBox = { innerField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF808080),
                        fontSize = 16.sp,
                    )
                }
                innerField()
            }
        },
    )
}
