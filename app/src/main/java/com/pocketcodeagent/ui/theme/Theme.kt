package com.pocketcodeagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val LocalPcaThemeMode = compositionLocalOf { PcaThemeMode.DarkPremium }

private val DarkColors = darkColorScheme(
    primary = SlateBlue,
    secondary = CalmSage,
    tertiary = WarmCopper,
    background = Color(0xFF0E0E10),
    surface = Color(0xFF18181C),
    surfaceVariant = Color(0xFF1E1E24),
    onPrimary = Color(0xFF0E0E10),
    onSecondary = Color(0xFF0E0E10),
    onTertiary = Color(0xFF0E0E10),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderGrey
)

private val IvoryColors = lightColorScheme(
    primary = IvoryAccent,
    secondary = IvoryAccentBlue,
    tertiary = IvorySuccess,
    background = IvoryBackground,
    surface = IvorySurface,
    surfaceVariant = IvorySurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = IvoryTextPrimary,
    onSurface = IvoryTextPrimary,
    onSurfaceVariant = IvoryTextSecondary,
    outline = IvoryBorder,
    error = IvoryError
)

@Composable
fun PocketCodeAgentTheme(
    themeMode: PcaThemeMode = PcaThemeMode.DarkPremium,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val resolvedMode = remember(themeMode, isDark) {
        when (themeMode) {
            PcaThemeMode.System -> if (isDark) PcaThemeMode.DarkPremium else PcaThemeMode.IvoryClaudeLike
            else -> themeMode
        }
    }
    val colorScheme = remember(resolvedMode) {
        when (resolvedMode) {
            PcaThemeMode.IvoryClaudeLike -> IvoryColors
            else -> DarkColors
        }
    }

    CompositionLocalProvider(LocalPcaThemeMode provides resolvedMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
