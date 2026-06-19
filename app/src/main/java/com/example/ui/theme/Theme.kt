package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Force dark cybernetic colors for MindOS HUD feel
private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricViolet,
    tertiary = CyberBlue,
    background = CosmicBackground,
    surface = CosmicSurface,
    surfaceVariant = CosmicSurfaceVariant,
    onBackground = OnCosmicBackground,
    onSurface = OnCosmicSurface,
    onSurfaceVariant = OnCosmicSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the sleek cybernetic MindOS vibe
    dynamicColor: Boolean = false, // Disable default dynamic light tints to preserve the precise neon HUD colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
