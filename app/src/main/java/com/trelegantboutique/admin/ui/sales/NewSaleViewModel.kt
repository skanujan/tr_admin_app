@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.trelegantboutique.admin.data.dbl
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
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
    qty: Int = 1,
    free: Boolean = false
) {
    var qty by mutableStateOf(qty)
    var free by mutableStateOf(free)
}

/** Holds the items of the sale being made, and sends it. Fields typed by the
 *  person are kept by the screen itself; the items live here so they survive
 *  turning the phone. */
class NewSaleViewModel(private val repo: ApiRepository) : ViewModel() {

    val lines = mutableStateListOf<SaleLine>()
    var submitting by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var doneCode by mutableStateOf<String?>(null)
    var doneOrderId by mutableStateOf(0)
    var sessionExpired by mutableStateOf(false)

    // One token per sale: if the network drops after the server saved it and
    // the person taps Save again, the server returns the same order instead
    // of making a second one.
    private var token = UUID.randomUUID().toString()

    fun addLine(product: JsonObject, color: JsonObject?, size: JsonObject?) {
        val cid = color?.int("cid") ?: 0
        val sid = size?.int("sid") ?: 0
        for (l in lines) {
            if (l.pid == product.int("pid") && l.cid == cid && l.sid == sid) {
                l.qty = l.qty + 1
                return
            }
        }
        var variant = ""
        if (color != null) variant = color.str("name")
        if (size != null) variant += (if (variant.isEmpty()) "" else " / ") + size.str("label")
        val image = if (color != null && color.str("image").isNotBlank()) color.str("image") else product.str("image")
        lines.add(
            SaleLine(
                pid = product.int("pid"), cid = cid, sid = sid,
                name = product.str("name"), variant = variant, image = image,
                price = product.dbl("price")
            )
        )
    }

    fun subtotal(): Double {
        var t = 0.0
        for (l in lines) if (!l.free) t += l.price * l.qty
        return t
    }

    fun submit(fields: JsonObject) {
        if (submitting) return
        error = null
        val items = JsonArray()
        for (l in lines) {
            items.add(
                jsonOf("pid" to l.pid, "cid" to l.cid, "sid" to l.sid, "qty" to l.qty, "free" to l.free)
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
