package com.trelegantboutique.admin.ui.sales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.obj
import com.trelegantboutique.admin.data.str
import kotlinx.coroutines.launch
import java.util.UUID

class SaleLine(
    val pid: Int,
    val cid: Int,
    val sid: Int,
    val name: String,
    val variant: String,
    val image: String,
    val price: Double,
    val cost: Double,
    val available: Int,
    qty: Int = 1,
    free: Boolean = false
) {
    var qty by mutableStateOf(qty)
    var free by mutableStateOf(free)
    var discount by mutableStateOf("")

    fun gross(): Double = if (free) 0.0 else price * qty
    fun lineDiscount(): Double = minOf(maxOf(0.0, discount.toDoubleOrNull() ?: 0.0), gross())
}

/** Items, defaults and server calls of the New sale screen (same rules as admin > New sale). */
class NewSaleViewModel(private val repo: ApiRepository) : ViewModel() {

    val lines = mutableStateListOf<SaleLine>()
    var setup by mutableStateOf<JsonObject?>(null)
    var submitting by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var info by mutableStateOf<String?>(null)
    var doneCode by mutableStateOf<String?>(null)
    var doneOrderId by mutableStateOf(0)
    var sessionExpired by mutableStateOf(false)
    var lookingUp by mutableStateOf(false)
    var lookupText by mutableStateOf("")

    // One token per sale: a second tap (or a retry after a network drop)
    // returns the same order instead of making another one.
    private var token = UUID.randomUUID().toString()

    init { loadSetup() }

    fun loadSetup() {
        viewModelScope.launch {
            when (val r = repo.getJson("sale-setup.php")) {
                is ApiResult.Success -> setup = r.data
                is ApiResult.Failure -> { if (r.unauthorized) sessionExpired = true }
            }
        }
    }

    fun addLine(product: JsonObject, color: JsonObject?, size: JsonObject?): String? {
        val cid = color?.int("cid") ?: 0
        val sid = size?.int("sid") ?: 0
        val available = when {
            size != null -> size.int("stock")
            color != null -> color.int("stock")
            else -> product.int("stock")
        }
        for (l in lines) {
            if (l.pid == product.int("pid") && l.cid == cid && l.sid == sid) {
                if (l.qty + 1 > l.available) return "Only ${l.available} in stock."
                l.qty = l.qty + 1
                return null
            }
        }
        if (available <= 0) return "This one is out of stock."
        var variant = ""
        if (color != null) variant = color.str("name")
        if (size != null) variant += (if (variant.isEmpty()) "" else " / ") + size.str("label")
        val image = if (color != null && color.str("image").isNotBlank()) color.str("image") else product.str("image")
        lines.add(
            SaleLine(
                pid = product.int("pid"), cid = cid, sid = sid,
                name = product.str("name"), variant = variant, image = image,
                price = product.dbl("price"), cost = product.dbl("cost"), available = available
            )
        )
        return null
    }

    fun pieces(): Int = lines.sumOf { it.qty }
    fun subtotal(): Double = lines.sumOf { it.gross() }
    fun lineDiscounts(): Double = lines.sumOf { it.lineDiscount() }
    fun itemsCost(): Double = lines.sumOf { it.cost * it.qty }

    /** Fills the customer from a saved one with this phone number. */
    fun lookup(phone: String, onFound: (JsonObject) -> Unit) {
        if (phone.filter { it.isDigit() }.length < 7) { lookupText = "Enter a full number to search."; return }
        lookingUp = true
        lookupText = "Searching..."
        viewModelScope.launch {
            when (val r = repo.getJson("customer-lookup.php", mapOf("phone" to phone))) {
                is ApiResult.Success -> {
                    val c = r.data.obj("customer")
                    if (r.data.bool("found") && c != null) {
                        lookupText = "Found: " + c.str("name").ifBlank { "saved customer" } + ". Details filled."
                        onFound(c)
                    } else {
                        lookupText = "No saved customer with this number."
                    }
                }
                is ApiResult.Failure -> lookupText = r.message
            }
            lookingUp = false
        }
    }

    /** Domex fare for these pieces (or a weight in kg). */
    fun calcFare(kg: String, onFare: (Double, Double) -> Unit) {
        viewModelScope.launch {
            val q = HashMap<String, String>()
            q["pcs"] = pieces().toString()
            if ((kg.toDoubleOrNull() ?: 0.0) > 0) q["kg"] = kg
            when (val r = repo.getJson("ship-calc.php", q)) {
                is ApiResult.Success -> onFare(r.data.dbl("fare"), r.data.dbl("kg"))
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun submit(fields: JsonObject) {
        if (submitting) return
        error = null
        val items = JsonArray()
        for (l in lines) {
            items.add(
                jsonOf(
                    "pid" to l.pid, "cid" to l.cid, "sid" to l.sid, "qty" to l.qty, "free" to l.free,
                    "discount" to l.lineDiscount()
                )
            )
        }
        fields.add("items", items)
        fields.addProperty("client_token", token)
        submitting = true
        viewModelScope.launch {
            when (val r = repo.postJson("orders-create.php", fields)) {
                is ApiResult.Success -> {
                    submitting = false
                    doneCode = r.data.str("code")
                    doneOrderId = r.data.int("order_id")
                    lines.clear()
                    token = UUID.randomUUID().toString()
                }
                is ApiResult.Failure -> {
                    submitting = false
                    error = r.message
                    if (r.unauthorized) sessionExpired = true
                }
            }
        }
    }

    fun finishAndReset() {
        doneCode = null
        doneOrderId = 0
    }
}
