package com.example.imagefeed.presentation.search

import com.example.imagefeed.model.AppError
import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface UnifiedSearchEffect : UiEffect {
    data object DismissKeyboard : UnifiedSearchEffect

    data object ScrollToTop : UnifiedSearchEffect

    data class ShowError(
        val error: AppError,
    ) : UnifiedSearchEffect
}
