package com.autograde

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autograde.databinding.ActivityLoginBinding
import com.autograde.ui.AppViewModelFactory
import com.autograde.ui.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/** Page 1 — choose Email or Google. Auto-skips when a session is saved. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: AuthViewModel by viewModels { AppViewModelFactory(application as AutoGradeApp) }

    /** Legacy Google Sign-In fallback for devices whose Play Services predate Credential Manager. */
    private val legacyGoogleSignIn =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(res.data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) toast("Google returned no ID token") else vm.google(idToken)
            } catch (e: ApiException) {
                toast("Google sign-in failed (code ${e.statusCode}): ${e.message}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            if ((application as AutoGradeApp).tokenStore.isLoggedIn()) {
                goProfile()
                return@launch
            }
        }

        binding.emailBtn.setOnClickListener {
            startActivity(Intent(this, EmailAuthActivity::class.java))
        }
        binding.googleBtn.setOnClickListener { signInWithGoogle() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    binding.progress.visibility = if (s.loading) View.VISIBLE else View.GONE
                    binding.googleBtn.isEnabled = !s.loading
                    binding.emailBtn.isEnabled = !s.loading
                    s.message?.let {
                        binding.statusText.text = it
                        toast(it)
                        vm.consumeMessage()
                    }
                    if (s.auth != null) goProfile()
                }
            }
        }
    }

    private fun goProfile() {
        startActivity(Intent(this, ProfileActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(this@LoginActivity)
                    .getCredential(this@LoginActivity, request)
                val idToken = try {
                    GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                } catch (e: GoogleIdTokenParsingException) {
                    toast("Could not read Google credential: ${e.message}")
                    return@launch
                }
                vm.google(idToken)
            } catch (e: NoCredentialException) {
                toast("No Google account found — sign into a Google account on this device/emulator first")
            } catch (e: GetCredentialException) {
                // Old Play Services (like this emulator's GMS 21.x) has no
                // Credential Manager provider — fall back to legacy sign-in.
                signInWithGoogleLegacy(webClientId)
            }
        }
    }

    private fun signInWithGoogleLegacy(webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        // Fresh account picker each time; avoids stale-account stalls.
        client.signOut().addOnCompleteListener {
            legacyGoogleSignIn.launch(client.signInIntent)
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
