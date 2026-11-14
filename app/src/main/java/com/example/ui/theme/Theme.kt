package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FireKernCasinoColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = FireKernVoid,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = FireKernFlame,
    onSecondary = FireKernVoid,
    secondaryContainer = FireKernEmberDark,
    onSecondaryContainer = GoldLight,
    tertiary = FireKernEmber,
    onTertiary = Color.White,
    background = FireKernVoid,
    onBackground = Color(0xFFFFF8F0),
    surface = FireKernCard,
    onSurface = Color(0xFFFFF8F0),
    surfaceVariant = FireKernCardElevated,
    onSurfaceVariant = FireKernAsh,
    outline = FireKernCardBorder,
    outlineVariant = GoldMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FireKernCasinoColorScheme,
        typography = Typography,
        content = content
    )
}
