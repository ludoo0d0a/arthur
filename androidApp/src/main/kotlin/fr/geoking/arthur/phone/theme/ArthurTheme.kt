package fr.geoking.arthur.phone.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Gallery-first Material 3 palette — indigo / cyan / blush on AMOLED. */
private val ArthurPrimary = Color(0xFFC4B5FD)
private val ArthurOnPrimary = Color(0xFF2A1F66)
private val ArthurPrimaryContainer = Color(0xFF4C3D99)
private val ArthurOnPrimaryContainer = Color(0xFFEDE7FF)
private val ArthurSecondary = Color(0xFF5EEAD4)
private val ArthurOnSecondary = Color(0xFF003731)
private val ArthurSecondaryContainer = Color(0xFF115E59)
private val ArthurOnSecondaryContainer = Color(0xFFCCFBF1)
private val ArthurTertiary = Color(0xFFF9A8D4)
private val ArthurOnTertiary = Color(0xFF4A1030)
private val ArthurTertiaryContainer = Color(0xFF831843)
private val ArthurOnTertiaryContainer = Color(0xFFFFE4F1)
private val ArthurBackground = Color(0xFF0B1020)
private val ArthurOnBackground = Color(0xFFE8E6F2)
private val ArthurSurface = Color(0xFF12182C)
private val ArthurOnSurface = Color(0xFFE8E6F2)
private val ArthurSurfaceVariant = Color(0xFF1C2440)
private val ArthurOnSurfaceVariant = Color(0xFFC5C0D6)
private val ArthurOutline = Color(0xFF8B86A3)
private val ArthurOutlineVariant = Color(0xFF3A4160)

private val ArthurDarkColors = darkColorScheme(
    primary = ArthurPrimary,
    onPrimary = ArthurOnPrimary,
    primaryContainer = ArthurPrimaryContainer,
    onPrimaryContainer = ArthurOnPrimaryContainer,
    secondary = ArthurSecondary,
    onSecondary = ArthurOnSecondary,
    secondaryContainer = ArthurSecondaryContainer,
    onSecondaryContainer = ArthurOnSecondaryContainer,
    tertiary = ArthurTertiary,
    onTertiary = ArthurOnTertiary,
    tertiaryContainer = ArthurTertiaryContainer,
    onTertiaryContainer = ArthurOnTertiaryContainer,
    background = ArthurBackground,
    onBackground = ArthurOnBackground,
    surface = ArthurSurface,
    onSurface = ArthurOnSurface,
    surfaceVariant = ArthurSurfaceVariant,
    onSurfaceVariant = ArthurOnSurfaceVariant,
    surfaceContainerHighest = Color(0xFF242C48),
    surfaceContainerHigh = Color(0xFF1C2440),
    surfaceContainer = Color(0xFF161D34),
    surfaceContainerLow = Color(0xFF12182C),
    surfaceContainerLowest = Color(0xFF080C18),
    outline = ArthurOutline,
    outlineVariant = ArthurOutlineVariant,
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val ArthurLightColors = lightColorScheme(
    primary = Color(0xFF5B4CB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF1E1460),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF9D174D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4F1),
    onTertiaryContainer = Color(0xFF4A1030),
    background = Color(0xFFF6F4FF),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xFFE6E0F5),
    onSurfaceVariant = Color(0xFF4A4660),
)

private val ArthurTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)

private val ArthurShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ArthurTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ArthurDarkColors else ArthurLightColors,
        typography = ArthurTypography,
        shapes = ArthurShapes,
        content = content,
    )
}
