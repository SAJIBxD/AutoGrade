package com.autograde.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.autograde.ui.assessment.RecentAssessment
import com.autograde.ui.assessment.sampleRecentAssessments
import com.autograde.ui.components.ElCard
import com.autograde.ui.components.StatusChip
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentDark
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Muted
import java.util.Calendar

private data class Stat(val value: String, val label: String)

private val stats = listOf(
    Stat("12", "Assessments"),
    Stat("428", "Sheets graded"),
    Stat("87%", "Avg. score"),
)

/** Home tab: start a new OMR assessment, glance at stats, reopen recent work. */
@Composable
fun DashboardScreen(
    firstName: String? = null,
    avatarInitials: String = "?",
    avatarUrl: String? = null,
    onStartMarking: () -> Unit = {},
    onOpenAssessment: () -> Unit = {},
    onSeeAll: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val greeting = remember { greetingForNow() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, fontSize = 14.sp, color = Muted)
                    Text(
                        firstName?.takeIf { it.isNotBlank() } ?: "Welcome back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                UserAvatar(initials = avatarInitials, imageUrl = avatarUrl, onClick = onOpenProfile)
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            StartMarkingCard(onStartMarking)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.forEach { stat ->
                    ElCard(Modifier.weight(1f)) {
                        Text(stat.value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(stat.label, fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent assessments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        items(sampleRecentAssessments, key = { it.title }) { assessment ->
            AssessmentCard(assessment, onClick = onOpenAssessment)
        }
    }
}

@Composable
private fun StartMarkingCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Accent, AccentDark)))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Start marking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                "Create an OMR assessment and grade sheets in minutes.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Accent)
        }
    }
}

/** One assessment summary card; shared by Home and the Assessments tab. */
@Composable
fun AssessmentCard(assessment: RecentAssessment, onClick: () -> Unit) {
    ElCard(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                assessment.title,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(assessment.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(assessment.meta, fontSize = 12.sp, color = Muted)
    }
}

@Composable
fun UserAvatar(
    imageUrl: String? = null,
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Accent,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = Color.White,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(initials, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun greetingForNow(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun DashboardScreenPreview() {
    AutoGradeTheme {
        DashboardScreen(firstName = "Rakib", avatarInitials = "R")
    }
}
