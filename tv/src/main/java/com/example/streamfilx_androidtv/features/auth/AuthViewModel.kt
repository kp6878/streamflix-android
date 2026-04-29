package com.example.streamfilx_androidtv.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.services.SupabaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUpMode: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseService: SupabaseService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null,
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            supabaseService.signIn(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Sign in failed") }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            supabaseService.signUp(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Sign up failed") }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email is required")
                false
            }
            !email.contains('@') -> {
                _uiState.value = _uiState.value.copy(error = "Enter a valid email address")
                false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}
