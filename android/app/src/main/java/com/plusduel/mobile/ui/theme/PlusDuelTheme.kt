package com.plusduel.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.AccentSkin
import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.data.ThemeMode

@Immutable
data class PlusDuelColors(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val orange: Color,
    val tint: Color,
    val onInk: Color,
    val success: Color,
    val danger: Color,
    val starGold: Color,
    val isDark: Boolean
)

val LightPlusDuelColors = PlusDuelColors(
    bg = Color(0xFFFAF9F6),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF141414),
    muted = Color(0xFF5D5D58),
    line = Color(0xFFD9D6CF),
    orange = Color(0xFFFF5C00),
    tint = Color(0xFFFFF0E6),
    onInk = Color(0xFFFFFFFF),
    success = Color(0xFF1E8E4E),
    danger = Color(0xFFD93025),
    starGold = Color(0xFFFF8C00),
    isDark = false
)

val DarkPlusDuelColors = PlusDuelColors(
    bg = Color(0xFF121211),
    surface = Color(0xFF1B1B19),
    ink = Color(0xFFF3F1EC),
    muted = Color(0xFFA3A19A),
    line = Color(0xFF34332F),
    orange = Color(0xFFFF7A2E),
    tint = Color(0xFF2C1B10),
    onInk = Color(0xFF121211),
    success = Color(0xFF38C172),
    danger = Color(0xFFFF5252),
    starGold = Color(0xFFFFAA33),
    isDark = true
)

val LocalPlusDuelColors = staticCompositionLocalOf { LightPlusDuelColors }

object PlusDuelTheme {
    val colors: PlusDuelColors
        @Composable
        get() = LocalPlusDuelColors.current
}

private val MonoFont = FontFamily.Monospace

private val PlusDuelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

@Composable
fun PlusDuelTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentSkin: AccentSkin = AccentSkin.ORANGE,
    language: AppLanguage = AppLanguage.RU,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val basePalette = if (useDark) DarkPlusDuelColors else LightPlusDuelColors
    val palette = basePalette.copy(
        orange = if (useDark) accentSkin.darkAccent else accentSkin.lightAccent,
        tint = if (useDark) accentSkin.darkTint else accentSkin.lightTint
    )

    val materialScheme = if (useDark) {
        darkColorScheme(
            primary = palette.orange,
            background = palette.bg,
            surface = palette.surface,
            onPrimary = Color.White,
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    } else {
        lightColorScheme(
            primary = palette.orange,
            background = palette.bg,
            surface = palette.surface,
            onPrimary = Color.White,
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    }

    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalPlusDuelColors provides palette,
        LocalLayoutDirection provides direction
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = PlusDuelTypography,
            content = content
        )
    }
}
