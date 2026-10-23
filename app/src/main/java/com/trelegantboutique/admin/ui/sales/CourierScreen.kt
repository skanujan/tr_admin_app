@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.trelegantboutique.admin.data.obj
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.data.strList
import com.trelegantboutique.admin.ui.common.*

/** Book / update / cancel / track a Domex parcel for one order (same as the Courier box on the website). */
@Composable
fun CourierScreen(
    viewModel: JsonLoaderViewModel,
    orderId: Int,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    HandleState(state, snackbar, viewModel, onSessionExpired)
    var history by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var confirmCancel by remember { mutableStateOf(false) }

    val info = state.data
    val form = info?.obj("form")

    PageScaffold(title = "Courier " + (info?.str("order_code") ?: ""), onBack = onBack, snackbar = snackbar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.error != null || info == null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else if (!info.bool("configured") || form == null) {
                Text(
                    "Domex is not set up yet. Add the Domex details on the website under Sales > Courier first.",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                val booked = info.bool("booked")
                var trackingNo by remember(form) { mutableStateOf(form.str("tracking_no")) }
                var city by remember(form) { mutableStateOf(form.str("receiver_city")) }
                var address by remember(form) { mutableStateOf(form.str("receiver_address")) }
                var phone1 by remember(form) { mutableStateOf(form.str("phone1")) }
                var phone2 by remember(form) { mutableStateOf(form.str("phone2")) }
                var weight by remember(form) { mutableStateOf(form.str("weight")) }
                var pieces by remember(form) { mutableStateOf(form.str("pieces")) }
                var itemType by remember(form) { mutableStateOf(form.str("item_type")) }
                var desc by remember(form) { mutableStateOf(form.str("package_desc")) }
                var charges by remember(form) { mutableStateOf(form.str("total_charges")) }
                var payMethod by remember(form) { mutableStateOf(form.str("payment_method")) }
                var isPaid by remember(form) { mutableStateOf(form.str("is_paid")) }
                var beforeDate by remember(form) { mutableStateOf(form.str("before_delivered")) }
                var remark by remember(form) { mutableStateOf(form.str("remark")) }
                var shipCharge by remember(form) { mutableStateOf(form.str("ship_charge")) }
                var notify by remember(form) { mutableStateOf(false) }
                val payOptions = form.strList("payment_options")

                fun send(action: String) {
                    viewModel.post(
                        "courier-action.php",
                        jsonOf(
                            "id" to orderId,
                            "action" to action,
                            "tracking_no" to trackingNo,
                            "receiver_city" to city,
                            "receiver_address" to address,
                            "phone1" to phone1,
                            "phone2" to phone2,
                            "weight" to weight,
                            "pieces" to pieces,
                            "item_type" to itemType,
                            "package_desc" to desc,
                            "total_charges" to charges,
                            "payment_method" to payMethod,
                            "is_paid" to isPaid,
                            "before_delivered" to beforeDate,
                            "remark" to remark,
                            "ship_charge" to shipCharge,
                            "notify_charge" to notify
                        ),
                        onDone = { res -> history = res.list("history") }
                    )
                }

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (booked) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Booked with Domex", fontWeight = FontWeight.Bold)
                                Text("Tracking: " + info.str("tracking_code"))
                                val st = info.str("courier_status")
                                if (st.isNotBlank()) Text("Status: $st")
                            }
                        }
                    } else {
                        Text("Not booked yet. Check the details and press Book.", style = MaterialTheme.typography.bodyMedium)
                    }
                    info.obj("plan")?.let { p ->
                        Text(
                            "Domex fare " + money(p.dbl("fare")) + "  |  Total to collect " + money(p.dbl("total")),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    LabeledField("Tracking number (leave as is)", trackingNo, { trackingNo = it })
                    LabeledField("Receiver city", city, { city = it })
                    LabeledField("Receiver address", address, { address = it }, singleLine = false, minLines = 2)
                    LabeledField("Phone 1", phone1, { phone1 = it }, phone = true)
                    LabeledField("Phone 2", phone2, { phone2 = it }, phone = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField("Weight (kg)", weight, { weight = it }, modifier = Modifier.weight(1f), number = true)
                        LabeledField("Pieces", pieces, { pieces = it }, modifier = Modifier.weight(1f), number = true)
                    }
                    LabeledField("Item type", itemType, { itemType = it })
                    LabeledField("Package description", desc, { desc = it })
                    LabeledField("Amount to collect", charges, { charges = it }, number = true)
                    LabeledField("Shipping charge", shipCharge, { shipCharge = it }, number = true)
                    if (payOptions.isNotEmpty()) {
                        ChoiceField("Payment method", payMethod, payOptions, { payMethod = it })
                    }
                    LabeledField("Deliver before (YYYY-MM-DD)", beforeDate, { beforeDate = it })
                    LabeledField("Remark", remark, { remark = it })
                    SwitchRow("Text the customer if the charge changed", notify) { notify = it }

                    if (!booked) {
                        Button(onClick = { send("book") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Book with Domex")
                        }
                    } else {
                        Button(onClick = { send("update") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Update booking")
                        }
                        OutlinedButton(onClick = { send("status") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Track: latest status")
                        }
                        OutlinedButton(onClick = { send("waybill") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Check waybill")
                        }
                        OutlinedButton(onClick = { send("resend") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Re-send booking")
                        }
                        TextButton(onClick = { confirmCancel = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel booking", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                    if (history.isNotEmpty()) {
                        SectionTitle("Tracking history")
                        for (h in history) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(h.str("status"), fontWeight = FontWeight.Bold)
                                    Text(h.str("date"), style = MaterialTheme.typography.bodySmall)
                                    val note = h.str("note")
                                    if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            title = "Cancel Domex booking?",
            text = "This cancels the parcel booking with Domex.",
            confirmLabel = "Cancel booking",
            onConfirm = {
                confirmCancel = false
                viewModel.post("courier-action.php", jsonOf("id" to orderId, "action" to "cancel"))
            },
            onDismiss = { confirmCancel = false }
        )
    }
}
