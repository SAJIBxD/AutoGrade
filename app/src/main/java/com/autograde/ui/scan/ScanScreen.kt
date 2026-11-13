package com.autograde.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.DarkBg
import com.autograde.ui.theme.DarkSurface
import com.autograde.ui.theme.Ink

private fun DrawScope.corner(x: Float, y: Float, dx: Float, dy: Float, len: Float, sw: Float) {
    val c = Accent
    drawLine(c, Offset(x, y), Offset(x + dx * len, y), sw, StrokeCap.Round)
    drawLine(c, Offset(x, y), Offset(x, y + dy * len), sw, StrokeCap.Round)
}

/**
 * Screen 7 — camera capture of an OMR answer sheet.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
) {
    var pages by remember { mutableIntStateOf(1) }
    var flash by remember { mutableIntStateOf(0) } // 0 = auto, 1 = on, 2 = off

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: back, title, flash toggle
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkSurface, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                "Scan OMR Sheet",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { flash = (flash + 1) % 3 },
                modifier = Modifier.background(DarkSurface, CircleShape),
            ) {
                Text(
                    when (flash) { 1 -> "⚡ On"; 2 -> "⚡ Off"; else -> "⚡ Auto" },
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }

        // Viewfinder
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF16161F)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val fw = size.width * 0.74f
                val fh = size.height * 0.84f
                val l = (size.width - fw) / 2
                val t = (size.height - fh) / 2

                // simulated paper
                drawRoundRect(Color(0xFFF7F5EC), Offset(l, t), Size(fw, fh), CornerRadius(10f))

                // OMR bubble rows preview
                val rowCount = 8
                val bubbleRadius = 5.dp.toPx()
                for (row in 0 until rowCount) {
                    val y = t + fh * (0.15f + row * 0.10f)
                    val startX = l + fw * 0.25f
                    val gapX = fw * 0.15f
                    for (col in 0 until 4) {
                        val cx = startX + col * gapX
                        val filled = (row + col) % 3 == 0
                        if (filled) {
                            drawCircle(Color(0xFF2B3A8C), radius = bubbleRadius, center = Offset(cx, y))
                        } else {
                            drawCircle(Color(0xFF8C90A0), radius = bubbleRadius, center = Offset(cx, y), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }

                // corner brackets
                val len = 34.dp.toPx()
                val sw = 4.dp.toPx()
                val o = 6.dp.toPx()
                corner(l - o, t - o, 1f, 1f, len, sw)
                corner(l + fw + o, t - o, -1f, 1f, len, sw)
                corner(l - o, t + fh + o, 1f, -1f, len, sw)
                corner(l + fw + o, t + fh + o, -1f, -1f, len, sw)
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Page $pages of ${pages + 3}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(Accent, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // Page thumbnails
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { i ->
                val active = i == pages - 1
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F5EC))
                        .border(
                            2.dp,
                            if (active) Accent else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { pages = i + 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .clickable { pages++ },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
        }

        // Actions
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = { if (pages > 1) pages-- },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
            ) {
                Text("Retake", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            TextButton(
                onClick = onCapture,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent),
            ) {
                Text("Use Scan ($pages)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Preview
@Composable
private fun ScanScreenPreview() {
    AutoGradeTheme {
        ScanScreen()
    }
}
