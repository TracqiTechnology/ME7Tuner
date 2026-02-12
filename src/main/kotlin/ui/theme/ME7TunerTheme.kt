package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Material 3 dark theme colors
val Primary = Color(0xFFFFEBCE)
val OnPrimary = Color(0xFF412D00)
val PrimaryContainer = Color(0xFFF9B925)
val OnPrimaryContainer = Color(0xFF463100)
val Secondary = Color(0xFFE5C282)
val OnSecondary = Color(0xFF412D00)
val SecondaryContainer = Color(0xFF503905)
val OnSecondaryContainer = Color(0xFFF0CC8B)
val Tertiary = Color(0xFFE2F971)
val OnTertiary = Color(0xFF2C3400)
val TertiaryContainer = Color(0xFFB8CE4C)
val OnTertiaryContainer = Color(0xFF2F3800)
val Background = Color(0xFF18130A)
val OnBackground = Color(0xFFEDE1D1)
val Surface = Color(0xFF18130A)
val OnSurface = Color(0xFFEDE1D1)
val SurfaceVariant = Color(0xFF504533)
val OnSurfaceVariant = Color(0xFFD4C4AD)
val SurfaceDim = Color(0xFF18130A)
val SurfaceBright = Color(0xFF3F382E)
val SurfaceContainerLowest = Color(0xFF120D06)
val SurfaceContainerLow = Color(0xFF201B12)
val SurfaceContainer = Color(0xFF241F15)
val SurfaceContainerHigh = Color(0xFF2F291F)
val SurfaceContainerHighest = Color(0xFF3A3429)
val Outline = Color(0xFF9D8F79)
val OutlineVariant = Color(0xFF504533)
val ErrorColor = Color(0xFFFFB4AB)
val OnError = Color(0xFF690005)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)
val InverseSurface = Color(0xFFEDE1D1)
val InverseOnSurface = Color(0xFF363025)
val InversePrimary = Color(0xFF7B5800)
val Scrim = Color(0xFF000000)

// Chart colors
val ChartRed = Color(0xFFD32F2F)
val ChartGreen = Color(0xFF388E3C)
val ChartMagenta = Color(0xFF7B1FA2)
val ChartBlue = Color(0xFF1976D2)
val ChartOrange = Color(0xFFF57C00)
val ChartCyan = Color(0xFF00BCD4)
val GridColor = Color(0x40EDE1D1)

private val ME7ColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    scrim = Scrim,
)

private val ME7Typography = Typography(
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        color = OnBackground
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp,
        color = OnBackground
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        color = OnBackground
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        color = OnBackground
    )
)

@Composable
fun ME7TunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ME7ColorScheme,
        typography = ME7Typography,
        content = content
    )
}
