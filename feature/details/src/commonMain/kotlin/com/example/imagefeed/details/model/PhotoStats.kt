package com.example.imagefeed.details.model

import kotlinx.serialization.Serializable

@Serializable
data class StatsValue(
    val date: String,
    val value: Int,
)

@Serializable
data class HistoricalStats(
    val change: Int? = null,
    val average: Int? = null,
    val resolution: String? = null,
    val quantity: Int? = null,
    val values: List<StatsValue> = emptyList(),
)

@Serializable
data class StatMetric(
    val total: Int,
    val historical: HistoricalStats? = null,
)

@Serializable
data class PhotoStats(
    val id: String,
    val downloads: StatMetric,
    val views: StatMetric,
    val likes: StatMetric? = null,
)
