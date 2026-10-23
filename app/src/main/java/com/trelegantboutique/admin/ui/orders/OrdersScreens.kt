@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.obj
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*
import com.trelegantboutique.admin.ui.theme.*

private val FILTERS = listOf("" to "All", "Pending" to "Pending", "Confirmed" to "Confirmed", "Shipped" to "Dispatched",
    "Delivered" to "Delivered", "Completed" to "Completed", "Returned" to "Returned", "Cancelled" to "Cancelled")
val ORDER_STATUSES = listOf("Pending", "Confirmed", "Shipped", "Delivered", "Returned", "Cancelled")

private fun isPaid(s: String) = s == "paid" || s == "completed" || s == "success"

/** Orders list like admin > Orders: status filter, search, coloured status. */
@Composable
fun OrdersListScreen(
    viewModel: JsonLoaderViewModel,
    initialStatus: String,
    onOpenOrder: (Int) -> Unit,
    onNewSale: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var status by remember { mutableStateOf(initialStatus) }
    var q by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    HandleState(state, snackbar, viewModel, onSessionExpired)

    fun run() {
        val m = HashMap<String, String>()
        if (status.isNotEmpty()) m["status"] = status
        if (q.isNotBlank()) m["q"] = q
        viewModel.setQuery(m)
    }

    LaunchedEffect(listState, state.items) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= state.items.size - 3) viewModel.loadMore()
        }
    }

    PageScaffold(
        title = "Orders",
        onBack = null,
        snackbar = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewSale,
                containerColor = Gold,
                contentColor = MaroonDeep,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New sale") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchBox(q, { q = it }, "Search name, phone, order no, city", { run() })
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((key, label) in FILTERS) {
                    FilterChip(
                        selected = status == key,
                        onClick = { status = key; run() },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonDeep,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                if (state.loading || state.error != null) {
                    LoadingOrError(state.loading, state.error, { viewModel.load() })
                } else if (state.items.isEmpty()) {
                    Text("No orders found.", color = Muted, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items) { o ->
                            val (bg, fg) = statusColors(o.str("status"))
                            AdminCard(onClick = { onOpenOrder(o.int("id")) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(o.str("order_code"), color = DeepGreen, fontWeight = FontWeight.Bold)
                                        Text(o.str("customer_name"))
                                        Text(o.str("phone") + "  |  " + o.str("created_at").take(16), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(money(o.dbl("total")), fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        if (o.bool("is_completed")) Pill("Completed", Color(0xFFC8F2D3), Color(0xFF0F5A32))
                                        else Pill(o.str("status_label").ifBlank { statusLabel(o.str("status")) }, bg, fg)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(o.str("payment_method"), style = MaterialTheme.typography.bodySmall)
                                    if (isPaid(o.str("payment_status"))) Pill("Paid", Color(0xFFC8F2D3), Color(0xFF13703A))
                                    if (o.str("tracking_code").isNotBlank()) Pill("Domex " + o.str("tracking_code"), Color(0xFFE3E0FF), Color(0xFF5B46C9))
                                }
                            }
                        }
                        if (state.loadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), color = DeepGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One order: customer, items, money, payment, status, courier - like admin > order page. */
@Composable
fun OrderDetailScreen(
    viewModel: JsonLoaderViewModel,
    orderId: Int,
    onBack: () -> Unit,
    onOpenCourier: (Int) -> Unit,
    onOpenWeb: (String, String) -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var askReturnCost by remember { mutableStateOf(false) }
    var returnCost by remember { mutableStateOf("") }
    HandleState(state, snackbar, viewModel, onSessionExpired)
    val o = state.data?.obj("order")

    fun setStatus(s: String, cost: String?) {
        viewModel.post(
            "orders-update-status.php",
            jsonOf("id" to orderId, "status" to s, "return_shipping_cost" to cost)
        )
    }
    fun open(uri: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) } catch (e: Exception) { }
    }

    PageScaffold(title = o?.str("order_code") ?: "Order", onBack = onBack, snackbar = snackbar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (o == null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                val status = o.str("status")
                val (bg, fg) = statusColors(status)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.str("order_code"), style = CardTitle)
                                Text(o.str("created_at").take(16) + "  |  " + o.str("source"), style = MaterialTheme.typography.bodySmall)
                            }
                            if (o.bool("is_completed")) Pill("Completed", Color(0xFFC8F2D3), Color(0xFF0F5A32))
                            else Pill(o.str("status_label").ifBlank { statusLabel(status) }, bg, fg)
                        }
                    }

                    AdminCard(title = "Customer") {
                        Text(o.str("customer_name"), fontWeight = FontWeight.SemiBold)
                        val phone = o.str("phone")
                        if (phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(phone, modifier = Modifier.weight(1f))
                                IconButton(onClick = { open("tel:$phone") }) { Icon(Icons.Filled.Call, contentDescription = "Call", tint = DeepGreen) }
                                IconButton(onClick = { open("https://wa.me/" + phone.filter { it.isDigit() }) }) {
                                    Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", tint = Color(0xFF128C7E))
                                }
                            }
                        }
                        val p2 = o.str("phone2")
                        if (p2.isNotBlank()) Text(p2)
                        if (o.str("email").isNotBlank()) Text(o.str("email"), style = MaterialTheme.typography.bodySmall)
                        val addr = listOf(o.str("address"), o.str("city")).filter { it.isNotBlank() }.joinToString(", ")
                        if (addr.isNotBlank()) Text(addr, style = MaterialTheme.typography.bodyMedium)
                        if (o.str("note").isNotBlank()) Text("Note: " + o.str("note"), style = MaterialTheme.typography.bodySmall)
                    }

                    AdminCard(title = "Items") {
                        for ((i, it) in o.list("items").withIndex()) {
                            if (i > 0) HorizontalDivider(color = Line)
                            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(it.str("product_name"), fontWeight = FontWeight.SemiBold)
                                    val v = listOf(it.str("color_name"), it.str("size_name")).filter { s -> s.isNotBlank() }.joinToString(" / ")
                                    if (v.isNotBlank()) Text(v, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "x" + it.int("quantity") + (if (it.bool("is_free")) "  (free)" else "  at " + money(it.dbl("unit_price"))),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(money(it.dbl("line_total")), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = Line)
                        Spacer(Modifier.height(6.dp))
                        MoneyRow("Subtotal", o.dbl("subtotal"))
                        MoneyRow("Delivery", o.dbl("shipping"))
                        if (o.dbl("discount") > 0) MoneyRow("Discount", -o.dbl("discount"))
                        MoneyRow("Total", o.dbl("total"), bold = true)
                    }

                    AdminCard(title = "Payment") {
                        Text(o.str("payment_method"))
                        if (o.str("cash_given").isNotBlank()) {
                            Text("Cash given " + money(o.dbl("cash_given")) + "  |  Change " + money(o.dbl("cash_change")), style = MaterialTheme.typography.bodySmall)
                        }
                        SwitchRow("Payment received", isPaid(o.str("payment_status"))) { on ->
                            viewModel.post("orders-payment.php", jsonOf("id" to orderId, "received" to on))
                        }
                    }

                    AdminCard(title = "Order status") {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (s in ORDER_STATUSES) {
                                val (sb, sf) = statusColors(s)
                                FilterChip(
                                    selected = status == s,
                                    enabled = !state.busy,
                                    onClick = {
                                        if (s != status) {
                                            if (s == "Returned") askReturnCost = true else setStatus(s, null)
                                        }
                                    },
                                    label = { Text(statusLabel(s)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = sb,
                                        selectedLabelColor = sf,
                                        containerColor = Color.White
                                    )
                                )
                            }
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp), color = Gold)
                        Text(
                            "Dispatched and Delivered send the customer their WhatsApp message, same as the website.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    AdminCard(title = "Courier") {
                        val tc = o.str("tracking_code")
                        if (tc.isNotBlank()) {
                            Text("Tracking: $tc")
                            if (o.str("courier_status").isNotBlank()) Text("Status: " + o.str("courier_status"), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Not booked yet.", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { onOpenCourier(orderId) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp)) {
                            Text(if (tc.isBlank()) "Book with Domex" else "Track / update booking")
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenWeb("admin/order-view.php?id=$orderId", o.str("order_code")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) { Text("Full order page (edit, invoice, labels, history)") }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (askReturnCost) {
        AlertDialog(
            onDismissRequest = { askReturnCost = false },
            title = { Text("Mark as Returned", fontFamily = Marcellus) },
            text = {
                Column {
                    Text("Stock goes back automatically. Return courier cost (optional):")
                    Spacer(Modifier.height(8.dp))
                    LabeledField("Return shipping cost", returnCost, { returnCost = it }, number = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    askReturnCost = false
                    setStatus("Returned", returnCost.ifBlank { "0" })
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { askReturnCost = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MoneyRow(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(money(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) DeepGreen else Ink)
    }
}
