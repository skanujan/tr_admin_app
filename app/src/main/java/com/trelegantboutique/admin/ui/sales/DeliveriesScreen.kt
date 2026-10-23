@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

private val FILTERS = listOf("all" to "All", "dispatch" to "To dispatch", "delivered" to "Delivered", "unpaid" to "Unpaid")

@Composable
fun DeliveriesScreen(
    viewModel: JsonLoaderViewModel,
    onBack: () -> Unit,
    onOpenOrder: (Int) -> Unit,
    onOpenCourier: (Int) -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf("all") }
    val listState = rememberLazyListState()
    HandleState(state, snackbar, viewModel, onSessionExpired)

    LaunchedEffect(listState, state.items) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= state.items.size - 3) viewModel.loadMore()
        }
    }

    PageScaffold(title = "Deliveries", onBack = onBack, snackbar = snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((key, label) in FILTERS) {
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key; viewModel.setQuery(mapOf("filter" to key)) },
                        label = { Text(label) }
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                if (state.loading || state.error != null) {
                    LoadingOrError(state.loading, state.error, { viewModel.load() })
                } else if (state.items.isEmpty()) {
                    Text("Nothing here.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items) { d ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(d.str("order_code") + "  " + d.str("customer_name"), fontWeight = FontWeight.Bold)
                                            Text(d.str("phone") + "  " + d.str("city"), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(money(d.dbl("total")), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        d.str("status_label") + "  |  " + d.str("payment_method"),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val track = d.str("tracking_code")
                                    if (track.isNotBlank()) {
                                        Text(
                                            "Courier: " + track + "  " + d.str("courier_status"),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Payment received", modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = d.bool("payment_received"),
                                            onCheckedChange = { on ->
                                                viewModel.post(
                                                    "orders-payment.php",
                                                    jsonOf("id" to d.int("id"), "received" to on)
                                                )
                                            }
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { onOpenOrder(d.int("id")) }) { Text("Order") }
                                        Button(onClick = { onOpenCourier(d.int("id")) }) { Text("Courier") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
