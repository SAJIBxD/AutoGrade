package com.autograde

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.autograde.databinding.ActivityProfileBinding
import com.autograde.ui.AppViewModelFactory
import com.autograde.ui.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File

/** Page 3 — profile info, edit, photo upload, logout. */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val vm: ProfileViewModel by viewModels { AppViewModelFactory(application as AutoGradeApp) }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            val file = uriToFile(uri)
            if (file != null) vm.upload(file) else toast("Could not read image")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.load(intent.getStringExtra(EXTRA_USER_ID))

        binding.saveBtn.setOnClickListener {
            vm.save(
                binding.firstNameInput.text.toString().trim(),
                binding.lastNameInput.text.toString().trim(),
                binding.phoneInput.text.toString().trim(),
            )
        }
        binding.uploadBtn.setOnClickListener { pickImage.launch("image/*") }
        binding.logoutBtn.setOnClickListener { vm.logout() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    binding.progress.visibility = if (s.loading) View.VISIBLE else View.GONE
                    s.message?.let {
                        binding.statusText.text = it
                        toast(it)
                        vm.consumeMessage()
                    }
                    if (s.loggedOut) {
                        startActivity(
                            Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                        return@collect
                    }
                    s.profile?.let { p ->
                        val name = listOf(p.firstName, p.lastName)
                            .mapNotNull { it?.takeIf(String::isNotBlank) }
                            .joinToString(" ").ifBlank { "Unnamed user" }
                        binding.nameText.text = name
                        binding.emailText.text = p.email.orEmpty()
                        binding.metaText.text =
                            listOfNotNull(
                                p.userType,
                                p.verified?.let { "verified=$it" },
                                p.phone?.let { "phone=$it" },
                            ).joinToString("  •  ")
                        binding.avatarText.text = name.firstOrNull()?.uppercase() ?: "?"
                        if (!p.imageUrl.isNullOrBlank()) {
                            binding.avatarImage.visibility = View.VISIBLE
                            binding.avatarImage.load(p.imageUrl) {
                                crossfade(true)
                                listener(
                                    onError = { _, _ ->
                                        binding.avatarImage.visibility = View.GONE
                                    },
                                )
                            }
                        } else {
                            binding.avatarImage.visibility = View.GONE
                        }
                        if (binding.firstNameInput.text.isNullOrBlank()) {
                            binding.firstNameInput.setText(p.firstName.orEmpty())
                        }
                        if (binding.lastNameInput.text.isNullOrBlank()) {
                            binding.lastNameInput.setText(p.lastName.orEmpty())
                        }
                        if (binding.phoneInput.text.isNullOrBlank()) {
                            binding.phoneInput.setText(p.phone.orEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val out = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { ins ->
                out.outputStream().use { ins.copyTo(it) }
            }
            out.takeIf { it.exists() && it.length() > 0 }
        } catch (_: Exception) {
            null
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_USER_ID = "user_id"
    }
}
