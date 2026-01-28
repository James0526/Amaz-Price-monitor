package com.example.amazpricetracker.di

import android.content.Context
import com.example.amazpricetracker.data.db.PriceDatabase
import com.example.amazpricetracker.data.network.AmazonPriceService
import com.example.amazpricetracker.data.repo.PriceRepository

object ServiceLocator {
    fun provideRepository(context: Context): PriceRepository {
        val database = PriceDatabase.getInstance(context)
        val service = AmazonPriceService.create()
        return PriceRepository(database.priceItemDao(), service)
    }
}
