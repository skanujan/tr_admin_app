package com.trelegantboutique.admin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
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
import com.trelegantboutique.admin.ui.theme.TrAdminTheme

private object Routes {
    const val LOGIN = "login"
    const val UNLOCK = "unlock"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val PRODUCTS = "products"
    const val ORDER_DETAIL = "order_detail/{id}"
    const val PRODUCT_DETAIL = "product_detail/{id}"
    fun orderDetail(id: Int) = "order_detail/$id"
    fun productDetail(id: Int) = "product_detail/$id"
}

private val BOTTOM_TABS = listOf(Routes.DASHBOARD, Routes.ORDERS, Routes.PRODUCTS)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AdminApp

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
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.PRODUCTS) {
                val vm: ProductsListViewModel = viewModel(factory = viewModelFactory {
                    initializer { ProductsListViewModel(repo) }
                })
                ProductsListScreen(
                    viewModel = vm,
                    onOpenProduct = { id -> navController.navigate(Routes.productDetail(id)) },
                    onSessionExpired = { goToSessionExpired() }
                )
            }
            composable(Routes.ORDER_DETAIL) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val vm: OrderDetailViewModel = viewModel(factory = viewModelFactory {
                    initializer { OrderDetailViewModel(repo, id) }
                })
                OrderDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSessionExpired = { goToSessionExpired() }
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
