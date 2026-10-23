@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

/** The site-wide Deepawali style offer: on/off, percent, code, label, end date. */
@Composable
fun OfferScreen(viewModel: JsonLoaderViewModel, onBack: (() -> Unit)?, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    HandleState(state, snackbar, viewModel, onSessionExpired)
    val d = state.data

    PageScaffold(title = "Site offer", onBack = onBack, snackbar = snackbar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.error != null || d == null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                var on by remember(d) { mutableStateOf(d.bool("on")) }
                var pct by remember(d) { mutableStateOf(money(d.dbl("pct"))) }
                var code by remember(d) { mutableStateOf(d.str("code")) }
                var label by remember(d) { mutableStateOf(d.str("label")) }
                var until by remember(d) { mutableStateOf(d.str("until")) }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (d.bool("active_now")) "The offer is showing on the website right now."
                        else "The offer is not showing on the website right now.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SwitchRow("Offer ON", on) { on = it }
                    LabeledField("Percent off", pct, { pct = it }, number = true)
                    LabeledField("Promo code customers type at checkout", code, { code = it })
                    LabeledField("Offer name", label, { label = it })
                    LabeledField("Ends on YYYY-MM-DD (blank = no end)", until, { until = it })
                    Text("The code is never valid with Payzy.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            viewModel.post(
                                "offer.php",
                                jsonOf("on" to on, "pct" to pct, "code" to code, "label" to label, "until" to until)
                            )
                        },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save offer") }
                }
            }
        }
    }
}
