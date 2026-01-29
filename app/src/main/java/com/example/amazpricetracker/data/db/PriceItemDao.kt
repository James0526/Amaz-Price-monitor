package com.example.amazpricetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceItemDao {
    @Query("SELECT * FROM price_items ORDER BY id DESC")
    fun observeAll(): Flow<List<PriceItemEntity>>

    @Query("SELECT * FROM price_items ORDER BY id DESC")
    suspend fun getAll(): List<PriceItemEntity>

    @Query("SELECT COUNT(*) FROM price_items")
    suspend fun countItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PriceItemEntity): Long

    @Update
    suspend fun update(item: PriceItemEntity)

    @Delete
    suspend fun delete(item: PriceItemEntity)
}
