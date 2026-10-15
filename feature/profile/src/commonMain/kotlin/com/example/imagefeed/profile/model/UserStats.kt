package com.example.imagefeed.profile.model

import com.example.imagefeed.model.StatMetric
import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val username: String,
    val downloads: StatMetric,
    val views: StatMetric,
)
