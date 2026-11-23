package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
  primary = InkPrimary,
  onPrimary = InkOnPrimary,
  primaryContainer = InkPrimaryContainer,
  onPrimaryContainer = InkOnPrimaryContainer,
  secondary = AmberSecondary,
  onSecondary = AmberOnSecondary,
  secondaryContainer = AmberSecondaryContainer,
  onSecondaryContainer = AmberOnSecondaryContainer,
  tertiary = SageTertiary,
  onTertiary = SageOnTertiary,
  tertiaryContainer = SageTertiaryContainer,
  onTertiaryContainer = SageOnTertiaryContainer,
  background = PaperBackground,
  onBackground = InkTextPrimary,
  surface = PaperSurface,
  onSurface = InkTextPrimary,
  surfaceVariant = PaperSurfaceVariant,
  onSurfaceVariant = InkTextMuted,
  outline = PaperOutline,
  outlineVariant = PaperOutlineSubtle,
  error = ErrorLight,
  errorContainer = ErrorContainerLight,
)

private val DarkColors = darkColorScheme(
  primary = SkyPrimary,
  onPrimary = SkyOnPrimary,
  primaryContainer = SkyPrimaryContainer,
  onPrimaryContainer = SkyOnPrimaryContainer,
  secondary = GoldSecondary,
  onSecondary = GoldOnSecondary,
  secondaryContainer = GoldSecondaryContainer,
  onSecondaryContainer = GoldOnSecondaryContainer,
  tertiary = MintTertiary,
  onTertiary = MintOnTertiary,
  tertiaryContainer = MintTertiaryContainer,
  onTertiaryContainer = MintOnTertiaryContainer,
  background = SlateBackground,
  onBackground = SlateTextPrimary,
  surface = SlateSurface,
  onSurface = SlateTextPrimary,
  surfaceVariant = SlateSurfaceVariant,
  onSurfaceVariant = SlateTextMuted,
  outline = SlateOutline,
  outlineVariant = SlateOutlineSubtle,
  error = ErrorDark,
  errorContainer = ErrorContainerDark,
)

val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SozlukFiltreTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
