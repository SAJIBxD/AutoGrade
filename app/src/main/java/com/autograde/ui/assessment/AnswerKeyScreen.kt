package com.autograde.ui.assessment

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.components.ProgressBar
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted
import com.autograde.ui.theme.Track

/**
 * OMR step 3 — the answer key. Lives in the flow's ViewModel, so it survives
 * going back to add more sheets. Tap a selected bubble again to clear it.
 */
@Composable
fun AnswerKeyScreen(
    state: AssessmentUiState,
    onBack: () -> Unit,
    onQuestionCount: (Int) -> Unit,
    onAnswer: (index: Int, option: String) -> Unit,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.questionCount
    val answered = state.answeredCount

    OmrFlowScaffold(
        title = "Answer key",
        step = 3,
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                if (state.answerKeyComplete) "Evaluate ${state.sheetCount} sheet${if (state.sheetCount == 1) "" else "s"}"
                else "${total - answered} question${if (total - answered == 1) "" else "s"} left",
                Modifier.fillMaxWidth(),
                enabled = state.answerKeyComplete,
                onClick = onEvaluate,
            )
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                QuestionCountCard(
                    count = total,
                    answered = answered,
                    onChange = onQuestionCount,
                )
            }
            itemsIndexed(state.answerKey, key = { i, _ -> i }) { index, selected ->
                QuestionRow(
                    number = index + 1,
                    selected = selected,
                    onSelect = { onAnswer(index, it) },
                )
            }
        }
    }
}

@Composable
private fun QuestionCountCard(count: Int, answered: Int, onChange: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Questions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("$answered of $count answered", fontSize = 12.sp, color = Muted)
            }
            StepperButton("−", enabled = count > MIN_QUESTIONS) { onChange(count - 1) }
            Text(
                "$count",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton("+", enabled = count < MAX_QUESTIONS) { onChange(count + 1) }
        }
        ProgressBar(if (count == 0) 0f else answered / count.toFloat())
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) AccentSoft else Track)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (enabled) Accent else Muted)
    }
}

@Composable
private fun QuestionRow(
    number: Int,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, if (selected == null) Line else AccentSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Q$number",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected == null) Muted else Ink,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OMR_OPTIONS.forEach { opt ->
                val isSelected = opt == selected
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Accent else Color.White)
                        .border(1.5.dp, if (isSelected) Accent else Line, CircleShape)
                        .clickable(role = Role.RadioButton) { onSelect(opt) }
                        .semantics {
                            this.selected = isSelected
                            contentDescription = "Question $number, option $opt"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        opt,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Ink,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun AnswerKeyPreview() {
    AutoGradeTheme {
        AnswerKeyScreen(
            state = AssessmentUiState(answerKey = List(10) { i -> if (i < 6) OMR_OPTIONS[i % 4] else null }),
            onBack = {}, onQuestionCount = {}, onAnswer = { _, _ -> }, onEvaluate = {},
        )
    }
}
