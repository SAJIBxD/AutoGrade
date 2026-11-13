package com.autograde.ui.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.AppTopBar
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.components.StatusChip
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.GreenText
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted

private enum class Filter(val label: String) { ALL("All"), GRADED("Graded"), REVIEW("Needs review") }

/**
 * Graded OMR submissions with a filter. Reached once evaluation finishes, so
 * Back returns to the dashboard rather than into the finished flow.
 */
@Composable
fun SubmissionsListScreen(
    assessmentTitle: String,
    onBack: () -> Unit,
    onViewReport: () -> Unit,
    modifier: Modifier = Modifier,
    submissions: List<StudentSubmission> = sampleSubmissions,
) {
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filter = Filter.entries[filterIndex]

    val needsReview = remember(submissions) { submissions.filter { it.needsReview || it.score == null } }
    val graded = remember(submissions) { submissions - needsReview.toSet() }
    val visible = when (filter) {
        Filter.ALL -> submissions
        Filter.GRADED -> graded
        Filter.REVIEW -> needsReview
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppTopBar("Results", onBack)
            Column {
                Text(
                    assessmentTitle.ifBlank { "Untitled assessment" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${submissions.size} sheets graded", fontSize = 13.sp, color = Muted)
            }
            FilterTabs(
                selected = filter,
                counts = mapOf(
                    Filter.ALL to submissions.size,
                    Filter.GRADED to graded.size,
                    Filter.REVIEW to needsReview.size,
                ),
                onSelect = { filterIndex = it.ordinal },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item {
                    Text(
                        "Nothing here.",
                        fontSize = 13.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            }
            items(visible, key = { it.id }) { sub -> SubmissionRow(sub) }
        }

        HorizontalDivider(color = Line)
        PrimaryButton(
            "View class report",
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            onClick = onViewReport,
        )
    }
}

@Composable
private fun FilterTabs(
    selected: Filter,
    counts: Map<Filter, Int>,
    onSelect: (Filter) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Filter.entries.forEach { f ->
            val isSelected = f == selected
            Text(
                "${f.label} ${counts[f] ?: 0}",
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) Accent else Color.Transparent)
                    .clickable { onSelect(f) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SubmissionRow(sub: StudentSubmission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                sub.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(sub.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ID ${sub.id}", fontSize = 12.sp, color = Muted)
        }
        if (sub.needsReview || sub.score == null) {
            StatusChip("Review")
        } else {
            Text(
                "${sub.score}",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenText,
            )
            Text("/ 100", fontSize = 12.sp, color = Muted)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun SubmissionsListPreview() {
    AutoGradeTheme {
        SubmissionsListScreen(assessmentTitle = "Midterm — Operating System", onBack = {}, onViewReport = {})
    }
}
