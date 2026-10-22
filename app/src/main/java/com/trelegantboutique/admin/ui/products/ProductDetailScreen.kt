package com.trelegantboutique.admin.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(viewModel: ProductDetailViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::load) { Text("Retry") }
                }
                state.product != null -> ProductEditForm(state.product!!, state, onSave = viewModel::save)
            }
        }
    }
}

@Composable
private fun ProductEditForm(
    product: com.trelegantboutique.admin.data.ProductDetail,
    state: ProductDetailUiState,
    onSave: (active: Boolean, stock: Int, price: Double, salePrice: Double?) -> Unit
) {
    var active by remember(product.id) { mutableStateOf(product.active ?: true) }
    var stockText by remember(product.id) { mutableStateOf((product.stock ?: 0).toString()) }
    var priceText by remember(product.id) { mutableStateOf((product.price ?: 0.0).toString()) }
    var salePriceText by remember(product.id) { mutableStateOf(product.salePrice?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AsyncImage(
            model = product.image,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        product.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Active", modifier = Modifier.weight(1f))
            Switch(checked = active, onCheckedChange = { active = it })
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = stockText,
            onValueChange = { stockText = it },
            label = { Text("Stock") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = salePriceText,
            onValueChange = { salePriceText = it },
            label = { Text("Sale price (leave blank for none)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!state.colors.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                state.colors.forEach { color ->
                    AssistChip(onClick = {}, label = { Text(color.name ?: color.hexCode ?: "") })
                }
            }
        }

        if (state.saveError != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.saveError, color = MaterialTheme.colorScheme.error)
        }
        if (state.saved) {
            Spacer(Modifier.height(8.dp))
            Text("Saved.", color = com.trelegantboutique.admin.ui.theme.DeepGreen)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val stock = stockText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val price = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                val salePrice = salePriceText.toDoubleOrNull()?.takeIf { it > 0 }
                onSave(active, stock, price, salePrice)
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save changes")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
