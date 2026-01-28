package com.example.amazpricetracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.amazpricetracker.data.model.PriceItem
import com.example.amazpricetracker.data.repo.AddItemResult
import com.example.amazpricetracker.data.repo.PriceRepository
import com.example.amazpricetracker.di.ServiceLocator
import com.example.amazpricetracker.notifications.PriceWorkScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val items: List<PriceItem>,
    val isRefreshing: Boolean
)

class PriceTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PriceRepository = ServiceLocator.provideRepository(application)

    private val refreshing = MutableStateFlow(false)
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val uiState: StateFlow<UiState> = combine(
        repository.itemsFlow,
        refreshing
    ) { items, isRefreshing ->
        UiState(items = items, isRefreshing = isRefreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState(emptyList(), false))

    val messageFlow = messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.itemsFlow.collect { items ->
                PriceWorkScheduler.updateWork(
                    getApplication(),
                    items.any { it.notifyOnDrop }
                )
            }
        }
    }

    fun addItem(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            messages.tryEmit("Please enter an Amazon link.")
            return
        }
        if (!isAmazonUrl(trimmed)) {
            messages.tryEmit("Please enter a valid Amazon product link.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.addItem(trimmed)) {
                is AddItemResult.Success -> {
                    messages.tryEmit("Item added.")
                }
                is AddItemResult.Failure -> {
                    messages.tryEmit(result.message)
                }
            }
        }
    }

    fun refreshAll() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val outcome = repository.refreshAll()
            outcome.dropEvents.forEach { event ->
                com.example.amazpricetracker.notifications.NotificationHelper.showPriceDrop(
                    getApplication(),
                    event
                )
            }
            if (outcome.failed > 0) {
                messages.tryEmit("Updated ${outcome.updated} items, ${outcome.failed} failed.")
            } else if (outcome.updated > 0) {
                messages.tryEmit("Updated ${outcome.updated} items.")
            }
            refreshing.value = false
        }
    }

    fun updateNotify(item: PriceItem, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotify(item, enabled)
        }
    }

    fun deleteItem(item: PriceItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun showMessage(message: String) {
        messages.tryEmit(message)
    }

    private fun isAmazonUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host ?: return false
        return host.contains("amazon.")
    }
}
