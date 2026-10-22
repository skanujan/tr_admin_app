package com.trelegantboutique.admin.ui.orders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(viewModel: OrderDetailViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showReturnDialog by remember { mutableStateOf(false) }
    var pendingStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.order?.orderNumber ?: "Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::load) { Text("Retry") }
                }
                state.order != null -> {
                    val order = state.order!!
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card {
                                Column(Modifier.padding(16.dp)) {
                                    Text(order.customerName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    order.phone?.let { Text(it) }
                                    order.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    order.address?.let {
                                        Spacer(Modifier.height(8.dp))
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Status: ${order.status}", fontWeight = FontWeight.Bold)
                                        order.total?.let { Text("%.2f".format(it), fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                        item {
                            Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        items(state.items, key = { it.id }) { item ->
                            Card {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = item.image, contentDescription = null, modifier = Modifier.size(44.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name ?: "", maxLines = 2)
                                        Text("Qty: ${item.qty ?: 0}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    item.price?.let { Text("%.2f".format(it)) }
                                }
                            }
                        }
                        item {
                            Text("Change status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ORDER_STATUS_OPTIONS.forEach { option ->
                                    FilterChip(
                                        selected = order.status == option,
                                        enabled = !state.updating,
                                        onClick = {
                                            if (option == "Returned") {
                                                pendingStatus = option
                                                showReturnDialog = true
                                            } else {
                                                viewModel.updateStatus(option)
                                            }
                                        },
                                        label = { Text(option) }
                                    )
                                }
                            }
                        }
                        if (state.updateError != null) {
                            item { Text(state.updateError!!, color = MaterialTheme.colorScheme.error) }
                        }
                        if (state.updating) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReturnDialog) {
        var shippingCostText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Mark as returned") },
            text = {
                Column {
                    Text("Enter the return shipping cost, if any (leave blank for 0).")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shippingCostText,
                        onValueChange = { shippingCostText = it },
                        label = { Text("Return shipping cost") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cost = shippingCostText.toDoubleOrNull()
                    viewModel.updateStatus("Returned", cost)
                    showReturnDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            }
        )
    }
}
