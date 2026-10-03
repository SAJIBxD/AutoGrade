package com.autograde.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autograde.data.AuthRepository
import com.autograde.data.ProfileRepository
import com.autograde.data.local.TokenStore
import com.autograde.data.model.Profile
import com.autograde.data.model.ProfilePatch
import com.autograde.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProfileUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val profile: Profile? = null,
    val uploadedImageUrl: String? = null,
    val loggedOut: Boolean = false,
)

class ProfileViewModel(
    private val profileRepo: ProfileRepository,
    private val authRepo: AuthRepository,
    private val store: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(loading = true))
    val state: StateFlow<ProfileUiState> = _state

    private var userId: String? = null

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun load(fallbackId: String? = null) {
        viewModelScope.launch {
            val id = fallbackId?.takeIf { it.isNotBlank() } ?: store.userId()
            if (id.isNullOrBlank()) {
                _state.value = ProfileUiState(message = "No saved session. Please log in again.")
                return@launch
            }
            userId = id
            _state.value = _state.value.copy(loading = true, message = null)
            _state.value = when (val r = profileRepo.load(id)) {
                is ApiResult.Ok -> ProfileUiState(profile = r.value)
                is ApiResult.Err -> ProfileUiState(message = r.message)
                ApiResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }

    fun save(firstName: String, lastName: String, phone: String) {
        val id = userId ?: return
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val patch = ProfilePatch(
                firstName = firstName.ifBlank { null },
                lastName = lastName.ifBlank { null },
                phone = phone.ifBlank { null },
            )
            _state.value = when (val r = profileRepo.update(id, patch)) {
                is ApiResult.Ok -> ProfileUiState(profile = r.value, message = "Profile saved")
                is ApiResult.Err -> _state.value.copy(loading = false, message = r.message)
                ApiResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }

    fun upload(file: File) {
        val id = userId
        if (id == null) {
            _state.value = _state.value.copy(message = "No saved session. Please log in again.")
            return
        }
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            when (val up = profileRepo.uploadImage(file)) {
                is ApiResult.Ok -> {
                    val imageId = up.value?.id
                    if (imageId == null) {
                        _state.value = _state.value.copy(
                            loading = false,
                            message = "Upload succeeded but returned no id",
                        )
                        return@launch
                    }
                    // Link the uploaded image to the profile, then reload.
                    when (val patched = profileRepo.update(id, ProfilePatch(image = imageId))) {
                        is ApiResult.Ok -> load(id)
                        is ApiResult.Err -> _state.value = _state.value.copy(
                            loading = false,
                            message = "Uploaded (id $imageId) but attach failed: ${patched.message}",
                        )
                        ApiResult.Loading -> _state.value.copy(loading = true)
                    }
                }
                is ApiResult.Err -> _state.value = _state.value.copy(
                    loading = false,
                    message = up.message,
                )
                ApiResult.Loading -> _state.value = _state.value.copy(loading = true)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _state.value = ProfileUiState(loggedOut = true)
        }
    }
}
