@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

private val PRE_STATUSES = listOf("Pending", "Confirmed", "Shipped", "Delivered", "Cancelled")

@Composable
fun PreordersScreen(
    viewModel: JsonLoaderViewModel,
    pickerViewModel: JsonLoaderViewModel,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var pickProduct by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf<JsonObject?>(null) }
    var statusFor by remember { mutableStateOf<JsonObject?>(null) }
    val listState = rememberLazyListState()
    HandleState(state, snackbar, viewModel, onSessionExpired)

    LaunchedEffect(listState, state.items) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= state.items.size - 3) viewModel.loadMore()
        }
    }

    PageScaffold(
        title = "Pre-orders",
        onBack = onBack,
        snackbar = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, contentDescription = "Add pre-order") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (s in listOf("") + PRE_STATUSES) {
                    FilterChip(
                        selected = filter == s,
                        onClick = { filter = s; viewModel.setQuery(if (s.isEmpty()) emptyMap() else mapOf("status" to s)) },
                        label = { Text(if (s.isEmpty()) "All" else s) }
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                if (state.loading || state.error != null) {
                    LoadingOrError(state.loading, state.error, { viewModel.load() })
                } else if (state.items.isEmpty()) {
                    Text("No pre-orders.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items) { p ->
                            Card(onClick = { statusFor = p }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row {
                                        Text(p.str("code"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(p.str("status"))
                                    }
                                    var line = p.str("product_name")
                                    if (p.str("color_name").isNotBlank()) line += " / " + p.str("color_name")
                                    if (p.str("size_name").isNotBlank()) line += " / " + p.str("size_name")
                                    Text(line + "  x" + p.int("quantity"))
                                    Text(p.str("customer_name") + "  " + p.str("phone"), style = MaterialTheme.typography.bodySmall)
                                    val note = p.str("note")
                                    if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val sf = statusFor
    if (sf != null) {
        AlertDialog(
            onDismissRequest = { statusFor = null },
            title = { Text("Set status for " + sf.str("code")) },
            text = {
                Column {
                    for (s in PRE_STATUSES) {
                        TextButton(onClick = {
                            viewModel.post("preorders.php", jsonOf("action" to "status", "id" to sf.int("id"), "status" to s))
                            statusFor = null
                        }) { Text(s) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { statusFor = null }) { Text("Close") } }
        )
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New pre-order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickProduct = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(product?.str("name") ?: "Choose product")
                    }
                    LabeledField("Customer name", name, { name = it })
                    LabeledField("Phone", phone, { phone = it }, phone = true)
                    LabeledField("Quantity", qty, { qty = it }, number = true)
                    LabeledField("Note (optional)", note, { note = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pr = product
                    if (pr == null) {
                        viewModel.showMessage("Choose a product first.")
                    } else {
                        viewModel.post(
                            "preorders.php",
                            jsonOf(
                                "action" to "add",
                                "product_id" to pr.int("pid"),
                                "customer_name" to name,
                                "phone" to phone,
                                "quantity" to (qty.toIntOrNull() ?: 1),
                                "note" to note
                            )
                        )
                        adding = false
                        product = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }
        )
    }
    if (pickProduct) {
        ProductPickerDialog(
            viewModel = pickerViewModel,
            requireVariant = false,
            onPicked = { p, _, _ -> product = p; pickProduct = false },
            onDismiss = { pickProduct = false }
        )
    }
}
