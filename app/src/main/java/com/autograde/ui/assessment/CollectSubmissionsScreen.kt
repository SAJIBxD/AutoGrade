package com.autograde.ui.assessment

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.R
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.components.UploadedFileRow
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.util.newCameraCapture
import com.autograde.util.uriToFile
import java.io.File

/** Answer sheets can be photos (images) or exported PDFs. */
private val SHEET_MIME_TYPES = arrayOf("image/*", "application/pdf")

/** OMR step 2 — collect student answer sheets by taking photos or uploading files. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectSubmissionsScreen(
    state: AssessmentUiState,
    onBack: () -> Unit,
    onUpload: (List<File>) -> Unit,
    onRemove: (id: Long) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val files = uris.mapNotNull { context.uriToFile(it, "omr_submission") }
        if (files.isNotEmpty()) onUpload(files)
    }

    // Path of the photo the camera app is writing. Saveable, because the camera
    // can push this activity out of memory before it returns.
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val photo = pendingPhoto?.let(::File)
        pendingPhoto = null
        when {
            photo == null -> Unit
            saved && photo.length() > 0 -> onUpload(listOf(photo))
            else -> photo.delete()
        }
    }
    val takePhoto: () -> Unit = {
        val (file, uri) = context.newCameraCapture()
        pendingPhoto = file.absolutePath
        try {
            camera.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingPhoto = null
            Toast.makeText(context, "No camera app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val ready = state.uploadedSubmissions
    OmrFlowScaffold(
        title = "Answer sheets",
        step = 2,
        onBack = onBack,
        modifier = modifier,
        subtitle = "Take photos of or upload your students' filled-in OMR sheets.",
        bottomBar = {
            PrimaryButton(
                when {
                    state.uploadsInFlight -> "Uploading… ($ready/${state.submissions.size})"
                    ready > 0 -> "Continue with $ready sheet${if (ready == 1) "" else "s"}"
                    else -> "Add sheets to continue"
                },
                Modifier.fillMaxWidth(),
                enabled = ready > 0 && !state.uploadsInFlight,
                onClick = onContinue,
            )
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceCard(
                        iconRes = R.drawable.ic_camera,
                        title = "Take photo",
                        subtitle = "One sheet per photo",
                        onClick = takePhoto,
                        modifier = Modifier.weight(1f),
                    )
                    SourceCard(
                        iconRes = R.drawable.ic_upload,
                        title = "Upload",
                        subtitle = "Images or PDF",
                        onClick = { multiPicker.launch(SHEET_MIME_TYPES) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.submissions.isEmpty()) {
                item {
                    Text(
                        "No sheets yet. Added sheets will appear here.",
                        fontSize = 13.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            } else {
                item {
                    Text(
                        "Sheets · $ready of ${state.submissions.size} uploaded",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(state.submissions, key = { it.id }) { file ->
                    UploadedFileRow(
                        file,
                        modifier = Modifier.animateItemPlacement(),
                        onRemove = { onRemove(file.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun CollectSubmissionsPreview() {
    AutoGradeTheme {
        CollectSubmissionsScreen(
            state = AssessmentUiState(
                submissions = listOf(
                    UploadedFile("sheet_001.jpg", "1.8 MB", UploadState.DONE, 31, id = 1),
                    UploadedFile("sheet_002.jpg", "1.6 MB", UploadState.UPLOADING, id = 2),
                ),
            ),
            onBack = {}, onUpload = {}, onRemove = {}, onContinue = {},
        )
    }
}
