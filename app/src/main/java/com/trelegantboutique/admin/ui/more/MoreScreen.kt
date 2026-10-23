@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MenuEntry(val title: String, val subtitle: String, val onClick: () -> Unit)

/** The Catalog and Sales menus, mirroring the website's admin menus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onProducts: () -> Unit,
    onCollections: () -> Unit,
    onStock: () -> Unit,
    onNewSale: () -> Unit,
    onOrders: () -> Unit,
    onDeliveries: () -> Unit,
    onPreorders: () -> Unit,
    onPromos: () -> Unit,
    onOffer: () -> Unit,
    onSignOut: () -> Unit
) {
    val catalog = listOf(
        MenuEntry("Products", "Add and edit products, colours, sizes, photos", onProducts),
        MenuEntry("Collections", "Add, rename or hide collections", onCollections),
        MenuEntry("Stock report", "Stock of every item by colour", onStock)
    )
    val sales = listOf(
        MenuEntry("New sale", "Make an online order or a store sale", onNewSale),
        MenuEntry("Orders", "All orders and their status", onOrders),
        MenuEntry("Deliveries and courier", "Payment received, book and track Domex", onDeliveries),
        MenuEntry("Pre-orders", "Customers waiting for a product", onPreorders),
        MenuEntry("Promo codes", "Discount codes", onPromos),
        MenuEntry("Site offer", "Deepawali style offer on/off", onOffer)
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Menu") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Catalog", style = MaterialTheme.typography.titleMedium) }
            for (e in catalog) item { EntryCard(e) }
            item { Spacer(Modifier.height(8.dp)) }
            item { Text("Sales", style = MaterialTheme.typography.titleMedium) }
            for (e in sales) item { EntryCard(e) }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "Not in the app: barcode printing, banners and Domex settlement. Use the website for those.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                TextButton(onClick = onSignOut) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryCard(e: MenuEntry) {
    Card(onClick = e.onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall)
                Text(e.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
