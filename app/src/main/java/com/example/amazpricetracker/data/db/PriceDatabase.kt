package com.example.amazpricetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PriceItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PriceDatabase : RoomDatabase() {
    abstract fun priceItemDao(): PriceItemDao

    companion object {
        @Volatile
        private var INSTANCE: PriceDatabase? = null

        fun getInstance(context: Context): PriceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PriceDatabase::class.java,
                    "price_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
