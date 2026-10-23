@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trelegantboutique.admin.ui.theme.*

/** Opens the side menu. Provided once at the root, used by every top bar. */
val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }

/** True while the side menu is open (pages then leave the Back button to the menu). */
val LocalDrawerIsOpen = compositionLocalOf { false }

/** Page frame like the website admin: deep green bar, Marcellus title.
 *  With [onBack] null it shows the menu button (a main page); otherwise a back arrow. */
@Composable
fun PageScaffold(
    title: String,
    onBack: (() -> Unit)?,
    snackbar: SnackbarHostState,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val openDrawer = LocalOpenDrawer.current
    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontFamily = Marcellus, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    } else {
                        IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, contentDescription = "Menu") }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonDeep,
                    titleContentColor = Color.White,
                    navigationIconContentColor = SideText,
                    actionIconContentColor = SideText
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

/** White rounded card with the thin green-grey border of the website's .card. */
@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = CardDefaults.cardColors(containerColor = Color.White)
    val border = BorderStroke(1.dp, Line)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(14.dp)) {
            if (title != null) {
                Text(title, style = CardTitle)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = inner)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = inner)
    }
}

/** Coloured status pill, same colours as the website (order_status_style). */
@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

fun hexColor(h: String, fallback: Color = Color(0xFFEEEEEE)): Color {
    return try {
        if (h.isBlank()) fallback else Color(android.graphics.Color.parseColor(h))
    } catch (e: Exception) {
        fallback
    }
}

/** Website order status colours: background, text. */
fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "Pending" -> Color(0xFFFDF0D8) to Color(0xFF9A6B14)
    "Confirmed" -> Color(0xFFDCEAFF) to Color(0xFF1F5FB0)
    "Shipped" -> Color(0xFFE3E0FF) to Color(0xFF5B46C9)
    "Delivered" -> Color(0xFFC8F2D3) to Color(0xFF13703A)
    "Returned" -> Color(0xFFFDE3CC) to Color(0xFFA5541F)
    "Cancelled" -> Color(0xFFFBE2E6) to Color(0xFFB02A3E)
    else -> Color(0xFFEEEEEE) to Color(0xFF555555)
}

/** Website order status names (Shipped is shown as Dispatched). */
fun statusLabel(status: String): String = if (status == "Shipped") "Dispatched" else status

/** Shows [state].message once in the snackbar, and leaves for the login screen if the session ended. */
@Composable
fun HandleState(
    state: JsonUiState,
    snackbar: SnackbarHostState,
    viewModel: JsonLoaderViewModel,
    onSessionExpired: () -> Unit
) {
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(state.message) {
        val m = state.message
        if (m != null) {
            snackbar.showSnackbar(m)
            viewModel.messageShown()
        }
    }
}

@Composable
fun LoadingOrError(loading: Boolean, error: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) {
            CircularProgressIndicator(color = DeepGreen)
        } else if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    phone: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Decimal else if (phone) KeyboardType.Phone else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedBorderColor = Line,
            focusedBorderColor = DeepGreen
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = DeepGreen)
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = CardTitle, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Marcellus) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A drop-down of fixed choices. */
@Composable
fun ChoiceField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = !open }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Line,
                focusedBorderColor = DeepGreen
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (o in options) {
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

/** The search box used on list pages. */
@Composable
fun SearchBox(value: String, onChange: (String) -> Unit, hint: String, onSearch: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint) },
        singleLine = true,
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedBorderColor = Line,
            focusedBorderColor = DeepGreen
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
