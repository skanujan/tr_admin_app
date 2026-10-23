package com.trelegantboutique.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trelegantboutique.admin.auth.BiometricHelper
import com.trelegantboutique.admin.ui.catalog.CollectionsScreen
import com.trelegantboutique.admin.ui.catalog.ProductEditScreen
import com.trelegantboutique.admin.ui.catalog.StockReportScreen
import com.trelegantboutique.admin.ui.common.JsonLoaderViewModel
import com.trelegantboutique.admin.ui.common.LocalDrawerIsOpen
import com.trelegantboutique.admin.ui.common.LocalOpenDrawer
import com.trelegantboutique.admin.ui.dashboard.DashboardScreen
import com.trelegantboutique.admin.ui.login.LoginScreen
import com.trelegantboutique.admin.ui.login.LoginViewModel
import com.trelegantboutique.admin.ui.login.UnlockScreen
import com.trelegantboutique.admin.ui.orders.OrderDetailScreen
import com.trelegantboutique.admin.ui.orders.OrdersListScreen
import com.trelegantboutique.admin.ui.products.ProductsListScreen
import com.trelegantboutique.admin.ui.sales.CourierScreen
import com.trelegantboutique.admin.ui.sales.DeliveriesScreen
import com.trelegantboutique.admin.ui.sales.NewSaleScreen
import com.trelegantboutique.admin.ui.sales.NewSaleViewModel
import com.trelegantboutique.admin.ui.sales.OfferScreen
import com.trelegantboutique.admin.ui.sales.PreordersScreen
import com.trelegantboutique.admin.ui.sales.PromoCodesScreen
import com.trelegantboutique.admin.ui.shell.ActiveItem
import com.trelegantboutique.admin.ui.shell.AdminDrawerContent
import com.trelegantboutique.admin.ui.shell.MenuItem
import com.trelegantboutique.admin.ui.shell.Native
import com.trelegantboutique.admin.ui.shell.WebPageScreen
import com.trelegantboutique.admin.ui.theme.TrAdminTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private object Routes {
    const val LOGIN = "login"
    const val UNLOCK = "unlock"
    const val ORDERS = "orders?status={status}"
    const val ORDER = "order/{id}"
    const val PRODUCT_EDIT = "product_edit/{id}"
    const val COURIER = "courier/{id}"
    const val NEW_SALE = "new_sale?back={back}"
    const val WEB = "web?path={path}&title={title}&back={back}"
    fun orders(status: String) = if (status.isEmpty()) "orders" else "orders?status=" + Uri.encode(status)
    fun order(id: Int) = "order/$id"
    fun productEdit(id: Int) = "product_edit/$id"
    fun courier(id: Int) = "courier/$id"
    fun newSale(back: Boolean) = if (back) "new_sale?back=1" else "new_sale"
    fun web(path: String, title: String, back: Boolean) =
        "web?path=" + Uri.encode(path) + "&title=" + Uri.encode(title) + "&back=" + (if (back) "1" else "0")
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AdminApp

        // Android 13 and newer ask before showing notifications (new order alerts).
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Hand this phone's push token to the server on every start, so new
        // order alerts keep reaching it. Does nothing until Firebase is set up.
        if (app.session.isSignedIn) {
            lifecycleScope.launch {
                try {
                    val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                    app.repository.registerPush(token)
                } catch (e: Throwable) {
                    // No Firebase yet, or offline - fine.
                }
            }
        }

        setContent {
            TrAdminTheme {
                AdminAppRoot(app, this)
            }
        }
    }
}

@Composable
private fun AdminAppRoot(app: AdminApp, activity: FragmentActivity) {
    val session = app.session
    val repo = app.repository
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var inboxCount by remember { mutableStateOf(0) }

    val startDestination = remember {
        when {
            !session.isSignedIn -> Routes.LOGIN
            session.biometricEnabled && BiometricHelper.availability(activity) == BiometricHelper.Availability.AVAILABLE -> Routes.UNLOCK
            else -> Native.DASHBOARD
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val signedInArea = route != null && route != Routes.LOGIN && route != Routes.UNLOCK
    val active = when {
        route == Routes.WEB -> ActiveItem(null, entry?.arguments?.getString("path"))
        route == Routes.ORDERS -> ActiveItem(Native.ORDERS, null)
        route == Routes.NEW_SALE -> ActiveItem(Native.NEW_SALE, null)
        else -> ActiveItem(route, null)
    }
    val baseUrl = session.apiBaseUrl
    val siteHost = Uri.parse(baseUrl).host ?: ""
    val siteRoot = baseUrl.trimEnd('/').removeSuffix("/api") + "/"

    fun goToSessionExpired() {
        scope.launch { drawerState.close() }
        nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
    }

    /** Opening a menu page: it replaces whatever was open, Back returns to the Dashboard. */
    fun navigateTop(r: String) {
        nav.navigate(r) {
            popUpTo(Native.DASHBOARD) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun openMenu(item: MenuItem) {
        scope.launch { drawerState.close() }
        when {
            item.external -> try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(siteRoot)))
            } catch (e: Exception) { }
            item.route == Native.ORDERS -> navigateTop(Routes.orders(""))
            item.route == Native.NEW_SALE -> navigateTop(Routes.newSale(false))
            item.route != null -> navigateTop(item.route)
            item.web != null -> navigateTop(Routes.web(item.web, item.label, false))
        }
    }

    fun signOut() {
        scope.launch {
            drawerState.close()
            repo.logout()
            session.clear()
            try { CookieManager.getInstance().removeAllCookies(null) } catch (e: Exception) { }
            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    val perms = if (session.perms.isEmpty()) setOf("*") else session.perms

    // Back closes the side menu first, like any Android app.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(
        LocalOpenDrawer provides { scope.launch { drawerState.open() } },
        LocalDrawerIsOpen provides drawerState.isOpen
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = signedInArea,
            drawerContent = {
                if (signedInArea) {
                    AdminDrawerContent(
                        siteName = "TR Elegant Boutique",
                        adminName = session.adminName ?: "Admin",
                        perms = perms,
                        active = active,
                        inboxCount = inboxCount,
                        onOpen = { openMenu(it) },
                        onSignOut = { signOut() }
                    )
                }
            }
        ) {
            NavHost(navController = nav, startDestination = startDestination) {
                composable(Routes.LOGIN) {
                    val vm: LoginViewModel = viewModel(factory = viewModelFactory {
                        initializer { LoginViewModel(repo, session) }
                    })
                    LoginScreen(viewModel = vm, onLoggedIn = {
                        if (BiometricHelper.availability(activity) == BiometricHelper.Availability.AVAILABLE) {
                            session.biometricEnabled = true
                        }
                        nav.navigate(Native.DASHBOARD) { popUpTo(0) { inclusive = true } }
                    })
                }
                composable(Routes.UNLOCK) {
                    UnlockScreen(
                        onUnlocked = {
                            nav.navigate(Native.DASHBOARD) { popUpTo(Routes.UNLOCK) { inclusive = true } }
                        },
                        onUsePasswordInstead = {
                            session.clear()
                            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    )
                }

                composable(Native.DASHBOARD) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "dashboard-full.php", null, mapOf("range" to "last30")) }
                    })
                    DashboardScreen(
                        viewModel = vm,
                        onNewSale = { nav.navigate(Routes.newSale(true)) },
                        onOrders = { st -> navigateTop(Routes.orders(st ?: "")) },
                        onProducts = { navigateTop(Native.PRODUCTS) },
                        onOpenOrder = { id -> nav.navigate(Routes.order(id)) },
                        onInboxCount = { inboxCount = it },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(
                    Routes.ORDERS,
                    arguments = listOf(navArgument("status") { type = NavType.StringType; defaultValue = "" })
                ) { e ->
                    val status = e.arguments?.getString("status") ?: ""
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            JsonLoaderViewModel(repo, "orders-list.php", "orders", if (status.isEmpty()) emptyMap() else mapOf("status" to status))
                        }
                    })
                    OrdersListScreen(
                        viewModel = vm,
                        initialStatus = status,
                        onOpenOrder = { id -> nav.navigate(Routes.order(id)) },
                        onNewSale = { nav.navigate(Routes.newSale(true)) },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(Routes.ORDER) { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "orders-detail.php", null, mapOf("id" to id.toString())) }
                    })
                    OrderDetailScreen(
                        viewModel = vm,
                        orderId = id,
                        onBack = { nav.popBackStack() },
                        onOpenCourier = { oid -> nav.navigate(Routes.courier(oid)) },
                        onOpenWeb = { p, t -> nav.navigate(Routes.web(p, t, true)) },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(Native.PRODUCTS) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "products-list.php", "products") }
                    })
                    ProductsListScreen(
                        viewModel = vm,
                        onOpenProduct = { id -> nav.navigate(Routes.productEdit(id)) },
                        onAddProduct = { nav.navigate(Routes.productEdit(0)) },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(Routes.PRODUCT_EDIT) { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "product-full.php", null, mapOf("id" to id.toString())) }
                    })
                    ProductEditScreen(vm, id, onBack = { nav.popBackStack() }, onSessionExpired = { goToSessionExpired() })
                }

                composable(Native.COLLECTIONS) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "categories.php") }
                    })
                    CollectionsScreen(vm, onBack = null, onSessionExpired = { goToSessionExpired() })
                }

                composable(Native.STOCK) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "stock-report.php") }
                    })
                    StockReportScreen(vm, onBack = null, onSessionExpired = { goToSessionExpired() })
                }

                composable(
                    Routes.NEW_SALE,
                    arguments = listOf(navArgument("back") { type = NavType.StringType; defaultValue = "0" })
                ) { e ->
                    val back = e.arguments?.getString("back") == "1"
                    val vm: NewSaleViewModel = viewModel(factory = viewModelFactory {
                        initializer { NewSaleViewModel(repo) }
                    })
                    val picker: JsonLoaderViewModel = viewModel(key = "sale_picker", factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "catalog-variants.php", "products") }
                    })
                    NewSaleScreen(
                        viewModel = vm,
                        pickerViewModel = picker,
                        onBack = if (back) ({ nav.popBackStack(); Unit }) else null,
                        onOpenOrder = { oid ->
                            nav.navigate(Routes.order(oid)) {
                                popUpTo(Routes.NEW_SALE) { inclusive = true }
                            }
                        },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(Native.DELIVERIES) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "deliveries-list.php", "deliveries", mapOf("filter" to "all")) }
                    })
                    DeliveriesScreen(
                        viewModel = vm,
                        onBack = null,
                        onOpenOrder = { oid -> nav.navigate(Routes.order(oid)) },
                        onOpenCourier = { oid -> nav.navigate(Routes.courier(oid)) },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }

                composable(Routes.COURIER) { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "courier-info.php", null, mapOf("id" to id.toString())) }
                    })
                    CourierScreen(vm, id, onBack = { nav.popBackStack() }, onSessionExpired = { goToSessionExpired() })
                }

                composable(Native.PREORDERS) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "preorders.php", "preorders") }
                    })
                    val picker: JsonLoaderViewModel = viewModel(key = "preorder_picker", factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "catalog-variants.php", "products") }
                    })
                    PreordersScreen(vm, picker, onBack = null, onSessionExpired = { goToSessionExpired() })
                }

                composable(Native.PROMOS) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "promo-codes.php") }
                    })
                    PromoCodesScreen(vm, onBack = null, onSessionExpired = { goToSessionExpired() })
                }

                composable(Native.OFFER) {
                    val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                        initializer { JsonLoaderViewModel(repo, "offer.php") }
                    })
                    OfferScreen(vm, onBack = null, onSessionExpired = { goToSessionExpired() })
                }

                composable(
                    Routes.WEB,
                    arguments = listOf(
                        navArgument("path") { type = NavType.StringType; defaultValue = "admin/index.php" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "Admin" },
                        navArgument("back") { type = NavType.StringType; defaultValue = "0" }
                    )
                ) { e ->
                    val path = e.arguments?.getString("path") ?: "admin/index.php"
                    val title = e.arguments?.getString("title") ?: "Admin"
                    val back = e.arguments?.getString("back") == "1"
                    WebPageScreen(
                        repo = repo,
                        siteHost = siteHost,
                        path = path,
                        title = title,
                        showBack = back,
                        onExit = { if (!nav.popBackStack()) activity.finish() },
                        onSessionExpired = { goToSessionExpired() }
                    )
                }
            }
        }
    }
}
