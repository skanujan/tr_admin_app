@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.catalog

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
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

@Composable
fun StockReportScreen(viewModel: JsonLoaderViewModel, onBack: (() -> Unit)?, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var q by remember { mutableStateOf("") }
    var inStockOnly by remember { mutableStateOf(false) }
    HandleState(state, snackbar, viewModel, onSessionExpired)

    fun run() {
        viewModel.setQuery(mapOf("q" to q, "instock" to if (inStockOnly) "1" else "0"))
    }

    PageScaffold(title = "Stock report", onBack = onBack, snackbar = snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                label = { Text("Search name, colour or SKU") },
                singleLine = true,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { run() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = inStockOnly, onCheckedChange = { inStockOnly = it; run() })
                Text("In stock only", modifier = Modifier.weight(1f))
                val total = state.data?.int("total_units") ?: 0
                Text("$total units", fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f)) {
                if (state.loading || state.error != null) {
                    LoadingOrError(state.loading, state.error, { viewModel.load() })
                } else if (state.data != null) {
                    val rows = state.data!!.list("items")
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rows) { r ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = r.str("image"), contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(r.str("name"), fontWeight = FontWeight.Bold, maxLines = 2)
                                        val colour = r.str("color")
                                        if (colour.isNotBlank()) Text(colour, style = MaterialTheme.typography.bodySmall)
                                        val sku = r.str("sku")
                                        if (sku.isNotBlank()) Text(sku, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        r.int("stock").toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.int("stock") <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
