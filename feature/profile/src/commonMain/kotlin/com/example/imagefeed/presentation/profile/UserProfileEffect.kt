package com.example.imagefeed.presentation.profile

import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface UserProfileEffect : UiEffect {
    data object NavigateBack : UserProfileEffect

    data class ShowError(
        val message: String,
    ) : UserProfileEffect
}
