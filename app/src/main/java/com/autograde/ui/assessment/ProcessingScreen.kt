package com.autograde.ui.assessment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.GreenText
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.ui.theme.Track
import kotlinx.coroutines.delay

private data class Step(val label: String, val at: Float)

private val steps = listOf(
    Step("Reading uploaded sheets", 0.05f),
    Step("Scanning bubble markings", 0.35f),
    Step("Comparing against answer key", 0.65f),
    Step("Calculating final scores", 0.90f),
)

/**
 * OMR step 4 — evaluation progress. Back is disabled while it runs; when done
 * it moves on to the submissions list by itself.
 */
@Composable
fun ProcessingScreen(
    sheetCount: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) { /* evaluation can't be interrupted midway */ }
    val sheetsLabel = if (sheetCount > 0) "$sheetCount / $sheetCount" else "done"

    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress",
    )

    val latestOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress = (progress + 0.05f).coerceAtMost(1f)
        }
        delay(400)
        latestOnDone()
    }

    OmrFlowScaffold(
        title = "Evaluating sheets",
        step = 4,
        onBack = null,
        modifier = modifier,
        bottomBar = {
            Text(
                "Keep this screen open. Results open automatically when grading finishes.",
                fontSize = 13.sp,
                color = Muted,
                lineHeight = 18.sp,
            )
        },
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 14.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        drawArc(
                            Track, 0f, 360f, false,
                            Offset(inset, inset), arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                        drawArc(
                            Accent, -90f, 360f * animated, false,
                            Offset(inset, inset), arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(animated * 100).toInt()}%",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                        )
                        Text("Processing…", fontSize = 13.sp, color = Muted)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEach { step ->
                    val done = animated >= step.at + 0.20f
                    val active = !done && animated >= step.at
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) AccentSoft else Color.White)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        done -> GreenText
                                        active -> Accent
                                        else -> Line
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            step.label,
                            fontSize = 14.sp,
                            fontWeight = if (done || active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (done || active) Ink else Muted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (done) sheetsLabel else if (active) "…" else "",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun ProcessingPreview() {
    AutoGradeTheme {
        ProcessingScreen(sheetCount = 60, onDone = {})
    }
}
