package com.autograde.ui.result

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.AppTopBar
import com.autograde.ui.components.OutlineButton
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.components.StatusChip
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.BlueBg
import com.autograde.ui.theme.BlueText
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.ui.assessment.questionPerformance
import com.autograde.ui.assessment.scoreDistribution

private data class Summary(val value: String, val label: String, val tint: Color)

private val summaries = listOf(
    Summary("83.6 / 100", "Average", AccentSoft),
    Summary("92%", "Pass Rate", BlueBg),
    Summary("60", "Submissions", AccentSoft),
)

/**
 * Screen 12 — class report for a finished assessment: summary tiles, score
 * distribution chart and per-question performance.
 */
@Composable
fun ResultScreen(onBack: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppTopBar("Class report", onBack)

        // Title + status
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Theory Final — Operating System", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 22.sp)
                Text("BSc 3rd Year · Nov 6, 2026", fontSize = 13.sp, color = Muted)
            }
            StatusChip("Completed")
        }

        // Summary tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            summaries.forEach { s ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(s.tint)
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(s.value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
                    Text(s.label, fontSize = 11.sp, color = Muted)
                }
            }
        }

        // Score distribution
        CardShell("Score Distribution") {
            val maxCount = scoreDistribution.maxOf { it.second }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                scoreDistribution.forEach { (label, count) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((100f * count / maxCount).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(label, fontSize = 9.sp, color = Muted, textAlign = TextAlign.Center, lineHeight = 11.sp)
                    }
                }
            }
        }

        // Question-wise performance
        CardShell("Question-wise Performance") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                questionPerformance.forEach { (q, score) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(q, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.width(28.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Line),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(score)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Accent),
                            )
                        }
                        Text(
                            "${(score * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton("⬇ Export", Modifier.weight(1f)) { }
            PrimaryButton("Share Report", Modifier.weight(1f)) { }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CardShell(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun ResultScreenPreview() {
    AutoGradeTheme {
        ResultScreen()
    }
}
