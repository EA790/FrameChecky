package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FrameCheckDarkColorScheme =
  darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = TextPrimary,
    secondaryContainer = AccentGreenDark,
    onSecondaryContainer = TextPrimary,
    tertiary = VerdictPlayableColor,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = CharcoalCard,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedChip,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    outlineVariant = SubtleBorder,
    error = VerdictUnsupportedColor,
    onError = TextPrimary,
  )

private val FrameCheckShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
  )

@Composable
fun FrameCheckTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = FrameCheckDarkColorScheme,
    typography = FrameCheckTypography,
    shapes = FrameCheckShapes,
    content = content,
  )
}
