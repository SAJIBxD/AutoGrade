package com.autograde

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.autograde.ui.AppViewModelFactory
import com.autograde.ui.AuthViewModel
import com.autograde.ui.AutoGradeAppNav
import com.autograde.ui.ProfileViewModel
import com.autograde.ui.Routes
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.util.uriToFile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/**
 * Single Activity entry point for the application following Jetpack Compose best practices.
 */
class MainActivity : AppCompatActivity() {

    private val authVm: AuthViewModel by viewModels {
        AppViewModelFactory(application as AutoGradeApp)
    }

    private val profileVm: ProfileViewModel by viewModels {
        AppViewModelFactory(application as AutoGradeApp)
    }

    /** False until the saved-session check finishes. */
    private var sessionChecked by mutableStateOf(false)
    private var startDestination by mutableStateOf(Routes.LOGIN)

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            val file = uriToFile(uri)
            if (file != null) profileVm.upload(file) else toast("Could not read image")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val loggedIn = (application as AutoGradeApp).tokenStore.isLoggedIn()
            if (loggedIn) {
                startDestination = Routes.DASHBOARD
                profileVm.load()
            } else {
                startDestination = Routes.LOGIN
            }
            sessionChecked = true
        }

        setContent {
            AutoGradeTheme {
                if (!sessionChecked) {
                    Box(Modifier.fillMaxSize().background(Bg))
                    return@AutoGradeTheme
                }

                val authState by authVm.state.collectAsState()
                val profileState by profileVm.state.collectAsState()
                var statusText by rememberSaveable { mutableStateOf<String?>(null) }

                LaunchedEffect(authState.message) {
                    val msg = authState.message ?: return@LaunchedEffect
                    statusText = msg
                    toast(msg)
                    authVm.consumeMessage()
                }

                LaunchedEffect(profileState.message) {
                    val msg = profileState.message ?: return@LaunchedEffect
                    statusText = msg
                    toast(msg)
                    profileVm.consumeMessage()
                }

                AutoGradeAppNav(
                    startDestination = startDestination,
                    authUiState = authState,
                    profileUiState = profileState,
                    statusText = statusText,
                    onGoogleClick = { signInWithGoogle() },
                    onRequestOtp = { email -> authVm.requestOtp(email) },
                    onVerifyOtp = { email, otp -> authVm.verifyOtp(email, otp) },
                    onEditEmail = { authVm.editEmail() },
                    onAuthSuccess = { profileVm.load() },
                    onSaveProfile = { first, last, phone -> profileVm.save(first, last, phone) },
                    onUploadPhoto = { pickImage.launch("image/*") },
                    onLogout = { profileVm.logout() },
                )
            }
        }
    }

    private fun signInWithGoogle() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            toast("Google sign-in is not configured (missing web client ID)")
            return
        }
        lifecycleScope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts = false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                val result = CredentialManager.create(this@MainActivity)
                    .getCredential(this@MainActivity, request)
                val idToken = try {
                    GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                } catch (e: GoogleIdTokenParsingException) {
                    toast("Could not read Google credential: ${e.message}")
                    return@launch
                }
                authVm.google(idToken)
            } catch (_: GetCredentialCancellationException) {
                // User cancelled credential picker flow
            } catch (_: NoCredentialException) {
                toast("No Google account found — sign into a Google account on this device first")
            } catch (e: GetCredentialException) {
                toast("Google sign-in failed: ${e.message}")
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
