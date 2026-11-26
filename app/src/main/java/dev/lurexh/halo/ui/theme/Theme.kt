package dev.lurexh.halo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HaloColors = darkColorScheme(
    primary = Proton,
    onPrimary = Color.White,
    secondary = HaloCyan,
    onSecondary = Void,
    tertiary = HaloPink,
    background = Void,
    onBackground = TextPrimary,
    surface = HaloSurface,
    onSurface = TextPrimary,
    surfaceVariant = HaloSurfaceHigh,
    onSurfaceVariant = TextMuted,
    outline = HaloBorder,
    error = Danger,
    onError = Color.White,
)

@Composable
fun HaloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HaloColors, typography = HaloType, content = content)
}
