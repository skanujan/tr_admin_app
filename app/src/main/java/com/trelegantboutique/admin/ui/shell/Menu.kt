package com.trelegantboutique.admin.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The website admin's left menu (admin/layout-top.php), item for item, in the
 * same order, with the same group colours. [route] opens a screen built into
 * the app; [web] opens that website admin page inside the app, signed in.
 * [perm] is the tick the page needs (admin/auth.php admin_page_perm):
 * "" = everyone, "*" = owner only.
 */
class MenuItem(
    val label: String,
    val perm: String,
    val route: String? = null,
    val web: String? = null,
    val external: Boolean = false
)

class MenuSection(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val items: List<MenuItem>,
    val single: Boolean = false
)

object Native {
    const val DASHBOARD = "dashboard"
    const val NEW_SALE = "new_sale"
    const val ORDERS = "orders"
    const val DELIVERIES = "deliveries"
    const val PREORDERS = "preorders"
    const val PROMOS = "promo_codes"
    const val OFFER = "site_offer"
    const val PRODUCTS = "products"
    const val COLLECTIONS = "collections"
    const val STOCK = "stock_report"
}

private fun w(label: String, path: String, perm: String) = MenuItem(label, perm, web = path)
private fun n(label: String, route: String, perm: String) = MenuItem(label, perm, route = route)

val ADMIN_MENU: List<MenuSection> = listOf(
    MenuSection("Dashboard", Color(0xFF3B6FD6), Icons.Filled.Dashboard, listOf(n("Dashboard", Native.DASHBOARD, "")), single = true),
    MenuSection("New sale", Color(0xFF1F8A4C), Icons.Filled.AddShoppingCart, listOf(n("New sale", Native.NEW_SALE, "orders")), single = true),
    MenuSection("Orders", Color(0xFFC26A12), Icons.Filled.Inventory2, listOf(n("Orders", Native.ORDERS, "orders")), single = true),
    MenuSection(
        "Sales", Color(0xFF5B46C9), Icons.Filled.LocalShipping, listOf(
            n("Orders", Native.ORDERS, "orders"),
            n("Deliveries", Native.DELIVERIES, "orders"),
            w("Track order", "admin/courier-track.php", "orders"),
            w("Domex settlement", "admin/domex-settlement.php", "orders"),
            w("Courier (Domex)", "admin/courier.php", "orders"),
            n("Preorders", Native.PREORDERS, "orders"),
            n("Promo codes", Native.PROMOS, "orders"),
            n("Deepawali offer", Native.OFFER, "*"),
            w("App notifications", "admin/app-push.php", "*"),
            w("Promotion banners", "admin/banners.php", "orders")
        )
    ),
    MenuSection(
        "WhatsApp", Color(0xFF128C7E), Icons.Filled.Chat, listOf(
            w("Inbox", "admin/whatsapp.php", "whatsapp"),
            w("Phone app", "admin/wa-app/index.php", "whatsapp"),
            w("Templates", "admin/wa-templates.php", "whatsapp"),
            w("Carousel templates", "admin/wa-carousel.php", "whatsapp"),
            w("Send to customers", "admin/wa-send.php", "whatsapp"),
            w("Broadcast", "admin/whatsapp-broadcast.php", "whatsapp"),
            w("Bot and auto replies", "admin/wa-bot.php", "whatsapp"),
            w("WhatsApp settings", "admin/settings.php?tab=whatsapp", "settings"),
            w("Numbers", "admin/wa-accounts.php", "whatsapp"),
            w("Meta Business", "admin/wa-business.php", "whatsapp"),
            w("Catalog and webhook", "admin/wa-catalog.php", "whatsapp")
        )
    ),
    MenuSection(
        "Facebook", Color(0xFF1877F2), Icons.Filled.Forum, listOf(
            w("Messenger and Page AI", "admin/facebook.php", "whatsapp"),
            w("Posts", "admin/fb-posts.php", "reports"),
            w("Linked posts", "admin/fb-linked-posts.php", "*"),
            w("Broadcast", "admin/fb-broadcast.php", "*"),
            w("Create ad", "admin/ad-create.php", "reports"),
            w("Ads results", "admin/ads.php", "reports")
        )
    ),
    MenuSection("Mail", Color(0xFFC0392B), Icons.Filled.Email, listOf(w("Mail", "admin/mail.php", "mail")), single = true),
    MenuSection("Auto messages", Color(0xFF128C7E), Icons.Filled.Sms, listOf(w("Auto messages", "admin/messages.php", "messages")), single = true),
    MenuSection("SMS", Color(0xFF3A86C8), Icons.Filled.PhoneAndroid, listOf(w("SMS", "admin/settings.php?tab=sms", "settings")), single = true),
    MenuSection(
        "Customers", Color(0xFFD6336C), Icons.Filled.People, listOf(
            w("All customers", "admin/customers.php", "customers"),
            w("Resellers", "admin/resellers.php", "customers"),
            w("Enquiries", "admin/enquiries.php", "customers"),
            w("Customer feedback", "admin/feedback.php", "customers")
        )
    ),
    MenuSection(
        "Catalog", Color(0xFFC9821A), Icons.Filled.LocalOffer, listOf(
            n("Products", Native.PRODUCTS, "products"),
            n("Collections", Native.COLLECTIONS, "products"),
            n("Stock report", Native.STOCK, "products"),
            w("Barcodes", "admin/barcodes.php", "products"),
            w("Bulk upload", "admin/import.php", "products"),
            w("Reviews", "admin/reviews.php", "products"),
            w("Damaged stock", "admin/damaged-stock.php", "products"),
            w("Optimize images", "admin/optimize-images.php", "products")
        )
    ),
    MenuSection(
        "Insights", Color(0xFF0F9488), Icons.Filled.BarChart, listOf(
            w("Analytics", "admin/analytics.php", "reports"),
            w("Reports", "admin/reports.php", "reports"),
            w("Expenses", "admin/expenses.php", "reports")
        )
    ),
    MenuSection(
        "Setup", Color(0xFF6F5F5A), Icons.Filled.Tune, listOf(
            w("Branding and contact", "admin/settings.php?tab=general", "settings"),
            w("Shop and payments", "admin/settings.php?tab=shop", "settings"),
            w("Orders and delivery", "admin/settings.php?tab=orders", "settings"),
            w("Website content", "admin/settings.php?tab=content", "settings"),
            w("Admin account", "admin/settings.php?tab=account", "settings"),
            w("Website text", "admin/text.php", "settings"),
            w("Share tracking", "admin/share-tracking.php", "settings"),
            w("Admin App", "admin/app-setup.php", "settings"),
            w("Users and levels", "admin/users.php", "users"),
            w("My account", "admin/my-account.php", "")
        )
    ),
    MenuSection("View store", Color(0xFF8A7A52), Icons.Filled.Storefront, listOf(MenuItem("View store", "", external = true)), single = true)
)

/** Same rule as the website: '*' means everything (owner). */
fun canSee(perms: Set<String>, perm: String): Boolean {
    if (perm == "") return true
    if (perms.contains("*")) return true
    return perm != "*" && perms.contains(perm)
}
