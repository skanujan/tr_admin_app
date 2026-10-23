@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.catalog

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
import com.trelegantboutique.admin.data.int
import com.trelegantboutique.admin.data.jsonOf
import com.trelegantboutique.admin.data.list
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(viewModel: JsonLoaderViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    HandleState(state, snackbar, viewModel, onSessionExpired)

    // editing == null: closed. editing.int("id") == 0: adding a new one.
    var editing by remember { mutableStateOf<JsonObject?>(null) }

    PageScaffold(
        title = "Collections",
        onBack = onBack,
        snackbar = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = JsonObject() }) { Icon(Icons.Filled.Add, contentDescription = "Add collection") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.error != null) {
                LoadingOrError(state.loading, state.error, { viewModel.load() })
            } else {
                val all = state.data?.list("collections") ?: emptyList()
                val names = HashMap<Int, String>()
                for (c in all) names[c.int("id")] = c.str("name")
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(all) { c ->
                        Card(onClick = { editing = c }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.str("name"), fontWeight = FontWeight.Bold)
                                    val parent = c.int("parent_id")
                                    if (parent > 0) Text("Inside: " + (names[parent] ?: ""), style = MaterialTheme.typography.bodySmall)
                                    Text("${c.int("products")} products", style = MaterialTheme.typography.bodySmall)
                                }
                                if (!c.bool("active")) AssistChip(onClick = {}, label = { Text("Hidden") })
                            }
                        }
                    }
                }
            }
        }
    }

    val cur = editing
    if (cur != null) {
        val isNew = cur.int("id") == 0
        var name by remember(cur) { mutableStateOf(cur.str("name")) }
        var tagline by remember(cur) { mutableStateOf(cur.str("tagline")) }
        var sort by remember(cur) { mutableStateOf(if (isNew) "0" else cur.int("sort_order").toString()) }
        var parent by remember(cur) { mutableStateOf(cur.int("parent_id")) }
        var active by remember(cur) { mutableStateOf(if (isNew) true else cur.bool("active")) }
        var confirmDelete by remember(cur) { mutableStateOf(false) }
        val all = state.data?.list("collections") ?: emptyList()
        val mains = all.filter { it.int("parent_id") == 0 && it.int("id") != cur.int("id") }
        val parentName = if (parent == 0) "None (main collection)" else (all.firstOrNull { it.int("id") == parent }?.str("name") ?: "None (main collection)")

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isNew) "New collection" else "Edit collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Name", name, { name = it })
                    LabeledField("Tagline (optional)", tagline, { tagline = it })
                    LabeledField("Order number", sort, { sort = it }, number = true)
                    ChoiceField(
                        "Inside collection",
                        parentName,
                        listOf("None (main collection)") + mains.map { it.str("name") },
                        { picked ->
                            parent = mains.firstOrNull { it.str("name") == picked }?.int("id") ?: 0
                        }
                    )
                    SwitchRow("Visible on the website", active) { active = it }
                    if (!isNew) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("Delete this collection", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.post(
                        "categories.php",
                        jsonOf(
                            "action" to (if (isNew) "add" else "edit"),
                            "id" to cur.int("id"),
                            "name" to name,
                            "tagline" to tagline,
                            "sort_order" to (sort.toIntOrNull() ?: 0),
                            "parent_id" to parent,
                            "active" to active
                        )
                    )
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete collection?",
                text = "Only empty collections can be deleted.",
                confirmLabel = "Delete",
                onConfirm = {
                    viewModel.post("categories.php", jsonOf("action" to "delete", "id" to cur.int("id")))
                    confirmDelete = false
                    editing = null
                },
                onDismiss = { confirmDelete = false }
            )
        }
    }
}
