package com.autograde.ui.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.AppTopBar
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.ui.theme.Track

/** The four stages of creating an OMR assessment, in order. */
val OmrSteps = listOf("Details", "Answer sheets", "Answer key", "Evaluate")

/**
 * Shared frame for every OMR-flow screen: top bar, step progress, a content
 * area that fills the middle, and a bottom action bar that stays put while the
 * content scrolls (and rises above the keyboard).
 *
 * [onBack] null hides the back button (e.g. while evaluating).
 */
@Composable
fun OmrFlowScaffold(
    title: String,
    step: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (onBack != null) {
                AppTopBar(title, onBack)
            } else {
                Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
            StepIndicator(step)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = Muted, lineHeight = 19.sp)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content,
        )

        if (bottomBar != null) {
            HorizontalDivider(color = Line)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = bottomBar,
            )
        }
    }
}

/** "Step 2 of 4 · Answer sheets" over a segmented progress bar. */
@Composable
fun StepIndicator(step: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OmrSteps.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < step) Accent else Track),
                )
            }
        }
        Text(
            "Step $step of ${OmrSteps.size} · ${OmrSteps[step - 1]}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Muted,
        )
    }
}
