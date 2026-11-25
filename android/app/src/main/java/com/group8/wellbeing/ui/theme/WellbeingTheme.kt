package com.group8.wellbeing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B59),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8EDE6),
    onPrimaryContainer = Color(0xFF0B352C),
    secondary = Color(0xFF475B56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EBE8),
    onSecondaryContainer = Color(0xFF1A2825),
    tertiary = Color(0xFFB55A1A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9E6D6),
    onTertiaryContainer = Color(0xFF4A2005),
    background = Color(0xFFEDF2F0),
    onBackground = Color(0xFF14201E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14201E),
    surfaceVariant = Color(0xFFE3ECE9),
    onSurfaceVariant = Color(0xFF4B5E5A),
    outline = Color(0xFFCDD9D5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3BB296),
    onPrimary = Color(0xFF05241D),
    primaryContainer = Color(0xFF163A32),
    onPrimaryContainer = Color(0xFFA8E6D6),
    secondary = Color(0xFF97ACA7),
    onSecondary = Color(0xFF0E1A18),
    secondaryContainer = Color(0xFF1E2A28),
    onSecondaryContainer = Color(0xFFDCE6E3),
    tertiary = Color(0xFFE29354),
    onTertiary = Color(0xFF2B1404),
    tertiaryContainer = Color(0xFF3A2414),
    onTertiaryContainer = Color(0xFFF7CFAF),
    background = Color(0xFF0E1413),
    onBackground = Color(0xFFECF2F0),
    surface = Color(0xFF161F1D),
    onSurface = Color(0xFFECF2F0),
    surfaceVariant = Color(0xFF1E2927),
    onSurfaceVariant = Color(0xFF9EB1AC),
    outline = Color(0xFF2A3936)
)

private val WellbeingTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
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
fun WellbeingTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WellbeingTypography,
        content = content
    )
}
