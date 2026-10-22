package com.trelegantboutique.admin.ui.orders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trelegantboutique.admin.data.OrderSummary
import com.trelegantboutique.admin.ui.theme.DeepGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    viewModel: OrdersListViewModel,
    onOpenOrder: (Int) -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }

    LaunchedEffect(listState, state.orders) {
        snapshotFlow { listState.layoutInfo }
            .collect { info ->
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisible >= state.orders.size - 3) viewModel.loadMore()
            }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Orders") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Search by name, phone, order #") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.search() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ORDER_STATUS_FILTERS.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { viewModel.setStatus(status) },
                        label = { Text(status ?: "All") }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.error!!)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::refresh) { Text("Retry") }
                    }
                    state.orders.isEmpty() -> Text(
                        "No orders found.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.orders, key = { it.id }) { order ->
                            OrderRow(order, onClick = { onOpenOrder(order.id) })
                        }
                        if (state.loadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(order.orderNumber ?: "#${order.id}", fontWeight = FontWeight.Bold)
                Text(order.customerName ?: "", style = MaterialTheme.typography.bodySmall)
                order.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Column(horizontalAlignment = Alignment.End) {
                AssistChip(onClick = {}, label = { Text(order.status) })
                order.total?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("${"%.0f".format(it)}", fontWeight = FontWeight.Bold, color = DeepGreen)
                }
            }
        }
    }
}
