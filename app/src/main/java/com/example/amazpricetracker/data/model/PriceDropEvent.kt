package com.example.amazpricetracker.data.model

data class PriceDropEvent(
    val itemId: Long,
    val title: String,
    val previousPrice: String,
    val newPrice: String
)
