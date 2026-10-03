package com.autograde.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autograde.data.AuthRepository
import com.autograde.data.model.AuthResponse
import com.autograde.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val message: String? = null,
    /** Set when the server accepted the OTP request; drives showing the OTP step. */
    val otpSent: Boolean = false,
    val cooldownSeconds: Int = 0,
    val auth: AuthResponse? = null,
)

/** Shared by the login-choice and email-OTP screens. */
class AuthViewModel(private val authRepo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun requestOtp(rawEmail: String) {
        val email = rawEmail.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = _state.value.copy(message = "Enter a valid email address")
            return
        }
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            _state.value = when (val r = authRepo.requestOtp(email)) {
                is ApiResult.Ok -> _state.value.copy(
                    loading = false,
                    otpSent = true,
                    cooldownSeconds = 60,
                    message = r.value,
                )
                is ApiResult.Err -> _state.value.copy(loading = false, message = r.message)
                ApiResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }

    fun verifyOtp(rawEmail: String, rawOtp: String) {
        val email = rawEmail.trim().lowercase()
        val otp = rawOtp.trim()
        if (otp.length != 6) {
            _state.value = _state.value.copy(message = "OTP must be 6 digits")
            return
        }
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            _state.value = when (val r = authRepo.verifyOtp(email, otp)) {
                is ApiResult.Ok -> _state.value.copy(
                    loading = false,
                    auth = r.value,
                    message = if (r.value.isNewUser) "Welcome!" else "Welcome back!",
                )
                is ApiResult.Err -> _state.value.copy(loading = false, message = r.message)
                ApiResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }

    fun google(idToken: String) {
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            _state.value = when (val r = authRepo.google(idToken)) {
                is ApiResult.Ok -> _state.value.copy(
                    loading = false,
                    auth = r.value,
                    message = "Google sign-in successful",
                )
                is ApiResult.Err -> _state.value.copy(loading = false, message = r.message)
                ApiResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }
}
