@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.JsonLoaderViewModel

/**
 * Search the catalogue and pick a product. With [requireVariant] the person
 * then chooses the colour (and size, when the product has sizes) - used by
 * New sale. Without it, tapping a product returns it straight away.
 */
@Composable
fun ProductPickerDialog(
    viewModel: JsonLoaderViewModel,
    requireVariant: Boolean,
    onPicked: (JsonObject, JsonObject?, JsonObject?) -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var q by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JsonObject?>(null) }
    var color by remember { mutableStateOf<JsonObject?>(null) }
    var size by remember { mutableStateOf<JsonObject?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.items) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= state.items.size - 3) viewModel.loadMore()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.padding(12.dp)) {
                val sel = selected
                if (sel == null) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        label = { Text("Search product or SKU") },
                        singleLine = true,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { viewModel.setQuery(mapOf("q" to q)) }
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else if (state.error != null) {
                            Text(state.error ?: "", Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(state.items) { p ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            if (requireVariant) {
                                                selected = p
                                                color = null
                                                size = null
                                            } else {
                                                onPicked(p, null, null)
                                            }
                                        }.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(model = p.str("image"), contentDescription = null, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(p.str("name"), fontWeight = FontWeight.Bold, maxLines = 2)
                                            Text(
                                                money(p.dbl("price")) + "   Stock " + p.int("stock"),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                } else {
                    val colors = sel.list("colors")
                    val hasSizes = sel.bool("has_sizes")
                    Text(sel.str("name"), fontWeight = FontWeight.Bold)
                    Text("Price " + money(sel.dbl("price")), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.weight(1f).verticalScrollCompat()) {
                        if (colors.isEmpty()) {
                            Text("No colours on this product. It will be added as it is (stock ${sel.int("stock")}).")
                        } else {
                            Text("Colour", fontWeight = FontWeight.Bold)
                            for (c in colors) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { color = c; size = null }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = color?.int("cid") == c.int("cid"), onClick = { color = c; size = null })
                                    AsyncImage(model = c.str("image"), contentDescription = null, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(c.str("name"), modifier = Modifier.weight(1f))
                                    Text("Stock " + c.int("stock"), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val cc = color
                            if (hasSizes && cc != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("Size", fontWeight = FontWeight.Bold)
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (s in cc.list("sizes")) {
                                        FilterChip(
                                            selected = size?.int("sid") == s.int("sid"),
                                            onClick = { size = s },
                                            label = { Text(s.str("label") + " (" + s.int("stock") + ")") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val ready = colors.isEmpty() || (color != null && (!hasSizes || size != null))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selected = null }) { Text("Back") }
                        Button(enabled = ready, onClick = { onPicked(sel, color, size) }) { Text("Add") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))
