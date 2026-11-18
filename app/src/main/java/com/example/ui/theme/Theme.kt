package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = PureWhite,
    primaryContainer = DeepBlueContainer,
    onPrimaryContainer = OnDeepBlueContainer,
    secondary = VibrantOrange,
    onSecondary = PureWhite,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = SlateTeal,
    onTertiary = PureWhite,
    tertiaryContainer = TealContainer,
    background = SoftGrayBg,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlueLight,
    onPrimary = DeepBlueDark,
    primaryContainer = DeepBlue,
    onPrimaryContainer = PureWhite,
    secondary = OrangeLight,
    onSecondary = OnOrangeContainer,
    secondaryContainer = VibrantOrangeDark,
    onSecondaryContainer = PureWhite,
    background = DarkBg,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkMuted,
    outline = ColorTokens.DarkBorder,
    error = ErrorRed,
    onError = PureWhite
)

private object ColorTokens {
    val DarkMuted = androidx.compose.ui.graphics.Color(0xFF94A3B8)
    val DarkBorder = androidx.compose.ui.graphics.Color(0xFF334155)
}

val KenaKataShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KenaKataTheme(
    darkTheme: Boolean = false,
    isBengali: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = kenaKataTypography(isBengali),
        shapes = KenaKataShapes,
        content = content
    )
}
