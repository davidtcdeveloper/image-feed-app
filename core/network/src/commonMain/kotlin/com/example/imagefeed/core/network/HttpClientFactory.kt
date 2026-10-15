package com.example.imagefeed.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Factory to create HttpClient configured for Unsplash API
fun createUnsplashHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode >= 300) {
                    val errorText = response.bodyAsText()
                    // Try to parse the error message if it's JSON error format
                    val errorMessage =
                        try {
                            val element = Json.parseToJsonElement(errorText)
                            val errors = element.jsonObject["errors"]?.jsonArray
                            errors?.map { it.jsonPrimitive.content }?.joinToString(", ")
                                ?: errorText
                        } catch (e: Exception) {
                            errorText
                        }
                    throw Exception("Unsplash API Error (Status $statusCode): $errorMessage")
                }
            }
        }
        defaultRequest {
            url("https://api.unsplash.com/")
            header("Accept-Version", "v1")
            header("Authorization", "Client-ID ${BuildKonfig.UNSPLASH_API_KEY}")
        }
    }
