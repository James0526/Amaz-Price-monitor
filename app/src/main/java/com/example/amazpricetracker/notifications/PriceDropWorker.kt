package com.example.amazpricetracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.amazpricetracker.di.ServiceLocator

class PriceDropWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val repository = ServiceLocator.provideRepository(applicationContext)
            val outcome = repository.refreshAll()
            outcome.dropEvents.forEach { event ->
                NotificationHelper.showPriceDrop(applicationContext, event)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
