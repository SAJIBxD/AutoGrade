package com.autograde.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.assessment.UploadState
import com.autograde.ui.assessment.UploadedFile
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AmberBg
import com.autograde.ui.theme.AmberText
import com.autograde.ui.theme.BlueBg
import com.autograde.ui.theme.BlueText
import com.autograde.ui.theme.GreenBg
import com.autograde.ui.theme.GreenText
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.ui.theme.Track
import com.autograde.util.uriToFile
import java.io.File

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFC5CEC8)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
fun ElCard(
    modifier: Modifier = Modifier,
    container: Color = Color.White,
    border: Color = Line,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = Accent,
        trackColor = Track
    )
}

@Composable
fun RoundIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(Track, CircleShape)
    ) { Icon(icon, contentDescription = desc, tint = Ink) }
}

/** Back arrow + centred title, the header used by every pushed screen. */
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) trailing() else Spacer(Modifier.size(44.dp))
    }
}

/** Small pill used for Completed / Processing / Review states. */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (text.lowercase()) {
        "completed", "done", "ready" -> GreenBg to GreenText
        "processing", "uploading", "scanning" -> BlueBg to BlueText
        "review", "needs review" -> AmberBg to AmberText
        else -> AmberBg to AmberText
    }
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Section label above form fields. */
@Composable
fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text + if (required) " *" else "",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/**
 * One row in an uploaded-file list: name + size + state.
 * Shows a spinner while uploading, an error line on failure, and an optional
 * remove affordance once the file is on the server.
 */
@Composable
fun UploadedFileRow(
    file: UploadedFile,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (file.state) {
            UploadState.UPLOADING -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Accent,
            )
            UploadState.ERROR -> Text("!", fontWeight = FontWeight.Bold, color = AmberText)
            else -> Text("✓", fontWeight = FontWeight.Bold, color = GreenText)
        }
        Column(Modifier.weight(1f)) {
            Text(
                file.localName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (file.state) {
                    UploadState.UPLOADING -> "Uploading…"
                    UploadState.ERROR -> file.error ?: "Upload failed"
                    else -> "${file.displaySize} · uploaded"
                },
                fontSize = 12.sp,
                color = if (file.state == UploadState.ERROR) AmberText else Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRemove != null && file.state != UploadState.UPLOADING) {
            Text(
                "✕",
                fontSize = 16.sp,
                color = Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(8.dp),
            )
        }
    }
}

/**
 * Dashed "choose file" drop zone. Opens the system document picker and hands
 * the resolved cache copy to [onPicked]; [accept] is the MIME filter.
 */
@Composable
fun FilePickerBox(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accept: String = "*/*",
    buttonLabel: String = "Choose File",
    onPicked: (File) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            context.uriToFile(uri)?.let(onPicked)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                BorderStroke(2.dp, Line),
                RoundedCornerShape(16.dp),
            )
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(com.autograde.ui.theme.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("↑", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Accent)
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, fontSize = 12.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { launcher.launch(accept) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) { Text(buttonLabel, fontWeight = FontWeight.SemiBold) }
    }
}
