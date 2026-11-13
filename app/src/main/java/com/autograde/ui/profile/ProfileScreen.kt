package com.autograde.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.autograde.data.model.Profile
import com.autograde.ui.ProfileUiState
import com.autograde.ui.components.OutlineButton
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.theme.Ink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Typography

/**
 * Profile info, edit, photo upload, logout screen. Field values live here and are
 * filled from the profile only while blank, so reloads never clobber edits.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    statusText: String?,
    onSave: (firstName: String, lastName: String, phone: String) -> Unit,
    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(profile) {
        if (profile == null) return@LaunchedEffect
        if (firstName.isBlank()) firstName = profile.firstName.orEmpty()
        if (lastName.isBlank()) lastName = profile.lastName.orEmpty()
        if (phone.isBlank()) phone = profile.phone.orEmpty()
    }

    val displayName = profile?.let {
        listOfNotNull(it.firstName, it.lastName)
            .asSequence()
            .filter { name -> name.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Unnamed user" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
    ) {
        Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(20.dp))

        // Identity row: photo when available, otherwise the initial letter.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = profile?.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Text(displayName?.firstOrNull()?.uppercase() ?: "?", style = Typography.titleMedium)
                }
            }

            Spacer(Modifier.width(16.dp))
            Column {
                Text(displayName ?: "", style = Typography.titleMedium)
                Text(profile?.email.orEmpty(), style = Typography.bodySmall)
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Edit details", style = Typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Save changes",
            Modifier.fillMaxWidth(),
            enabled = !uiState.loading,
            onClick = { onSave(firstName.trim(), lastName.trim(), phone.trim()) },
        )

        Spacer(Modifier.height(12.dp))
        OutlineButton("Change profile photo", Modifier.fillMaxWidth(), onClick = onUploadPhoto)

        if (uiState.loading) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(Modifier.size(28.dp))
        }

        statusText?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = Typography.bodySmall)
        }

        Spacer(Modifier.height(40.dp))
        TextButton(
            onClick = onLogout,
            enabled = !uiState.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log out", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun ProfileScreenPreview() {
    AutoGradeTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                profile = Profile(
                    id = "1",
                    firstName = "Ada",
                    lastName = "Lovelace",
                    email = "ada@example.com",
                    phone = "+123456",
                    dateOfBirth = null,
                    gender = null,
                    userType = "student",
                    verified = true,
                    isActive = true,
                    imageUrl = null,
                ),
            ),
            statusText = null,
            onSave = { _, _, _ -> },
            onUploadPhoto = {},
            onLogout = {},
        )
    }
}
