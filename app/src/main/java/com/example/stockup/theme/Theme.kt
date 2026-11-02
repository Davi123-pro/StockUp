package com.example.stockup.theme

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
    primary = Color(0xFF88D7A2),
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF2E7D50),
    onPrimaryContainer = Color(0xFFCEFFDA),
    secondary = Color(0xFFA0D2AD),
    onSecondary = Color(0xFF06381F),
    secondaryContainer = Color(0xFF245236),
    onSecondaryContainer = Color(0xFF93C4A0),
    tertiary = Color(0xFFFFB2BB),
    onTertiary = Color(0xFF5B1826),
    tertiaryContainer = Color(0xFFA95562),
    onTertiaryContainer = Color(0xFFFFF0F0),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE0E4DD),
    surface = Color(0xFF181D19),
    onSurface = Color(0xFFE0E4DD),
    surfaceVariant = Color(0xFF272B27),
    onSurfaceVariant = Color(0xFFBFC9BF),
    outline = Color(0xFF89938A),
    outlineVariant = Color(0xFF3F4941),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF196C41),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA4F4BD),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4A635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBCEFC8),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF82535A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DD),
    onTertiaryContainer = Color(0xFF3E0212),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE0E5E0),
    onSurfaceVariant = Color(0xFF424943),
    outline = Color(0xFF727973),
    outlineVariant = Color(0xFFC2C9C2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun StockUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desativado por padrão para preservar paleta de marca consistente
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
