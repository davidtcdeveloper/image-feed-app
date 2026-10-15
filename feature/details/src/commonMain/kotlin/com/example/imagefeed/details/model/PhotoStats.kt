package com.example.imagefeed.details.model

import kotlinx.serialization.Serializable

typealias StatsValue = com.example.imagefeed.model.StatsValue
typealias HistoricalStats = com.example.imagefeed.model.HistoricalStats
typealias StatMetric = com.example.imagefeed.model.StatMetric

@Serializable
data class PhotoStats(
    val id: String,
    val downloads: StatMetric,
    val views: StatMetric,
    val likes: StatMetric? = null,
)
