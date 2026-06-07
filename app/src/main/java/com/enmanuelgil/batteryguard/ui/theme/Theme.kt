package com.enmanuelgil.batteryguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta verde-energía
val BatteryGreen      = Color(0xFF4CAF50)
val BatteryGreenLight = Color(0xFF81C784)
val BatteryYellow     = Color(0xFFFFB74D)
val BatteryRed        = Color(0xFFEF5350)
val BatteryBlue       = Color(0xFF42A5F5)

val BackgroundDark    = Color(0xFF0D1117)
val SurfaceDark       = Color(0xFF161B22)
val CardDark          = Color(0xFF21262D)
val TextPrimary       = Color(0xFFE6EDF3)
val TextSecondary     = Color(0xFF8B949E)

private val DarkColors = darkColorScheme(
    primary         = BatteryGreen,
    onPrimary       = Color.Black,
    secondary       = BatteryBlue,
    background      = BackgroundDark,
    surface         = SurfaceDark,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary
)

@Composable
fun BatteryGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}

fun batteryColor(level: Int): Color = when {
    level > 50 -> BatteryGreen
    level > 20 -> BatteryYellow
    else       -> BatteryRed
}

fun tempColor(temp: Float): Color = when {
    temp < 35f -> BatteryGreen
    temp < 45f -> BatteryYellow
    else       -> BatteryRed
}
