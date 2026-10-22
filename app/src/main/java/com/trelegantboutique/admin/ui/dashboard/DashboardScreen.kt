package com.trelegantboutique.admin.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trelegantboutique.admin.ui.theme.DeepGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                state.data != null -> DashboardContent(state.data!!)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun DashboardContent(data: com.trelegantboutique.admin.data.DashboardResponse) {
    val currency = data.currency ?: "Rs."
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hello, ${data.adminName ?: "Admin"}", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Today's orders",
                    value = (data.todayOrders ?: 0).toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Today's revenue",
                    value = "$currency ${"%.0f".format(data.todayRevenue ?: 0.0)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Completed today",
                    value = (data.completedToday ?: 0).toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending orders",
                    value = (data.pendingOrders ?: 0).toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!data.statusCounts.isNullOrEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Orders by status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        data.statusCounts.forEach { (status, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(status)
                                Text(count.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (!data.lowStock.isNullOrEmpty()) {
            item {
                Text("Low stock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(data.lowStock) { product ->
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = product.image,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.name, maxLines = 1)
                        }
                        Text(
                            "${product.stock} left",
                            color = if (product.stock <= 0) MaterialTheme.colorScheme.error else DeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
