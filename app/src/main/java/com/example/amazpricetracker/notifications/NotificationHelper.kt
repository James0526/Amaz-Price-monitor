package com.example.amazpricetracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.amazpricetracker.R
import com.example.amazpricetracker.data.model.PriceDropEvent

object NotificationHelper {
    private const val CHANNEL_ID = "price_drop_channel"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Price Drop Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when an Amazon item price drops."
        }
        manager.createNotificationChannel(channel)
    }

    fun showPriceDrop(context: Context, event: PriceDropEvent) {
        if (!areNotificationsAllowed(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Price drop: ${event.title}")
            .setContentText("${event.previousPrice} → ${event.newPrice}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Price dropped from ${event.previousPrice} to ${event.newPrice}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(event.itemId.toInt(), notification)
    }

    private fun areNotificationsAllowed(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return permission == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
    }
}
