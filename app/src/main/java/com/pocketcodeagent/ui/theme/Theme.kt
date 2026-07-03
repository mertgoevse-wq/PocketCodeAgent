package com.pocketcodeagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DeepSlateBackground,
    surface = DarkSurface,
    onPrimary = DeepSlateBackground,
    onSecondary = DeepSlateBackground,
    onTertiary = DeepSlateBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PocketCodeAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
