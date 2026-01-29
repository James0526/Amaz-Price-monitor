package com.example.amazpricetracker.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface AmazonPriceApi {
    @GET("price")
    suspend fun fetchPrice(@Query("url") url: String): PriceResponse
}
