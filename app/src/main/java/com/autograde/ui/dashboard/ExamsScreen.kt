package com.autograde.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.assessment.RecentAssessment
import com.autograde.ui.assessment.sampleRecentAssessments
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.Ink

private val allAssessments = sampleRecentAssessments + listOf(
    RecentAssessment("Physics Midterm", "Class 9-B · 24 of 30 graded", "Processing"),
    RecentAssessment("History Essay Test", "Class 10-A · 9 of 28 graded", "Review"),
    RecentAssessment("Math Quiz 3", "Class 9-A · Completed", "Completed"),
)

/** Assessments tab: every assessment, newest first, plus a shortcut to create one. */
@Composable
fun ExamsScreen(
    onOpenAssessment: () -> Unit,
    onCreate: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            // Bottom padding keeps the last card clear of the FAB.
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Assessments",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(allAssessments, key = { it.title }) { assessment ->
                AssessmentCard(assessment, onClick = onOpenAssessment)
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreate,
            containerColor = Accent,
            contentColor = Color.White,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New assessment", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}
