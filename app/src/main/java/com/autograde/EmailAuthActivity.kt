package com.autograde

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autograde.databinding.ActivityEmailAuthBinding
import com.autograde.ui.AppViewModelFactory
import com.autograde.ui.AuthViewModel
import kotlinx.coroutines.launch

/** Page 2 — email + Request OTP, then OTP + Verify on success. */
class EmailAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailAuthBinding
    private val vm: AuthViewModel by viewModels { AppViewModelFactory(application as AutoGradeApp) }
    private var cooldown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.requestBtn.setOnClickListener {
            binding.emailLayout.error = null
            vm.requestOtp(binding.emailInput.text.toString())
        }
        binding.verifyBtn.setOnClickListener {
            binding.otpLayout.error = null
            vm.verifyOtp(
                binding.emailInput.text.toString(),
                binding.otpInput.text.toString(),
            )
        }
        binding.resendBtn.setOnClickListener {
            vm.requestOtp(binding.emailInput.text.toString())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    binding.progress.visibility = if (s.loading) View.VISIBLE else View.GONE
                    binding.requestBtn.isEnabled = !s.loading
                    binding.verifyBtn.isEnabled = !s.loading
                    s.message?.let {
                        binding.statusText.text = it
                        if (!s.otpSent && s.auth == null) binding.emailLayout.error = it
                        toast(it)
                        vm.consumeMessage()
                    }
                    if (s.otpSent && binding.otpCard.visibility != View.VISIBLE) {
                        binding.otpCard.visibility = View.VISIBLE
                        binding.otpInput.requestFocus()
                        startCooldown(s.cooldownSeconds)
                    }
                    if (s.auth != null) {
                        startActivity(
                            Intent(this@EmailAuthActivity, ProfileActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldown?.cancel()
        binding.resendBtn.isEnabled = false
        cooldown = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(left: Long) {
                binding.resendBtn.text = "Resend code (${left / 1000}s)"
            }
            override fun onFinish() {
                binding.resendBtn.text = "Resend code"
                binding.resendBtn.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        cooldown?.cancel()
        super.onDestroy()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
