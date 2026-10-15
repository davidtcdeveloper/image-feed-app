package com.example.imagefeed.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val username: String,
    val downloads: StatMetric,
    val views: StatMetric,
)
