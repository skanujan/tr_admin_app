@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.intList
import com.trelegantboutique.admin.data.jsonIntArray
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.obj
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.data.strList
import com.trelegantboutique.admin.ui.common.*
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.util.ColorTools
import com.trelegantboutique.admin.util.ImageUtil
import com.trelegantboutique.admin.util.dominantColorOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class PhotoTarget(val kind: String, val colorId: Int)

private fun parseHex(h: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(h))
    } catch (e: Exception) {
        Color.LightGray
    }
}

/**
 * Add or edit a product the way the website does: main details and
 * collections first, then (once the product exists) its photos, colours,
 * sizes and stock. Every button here saves straight away.
 */
@Composable
fun ProductEditScreen(
    viewModel: JsonLoaderViewModel,
    productId: Int,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pid by remember { mutableStateOf(productId) }
    HandleState(state, snackbar, viewModel, onSessionExpired)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<PhotoTarget?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val t = target
        target = null
        if (uri != null && t != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { ImageUtil.toJpeg(context, uri) }
                if (bytes == null) {
                    viewModel.showMessage("Could not read that photo.")
                } else {
                    val fields = HashMap<String, String>()
                    fields["product_id"] = pid.toString()
                    fields["kind"] = t.kind
                    fields["real"] = "1"
                    if (t.colorId > 0) fields["color_id"] = t.colorId.toString()
                    viewModel.upload("product-photo.php", fields, bytes)
                }
            }
        }
    }
    // Extra photos: pick several at once, they are uploaded one after another.
    var multiColorId by remember { mutableStateOf(0) }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        val cid = multiColorId
        if (uris.isNotEmpty()) {
            scope.launch {
                viewModel.setBusy(true)
                var ok = 0
                var failed = 0
                for (u in uris.take(20)) {
                    val bytes = withContext(Dispatchers.IO) { ImageUtil.toJpeg(context, u) }
                    if (bytes == null) { failed++; continue }
                    val f = HashMap<String, String>()
                    f["product_id"] = pid.toString()
                    f["kind"] = "extra"
                    f["real"] = "1"
                    if (cid > 0) f["color_id"] = cid.toString()
                    when (viewModel.repository().uploadPhoto("product-photo.php", f, bytes)) {
                        is ApiResult.Success -> ok++
                        is ApiResult.Failure -> failed++
                    }
                }
                viewModel.setBusy(false)
                viewModel.showMessage("$ok photo(s) added" + (if (failed > 0) ", $failed could not be added" else "") + ".")
                viewModel.reloadQuiet()
            }
        }
    }
    fun pickPhoto(kind: String, colorId: Int) {
        if (pid <= 0) return
        if (kind == "extra") {
            multiColorId = colorId
            multiPicker.launch("image/*")
            return
        }
        target = PhotoTarget(kind, colorId)
        picker.launch("image/*")
    }
    fun removePhoto(imageId: Int) {
        viewModel.upload(
            "product-photo.php",
            mapOf("product_id" to pid.toString(), "kind" to "delete", "image_id" to imageId.toString()),
            null
        )
    }
    fun colorPost(body: JsonObject, onDone: ((JsonObject) -> Unit)? = null) {
        viewModel.post("product-colors.php", body, onDone = onDone)
    }

    val d = state.data
    val product = d?.obj("product")

    PageScaffold(
        title = if (pid > 0) "Edit product" else "New product",
        onBack = onBack,
        snackbar = snackbar
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.error != null || d == null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                val formKey = product?.int("id") ?: 0
                var name by remember(formKey) { mutableStateOf(product?.str("name") ?: "") }
                var sku by remember(formKey) { mutableStateOf(product?.str("sku") ?: "") }
                var shortName by remember(formKey) { mutableStateOf(product?.str("short_name") ?: "") }
                var price by remember(formKey) { mutableStateOf(product?.str("price") ?: "") }
                var sale by remember(formKey) { mutableStateOf(product?.str("sale_price") ?: "") }
                var cost by remember(formKey) { mutableStateOf(product?.str("cost_price") ?: "") }
                var inbound by remember(formKey) { mutableStateOf(product?.str("inbound_shipping") ?: "") }
                var weight by remember(formKey) { mutableStateOf(product?.str("weight_g") ?: "") }
                var fabric by remember(formKey) { mutableStateOf(product?.str("fabric") ?: "") }
                var colorText by remember(formKey) { mutableStateOf(product?.str("color") ?: "") }
                var occasion by remember(formKey) { mutableStateOf(product?.str("occasion") ?: "") }
                var blouse by remember(formKey) { mutableStateOf(product?.str("blouse") ?: "") }
                var description by remember(formKey) { mutableStateOf(product?.str("description") ?: "") }
                var stock by remember(formKey) { mutableStateOf(product?.str("stock") ?: "0") }
                var featured by remember(formKey) { mutableStateOf(product?.bool("featured") ?: false) }
                var active by remember(formKey) { mutableStateOf(if (product == null) true else product.bool("active")) }
                var newArrival by remember(formKey) { mutableStateOf(product?.bool("new_arrival") ?: false) }
                var splitColors by remember(formKey) { mutableStateOf(if (product == null) true else product.bool("split_colors")) }
                var cats by remember(formKey) { mutableStateOf((product?.intList("category_ids") ?: emptyList()).toSet()) }

                val collections = d.list("collections")
                val colors = product?.list("colors") ?: emptyList()
                val hasSizes = product?.bool("has_sizes") ?: false
                val presets = d.strList("size_presets")

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionTitle("Details")
                    LabeledField("Product name", name, { name = it })
                    LabeledField("Short name (optional)", shortName, { shortName = it })
                    LabeledField("SKU / code", sku, { sku = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField("Price", price, { price = it }, modifier = Modifier.weight(1f), number = true)
                        LabeledField("Sale price", sale, { sale = it }, modifier = Modifier.weight(1f), number = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField("Cost price", cost, { cost = it }, modifier = Modifier.weight(1f), number = true)
                        LabeledField("Inbound shipping", inbound, { inbound = it }, modifier = Modifier.weight(1f), number = true)
                    }
                    LabeledField("Weight (grams)", weight, { weight = it }, number = true)
                    LabeledField("Fabric", fabric, { fabric = it })
                    LabeledField("Colour (text for search)", colorText, { colorText = it })
                    LabeledField("Occasion", occasion, { occasion = it })
                    LabeledField("Blouse", blouse, { blouse = it })
                    LabeledField("Description", description, { description = it }, singleLine = false, minLines = 3)
                    if (colors.isEmpty()) {
                        LabeledField("Stock", stock, { stock = it }, number = true)
                    }
                    SwitchRow("Show on website", active) { active = it }
                    SwitchRow("Featured", featured) { featured = it }
                    SwitchRow("New arrival", newArrival) { newArrival = it }
                    SwitchRow("Show each colour as its own item", splitColors) { splitColors = it }

                    SectionTitle("Collections (choose one or more)")
                    val mains = collections.filter { it.int("parent_id") == 0 }
                    for (m in mains) {
                        CollectionCheck(m.str("name"), cats.contains(m.int("id")), 0) { on ->
                            cats = if (on) cats + m.int("id") else cats - m.int("id")
                        }
                        for (sub in collections.filter { it.int("parent_id") == m.int("id") }) {
                            CollectionCheck(sub.str("name"), cats.contains(sub.int("id")), 1) { on ->
                                cats = if (on) cats + sub.int("id") else cats - sub.int("id")
                            }
                        }
                    }

                    Button(
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val body = jsonOf(
                                "id" to pid,
                                "name" to name,
                                "short_name" to shortName,
                                "sku" to sku,
                                "price" to price,
                                "sale_price" to sale,
                                "cost_price" to cost,
                                "inbound_shipping" to inbound,
                                "weight_g" to weight,
                                "fabric" to fabric,
                                "color" to colorText,
                                "occasion" to occasion,
                                "blouse" to blouse,
                                "description" to description,
                                "stock" to stock,
                                "featured" to featured,
                                "active" to active,
                                "new_arrival" to newArrival,
                                "split_colors" to splitColors,
                                "category_ids" to jsonIntArray(cats)
                            )
                            viewModel.post("products-save.php", body, reload = pid > 0, onDone = { res ->
                                if (pid <= 0) {
                                    val newId = res.int("id")
                                    if (newId > 0) {
                                        pid = newId
                                        viewModel.setQuery(mapOf("id" to newId.toString()))
                                    }
                                }
                            })
                        }
                    ) { Text(if (pid > 0) "Save details" else "Add product") }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                    if (pid > 0 && product != null) {
                        // ---- Photos ----
                        SectionTitle("Main photo")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = product.str("image"), contentDescription = null, modifier = Modifier.size(96.dp))
                            Spacer(Modifier.width(12.dp))
                            OutlinedButton(onClick = { pickPhoto("main", 0) }) { Text("Change main photo") }
                        }
                        Text("More photos", fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (g in product.list("gallery")) {
                                PhotoThumb(g.str("url")) { removePhoto(g.int("id")) }
                            }
                            OutlinedButton(onClick = { pickPhoto("extra", 0) }) { Text("Add photo") }
                        }

                        // ---- Colours ----
                        SectionTitle("Colours and sizes")
                        SwitchRow("This product has sizes", hasSizes) { on ->
                            colorPost(jsonOf("action" to "sizes_toggle", "product_id" to pid, "has_sizes" to on))
                        }
                        for (c in colors) {
                            ColorCard(
                                color = c,
                                hasSizes = hasSizes,
                                presets = presets,
                                busy = state.busy,
                                onSave = { n, hex, csku, cstock, def ->
                                    colorPost(
                                        jsonOf(
                                            "action" to "color_save", "product_id" to pid, "color_id" to c.int("id"),
                                            "name" to n, "hex" to hex, "sku" to csku, "stock" to cstock, "is_default" to def
                                        )
                                    )
                                },
                                onDelete = {
                                    colorPost(jsonOf("action" to "color_delete", "product_id" to pid, "color_id" to c.int("id")))
                                },
                                onPickMain = { pickPhoto("color", c.int("id")) },
                                onPickExtra = { pickPhoto("extra", c.int("id")) },
                                onRemovePhoto = { removePhoto(it) },
                                onSizePost = { body -> colorPost(body) },
                                productId = pid
                            )
                        }
                        AddColorCard(hasSizes) { n, hex, csku, cstock, photo ->
                            colorPost(
                                jsonOf(
                                    "action" to "color_add", "product_id" to pid,
                                    "name" to n, "hex" to hex, "sku" to csku, "stock" to cstock
                                ),
                                onDone = { res ->
                                    val newCid = res.int("color_id")
                                    if (photo != null && newCid > 0) {
                                        viewModel.upload(
                                            "product-photo.php",
                                            mapOf("product_id" to pid.toString(), "kind" to "color", "color_id" to newCid.toString(), "real" to "1"),
                                            photo
                                        )
                                    }
                                }
                            )
                        }
                        BatchColorsCard(viewModel = viewModel, productId = pid, hasSizes = hasSizes, existing = colors)
                        if (hasSizes && colors.isNotEmpty()) {
                            AddSizeToAll(presets) { label, st ->
                                colorPost(jsonOf("action" to "size_addall", "product_id" to pid, "label" to label, "stock" to st))
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CollectionCheck(label: String, checked: Boolean, level: Int, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = (level * 24).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun PhotoThumb(url: String, onRemove: () -> Unit) {
    Box {
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(80.dp))
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove photo")
        }
    }
}

@Composable
private fun ColorCard(
    color: JsonObject,
    hasSizes: Boolean,
    presets: List<String>,
    busy: Boolean,
    productId: Int,
    onSave: (String, String, String, String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onPickMain: () -> Unit,
    onPickExtra: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSizePost: (JsonObject) -> Unit
) {
    var name by remember(color) { mutableStateOf(color.str("name")) }
    var hex by remember(color) { mutableStateOf(color.str("hex")) }
    var sku by remember(color) { mutableStateOf(color.str("sku")) }
    var stock by remember(color) { mutableStateOf(color.str("stock")) }
    var isDefault by remember(color) { mutableStateOf(color.bool("is_default")) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        ColorPickerDialog(
            initialHex = hex,
            photoUrl = color.str("image").ifBlank { null },
            onPick = { h, suggested ->
                val oldAuto = ColorTools.parseHex(hex)?.let { ColorTools.name(it) } ?: ""
                if (name.isBlank() || name == oldAuto) name = suggested
                hex = h
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
    val cid = color.int("id")

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(parseHex(hex), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(color.str("name"), fontWeight = FontWeight.Bold)
            }
            LabeledField("Colour name", name, { name = it })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorDot(hex, 40) { showPicker = true }
                LabeledField("Dot colour (tap the circle)", hex, { hex = it }, modifier = Modifier.weight(1f))
            }
            LabeledField("Colour SKU (blank = automatic)", sku, { sku = it })
            if (!hasSizes) LabeledField("Stock", stock, { stock = it }, number = true)
            SwitchRow("Default colour", isDefault) { isDefault = it }
            Button(onClick = { onSave(name, hex, sku, stock, isDefault) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Save colour")
            }

            Text("Photos of this colour", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val main = color.str("image")
                if (main.isNotBlank()) AsyncImage(model = main, contentDescription = null, modifier = Modifier.size(80.dp))
                OutlinedButton(onClick = onPickMain) { Text(if (main.isBlank()) "Add main photo" else "Change") }
                for (x in color.list("extra_images")) {
                    PhotoThumb(x.str("url")) { onRemovePhoto(x.int("id")) }
                }
                OutlinedButton(onClick = onPickExtra) { Text("More") }
            }

            if (hasSizes) {
                Text("Sizes", fontWeight = FontWeight.Bold)
                for (s in color.list("sizes")) {
                    SizeRow(s, onSave = { st ->
                        onSizePost(
                            jsonOf(
                                "action" to "size_save", "product_id" to productId, "color_id" to cid,
                                "size_id" to s.int("id"), "stock" to st, "sku" to s.str("sku")
                            )
                        )
                    }, onDelete = {
                        onSizePost(
                            jsonOf("action" to "size_delete", "product_id" to productId, "color_id" to cid, "size_id" to s.int("id"))
                        )
                    })
                }
                var newLabel by remember(color) { mutableStateOf("") }
                var newStock by remember(color) { mutableStateOf("0") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChoiceField("Size", newLabel, presets, { newLabel = it }, modifier = Modifier.weight(1f))
                    LabeledField("Stock", newStock, { newStock = it }, modifier = Modifier.weight(1f), number = true)
                }
                OutlinedButton(
                    onClick = {
                        onSizePost(
                            jsonOf(
                                "action" to "size_add", "product_id" to productId, "color_id" to cid,
                                "label" to newLabel, "stock" to newStock
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add size to this colour") }
            }

            TextButton(onClick = { confirmDelete = true }) {
                Text("Delete this colour", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete colour?",
            text = "The colour, its sizes and its photos will be removed.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun SizeRow(size: JsonObject, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var stock by remember(size) { mutableStateOf(size.str("stock")) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(size.str("label"), modifier = Modifier.width(72.dp), fontWeight = FontWeight.Bold)
        LabeledField("Stock", stock, { stock = it }, modifier = Modifier.weight(1f), number = true)
        TextButton(onClick = { onSave(stock) }) { Text("Save") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Remove size") }
    }
}

@Composable
private fun AddColorCard(hasSizes: Boolean, onAdd: (String, String, String, String, ByteArray?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("#14603a") }
    var sku by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("1") }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { ImageUtil.toJpeg(context, uri) }
                val rgb = withContext(Dispatchers.IO) { dominantColorOf(context, uri) }
                photo = bytes
                photoUri = uri
                if (rgb != null) {
                    hex = ColorTools.toHex(rgb)
                    name = ColorTools.name(rgb)
                }
            }
        }
    }
    if (showPicker) {
        ColorPickerDialog(
            initialHex = hex,
            photoUrl = null,
            onPick = { h, suggested -> hex = h; name = suggested; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add a colour", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (photoUri != null) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.size(72.dp))
                }
                OutlinedButton(onClick = { pick.launch("image/*") }) {
                    Text(if (photoUri == null) "Main photo of this colour" else "Change photo")
                }
            }
            Text("The colour and its name are picked up from the photo. Tap the circle to change them.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorDot(hex, 40) { showPicker = true }
                LabeledField("Colour name", name, { name = it }, modifier = Modifier.weight(1f))
            }
            LabeledField("Colour SKU (blank = automatic)", sku, { sku = it })
            if (!hasSizes) LabeledField("Stock", stock, { stock = it }, number = true)
            Button(
                onClick = {
                    onAdd(name, hex, sku, stock, photo)
                    name = ""; hex = "#14603a"; sku = ""; stock = "1"; photo = null; photoUri = null
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add colour") }
        }
    }
}

/** One photo in "Add several colours at once". mode: "new", "existing:<colour id>" or "batch:<index of an earlier card>". */
private class BatchItem(val uri: Uri, val jpeg: ByteArray, hex: String, name: String) {
    var hex by mutableStateOf(hex)
    var name by mutableStateOf(name)
    var stock by mutableStateOf("1")
    var mode by mutableStateOf("new")
    var real by mutableStateOf(true)
    var status by mutableStateOf("")
    var done by mutableStateOf(false)
}

/**
 * Like "Add several colours at once" on the website: pick many photos in one
 * go. Each becomes a new colour (colour and name picked up from the photo),
 * or another photo of a colour above it in this list, or an extra photo of a
 * colour the product already has. Then "Add all colours" saves them in order.
 */
@Composable
private fun BatchColorsCard(viewModel: JsonLoaderViewModel, productId: Int, hasSizes: Boolean, existing: List<JsonObject>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<BatchItem>() }
    var reading by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var pickerFor by remember { mutableStateOf<BatchItem?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            reading = true
            scope.launch {
                for (u in uris.take(30)) {
                    val bytes = withContext(Dispatchers.IO) { ImageUtil.toJpeg(context, u) } ?: continue
                    val rgb = withContext(Dispatchers.IO) { dominantColorOf(context, u) } ?: 0x14603A
                    items.add(BatchItem(u, bytes, ColorTools.toHex(rgb), ColorTools.name(rgb)))
                }
                reading = false
            }
        }
    }

    val pf = pickerFor
    if (pf != null) {
        ColorPickerDialog(
            initialHex = pf.hex,
            photoUrl = null,
            onPick = { h, suggested -> pf.hex = h; pf.name = suggested; pickerFor = null },
            onDismiss = { pickerFor = null }
        )
    }

    fun runAll() {
        if (running) return
        running = true
        scope.launch {
            viewModel.setBusy(true)
            val repo = viewModel.repository()
            val resolved = HashMap<Int, Int>()
            var added = 0
            for ((idx, it) in items.withIndex()) {
                if (it.done) continue
                it.status = "Adding..."
                val mode = it.mode
                var cid = 0
                if (mode == "new") {
                    if (it.name.isBlank()) { it.status = "Name needed"; continue }
                    val r = repo.postJson(
                        "product-colors.php",
                        com.trelegantboutique.admin.data.jsonOf(
                            "action" to "color_add", "product_id" to productId,
                            "name" to it.name, "hex" to it.hex, "stock" to (if (hasSizes) "0" else it.stock)
                        )
                    )
                    if (r is ApiResult.Success) cid = r.data.int("color_id") else { it.status = (r as ApiResult.Failure).message; continue }
                    val up = repo.uploadPhoto(
                        "product-photo.php",
                        mapOf("product_id" to productId.toString(), "kind" to "color", "color_id" to cid.toString(), "real" to (if (it.real) "1" else "")),
                        it.jpeg
                    )
                    resolved[idx] = cid
                    it.status = if (up is ApiResult.Success) "Added" else "Colour added, photo failed"
                } else {
                    cid = if (mode.startsWith("existing:")) mode.removePrefix("existing:").toIntOrNull() ?: 0
                    else resolved[mode.removePrefix("batch:").toIntOrNull() ?: -1] ?: 0
                    if (cid <= 0) { it.status = "Its colour was not added"; continue }
                    val up = repo.uploadPhoto(
                        "product-photo.php",
                        mapOf("product_id" to productId.toString(), "kind" to "extra", "color_id" to cid.toString(), "real" to (if (it.real) "1" else "")),
                        it.jpeg
                    )
                    it.status = if (up is ApiResult.Success) "Added" else "Photo failed"
                }
                if (it.status == "Added") { it.done = true; added++ }
            }
            viewModel.setBusy(false)
            running = false
            viewModel.showMessage("$added added.")
            items.removeAll { x -> x.done }
            viewModel.reloadQuiet()
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add several colours at once", fontWeight = FontWeight.Bold)
            Text(
                "Pick many photos together: each one becomes a colour (colour and name picked up from the photo), " +
                    "or set it as another photo of a colour. Put each main photo before the other photos of the same colour.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = { pick.launch("image/*") }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
                Text(if (reading) "Reading photos..." else "Choose photos")
            }
            for ((idx, it) in items.withIndex()) {
                val options = ArrayList<Pair<String, String>>()
                options.add("new" to "New colour")
                for ((j, other) in items.withIndex()) {
                    if (j < idx && other.mode == "new") options.add("batch:$j" to "More photo of: " + other.name.ifBlank { "colour " + (j + 1) })
                }
                for (e in existing) options.add("existing:" + e.int("id") to "Extra photo of: " + e.str("name"))
                val label = options.firstOrNull { o -> o.first == it.mode }?.second ?: "New colour"
                Card(Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, com.trelegantboutique.admin.ui.theme.Line)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        AsyncImage(model = it.uri, contentDescription = null, modifier = Modifier.size(78.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChoiceField("This photo is", label, options.map { o -> o.second }, { picked ->
                                it.mode = options.firstOrNull { o -> o.second == picked }?.first ?: "new"
                            })
                            if (it.mode == "new") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ColorDot(it.hex, 34) { pickerFor = it }
                                    LabeledField("Colour name", it.name, { v -> it.name = v }, modifier = Modifier.weight(1f))
                                }
                                if (!hasSizes) LabeledField("Qty", it.stock, { v -> it.stock = v }, number = true)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = it.real, onCheckedChange = { v -> it.real = v })
                                Text("Real photo (not AI)", style = MaterialTheme.typography.bodySmall)
                            }
                            if (it.status.isNotBlank()) Text(it.status, style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            IconButton(onClick = { if (idx > 0) { val t = items[idx - 1]; items[idx - 1] = items[idx]; items[idx] = t } }, enabled = idx > 0 && !running) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(onClick = { if (idx < items.size - 1) { val t = items[idx + 1]; items[idx + 1] = items[idx]; items[idx] = t } }, enabled = idx < items.size - 1 && !running) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(onClick = { items.removeAt(idx) }, enabled = !running) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
            if (items.isNotEmpty()) {
                Button(onClick = { runAll() }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
                    Text(if (running) "Adding..." else "Add all colours (" + items.size + ")")
                }
            }
        }
    }
}

@Composable
private fun AddSizeToAll(presets: List<String>, onAdd: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Same size on every colour", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceField("Size", label, presets, { label = it }, modifier = Modifier.weight(1f))
                LabeledField("Stock", stock, { stock = it }, modifier = Modifier.weight(1f), number = true)
            }
            OutlinedButton(onClick = { onAdd(label, stock) }, modifier = Modifier.fillMaxWidth()) {
                Text("Apply to all colours")
            }
        }
    }
}
