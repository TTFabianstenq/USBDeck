package com.donutsmp.usbdeck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.donutsmp.usbdeck.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C4FF),
    onPrimary = Color(0xFF1A2D6B),
    primaryContainer = Color(0xFF314582),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFF9CD5C4),
    onSecondary = Color(0xFF00382C),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE3E8F4),
    surface = Color(0xFF121A2A),
    onSurface = Color(0xFFE3E8F4),
    surfaceVariant = Color(0xFF1C2638),
    onSurfaceVariant = Color(0xFFB8C0D4),
    outline = Color(0xFF3E4A63),
    error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF314582),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF0F6654),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF121826),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121826),
    surfaceVariant = Color(0xFFE7EBF5),
    onSurfaceVariant = Color(0xFF414B5E),
    outline = Color(0xFFC3CAD8),
    error = Color(0xFFBA1A1A)
)

@Composable
fun UsbDeckTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val context = LocalContext.current
    val scheme: ColorScheme = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
