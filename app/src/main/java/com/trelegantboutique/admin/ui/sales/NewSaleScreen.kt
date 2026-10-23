@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.money
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.data.strList
import com.trelegantboutique.admin.ui.common.*
import com.trelegantboutique.admin.ui.theme.*
import com.trelegantboutique.admin.util.WhatsAppPaste

private val MONTHS = listOf("Month", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

/** Make a sale or order from the phone: everything admin > New sale has. */
@Composable
fun NewSaleScreen(
    viewModel: NewSaleViewModel,
    pickerViewModel: JsonLoaderViewModel,
    onBack: (() -> Unit)?,
    onOpenOrder: (Int) -> Unit,
    onSessionExpired: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val setup = viewModel.setup

    var store by rememberSaveable { mutableStateOf(false) }
    var paste by rememberSaveable { mutableStateOf("") }
    var pasteStatus by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("+94 ") }
    var phone2 by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("ta") }
    var bMonth by rememberSaveable { mutableStateOf(0) }
    var bDay by rememberSaveable { mutableStateOf(0) }
    var payment by rememberSaveable { mutableStateOf("Cash on Delivery") }
    var status by rememberSaveable { mutableStateOf("Confirmed") }
    var deliveryCharge by rememberSaveable { mutableStateOf("") }
    var deliveryCost by rememberSaveable { mutableStateOf("") }
    var freeDelivery by rememberSaveable { mutableStateOf(false) }
    var kg by rememberSaveable { mutableStateOf("") }
    var fareText by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf("") }
    var cashGiven by rememberSaveable { mutableStateOf("") }
    var received by rememberSaveable { mutableStateOf(true) }
    var defaultsApplied by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

    // Website defaults (delivery charge and cost) once they arrive.
    LaunchedEffect(setup) {
        val s = setup
        if (s != null && !defaultsApplied) {
            deliveryCharge = money(s.dbl("delivery_charge"))
            deliveryCost = money(s.dbl("delivery_cost"))
            language = s.str("default_language").ifBlank { "ta" }
            defaultsApplied = true
        }
    }
    LaunchedEffect(viewModel.sessionExpired) { if (viewModel.sessionExpired) onSessionExpired() }
    LaunchedEffect(viewModel.error) {
        val e = viewModel.error
        if (e != null) { snackbar.showSnackbar(e); viewModel.error = null }
    }
    LaunchedEffect(viewModel.info) {
        val e = viewModel.info
        if (e != null) { snackbar.showSnackbar(e); viewModel.info = null }
    }

    fun fillFromPaste(text: String) {
        if (text.isBlank()) return
        val r = WhatsAppPaste.parse(text)
        val got = ArrayList<String>()
        if (r.name.isNotBlank()) { name = r.name; got.add("name") }
        if (r.phone.isNotBlank()) { phone = r.phone; got.add("mobile number") }
        if (r.address.isNotBlank()) { address = r.address; got.add("address") }
        if (r.city.isNotBlank() && city.isBlank()) { city = r.city; got.add("city") }
        pasteStatus = if (got.isEmpty()) "Could not pick out any details. Please fill the fields below."
        else "Picked up: " + got.joinToString(", ") + ". Please check before saving."
        if (r.phone.isNotBlank()) {
            viewModel.lookup(r.phone) { c ->
                // A saved customer: fill only what the message did not have.
                if (name.isBlank()) name = c.str("name")
                if (email.isBlank()) email = c.str("email")
                if (address.isBlank()) address = c.str("address")
                if (city.isBlank()) city = c.str("city")
                if (c.str("language").isNotBlank()) language = c.str("language")
                if (bMonth == 0) bMonth = c.int("birthday_month")
                if (bDay == 0) bDay = c.int("birthday_day")
            }
        }
    }

    val payOptions = if (setup != null) setup.strList(if (store) "payments_store" else "payments_online")
        .ifEmpty { if (store) listOf("Cash", "Card", "Bank Transfer") else listOf("Cash on Delivery", "Cash", "Card", "Bank Transfer") }
    else if (store) listOf("Cash", "Card", "Bank Transfer") else listOf("Cash on Delivery", "Cash", "Card", "Bank Transfer")
    val languages = setup?.list("languages") ?: emptyList()
    val cur = setup?.str("currency")?.ifBlank { "Rs." } ?: "Rs."
    fun m(v: Double) = "$cur " + money(v)

    val deliveryNum = if (store || freeDelivery) 0.0 else (deliveryCharge.toDoubleOrNull() ?: 0.0)
    val deliveryCostNum = if (store) 0.0 else (deliveryCost.toDoubleOrNull() ?: 0.0)
    val discountNum = discount.toDoubleOrNull() ?: 0.0
    val subtotal = viewModel.subtotal()
    val lineDisc = viewModel.lineDiscounts()
    val total = maxOf(0.0, subtotal - lineDisc + deliveryNum - discountNum)
    val profit = (subtotal - lineDisc - viewModel.itemsCost()) + (deliveryNum - deliveryCostNum) - discountNum

    PageScaffold(title = "New sale", onBack = onBack, snackbar = snackbar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Sale type ----
            AdminCard(title = "Sale type") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !store, onClick = {
                        store = false
                        if (payment == "Cash") payment = "Cash on Delivery"
                    }, label = { Text("Online order (delivery)") })
                    FilterChip(selected = store, onClick = {
                        store = true
                        if (payment == "Cash on Delivery") payment = "Cash"
                        freeDelivery = false
                    }, label = { Text("Store sale (walk-in)") })
                }
                if (store) Text(
                    "No delivery details needed. Name, phone and birthday are optional for a store sale.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ---- Items ----
            AdminCard(title = "Items") {
                if (viewModel.lines.isEmpty()) Text("No items yet.", color = Muted)
                for (l in viewModel.lines) {
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        AsyncImage(model = l.image, contentDescription = null, modifier = Modifier.size(48.dp, 60.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            if (l.variant.isNotBlank()) Text(l.variant, style = MaterialTheme.typography.bodySmall)
                            Text((if (l.free) "Free" else m(l.price) + " each") + "  |  " + l.available + " in stock", style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (l.qty > 1) l.qty = l.qty - 1 }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Less")
                                }
                                Text(l.qty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                IconButton(onClick = {
                                    if (l.qty < l.available) l.qty = l.qty + 1 else viewModel.info = "Only ${l.available} in stock."
                                }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = "More")
                                }
                                Spacer(Modifier.width(6.dp))
                                Checkbox(checked = l.free, onCheckedChange = { l.free = it })
                                Text("Free", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedTextField(
                                value = l.discount,
                                onValueChange = { l.discount = it },
                                label = { Text("Discount on this item") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(0.75f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = { viewModel.lines.remove(l) }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                            Text(m(l.gross() - l.lineDiscount()), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = Line)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { picking = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = MaroonDeep),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Add item") }
            }

            // ---- Customer ----
            AdminCard(title = "Customer" + (if (store) "" else " and delivery")) {
                Column(
                    Modifier.fillMaxWidth().background(Cream, RoundedCornerShape(8.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Paste from WhatsApp", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = paste,
                        onValueChange = { t ->
                            val jump = t.length - paste.length > 8 // a paste, not typing
                            paste = t
                            if (jump) fillFromPaste(t)
                        },
                        placeholder = { Text("Paste the customer's message here. Name, mobile number and address are picked out automatically.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val t = clipboard.getText()?.text ?: ""
                            if (t.isBlank()) pasteStatus = "Nothing copied. Copy the customer's message in WhatsApp first."
                            else { paste = t; fillFromPaste(t) }
                        }, shape = RoundedCornerShape(6.dp)) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste copied message")
                        }
                        OutlinedButton(onClick = { fillFromPaste(paste) }, shape = RoundedCornerShape(6.dp)) { Text("Read again") }
                    }
                    if (pasteStatus.isNotBlank()) {
                        Text(pasteStatus, fontSize = 12.sp, color = if (pasteStatus.startsWith("Picked")) OkGreen else DangerRed)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledField("Phone" + (if (store) "" else " *"), phone, { phone = it }, modifier = Modifier.weight(1f), phone = true)
                    OutlinedButton(onClick = {
                        viewModel.lookup(phone) { c ->
                            name = c.str("name").ifBlank { name }
                            email = c.str("email").ifBlank { email }
                            address = c.str("address").ifBlank { address }
                            city = c.str("city").ifBlank { city }
                            if (c.str("language").isNotBlank()) language = c.str("language")
                            if (c.int("birthday_month") > 0) bMonth = c.int("birthday_month")
                            if (c.int("birthday_day") > 0) bDay = c.int("birthday_day")
                        }
                    }, enabled = !viewModel.lookingUp, shape = RoundedCornerShape(6.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Find")
                    }
                }
                if (viewModel.lookupText.isNotBlank()) {
                    Text(viewModel.lookupText, fontSize = 12.sp, color = if (viewModel.lookupText.startsWith("Found")) OkGreen else Muted)
                }
                Spacer(Modifier.height(6.dp))
                LabeledField("Second phone (optional, given to Domex)", phone2, { phone2 = it }, phone = true)
                Spacer(Modifier.height(6.dp))
                LabeledField("Name" + (if (store) "" else " *"), name, { name = it })
                Spacer(Modifier.height(6.dp))
                LabeledField("Email (optional)", email, { email = it })
                if (!store) {
                    Spacer(Modifier.height(6.dp))
                    LabeledField("Delivery address", address, { address = it }, singleLine = false, minLines = 2)
                    Spacer(Modifier.height(6.dp))
                    LabeledField("City / Town", city, { city = it })
                }
                Spacer(Modifier.height(6.dp))
                if (languages.isNotEmpty()) {
                    val langLabel = languages.firstOrNull { it.str("code") == language }?.str("label") ?: language
                    ChoiceField("Customer language (messages are sent in it)", langLabel, languages.map { it.str("label") }, { picked ->
                        language = languages.firstOrNull { it.str("label") == picked }?.str("code") ?: language
                    })
                    Spacer(Modifier.height(6.dp))
                }
                LabeledField("Note (optional)", note, { note = it })
                Spacer(Modifier.height(6.dp))
                Text("Birthday (optional, for a birthday offer)", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceField("Month", MONTHS[bMonth.coerceIn(0, 12)], MONTHS, { picked -> bMonth = MONTHS.indexOf(picked).coerceAtLeast(0) }, modifier = Modifier.weight(1f))
                    ChoiceField("Day", if (bDay in 1..31) bDay.toString() else "Day", listOf("Day") + (1..31).map { it.toString() }, { picked ->
                        bDay = picked.toIntOrNull() ?: 0
                    }, modifier = Modifier.weight(1f))
                }
            }

            // ---- Payment and charges ----
            AdminCard(title = "Payment and charges") {
                ChoiceField("Payment", payment, payOptions, { payment = it })
                if (payment == "Cash") {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFFF1FAF4), RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LabeledField("Cash received", cashGiven, { cashGiven = it }, number = true)
                        // Quick amounts, like the website's cash buttons.
                        val quick = LinkedHashSet<Double>()
                        if (total > 0) {
                            quick.add(total)
                            for (step in listOf(100.0, 500.0, 1000.0, 5000.0)) quick.add(Math.ceil(total / step) * step)
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (q in quick) {
                                AssistChip(onClick = { cashGiven = money(q) }, label = { Text(money(q)) })
                            }
                        }
                        val given = cashGiven.toDoubleOrNull()
                        if (given != null) {
                            if (given >= total) Text("Balance to give back: " + m(given - total), fontWeight = FontWeight.Bold, color = OkGreen, fontSize = 18.sp)
                            else Text("Still to pay: " + m(total - given), fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 18.sp)
                        }
                    }
                }
                if (store && payment != "Cash") {
                    SwitchRow("Payment received (card or transfer has come through)", received) { received = it }
                }
                if (!store) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField("Delivery charge (customer)", deliveryCharge, { deliveryCharge = it }, modifier = Modifier.weight(1f), number = true)
                        LabeledField("Delivery cost (you pay)", deliveryCost, { deliveryCost = it }, modifier = Modifier.weight(1f), number = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField("kg (auto)", kg, { kg = it }, modifier = Modifier.width(110.dp), number = true)
                        OutlinedButton(onClick = {
                            fareText = "Calculating..."
                            viewModel.calcFare(kg) { fare, w ->
                                freeDelivery = false
                                deliveryCharge = money(fare)
                                deliveryCost = money(fare)
                                fareText = "Domex fare for " + money(w) + " kg: " + money(fare) + " (filled in). Change it if you charge differently."
                            }
                        }, shape = RoundedCornerShape(6.dp)) { Text("Calculate Domex fare") }
                    }
                    if (fareText.isNotBlank()) Text(fareText, style = MaterialTheme.typography.bodySmall)
                    SwitchRow("Free delivery", freeDelivery) { freeDelivery = it }
                }
                Spacer(Modifier.height(6.dp))
                LabeledField("Special discount (optional)", discount, { discount = it }, number = true)

                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth().background(Color(0xFFEEF4F0), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SumRow("Subtotal", m(subtotal))
                    if (lineDisc > 0) SumRow("Item discounts", "-" + m(lineDisc), DangerRed)
                    if (!store) SumRow("Delivery", if (freeDelivery) "Free" else m(deliveryNum))
                    if (discountNum > 0) SumRow("Special discount", "-" + m(discountNum), DangerRed)
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total", fontFamily = Marcellus, fontSize = 22.sp, color = DeepGreen, modifier = Modifier.weight(1f))
                        Text(m(total), fontFamily = Marcellus, fontSize = 22.sp, color = DeepGreen)
                    }
                    SumRow("Estimated profit", m(profit), Muted)
                }
                if (store) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Store sales are marked Completed straight away when the payment is received. No delivery step.",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF0F5A32), RoundedCornerShape(8.dp)).padding(10.dp)
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    ChoiceField("Status", status, setup?.strList("statuses")?.ifEmpty { null } ?: listOf("Confirmed", "Pending", "Delivered"), { status = it })
                }
            }

            Button(
                enabled = !viewModel.submitting && viewModel.lines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    viewModel.submit(
                        jsonOf(
                            "sale_type" to (if (store) "store" else "online"),
                            "name" to name,
                            "phone" to (if (phone.trim() == "+94") "" else phone),
                            "phone2" to phone2,
                            "email" to email,
                            "address" to (if (store) "" else address),
                            "city" to (if (store) "" else city),
                            "note" to note,
                            "payment" to payment,
                            "status" to status,
                            "language" to language,
                            "birthday_month" to bMonth,
                            "birthday_day" to bDay,
                            "delivery_charge" to deliveryNum,
                            "delivery_cost" to deliveryCostNum,
                            "discount" to discountNum,
                            "cash_given" to (if (payment == "Cash") cashGiven else ""),
                            "payment_received" to (store && payment != "Cash" && received)
                        )
                    )
                }
            ) { Text(if (viewModel.submitting) "Creating sale..." else "Create sale  " + m(total), fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picking) {
        ProductPickerDialog(
            viewModel = pickerViewModel,
            requireVariant = true,
            onPicked = { p, c, s ->
                val problem = viewModel.addLine(p, c, s)
                if (problem != null) viewModel.info = problem
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
            title = { Text("Sale saved", fontFamily = Marcellus) },
            text = { Text("Order $code was created.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.finishAndReset()
                    if (orderId > 0) onOpenOrder(orderId) else onBack?.invoke()
                }) { Text("Open order") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.finishAndReset()
                    paste = ""; pasteStatus = ""; name = ""; phone = "+94 "; phone2 = ""; email = ""; address = ""; city = ""; note = ""
                    bMonth = 0; bDay = 0; discount = ""; cashGiven = ""; received = true; freeDelivery = false; kg = ""; fareText = ""
                    defaultsApplied = false
                    viewModel.loadSetup()
                }) { Text("New sale") }
            }
        )
    }
}

@Composable
private fun SumRow(label: String, value: String, color: Color = Ink) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = if (color == Ink) Muted else color, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
