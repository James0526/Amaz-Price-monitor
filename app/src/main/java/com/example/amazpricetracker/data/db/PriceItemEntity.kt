package com.example.amazpricetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_items")
data class PriceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val priceText: String,
    val priceValue: Double?,
    val lastUpdated: Long,
    val notifyOnDrop: Boolean
)
