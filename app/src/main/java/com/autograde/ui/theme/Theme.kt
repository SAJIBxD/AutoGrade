package com.autograde.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    surface = Color.White,
    onSurface = Ink,
)

@Composable
fun AutoGradeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
fun ExamLensTheme(
    content: @Composable () -> Unit,
) {
    AutoGradeTheme(content = content)
}
