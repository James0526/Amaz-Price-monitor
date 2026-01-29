package com.example.amazpricetracker.data.model

data class PriceItem(
    val id: Long,
    val url: String,
    val title: String,
    val priceText: String,
    val priceValue: Double?,
    val lastUpdated: Long,
    val notifyOnDrop: Boolean
)
