@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*
import com.trelegantboutique.admin.ui.theme.*

/** The website Dashboard: date range, four totals, order status tiles, recent orders, low stock. */
@Composable
fun DashboardScreen(
    viewModel: JsonLoaderViewModel,
    onNewSale: () -> Unit,
    onOrders: (String?) -> Unit,
    onProducts: () -> Unit,
    onOpenOrder: (Int) -> Unit,
    onInboxCount: (Int) -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    HandleState(state, snackbar, viewModel, onSessionExpired)
    val d = state.data
    LaunchedEffect(d) { if (d != null) onInboxCount(d.int("wa_needs_reply")) }

    PageScaffold(
        title = "Dashboard",
        onBack = null,
        snackbar = snackbar,
        actions = { IconButton(onClick = { viewModel.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") } }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (d == null || (state.loading && d == null)) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                val cur = d.str("currency").ifBlank { "Rs." }
                fun m(v: Double) = cur + " " + money(v)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Welcome, " + d.str("admin_name"), style = MaterialTheme.typography.bodyMedium, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNewSale,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = MaroonDeep),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("+ New sale", maxLines = 1) }
                        Button(
                            onClick = { onOrders(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC26A12)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Orders", maxLines = 1) }
                        Button(
                            onClick = onProducts,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Products", maxLines = 1) }
                    }

                    // Date range, same presets as the website.
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in d.list("ranges")) {
                            val key = r.str("key")
                            FilterChip(
                                selected = d.str("range") == key,
                                onClick = { viewModel.setQuery(mapOf("range" to key)) },
                                label = { Text(r.str("label")) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)

                    if (d.has("revenue")) {
                        val profit = d.dbl("profit")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(Modifier.weight(1f), Color(0xFFEAF1FF), Color(0xFF3B6FD6), Color(0xFF2952A3), Icons.Filled.AttachMoney,
                                m(d.dbl("revenue")), "Revenue in range")
                            StatCard(
                                Modifier.weight(1f),
                                if (profit < 0) Color(0xFFFDEEF0) else Color(0xFFEAFAF0),
                                if (profit < 0) Color(0xFFB02A3E) else Color(0xFF1F8A4C),
                                if (profit < 0) Color(0xFFB02A3E) else Color(0xFF1F7A45),
                                Icons.Filled.BarChart,
                                m(profit),
                                "Profit in range" + (if (d.dbl("expenses") > 0) " (after " + m(d.dbl("expenses")) + " expenses)" else "")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(Modifier.weight(1f), Color(0xFFF1EDFC), Color(0xFF5B46C9), Color(0xFF4A3AA8), Icons.Filled.Inventory2,
                                d.int("orders").toString(), "Orders in range (" + d.int("pending_now") + " pending now)")
                            if (d.has("stock_value")) {
                                StatCard(Modifier.weight(1f), Color(0xFFFDF2E2), Color(0xFFC9821A), Color(0xFFA5670F), Icons.Filled.LocalOffer,
                                    m(d.dbl("stock_value")), "Stock value at cost")
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        AdminCard(title = "Order status overview") {
                            val tiles = d.list("status_tiles")
                            for (rowTiles in tiles.chunked(2)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    for (t in rowTiles) {
                                        StatusTile(Modifier.weight(1f), t) { onOrders(t.str("key")) }
                                    }
                                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        AdminCard(title = "Recent orders") {
                            val recent = d.list("recent")
                            if (recent.isEmpty()) Text("No orders yet.", color = Muted)
                            for ((i, o) in recent.withIndex()) {
                                if (i > 0) HorizontalDivider(color = Line)
                                Row(
                                    Modifier.fillMaxWidth().clickable { onOpenOrder(o.int("id")) }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(o.str("order_code"), color = DeepGreen, fontWeight = FontWeight.SemiBold)
                                        Text(o.str("customer_name"), style = MaterialTheme.typography.bodyMedium)
                                        Text(o.str("date"), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(m(o.dbl("total")), fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(4.dp))
                                        Pill(o.str("status_label"), hexColor(o.str("bg")), hexColor(o.str("fg"), Ink))
                                    }
                                }
                            }
                        }
                    }

                    if (d.has("low_stock")) {
                        AdminCard(title = "Low stock by colour") {
                            val low = d.list("low_stock")
                            if (low.isEmpty()) Text("Stock levels look healthy.", color = Muted)
                            for ((i, l) in low.withIndex()) {
                                if (i > 0) HorizontalDivider(color = Line)
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(l.str("name"))
                                        val sku = l.str("sku")
                                        Text(l.str("color") + (if (sku.isNotBlank()) "  ($sku)" else ""), style = MaterialTheme.typography.bodySmall)
                                    }
                                    val st = l.int("stock")
                                    if (st <= 0) Pill("Out of stock", Color(0xFFFBE2E6), Color(0xFFB02A3E))
                                    else Pill("$st left", Color(0xFFFDF0D8), Color(0xFF9A6B14))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, tint: Color, iconBg: Color, numColor: Color, icon: ImageVector, value: String, label: String) {
    Column(
        modifier
            .background(Brush.verticalGradient(listOf(tint, Color.White)), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Box(Modifier.size(34.dp).background(iconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, color = numColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
}

@Composable
private fun StatusTile(modifier: Modifier, t: JsonObject, onClick: () -> Unit) {
    val fg = hexColor(t.str("fg"), Ink)
    Column(
        modifier
            .background(hexColor(t.str("bg")), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(t.str("label"), color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(t.int("count").toString(), color = fg, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}
