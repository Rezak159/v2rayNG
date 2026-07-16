package com.v2ray.ang.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// a4vpn brand palette: cream paper, ink black, signal red (from a4flow logo)
val colorBrandInk = Color(0xFF17181C)
val colorBrandCream = Color(0xFFF2EDE3)
val colorBrandRed = Color(0xFFB3282D)

private val LightColor = lightColorScheme(
    primary = Color(0xFF17181C), // Ink
    onPrimary = Color(0xFFF5F1E8), // Cream
    primaryContainer = Color(0xFFE6DFD0), // Dark Cream
    onPrimaryContainer = Color(0xFF17181C), // Ink
    secondary = Color(0xFFB3282D), // Brand Red
    onSecondary = Color(0xFFFFF8F0), // Warm White
    secondaryContainer = Color(0xFFF0D7D0), // Pale Red
    onSecondaryContainer = Color(0xFF430A0D), // Deep Red
    tertiary = Color(0xFF2E7D5B), // Muted Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFC9E6D4), // Pale Green
    onTertiaryContainer = Color(0xFF0A2B1D), // Deep Green
    error = Color(0xFF9E1C21), // Dark Red
    errorContainer = Color(0xFFF5D8D3), // Pale Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410205), // Deep Red
    background = Color(0xFFF2EDE3), // Cream
    onBackground = Color(0xFF17181C), // Ink
    surface = Color(0xFFF2EDE3), // Cream
    onSurface = Color(0xFF17181C), // Ink
    surfaceVariant = Color(0xFFE7E0D1), // Darker Cream
    onSurfaceVariant = Color(0xFF56534A), // Warm Gray
    outline = Color(0xFF878376), // Warm Medium Gray
    outlineVariant = Color(0xFFD6CFBF), // Warm Light Gray
    inverseSurface = Color(0xFF17181C), // Ink
    inverseOnSurface = Color(0xFFF2EDE3), // Cream
    inversePrimary = Color(0xFFE8E2D3), // Cream
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF17181C), // Ink
    surfaceContainerLowest = Color(0xFFF9F5ED), // Lightest Cream
    surfaceContainerLow = Color(0xFFEFE9DC), // Light Cream
    surfaceContainer = Color(0xFFEAE3D4), // Cream
    surfaceContainerHigh = Color(0xFFE4DCCB), // Dark Cream
    surfaceContainerHighest = Color(0xFFDDD4C1), // Darkest Cream
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFEDE7DA), // Cream
    onPrimary = Color(0xFF1A1B20), // Ink
    primaryContainer = Color(0xFF34353C), // Graphite
    onPrimaryContainer = Color(0xFFEDE7DA), // Cream
    secondary = Color(0xFFD6494E), // Bright Red
    onSecondary = Color(0xFF2B0507), // Deep Red
    secondaryContainer = Color(0xFF5D1518), // Dark Red
    onSecondaryContainer = Color(0xFFF6D9D6), // Pale Red
    tertiary = Color(0xFF57B98A), // Mint Green
    onTertiary = Color(0xFF062819), // Deep Green
    tertiaryContainer = Color(0xFF1E4A36), // Dark Green
    onTertiaryContainer = Color(0xFFCDEDDB), // Pale Green
    error = Color(0xFFE36A6B), // Light Red
    errorContainer = Color(0xFF7A1A1D), // Dark Red
    onError = Color(0xFF3D0406), // Deep Red
    onErrorContainer = Color(0xFFF7D9D6), // Pale Red
    background = Color(0xFF131418), // Near Black
    onBackground = Color(0xFFE9E4D8), // Cream
    surface = Color(0xFF131418), // Near Black
    onSurface = Color(0xFFE9E4D8), // Cream
    surfaceVariant = Color(0xFF2A2B31), // Graphite
    onSurfaceVariant = Color(0xFFACA89C), // Warm Gray
    outline = Color(0xFF767268), // Warm Medium Gray
    outlineVariant = Color(0xFF34353B), // Graphite
    inverseSurface = Color(0xFFE9E4D8), // Cream
    inverseOnSurface = Color(0xFF17181C), // Ink
    inversePrimary = Color(0xFF17181C), // Ink
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFEDE7DA), // Cream
    surfaceContainerLowest = Color(0xFF0E0F12), // Black
    surfaceContainerLow = Color(0xFF17181D), // Near Black
    surfaceContainer = Color(0xFF1B1C22), // Graphite
    surfaceContainerHigh = Color(0xFF24252C), // Graphite
    surfaceContainerHighest = Color(0xFF2D2E35), // Light Graphite
)

// Semantic Colors
val colorPing = Color(0xFF3C9A6E) // Muted Green
val colorPingRed = Color(0xFFC2474C) // Soft Red
val colorFabActive = colorBrandRed // Brand Red
val colorFabInactiveLight = Color(0xFF212227) // Ink
val colorFabInactiveDark = Color(0xFF43444B) // Graphite
val dividerColorLight = Color(0xFFDCD4C3) // Warm Light Gray
val dividerColorDark = Color(0xFF2C2D33) // Graphite

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E) // Dark Gray
val toastNormalBgDark = Color(0xB34A4F54) // Darker Gray
val toastSuccessBg = Color(0xB32E7D5B) // Muted Green
val toastErrorBg = Color(0xB3B3282D) // Brand Red
val toastInfoBg = Color(0xB334373F) // Graphite
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        ),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColor else LightColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()

                AppSnackbarBridge(controller = snackbarController)
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
