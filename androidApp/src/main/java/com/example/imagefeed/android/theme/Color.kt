package com.example.imagefeed.android.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Surface & Container hierarchy - Dark
val SurfaceDark = Color(0xFF0F0F11)
val SurfaceContainerLowestDark = Color(0xFF0A0A0C)
val SurfaceContainerLowDark = Color(0xFF16161A)
val SurfaceContainerDark = Color(0xFF1E1E24)
val SurfaceContainerHighDark = Color(0xFF25252D)
val SurfaceContainerHighestDark = Color(0xFF2C2C35)
val SurfaceDimDark = Color(0xFF0F0F11)
val SurfaceBrightDark = Color(0xFF383842)
val BackgroundDark = Color(0xFF0F0F11)
val OnSurfaceDark = Color(0xFFE6E1E5)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF2C2C35)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val InverseSurfaceDark = Color(0xFFE6E1E5)
val InverseOnSurfaceDark = Color(0xFF1E1E24)

// Surface & Container hierarchy - Light
val SurfaceLight = Color(0xFFFBFBFE)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF5F5F8)
val SurfaceContainerLight = Color(0xFFEEEEF2)
val SurfaceContainerHighLight = Color(0xFFE8E8EE)
val SurfaceContainerHighestLight = Color(0xFFE2E2E8)
val SurfaceDimLight = Color(0xFFDCDCE0)
val SurfaceBrightLight = Color(0xFFFBFBFE)
val BackgroundLight = Color(0xFFFBFBFE)
val OnSurfaceLight = Color(0xFF1A1C1E)
val OnBackgroundLight = Color(0xFF1A1C1E)
val SurfaceVariantLight = Color(0xFFE2E2EC)
val OnSurfaceVariantLight = Color(0xFF44474E)
val InverseSurfaceLight = Color(0xFF2F3033)
val InverseOnSurfaceLight = Color(0xFFF1F0F4)

// Accent & Brand Colors - Dark
val PrimaryDark = Color(0xFFE2E2EC)
val OnPrimaryDark = Color(0xFF1A1A22)
val PrimaryContainerDark = Color(0xFF2C2C35)
val OnPrimaryContainerDark = Color(0xFFE2E2EC)
val InversePrimaryDark = Color(0xFF43464F)

val SecondaryDark = Color(0xFFADC6FF)
val OnSecondaryDark = Color(0xFF102F60)
val SecondaryContainerDark = Color(0xFF2B4678)
val OnSecondaryContainerDark = Color(0xFFD8E2FF)

val TertiaryDark = Color(0xFFFFB77C)
val OnTertiaryDark = Color(0xFF4D2700)
val TertiaryContainerDark = Color(0xFF6E3900)
val OnTertiaryContainerDark = Color(0xFFFFDCC2)

// Accent & Brand Colors - Light
val PrimaryLight = Color(0xFF1A1A22)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE2E2EC)
val OnPrimaryContainerLight = Color(0xFF1A1A22)
val InversePrimaryLight = Color(0xFFE2E2EC)

val SecondaryLight = Color(0xFF2B4678)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD8E2FF)
val OnSecondaryContainerLight = Color(0xFF001A41)

val TertiaryLight = Color(0xFF8B5000)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDCC2)
val OnTertiaryContainerLight = Color(0xFF2C1500)

// Outlines & Borders
val OutlineDark = Color(0xFF49454F)
val OutlineVariantDark = Color(0xFF33333D)

val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// Error roles
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ScrimColor = Color(0xFF000000)

val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        inversePrimary = InversePrimaryDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        surfaceTint = PrimaryDark,
        inverseSurface = InverseSurfaceDark,
        inverseOnSurface = InverseOnSurfaceDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        scrim = ScrimColor,
        surfaceBright = SurfaceBrightDark,
        surfaceDim = SurfaceDimDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
    )

val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        inversePrimary = InversePrimaryLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        surfaceTint = PrimaryLight,
        inverseSurface = InverseSurfaceLight,
        inverseOnSurface = InverseOnSurfaceLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        scrim = ScrimColor,
        surfaceBright = SurfaceBrightLight,
        surfaceDim = SurfaceDimLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
    )
