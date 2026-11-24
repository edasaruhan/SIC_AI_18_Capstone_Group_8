package com.group8.wellbeing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TealPrimary = Color(0xFF1F6E5C)
val TealContainer = Color(0xFFE3F0EC)
val OnTealContainer = Color(0xFF103B31)
val WarmBackground = Color(0xFFF7F9F8)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceSoft = Color(0xFFEEF3F1)
val InkPrimary = Color(0xFF1A2624)
val InkSecondary = Color(0xFF52625E)
val BorderSubtle = Color(0xFFD9E3DF)
val AmberMuted = Color(0xFFB85D19)
val AmberContainer = Color(0xFFFBEDE2)

private val WellbeingLightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = InkSecondary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSoft,
    onSecondaryContainer = InkPrimary,
    tertiary = AmberMuted,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF4A2204),
    background = WarmBackground,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkSecondary,
    outline = BorderSubtle
)

private val WellbeingTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun WellbeingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WellbeingLightColors,
        typography = WellbeingTypography,
        content = content
    )
}
