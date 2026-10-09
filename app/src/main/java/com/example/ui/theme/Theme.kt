package com.example.ui.theme

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
    primary = PrimaryBlue,
    onPrimary = Color(0xFF11111B),
    primaryContainer = Color(0xFF313244),
    onPrimaryContainer = Color(0xFFCDD6F4),
    secondary = SecondaryTeal,
    onSecondary = Color(0xFF11111B),
    tertiary = AccentPurple,
    background = IdeBackground,
    onBackground = TerminalText,
    surface = IdeSurface,
    onSurface = TerminalText,
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = IdeBorder,
    error = ErrorRed,
    onError = Color(0xFF11111B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E66F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE0E8),
    onPrimaryContainer = Color(0xFF4C4F69),
    secondary = Color(0xFF179299),
    onSecondary = Color.White,
    tertiary = Color(0xFF8839EF),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFE6E9EF),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF5C5F77),
    outline = Color(0xFFBCC0CC),
    error = Color(0xFFD20F39),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark IDE theme
    dynamicColor: Boolean = false,
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
