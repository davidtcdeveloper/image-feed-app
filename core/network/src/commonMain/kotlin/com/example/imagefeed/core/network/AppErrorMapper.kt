package com.example.imagefeed.core.network

import com.example.imagefeed.model.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException

fun Throwable.toAppError(): AppError =
    when (this) {
        is SocketTimeoutException,
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        -> AppError.Timeout
        is ClientRequestException -> {
            if (response.status.value == 404) AppError.NotFound else AppError.Server(response.status.value)
        }
        is ServerResponseException -> AppError.Server(response.status.value)
        else -> {
            val messageText = message
            val status = messageText?.substringAfter("Unsplash API Error (Status ")?.substringBefore(")")?.toIntOrNull()
            if (status == 404) {
                AppError.NotFound
            } else if (status != null) {
                AppError.Server(status)
            } else {
                AppError.Network
            }
        }
    }
