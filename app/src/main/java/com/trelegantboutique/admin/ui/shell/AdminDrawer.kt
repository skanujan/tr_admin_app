package com.trelegantboutique.admin.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trelegantboutique.admin.ui.theme.Gold
import com.trelegantboutique.admin.ui.theme.Marcellus
import com.trelegantboutique.admin.ui.theme.MaroonDeep
import com.trelegantboutique.admin.ui.theme.SideText

/** Which item is open now, so it can be highlighted gold like the website. */
class ActiveItem(val route: String?, val web: String?)

private fun isActive(item: MenuItem, active: ActiveItem): Boolean {
    if (item.route != null) return item.route == active.route
    if (item.web != null) return item.web == active.web
    return false
}

/** The deep green side menu, matching the website admin sidebar. */
@Composable
fun AdminDrawerContent(
    siteName: String,
    adminName: String,
    perms: Set<String>,
    active: ActiveItem,
    inboxCount: Int,
    onOpen: (MenuItem) -> Unit,
    onSignOut: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val sections = ADMIN_MENU.mapNotNull { s ->
        val visible = s.items.filter { canSee(perms, it.perm) }
        if (visible.isEmpty()) null else MenuSection(s.label, s.color, s.icon, visible, s.single)
    }

    ModalDrawerSheet(
        drawerContainerColor = MaroonDeep,
        drawerContentColor = SideText,
        modifier = Modifier.width(300.dp)
    ) {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp)) {
                Text(siteName, fontFamily = Marcellus, fontSize = 22.sp, color = Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Admin", fontSize = 12.sp, color = SideText.copy(alpha = 0.7f))
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Find a page...", color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickButton("+ Sale", Modifier.weight(1f)) {
                        sections.flatMap { it.items }.firstOrNull { it.route == Native.NEW_SALE }?.let(onOpen)
                    }
                    QuickButton("Orders", Modifier.weight(1f)) {
                        sections.flatMap { it.items }.firstOrNull { it.route == Native.ORDERS }?.let(onOpen)
                    }
                    QuickButton(if (inboxCount > 0) "Inbox $inboxCount" else "Inbox", Modifier.weight(1f)) {
                        sections.flatMap { it.items }.firstOrNull { it.web == "admin/whatsapp.php" }?.let(onOpen)
                    }
                }
            }

            LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                val q = query.trim().lowercase()
                if (q.isNotEmpty()) {
                    for (s in sections) {
                        for (it in s.items) {
                            val label = if (s.single || s.label == it.label) it.label else s.label + " > " + it.label
                            if (label.lowercase().contains(q)) {
                                item { NavRow(label, s.color, s.icon, isActive(it, active)) { onOpen(it) } }
                            }
                        }
                    }
                } else {
                    for (s in sections) {
                        if (s.single) {
                            val it = s.items.first()
                            item { NavRow(s.label, s.color, s.icon, isActive(it, active)) { onOpen(it) } }
                        } else {
                            val hasActive = s.items.any { isActive(it, active) }
                            val open = expanded[s.label] ?: hasActive
                            item {
                                GroupRow(s.label, s.color, s.icon, open, hasActive) { expanded[s.label] = !open }
                            }
                            if (open) {
                                for (it in s.items) {
                                    item { SubRow(it.label, isActive(it, active)) { onOpen(it) } }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            HorizontalDivider(color = SideText.copy(alpha = 0.18f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(adminName, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Sign out", color = Gold, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onSignOut() }.padding(6.dp))
            }
        }
    }
}

@Composable
private fun QuickButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SideText.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SideText)
    ) { Text(text, fontSize = 12.sp, maxLines = 1) }
}

@Composable
private fun NavBadge(color: Color, icon: ImageVector) {
    Box(
        Modifier.size(30.dp).background(color, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun NavRow(label: String, color: Color, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(if (active) Gold else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBadge(color, icon)
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            color = if (active) MaroonDeep else SideText,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun GroupRow(label: String, color: Color, icon: ImageVector, open: Boolean, hasActive: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBadge(color, icon)
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            color = if (hasActive) Color.White else SideText,
            fontWeight = if (hasActive) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (open) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = SideText.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubRow(label: String, active: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 34.dp)) {
        Box(Modifier.width(1.dp).height(38.dp).background(SideText.copy(alpha = 0.18f)))
        Text(
            label,
            color = if (active) MaroonDeep else SideText,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                .fillMaxWidth()
                .background(if (active) Gold else Color.Transparent, RoundedCornerShape(6.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
