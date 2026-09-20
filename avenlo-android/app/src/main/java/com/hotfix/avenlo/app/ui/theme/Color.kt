package com.hotfix.avenlo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = AvenloTokens.Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = AvenloTokens.Success,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = AvenloTokens.Warning,
    background = AvenloTokens.Bg,
    onBackground = AvenloTokens.TextPrimary,
    surface = AvenloTokens.Surface,
    onSurface = AvenloTokens.TextPrimary,
    surfaceVariant = AvenloTokens.Surface,
    onSurfaceVariant = AvenloTokens.TextSecondary,
    outline = AvenloTokens.Border,
    error = AvenloTokens.Error,
)

@Composable
fun AvenloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = AvenloTypography,
        content = content,
    )
}
