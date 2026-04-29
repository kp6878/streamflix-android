package com.example.streamfilx_androidtv.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.services.AvatarGradients
import com.example.streamfilx_androidtv.services.Profile
import com.example.streamfilx_androidtv.services.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditorState(
    val name: String = "",
    val avatarGradient: String = AvatarGradients.default,
    val isKidsProfile: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileManager: ProfileManager,
) : ViewModel() {

    // ── Profile list (picker screen) ──────────────────────────────────────────

    val profiles: StateFlow<List<Profile>> = profileManager.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val canAddMore: StateFlow<Boolean> = profileManager.canAddMoreProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    // ── Editor state ──────────────────────────────────────────────────────────

    private val _editorState = MutableStateFlow(ProfileEditorState())
    val editorState: StateFlow<ProfileEditorState> = _editorState.asStateFlow()

    fun loadProfileForEdit(profileId: String) {
        viewModelScope.launch {
            val profile = profileManager.getById(profileId) ?: return@launch
            _editorState.value = ProfileEditorState(
                name = profile.name,
                avatarGradient = profile.avatarGradient,
                isKidsProfile = profile.isKidsProfile,
            )
        }
    }

    fun resetEditor() {
        _editorState.value = ProfileEditorState()
    }

    fun onNameChange(name: String) {
        _editorState.value = _editorState.value.copy(name = name, error = null)
    }

    fun onAvatarChange(gradient: String) {
        _editorState.value = _editorState.value.copy(avatarGradient = gradient)
    }

    fun onKidsToggle(isKids: Boolean) {
        _editorState.value = _editorState.value.copy(isKidsProfile = isKids)
    }

    fun saveNewProfile(onDone: (Profile) -> Unit) {
        val state = _editorState.value
        if (!validate(state)) return
        viewModelScope.launch {
            _editorState.value = state.copy(isLoading = true)
            val profile = profileManager.addProfile(state.name, state.avatarGradient, state.isKidsProfile)
            _editorState.value = _editorState.value.copy(isLoading = false, isSaved = true)
            onDone(profile)
        }
    }

    fun updateProfile(profileId: String, onDone: () -> Unit) {
        val state = _editorState.value
        if (!validate(state)) return
        viewModelScope.launch {
            _editorState.value = state.copy(isLoading = true)
            val existing = profileManager.getById(profileId) ?: return@launch
            profileManager.updateProfile(existing.copy(
                name = state.name,
                avatarGradient = state.avatarGradient,
                isKidsProfile = state.isKidsProfile,
            ))
            _editorState.value = _editorState.value.copy(isLoading = false, isSaved = true)
            onDone()
        }
    }

    fun deleteProfile(profileId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isLoading = true)
            profileManager.deleteProfile(profileId)
            _editorState.value = _editorState.value.copy(isLoading = false, isDeleted = true)
            onDone()
        }
    }

    private fun validate(state: ProfileEditorState): Boolean {
        return when {
            state.name.isBlank() -> {
                _editorState.value = state.copy(error = "Name cannot be empty")
                false
            }
            state.name.length > 20 -> {
                _editorState.value = state.copy(error = "Name must be 20 characters or less")
                false
            }
            else -> true
        }
    }
}
