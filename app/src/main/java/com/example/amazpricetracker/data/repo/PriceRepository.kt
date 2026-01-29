package com.example.amazpricetracker.data.repo

import com.example.amazpricetracker.data.db.PriceItemDao
import com.example.amazpricetracker.data.db.PriceItemEntity
import com.example.amazpricetracker.data.model.PriceDropEvent
import com.example.amazpricetracker.data.model.PriceItem
import com.example.amazpricetracker.data.network.AmazonPriceService
import com.example.amazpricetracker.data.network.PriceFetchResult
import com.example.amazpricetracker.data.network.PriceParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class AddItemResult {
    data class Success(val itemId: Long) : AddItemResult()
    data class Failure(val message: String) : AddItemResult()
}

data class RefreshOutcome(
    val updated: Int,
    val failed: Int,
    val dropEvents: List<PriceDropEvent>
)

class PriceRepository(
    private val dao: PriceItemDao,
    private val priceService: AmazonPriceService
) {
    val itemsFlow: Flow<List<PriceItem>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addItem(url: String): AddItemResult {
        val count = dao.countItems()
        if (count >= MAX_ITEMS) {
            return AddItemResult.Failure("Max 12 items reached.")
        }

        val fallbackTitle = PriceParser.fallbackTitleFromUrl(url)
        val fetchResult = priceService.fetchPrice(url, fallbackTitle)
        val now = System.currentTimeMillis()

        val entity = when (fetchResult) {
            is PriceFetchResult.Success -> {
                PriceItemEntity(
                    url = url,
                    title = fetchResult.snapshot.title,
                    priceText = fetchResult.snapshot.priceText,
                    priceValue = fetchResult.snapshot.priceValue,
                    lastUpdated = now,
                    notifyOnDrop = false
                )
            }
            is PriceFetchResult.Failure -> {
                PriceItemEntity(
                    url = url,
                    title = fallbackTitle,
                    priceText = "Unavailable",
                    priceValue = null,
                    lastUpdated = now,
                    notifyOnDrop = false
                )
            }
        }

        val id = dao.insert(entity)
        return AddItemResult.Success(id)
    }

    suspend fun refreshAll(): RefreshOutcome {
        val items = dao.getAll()
        var updated = 0
        var failed = 0
        val dropEvents = mutableListOf<PriceDropEvent>()

        for (item in items) {
            val fallbackTitle = PriceParser.fallbackTitleFromUrl(item.url)
            when (val result = priceService.fetchPrice(item.url, fallbackTitle)) {
                is PriceFetchResult.Success -> {
                    val now = System.currentTimeMillis()
                    val oldPriceValue = item.priceValue
                    val newPriceValue = result.snapshot.priceValue
                    val updatedEntity = item.copy(
                        title = result.snapshot.title,
                        priceText = result.snapshot.priceText,
                        priceValue = newPriceValue,
                        lastUpdated = now
                    )
                    dao.update(updatedEntity)
                    updated++
                    if (item.notifyOnDrop && oldPriceValue != null && newPriceValue != null) {
                        if (newPriceValue < oldPriceValue) {
                            dropEvents.add(
                                PriceDropEvent(
                                    itemId = item.id,
                                    title = updatedEntity.title,
                                    previousPrice = item.priceText,
                                    newPrice = updatedEntity.priceText
                                )
                            )
                        }
                    }
                }
                is PriceFetchResult.Failure -> {
                    failed++
                }
            }
        }

        return RefreshOutcome(updated, failed, dropEvents)
    }

    suspend fun updateNotify(item: PriceItem, enabled: Boolean) {
        dao.update(
            PriceItemEntity(
                id = item.id,
                url = item.url,
                title = item.title,
                priceText = item.priceText,
                priceValue = item.priceValue,
                lastUpdated = item.lastUpdated,
                notifyOnDrop = enabled
            )
        )
    }

    suspend fun deleteItem(item: PriceItem) {
        dao.delete(
            PriceItemEntity(
                id = item.id,
                url = item.url,
                title = item.title,
                priceText = item.priceText,
                priceValue = item.priceValue,
                lastUpdated = item.lastUpdated,
                notifyOnDrop = item.notifyOnDrop
            )
        )
    }

    companion object {
        const val MAX_ITEMS = 12
    }
}

private fun PriceItemEntity.toDomain(): PriceItem {
    return PriceItem(
        id = id,
        url = url,
        title = title,
        priceText = priceText,
        priceValue = priceValue,
        lastUpdated = lastUpdated,
        notifyOnDrop = notifyOnDrop
    )
}
