package com.trelegantboutique.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.trelegantboutique.admin.auth.BiometricHelper
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.SessionStore
import com.trelegantboutique.admin.ui.dashboard.DashboardScreen
import com.trelegantboutique.admin.ui.dashboard.DashboardViewModel
import com.trelegantboutique.admin.ui.login.LoginScreen
import com.trelegantboutique.admin.ui.login.LoginViewModel
import com.trelegantboutique.admin.ui.login.UnlockScreen
import com.trelegantboutique.admin.ui.orders.OrderDetailScreen
import com.trelegantboutique.admin.ui.orders.OrderDetailViewModel
import com.trelegantboutique.admin.ui.orders.OrdersListScreen
import com.trelegantboutique.admin.ui.orders.OrdersListViewModel
import com.trelegantboutique.admin.ui.products.ProductDetailScreen
import com.trelegantboutique.admin.ui.products.ProductDetailViewModel
import com.trelegantboutique.admin.ui.products.ProductsListScreen
import com.trelegantboutique.admin.ui.products.ProductsListViewModel
import com.trelegantboutique.admin.ui.catalog.CollectionsScreen
import com.trelegantboutique.admin.ui.catalog.ProductEditScreen
import com.trelegantboutique.admin.ui.catalog.StockReportScreen
import com.trelegantboutique.admin.ui.common.JsonLoaderViewModel
import com.trelegantboutique.admin.ui.more.MoreScreen
import com.trelegantboutique.admin.ui.sales.CourierScreen
import com.trelegantboutique.admin.ui.sales.DeliveriesScreen
import com.trelegantboutique.admin.ui.sales.NewSaleScreen
import com.trelegantboutique.admin.ui.sales.NewSaleViewModel
import com.trelegantboutique.admin.ui.sales.OfferScreen
import com.trelegantboutique.admin.ui.sales.PreordersScreen
import com.trelegantboutique.admin.ui.sales.PromoCodesScreen
import com.trelegantboutique.admin.ui.theme.TrAdminTheme

private object Routes {
    const val LOGIN = "login"
    const val UNLOCK = "unlock"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val PRODUCTS = "products"
    const val ORDER_DETAIL = "order_detail/{id}"
    const val PRODUCT_DETAIL = "product_detail/{id}"
    const val MORE = "more"
    const val STOCK = "stock_report"
    const val COLLECTIONS = "collections"
    const val NEW_SALE = "new_sale"
    const val DELIVERIES = "deliveries"
    const val PREORDERS = "preorders"
    const val PROMOS = "promo_codes"
    const val OFFER = "site_offer"
    const val PRODUCT_EDIT = "product_edit/{id}"
    const val COURIER = "courier/{id}"
    fun productEdit(id: Int) = "product_edit/$id"
    fun courier(id: Int) = "courier/$id"
    fun orderDetail(id: Int) = "order_detail/$id"
    fun productDetail(id: Int) = "product_detail/$id"
}

private val BOTTOM_TABS = listOf(Routes.DASHBOARD, Routes.ORDERS, Routes.PRODUCTS, Routes.MORE)

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
                Surface {
                    AdminAppRoot(app.session, app.repository, this)
                }
            }
        }
    }
}

@Composable
private fun AdminAppRoot(session: SessionStore, repo: ApiRepository, activity: FragmentActivity) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val startDestination = remember {
        when {
            !session.isSignedIn -> Routes.LOGIN
            session.biometricEnabled && BiometricHelper.availability(activity) == BiometricHelper.Availability.AVAILABLE -> Routes.UNLOCK
            else -> Routes.DASHBOARD
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_TABS

    fun goToSessionExpired() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.DASHBOARD,
                        onClick = { navigateToTab(navController, Routes.DASHBOARD) },
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ORDERS,
                        onClick = { navigateToTab(navController, Routes.ORDERS) },
                        icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                        label = { Text("Orders") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PRODUCTS,
                        onClick = { navigateToTab(navController, Routes.PRODUCTS) },
                        icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                        label = { Text("Products") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.MORE,
                        onClick = { navigateToTab(navController, Routes.MORE) },
                        icon = { Icon(Icons.Filled.Menu, contentDescription = null) },
                        label = { Text("Menu") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(factory = viewModelFactory {
                    initializer { LoginViewModel(repo, session) }
                })
                LoginScreen(viewModel = vm, onLoggedIn = {
                    // First successful password sign-in on this device enables
                    // the fingerprint unlock, if the phone supports it - this
                    // is what "login gated by fingerprint" means after today.
                    if (BiometricHelper.availability(activity) == BiometricHelper.Availability.AVAILABLE) {
                        session.biometricEnabled = true
                    }
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(Routes.UNLOCK) {
                UnlockScreen(
                    onUnlocked = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.UNLOCK) { inclusive = true }
                        }
                    },
                    onUsePasswordInstead = {
                        session.clear()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                val vm: DashboardViewModel = viewModel(factory = viewModelFactory {
                    initializer { DashboardViewModel(repo) }
                })
                DashboardScreen(viewModel = vm, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.ORDERS) {
                val vm: OrdersListViewModel = viewModel(factory = viewModelFactory {
                    initializer { OrdersListViewModel(repo) }
                })
                OrdersListScreen(
                    viewModel = vm,
                    onOpenOrder = { id -> navController.navigate(Routes.orderDetail(id)) },
                    onNewSale = { navController.navigate(Routes.NEW_SALE) },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.PRODUCTS) {
                val vm: ProductsListViewModel = viewModel(factory = viewModelFactory {
                    initializer { ProductsListViewModel(repo) }
                })
                ProductsListScreen(
                    viewModel = vm,
                    onOpenProduct = { id -> navController.navigate(Routes.productEdit(id)) },
                    onAddProduct = { navController.navigate(Routes.productEdit(0)) },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onProducts = { navigateToTab(navController, Routes.PRODUCTS) },
                    onCollections = { navController.navigate(Routes.COLLECTIONS) },
                    onStock = { navController.navigate(Routes.STOCK) },
                    onNewSale = { navController.navigate(Routes.NEW_SALE) },
                    onOrders = { navigateToTab(navController, Routes.ORDERS) },
                    onDeliveries = { navController.navigate(Routes.DELIVERIES) },
                    onPreorders = { navController.navigate(Routes.PREORDERS) },
                    onPromos = { navController.navigate(Routes.PROMOS) },
                    onOffer = { navController.navigate(Routes.OFFER) },
                    onSignOut = {
                        scope.launch {
                            repo.logout()
                            session.clear()
                            goToSessionExpired()
                        }
                    }
                )
            }
            composable(Routes.STOCK) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "stock-report.php") }
                })
                StockReportScreen(vm, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.COLLECTIONS) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "categories.php") }
                })
                CollectionsScreen(vm, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.PRODUCT_EDIT) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "product-full.php", null, mapOf("id" to id.toString())) }
                })
                ProductEditScreen(vm, id, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.NEW_SALE) {
                val vm: NewSaleViewModel = viewModel(factory = viewModelFactory {
                    initializer { NewSaleViewModel(repo) }
                })
                val picker: JsonLoaderViewModel = viewModel(key = "sale_picker", factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "catalog-variants.php", "products") }
                })
                NewSaleScreen(
                    viewModel = vm,
                    pickerViewModel = picker,
                    onBack = { navController.popBackStack() },
                    onOpenOrder = { oid ->
                        navController.navigate(Routes.orderDetail(oid)) {
                            popUpTo(Routes.NEW_SALE) { inclusive = true }
                        }
                    },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.DELIVERIES) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "deliveries-list.php", "deliveries", mapOf("filter" to "all")) }
                })
                DeliveriesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenOrder = { oid -> navController.navigate(Routes.orderDetail(oid)) },
                    onOpenCourier = { oid -> navController.navigate(Routes.courier(oid)) },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.COURIER) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "courier-info.php", null, mapOf("id" to id.toString())) }
                })
                CourierScreen(vm, id, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.PREORDERS) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "preorders.php", "preorders") }
                })
                val picker: JsonLoaderViewModel = viewModel(key = "preorder_picker", factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "catalog-variants.php", "products") }
                })
                PreordersScreen(vm, picker, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.PROMOS) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "promo-codes.php") }
                })
                PromoCodesScreen(vm, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.OFFER) {
                val vm: JsonLoaderViewModel = viewModel(factory = viewModelFactory {
                    initializer { JsonLoaderViewModel(repo, "offer.php") }
                })
                OfferScreen(vm, onBack = { navController.popBackStack() }, onSessionExpired = { goToSessionExpired() })
            }
            composable(Routes.ORDER_DETAIL) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val vm: OrderDetailViewModel = viewModel(factory = viewModelFactory {
                    initializer { OrderDetailViewModel(repo, id) }
                })
                OrderDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSessionExpired = { goToSessionExpired() },
                    onOpenCourier = { oid -> navController.navigate(Routes.courier(oid)) }
                )
            }
            composable(Routes.PRODUCT_DETAIL) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val vm: ProductDetailViewModel = viewModel(factory = viewModelFactory {
                    initializer { ProductDetailViewModel(repo, id) }
                })
                ProductDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
