package com.example.imagefeed.android.theme

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@SuppressLint("NewApi")
fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    context: Context? = null,
    sdkInt: Int = Build.VERSION.SDK_INT,
): ColorScheme =
    when {
        dynamicColor && sdkInt >= Build.VERSION_CODES.S && context != null -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocalContext.current else null
    val colorScheme =
        resolveColorScheme(
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
            context = context,
        )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
