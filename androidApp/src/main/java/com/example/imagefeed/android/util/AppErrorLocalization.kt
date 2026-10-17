package com.example.imagefeed.android.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.imagefeed.android.R
import com.example.imagefeed.model.AppError

@Composable
fun AppError.localizedMessage(): String =
    when (this) {
        is AppError.Network -> stringResource(R.string.error_network)
        is AppError.Timeout -> stringResource(R.string.error_timeout)
        is AppError.NotFound -> stringResource(R.string.error_not_found)
        is AppError.Server -> stringResource(R.string.error_server, code)
        is AppError.Unknown -> rawMessage?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown)
    }

fun AppError.localizedMessage(context: Context): String =
    when (this) {
        is AppError.Network -> context.getString(R.string.error_network)
        is AppError.Timeout -> context.getString(R.string.error_timeout)
        is AppError.NotFound -> context.getString(R.string.error_not_found)
        is AppError.Server -> context.getString(R.string.error_server, code)
        is AppError.Unknown -> rawMessage?.takeIf { it.isNotBlank() } ?: context.getString(R.string.error_unknown)
    }
