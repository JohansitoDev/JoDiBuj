package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JoDiBujColorScheme = lightColorScheme(
    primary = MacawBlue,
    onPrimary = PureWhite,
    primaryContainer = MacawBlue.copy(alpha = 0.12f),
    onPrimaryContainer = DeepNavy,
    secondary = FeatherGreen,
    onSecondary = PureWhite,
    secondaryContainer = FeatherGreen.copy(alpha = 0.14f),
    onSecondaryContainer = FeatherGreenDark,
    tertiary = LogoPurple,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderGray,
    error = CardinalRed,
    onError = PureWhite,
)

private val JoDiBujShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun JoDiBujTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JoDiBujColorScheme,
        typography = JoDiBujTypography,
        shapes = JoDiBujShapes,
        content = content,
    )
}
