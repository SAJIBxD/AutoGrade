package com.autograde.ui.login

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.R
import com.autograde.ui.AuthUiState
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import kotlinx.coroutines.delay

private const val OTP_LENGTH = 6
private val FieldShape = RoundedCornerShape(14.dp)

/**
 * Passwordless sign-in: email → one-time code, or Google.
 * The step shown is driven by [AuthUiState.otpSent]; [onEditEmail] steps back.
 */
@Composable
fun LoginScreen(
    uiState: AuthUiState = AuthUiState(),
    onRequestOtp: (email: String) -> Unit = {},
    onVerifyOtp: (email: String, otp: String) -> Unit = { _, _ -> },
    onEditEmail: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    // Which button started the in-flight request, so only that one spins.
    var googlePending by remember { mutableStateOf(false) }

    val backToEmail = {
        otp = ""
        onEditEmail()
    }
    BackHandler(enabled = uiState.otpSent, onBack = backToEmail)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to AccentSoft, 0.55f to Bg)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            AppLogo()
            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = uiState.otpSent,
                transitionSpec = {
                    val dir = if (targetState) 1 else -1
                    (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { dir * it / 8 }) togetherWith
                        fadeOut(tween(120))
                },
                label = "auth-step",
            ) { codeStep ->
                if (codeStep) {
                    CodeStep(
                        email = email.trim(),
                        otp = otp,
                        onOtpChange = { otp = it },
                        loading = uiState.loading,
                        otpSentAt = uiState.otpSentAt,
                        cooldownSeconds = uiState.cooldownSeconds,
                        onVerify = { code -> onVerifyOtp(email.trim(), code) },
                        onResend = { onRequestOtp(email.trim()) },
                        onChangeEmail = backToEmail,
                    )
                } else {
                    EmailStep(
                        email = email,
                        onEmailChange = { email = it },
                        loading = uiState.loading,
                        googlePending = googlePending,
                        onContinue = {
                            googlePending = false
                            onRequestOtp(it)
                        },
                        onGoogleClick = {
                            googlePending = true
                            onGoogleClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLogo() {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = Modifier
            .size(76.dp)
            .shadow(16.dp, RoundedCornerShape(17.dp), spotColor = Accent.copy(alpha = 0.45f)),
    )
}

@Composable
private fun Heading(title: String, subtitle: @Composable () -> Unit) {
    Text(
        title,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Ink,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    subtitle()
}

@Composable
private fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    loading: Boolean,
    googlePending: Boolean,
    onContinue: (email: String) -> Unit,
    onGoogleClick: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    val submit = {
        val trimmed = email.trim()
        if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            error = null
            onContinue(trimmed)
        } else {
            error = "Enter a valid email address"
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Heading("Welcome to AutoGrade") {
            Text(
                "Enter your email and we'll send you a one-time code. No password needed.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                onEmailChange(it)
                error = null
            },
            placeholder = { Text("you@school.edu", color = Muted.copy(alpha = 0.7f)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_mail), contentDescription = null, tint = Muted)
            },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            enabled = !loading,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        PrimaryAuthButton(
            text = "Continue with email",
            loading = loading && !googlePending,
            enabled = email.isNotBlank() && !loading,
            onClick = submit,
        )

        Spacer(Modifier.height(24.dp))
        OrDivider()
        Spacer(Modifier.height(24.dp))

        GoogleButton(
            loading = loading && googlePending,
            enabled = !loading,
            onClick = onGoogleClick,
        )
    }
}

@Composable
private fun CodeStep(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    loading: Boolean,
    otpSentAt: Long,
    cooldownSeconds: Int,
    onVerify: (otp: String) -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    var secondsLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(otpSentAt, cooldownSeconds) {
        while (true) {
            val elapsed = ((System.currentTimeMillis() - otpSentAt) / 1000).toInt()
            secondsLeft = (cooldownSeconds - elapsed).coerceAtLeast(0)
            if (secondsLeft == 0) break
            delay(1000)
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Heading("Check your email") {
            Text(
                buildAnnotatedString {
                    append("We sent a 6-digit code to\n")
                    withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.SemiBold)) {
                        append(email)
                    }
                },
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = onChangeEmail, enabled = !loading) {
            Text("Use a different email", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))

        OtpField(
            value = otp,
            enabled = !loading,
            focusRequester = focus,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(OTP_LENGTH)
                val completed = digits.length == OTP_LENGTH && otp.length < OTP_LENGTH
                onOtpChange(digits)
                if (completed) onVerify(digits)
            },
            onDone = { if (otp.length == OTP_LENGTH) onVerify(otp) },
        )
        Spacer(Modifier.height(28.dp))

        PrimaryAuthButton(
            text = "Verify & continue",
            loading = loading,
            enabled = otp.length == OTP_LENGTH && !loading,
            onClick = { onVerify(otp) },
        )
        Spacer(Modifier.height(8.dp))

        val canResend = secondsLeft == 0 && !loading
        TextButton(onClick = onResend, enabled = canResend) {
            Text(
                if (secondsLeft > 0) "Resend code in ${secondsLeft}s" else "Resend code",
                color = if (canResend) Accent else Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun OtpField(
    value: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(OTP_LENGTH) { i ->
                    val digit = value.getOrNull(i)
                    val active = enabled && i == value.length.coerceAtMost(OTP_LENGTH - 1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = when {
                                    active -> Accent
                                    digit != null -> Ink.copy(alpha = 0.35f)
                                    else -> Line
                                },
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            digit?.toString() ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = FieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.White,
            disabledContainerColor = Accent.copy(alpha = 0.45f),
            disabledContentColor = Color.White,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun GoogleButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = FieldShape,
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Ink,
            disabledContainerColor = Color.White,
            disabledContentColor = Ink.copy(alpha = 0.5f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Line)
        Text("or", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp))
        HorizontalDivider(Modifier.weight(1f), color = Line)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White,
    cursorColor = Accent,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginEmailPreview() {
    AutoGradeTheme { LoginScreen() }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginCodePreview() {
    AutoGradeTheme {
        LoginScreen(uiState = AuthUiState(otpSent = true, cooldownSeconds = 60))
    }
}
