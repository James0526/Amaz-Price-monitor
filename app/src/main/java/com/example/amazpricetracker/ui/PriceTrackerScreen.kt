package com.example.amazpricetracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amazpricetracker.data.model.PriceItem
import com.example.amazpricetracker.data.repo.PriceRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTrackerScreen(viewModel: PriceTrackerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var urlInput by rememberSaveable { mutableStateOf("") }
    var pendingNotifyItemId by rememberSaveable { mutableStateOf<Long?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val itemId = pendingNotifyItemId
        val item = if (itemId != null) uiState.items.find { it.id == itemId } else null
        if (granted && item != null) {
            viewModel.updateNotify(item, true)
        } else if (!granted) {
            viewModel.showMessage("Notifications permission denied.")
        }
        pendingNotifyItemId = null
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    LaunchedEffect(viewModel.messageFlow) {
        viewModel.messageFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Amaz Price Monitor") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Last updated: ${formatTimestamp(uiState.items.maxOfOrNull { it.lastUpdated } ?: 0L)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Amazon link") },
                    placeholder = { Text("https://www.amazon.com/...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.addItem(urlInput)
                        urlInput = ""
                    },
                    enabled = urlInput.isNotBlank() && uiState.items.size < PriceRepository.MAX_ITEMS
                ) {
                    Text("Add")
                }
            }
            Text(
                text = "Items: ${uiState.items.size} / ${PriceRepository.MAX_ITEMS}",
                style = MaterialTheme.typography.labelMedium
            )
            HorizontalDivider()
            if (uiState.items.isEmpty()) {
                Text(
                    text = "No items yet. Add up to 12 Amazon links.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        PriceItemCard(
                            item = item,
                            onDelete = { viewModel.deleteItem(item) },
                            onToggleNotify = { enabled ->
                                if (!enabled) {
                                    viewModel.updateNotify(item, false)
                                } else {
                                    if (!areNotificationsEnabled(context)) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Enable notifications in system settings.")
                                        }
                                        return@PriceItemCard
                                    }
                                    if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context)) {
                                        pendingNotifyItemId = item.id
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.updateNotify(item, true)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceItemCard(
    item: PriceItem,
    onDelete: () -> Unit,
    onToggleNotify: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { "Amazon Item" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.priceText.ifBlank { "Unavailable" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete item")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.notifyOnDrop) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = "Alerts"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Price drop alert")
                }
                Switch(
                    checked = item.notifyOnDrop,
                    onCheckedChange = onToggleNotify
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun areNotificationsEnabled(context: android.content.Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}
