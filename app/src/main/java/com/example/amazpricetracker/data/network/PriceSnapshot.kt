package com.example.amazpricetracker.data.network

data class PriceSnapshot(
    val title: String,
    val priceText: String,
    val priceValue: Double?
)
