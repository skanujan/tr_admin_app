@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.ui.common.*

private val PAY_ONLINE = listOf("Cash on Delivery", "Cash", "Card", "Bank Transfer")
private val PAY_STORE = listOf("Cash", "Card", "Bank Transfer")
private val SALE_STATUSES = listOf("Confirmed", "Pending", "Delivered")

/** Make a sale or order from the phone - same as New sale on the website. */
@Composable
fun NewSaleScreen(
    viewModel: NewSaleViewModel,
    pickerViewModel: JsonLoaderViewModel,
    onBack: () -> Unit,
    onOpenOrder: (Int) -> Unit,
    onSessionExpired: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    var store by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var phone2 by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf("Cash on Delivery") }
    var status by rememberSaveable { mutableStateOf("Confirmed") }
    var deliveryCharge by rememberSaveable { mutableStateOf("") }
    var deliveryCost by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf("") }
    var cashGiven by rememberSaveable { mutableStateOf("") }
    var received by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.sessionExpired) {
        if (viewModel.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(viewModel.error) {
        val e = viewModel.error
        if (e != null) {
            snackbar.showSnackbar(e)
            viewModel.error = null
        }
    }

    val payOptions = if (store) PAY_STORE else PAY_ONLINE
    val deliveryNum = if (store) 0.0 else (deliveryCharge.toDoubleOrNull() ?: 0.0)
    val discountNum = discount.toDoubleOrNull() ?: 0.0
    val total = viewModel.subtotal() + deliveryNum - discountNum

    PageScaffold(title = "New sale", onBack = onBack, snackbar = snackbar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !store, onClick = {
                    store = false
                    if (payment == "Cash") payment = "Cash on Delivery"
                }, label = { Text("Online order") })
                FilterChip(selected = store, onClick = {
                    store = true
                    if (payment == "Cash on Delivery") payment = "Cash"
                }, label = { Text("Store sale") })
            }

            SectionTitle("Items")
            for (l in viewModel.lines) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = l.image, contentDescription = null, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.name, fontWeight = FontWeight.Bold, maxLines = 2)
                            if (l.variant.isNotBlank()) Text(l.variant, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (l.free) "Free" else money(l.price) + " each",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = l.free, onCheckedChange = { l.free = it })
                                Text("Free item", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { if (l.qty > 1) l.qty = l.qty - 1 }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Less")
                        }
                        Text(l.qty.toString(), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { l.qty = l.qty + 1 }) {
                            Icon(Icons.Filled.Add, contentDescription = "More")
                        }
                        IconButton(onClick = { viewModel.lines.remove(l) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Add item") }

            SectionTitle("Customer")
            LabeledField("Name", name, { name = it })
            LabeledField("Phone", phone, { phone = it }, phone = true)
            if (!store) {
                LabeledField("Second phone (optional)", phone2, { phone2 = it }, phone = true)
                LabeledField("Email (optional)", email, { email = it })
                LabeledField("Address", address, { address = it }, singleLine = false, minLines = 2)
                LabeledField("City", city, { city = it })
            }
            LabeledField("Note (optional)", note, { note = it })

            SectionTitle("Payment")
            ChoiceField("Payment method", payment, payOptions, { payment = it })
            if (!store) {
                LabeledField("Delivery charge (customer pays)", deliveryCharge, { deliveryCharge = it }, number = true)
                LabeledField("Our delivery cost", deliveryCost, { deliveryCost = it }, number = true)
            }
            LabeledField("Discount (amount)", discount, { discount = it }, number = true)
            if (store && payment == "Cash") {
                LabeledField("Cash received", cashGiven, { cashGiven = it }, number = true)
                val given = cashGiven.toDoubleOrNull()
                if (given != null && given >= total) {
                    Text("Balance to give back: " + money(given - total), fontWeight = FontWeight.Bold)
                }
            }
            if (store && payment != "Cash") SwitchRow("Payment received", received) { received = it }
            ChoiceField("Order status", status, SALE_STATUSES, { status = it })

            Text("Total: " + money(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                enabled = !viewModel.submitting && viewModel.lines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.submit(
                        jsonOf(
                            "sale_type" to (if (store) "store" else "online"),
                            "name" to name,
                            "phone" to phone,
                            "phone2" to phone2,
                            "email" to email,
                            "address" to address,
                            "city" to city,
                            "note" to note,
                            "payment" to payment,
                            "status" to status,
                            "delivery_charge" to deliveryCharge,
                            "delivery_cost" to deliveryCost,
                            "discount" to discount,
                            "cash_given" to cashGiven,
                            "payment_received" to received
                        )
                    )
                }
            ) { Text(if (viewModel.submitting) "Saving..." else "Save sale") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picking) {
        ProductPickerDialog(
            viewModel = pickerViewModel,
            requireVariant = true,
            onPicked = { p, c, s ->
                viewModel.addLine(p, c, s)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }

    val code = viewModel.doneCode
    if (code != null) {
        val orderId = viewModel.doneOrderId
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Sale saved") },
            text = { Text("Order $code was created.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.finishAndReset()
                    if (orderId > 0) onOpenOrder(orderId) else onBack()
                }) { Text("Open order") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.finishAndReset()
                    name = ""; phone = ""; phone2 = ""; email = ""; address = ""; city = ""; note = ""
                    deliveryCharge = ""; deliveryCost = ""; discount = ""; cashGiven = ""; received = false
                }) { Text("New sale") }
            }
        )
    }
}
