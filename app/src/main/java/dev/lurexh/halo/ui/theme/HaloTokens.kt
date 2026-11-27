package dev.lurexh.halo.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HaloTokens {
    val ScreenPad = 20.dp
    val CardPad = 16.dp
    val CardRadius = 22.dp
    val PillRadius = 50.dp
    val RowRadius = 16.dp
    val TinyRadius = 12.dp

    val Hairline = 1.dp
    val Raised = 2.dp

    val Display = 30.sp
    val Title = 16.sp
    val Body = 13.sp
    val Label = 10.sp

    object Motion {
        const val Fast = 180
        const val Medium = 360
        const val Slow = 900
        const val Orbit = 42000
    }

    object Glow {
        const val Soft = 0.12f
        const val Medium = 0.28f
        const val Strong = 0.55f
    }
}
