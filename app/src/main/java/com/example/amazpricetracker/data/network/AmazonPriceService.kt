package com.example.amazpricetracker.data.network

import com.example.amazpricetracker.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class PriceFetchResult {
    data class Success(val snapshot: PriceSnapshot) : PriceFetchResult()
    data class Failure(val message: String) : PriceFetchResult()
}

class AmazonPriceService(private val api: AmazonPriceApi) {
    suspend fun fetchPrice(url: String, fallbackTitle: String): PriceFetchResult {
        return try {
            val response = api.fetchPrice(url)
            val title = PriceParser.normalizeTitle(response.title, fallbackTitle)
            val priceText = response.price?.trim().orEmpty().ifBlank { "Unavailable" }
            val priceValue = PriceParser.parsePriceValue(response.price)
            PriceFetchResult.Success(PriceSnapshot(title, priceText, priceValue))
        } catch (e: Exception) {
            PriceFetchResult.Failure(e.message ?: "Failed to fetch price")
        }
    }

    companion object {
        fun create(): AmazonPriceService {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val builder = chain.request().newBuilder()
                    val apiKey = BuildConfig.AMAZON_API_KEY
                    if (apiKey.isNotBlank()) {
                        builder.addHeader("x-api-key", apiKey)
                    }
                    chain.proceed(builder.build())
                }
                .addInterceptor(logger)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(
                    BuildConfig.AMAZON_API_BASE_URL.let { base ->
                        if (base.endsWith("/")) base else "$base/"
                    }
                )
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return AmazonPriceService(retrofit.create(AmazonPriceApi::class.java))
        }
    }
}
