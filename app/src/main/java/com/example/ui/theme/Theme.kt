package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode

fun getThemeColorScheme(themeMode: AppThemeMode): ColorScheme {
  return when (themeMode) {
    AppThemeMode.OLED_MIDNIGHT -> darkColorScheme(
      primary = OledPrimary,
      secondary = OledSecondary,
      background = OledBg,
      surface = OledSurface,
      surfaceVariant = OledSurfaceVariant,
      onPrimary = Color.White,
      onSecondary = Color.Black,
      onBackground = OledText,
      onSurface = OledText,
      onSurfaceVariant = OledTextMuted,
      outline = Color(0xFF2E384D)
    )
    AppThemeMode.DEEP_SPACE -> darkColorScheme(
      primary = ObsidianPrimary,
      secondary = ObsidianSecondary,
      background = ObsidianBg,
      surface = ObsidianSurface,
      surfaceVariant = ObsidianSurfaceVariant,
      onPrimary = Color.White,
      onSecondary = Color.White,
      onBackground = ObsidianText,
      onSurface = ObsidianText,
      onSurfaceVariant = ObsidianTextMuted,
      outline = Color(0xFF3F3F4E)
    )
    AppThemeMode.EMERALD_MATRIX -> darkColorScheme(
      primary = EmeraldPrimary,
      secondary = EmeraldSecondary,
      background = EmeraldBg,
      surface = EmeraldSurface,
      surfaceVariant = EmeraldSurfaceVariant,
      onPrimary = Color.Black,
      onSecondary = Color.Black,
      onBackground = EmeraldText,
      onSurface = EmeraldText,
      onSurfaceVariant = EmeraldTextMuted,
      outline = Color(0xFF1B4D36)
    )
    AppThemeMode.SLATE_GRAY -> darkColorScheme(
      primary = SlatePrimary,
      secondary = SlateSecondary,
      background = SlateBg,
      surface = SlateSurface,
      surfaceVariant = SlateSurfaceVariant,
      onPrimary = Color.White,
      onSecondary = Color.Black,
      onBackground = SlateText,
      onSurface = SlateText,
      onSurfaceVariant = SlateTextMuted,
      outline = Color(0xFF475569)
    )
    AppThemeMode.HIGH_CONTRAST -> darkColorScheme(
      primary = HighContrastPrimary,
      secondary = HighContrastSecondary,
      background = HighContrastBg,
      surface = HighContrastSurface,
      surfaceVariant = Color(0xFF1A1A1A),
      onPrimary = Color.Black,
      onSecondary = Color.Black,
      onBackground = HighContrastText,
      onSurface = HighContrastText,
      onSurfaceVariant = HighContrastTextMuted,
      outline = HighContrastBorder
    )
  }
}

@Composable
fun MyApplicationTheme(
  themeMode: AppThemeMode = AppThemeMode.OLED_MIDNIGHT,
  content: @Composable () -> Unit
) {
  val colorScheme = getThemeColorScheme(themeMode)
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
