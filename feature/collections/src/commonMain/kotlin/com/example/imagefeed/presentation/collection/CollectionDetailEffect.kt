package com.example.imagefeed.presentation.collection

import com.example.imagefeed.model.AppError
import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface CollectionDetailEffect : UiEffect {
    data object NavigateBack : CollectionDetailEffect

    data class ShowError(
        val error: AppError,
    ) : CollectionDetailEffect
}
