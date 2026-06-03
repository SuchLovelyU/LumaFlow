package com.example.rgbcontrller.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002E36),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFC0F4FF),
    secondary = NeonViolet,
    tertiary = AuroraGreen,
    background = SpaceDark,
    onBackground = Color(0xFFE7EBF0),
    surface = SpaceDarkAlt,
    onSurface = Color(0xFFE7EBF0),
    surfaceVariant = Color(0xFF232733),
    onSurfaceVariant = Color(0xFFC2C8D4),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00677A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EBFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF65558F),
    tertiary = Color(0xFF006C4B),
    background = CloudLight,
    onBackground = Color(0xFF171C20),
    surface = Color.White,
    onSurface = Color(0xFF171C20),
    surfaceVariant = CloudLightAlt,
    onSurfaceVariant = Color(0xFF3F4850),
)

@Composable
fun RGBContrllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
