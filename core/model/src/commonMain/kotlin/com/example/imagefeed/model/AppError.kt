package com.example.imagefeed.model

sealed class AppError {
    data object Network : AppError()

    data object Timeout : AppError()

    data object NotFound : AppError()

    data class Server(
        val code: Int,
    ) : AppError()

    data class Unknown(
        val rawMessage: String? = null,
    ) : AppError()
}
