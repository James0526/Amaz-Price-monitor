package com.example.amazpricetracker.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PriceWorkScheduler {
    private const val UNIQUE_WORK_NAME = "price_drop_worker"

    fun updateWork(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<PriceDropWorker>(
                6,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            manager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            manager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
