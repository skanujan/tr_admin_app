@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*
import com.trelegantboutique.admin.ui.theme.*

/** Products like admin > Catalog > Products: search, photo, price, stock, on/off. */
@Composable
fun ProductsListScreen(
    viewModel: JsonLoaderViewModel,
    onOpenProduct: (Int) -> Unit,
    onAddProduct: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var q by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    HandleState(state, snackbar, viewModel, onSessionExpired)

    LaunchedEffect(listState, state.items) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= state.items.size - 3) viewModel.loadMore()
        }
    }

    PageScaffold(
        title = "Products",
        onBack = null,
        snackbar = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                containerColor = Gold,
                contentColor = MaroonDeep,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add product") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchBox(q, { q = it }, "Search name or SKU", { viewModel.setQuery(if (q.isBlank()) emptyMap() else mapOf("q" to q)) })
            Box(Modifier.weight(1f)) {
                if (state.loading || state.error != null) {
                    LoadingOrError(state.loading, state.error, { viewModel.load() })
                } else if (state.items.isEmpty()) {
                    Text("No products found.", color = Muted, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items) { p ->
                            AdminCard(onClick = { onOpenProduct(p.int("id")) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = p.str("image"),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.str("name"), fontWeight = FontWeight.SemiBold, maxLines = 2)
                                        val cat = p.str("category")
                                        val sku = p.str("sku")
                                        Text(listOf(cat, sku).filter { it.isNotBlank() }.joinToString("  |  "), style = MaterialTheme.typography.bodySmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val sale = p.dbl("sale_price")
                                            if (sale > 0) {
                                                Text(money(sale), color = DeepGreen, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp))
                                                Text(money(p.dbl("price")), color = Muted, textDecoration = TextDecoration.LineThrough, style = MaterialTheme.typography.bodySmall)
                                            } else {
                                                Text(money(p.dbl("price")), color = DeepGreen, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            val st = p.int("stock")
                                            if (st <= 0) Pill("Out of stock", Color(0xFFFBE2E6), Color(0xFFB02A3E))
                                            else if (st <= 3) Pill("$st left", Color(0xFFFDF0D8), Color(0xFF9A6B14))
                                            else Pill("$st in stock", Color(0xFFC8F2D3), Color(0xFF13703A))
                                        }
                                    }
                                    Switch(
                                        checked = p.bool("active"),
                                        onCheckedChange = { on ->
                                            viewModel.post("products-update.php", jsonOf("id" to p.int("id"), "active" to on))
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = DeepGreen)
                                    )
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
