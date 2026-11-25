package dev.lurexh.halo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HaloColors = darkColorScheme(
    primary = HaloPurple,
    secondary = HaloCyan,
    tertiary = HaloPink,
    background = Void,
    surface = DeepSpace,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun HaloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HaloColors, typography = HaloType, content = content)
}
