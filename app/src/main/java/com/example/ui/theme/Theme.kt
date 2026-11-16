package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
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

// Sharp professional hardware-tool corners (2dp radius max)
private val FrameCheckShapes =
  Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
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
