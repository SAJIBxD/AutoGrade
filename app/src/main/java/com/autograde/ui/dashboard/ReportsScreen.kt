package com.autograde.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.ElCard
import com.autograde.ui.components.StatusChip
import com.autograde.ui.theme.Bg
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Muted

private data class Report(val title: String, val meta: String, val score: String, val status: String)

private val reports = listOf(
    Report("Theory Final — Operating System", "BSc 3rd Year · Nov 6, 2026", "83.6 avg", "Completed"),
    Report("DBMS — Quiz 2", "BSc 3rd Year · Oct 28, 2026", "79.2 avg", "Completed"),
    Report("Discrete Math — Final", "BSc 2nd Year · Oct 21, 2026", "71.4 avg", "Completed"),
    Report("CSE 310 — Mid Term", "BSc Final Year · Oct 14, 2026", "68.9 avg", "Review"),
)

/** Results tab: class reports from finished assessments. */
@Composable
fun ReportsScreen(onOpenReport: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Results", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Class reports from completed assessments.", fontSize = 14.sp, color = Muted)
            }
        }
        items(reports, key = { it.title }) { report ->
            ElCard(Modifier.clickable(onClick = onOpenReport)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(report.title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(report.meta, fontSize = 12.sp, color = Muted)
                    }
                    StatusChip(report.status)
                }
                Spacer(Modifier.height(8.dp))
                Text(report.score, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
        }
    }
}
