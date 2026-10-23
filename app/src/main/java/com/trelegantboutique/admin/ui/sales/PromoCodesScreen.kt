@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

@Composable
fun PromoCodesScreen(viewModel: JsonLoaderViewModel, onBack: (() -> Unit)?, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<JsonObject?>(null) }
    HandleState(state, snackbar, viewModel, onSessionExpired)

    PageScaffold(
        title = "Promo codes",
        onBack = onBack,
        snackbar = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, contentDescription = "Add code") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.error != null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                val codes = state.data?.list("codes") ?: emptyList()
                if (codes.isEmpty()) Text("No promo codes yet.", Modifier.align(Alignment.Center))
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(codes) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.str("code"), fontWeight = FontWeight.Bold)
                                    val off = if (c.str("type") == "fixed") money(c.dbl("value")) + " off" else money(c.dbl("value")) + "% off"
                                    Text(off, style = MaterialTheme.typography.bodySmall)
                                    var extra = "Used " + c.int("used_count")
                                    if (c.str("usage_limit").isNotBlank()) extra += " of " + c.int("usage_limit")
                                    if (c.dbl("min_subtotal") > 0) extra += "  |  Min " + money(c.dbl("min_subtotal"))
                                    if (c.str("expires_at").isNotBlank()) extra += "  |  Ends " + c.str("expires_at")
                                    if (c.bool("no_payzy")) extra += "  |  Not with Payzy"
                                    Text(extra, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = c.bool("active"),
                                    onCheckedChange = { viewModel.post("promo-codes.php", jsonOf("action" to "toggle", "id" to c.int("id"))) }
                                )
                                TextButton(onClick = { deleting = c }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    val del = deleting
    if (del != null) {
        ConfirmDialog(
            title = "Delete " + del.str("code") + "?",
            text = "This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.post("promo-codes.php", jsonOf("action" to "delete", "id" to del.int("id")))
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }

    if (adding) {
        var code by remember { mutableStateOf("") }
        var percent by remember { mutableStateOf(true) }
        var value by remember { mutableStateOf("") }
        var min by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("") }
        var expires by remember { mutableStateOf("") }
        var noPayzy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New promo code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Code", code, { code = it })
                    ChoiceField("Type", if (percent) "Percent" else "Fixed amount", listOf("Percent", "Fixed amount"), { percent = it == "Percent" })
                    LabeledField(if (percent) "Percent off" else "Amount off", value, { value = it }, number = true)
                    LabeledField("Minimum order (optional)", min, { min = it }, number = true)
                    LabeledField("Use limit (optional)", limit, { limit = it }, number = true)
                    LabeledField("Ends on YYYY-MM-DD (optional)", expires, { expires = it })
                    SwitchRow("Not valid with Payzy", noPayzy) { noPayzy = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.post(
                        "promo-codes.php",
                        jsonOf(
                            "action" to "add",
                            "code" to code,
                            "type" to (if (percent) "percent" else "fixed"),
                            "value" to value,
                            "min_subtotal" to min,
                            "usage_limit" to limit,
                            "expires_at" to expires,
                            "no_payzy" to noPayzy
                        )
                    )
                    adding = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }
        )
    }
}
