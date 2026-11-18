package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.screens.*
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VibrantOrange

object KenaKataRoutes {
    const val HOME = "home"
    const val SHOP = "shop"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val ACCOUNT = "account"
    const val ACCOUNT_SUB = "account?tab={tab}"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val COMPARE = "compare"
    const val ADMIN = "admin"

    fun productDetail(id: String) = "product/$id"
    fun accountTab(tab: AccountSubTab) = "account?tab=${tab.name}"
}

private data class BottomTabItem(
    val route: String,
    val label: (AppLanguage) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun KenaKataApp(vm: StoreViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val lang by vm.language.collectAsStateWithLifecycle()
    val cartItems by vm.cartItems.collectAsStateWithLifecycle()
    val orders by vm.userOrders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: KenaKataRoutes.HOME

    val bottomTabs = remember {
        listOf(
            BottomTabItem(KenaKataRoutes.HOME, Strings::navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomTabItem(KenaKataRoutes.SHOP, Strings::navShop, Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_shop"),
            BottomTabItem(KenaKataRoutes.CART, Strings::navCart, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_cart"),
            BottomTabItem(KenaKataRoutes.ORDERS, Strings::navOrders, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "nav_orders"),
            BottomTabItem(KenaKataRoutes.ACCOUNT, Strings::navAccount, Icons.Filled.Person, Icons.Outlined.Person, "nav_account")
        )
    }

    val showChrome = currentRoute == KenaKataRoutes.HOME ||
        currentRoute == KenaKataRoutes.SHOP ||
        currentRoute == KenaKataRoutes.CART ||
        currentRoute == KenaKataRoutes.ORDERS ||
        currentRoute.startsWith(KenaKataRoutes.ACCOUNT)

    val totalCartCount = cartItems.sumOf { it.cartItem.quantity }
    val activeOrdersCount = orders.count { it.orderStatus !in listOf("DELIVERED", "CANCELLED", "RETURNED") }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showChrome && !isWide) {
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 4.dp
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = if (tab.route == KenaKataRoutes.ACCOUNT) {
                                currentRoute.startsWith(KenaKataRoutes.ACCOUNT)
                            } else currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    val badgeCount = when (tab.route) {
                                        KenaKataRoutes.CART -> totalCartCount
                                        KenaKataRoutes.ORDERS -> activeOrdersCount
                                        else -> 0
                                    }
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge(containerColor = VibrantOrange) { Text("$badgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label(lang)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label(lang),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBlue,
                                    selectedTextColor = DeepBlue,
                                    indicatorColor = com.example.ui.theme.DeepBlueContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && isWide) {
                    NavigationRail(containerColor = PureWhite) {
                        Spacer(modifier = Modifier.height(12.dp))
                        bottomTabs.forEach { tab ->
                            val selected = if (tab.route == KenaKataRoutes.ACCOUNT) {
                                currentRoute.startsWith(KenaKataRoutes.ACCOUNT)
                            } else currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label(lang)
                                    )
                                },
                                label = { Text(tab.label(lang)) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = KenaKataRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(KenaKataRoutes.HOME) {
                        HomeScreen(
                            vm = vm,
                            onOpenSearch = { navigateTopTab(KenaKataRoutes.SHOP) },
                            onOpenProduct = { id -> navController.navigate(KenaKataRoutes.productDetail(id)) },
                            onOpenCompare = { navController.navigate(KenaKataRoutes.COMPARE) },
                            onOpenNotifications = { navController.navigate(KenaKataRoutes.accountTab(AccountSubTab.NOTIFICATIONS)) },
                            onOpenWishlist = { navController.navigate(KenaKataRoutes.accountTab(AccountSubTab.WISHLIST)) },
                            onOpenSupport = { navController.navigate(KenaKataRoutes.accountTab(AccountSubTab.SUPPORT)) }
                        )
                    }
                    composable(KenaKataRoutes.SHOP) {
                        CatalogSearchScreen(
                            vm = vm,
                            onOpenProduct = { id -> navController.navigate(KenaKataRoutes.productDetail(id)) },
                            onOpenCompare = { navController.navigate(KenaKataRoutes.COMPARE) }
                        )
                    }
                    composable(KenaKataRoutes.CART) {
                        CartCheckoutScreen(
                            vm = vm,
                            onBrowseShop = { navigateTopTab(KenaKataRoutes.SHOP) },
                            onOrderPlacedNavigateToOrders = { navigateTopTab(KenaKataRoutes.ORDERS) }
                        )
                    }
                    composable(KenaKataRoutes.ORDERS) {
                        OrdersTrackingScreen(
                            vm = vm,
                            onBrowseShop = { navigateTopTab(KenaKataRoutes.SHOP) }
                        )
                    }
                    composable(
                        route = KenaKataRoutes.ACCOUNT_SUB,
                        arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = AccountSubTab.PROFILE.name })
                    ) { entry ->
                        val tabArg = entry.arguments?.getString("tab")
                        val subTab = AccountSubTab.entries.firstOrNull { it.name == tabArg } ?: AccountSubTab.PROFILE
                        AccountHubScreen(
                            vm = vm,
                            initialSubTab = subTab,
                            onOpenProduct = { id -> navController.navigate(KenaKataRoutes.productDetail(id)) },
                            onOpenAdminDashboard = { navController.navigate(KenaKataRoutes.ADMIN) }
                        )
                    }
                    composable(
                        route = KenaKataRoutes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { entry ->
                        val pid = entry.arguments?.getString("productId").orEmpty()
                        ProductDetailScreen(
                            productId = pid,
                            vm = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProduct = { id -> navController.navigate(KenaKataRoutes.productDetail(id)) },
                            onGoToCart = { navigateTopTab(KenaKataRoutes.CART) }
                        )
                    }
                    composable(KenaKataRoutes.COMPARE) {
                        CompareScreen(
                            vm = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProduct = { id -> navController.navigate(KenaKataRoutes.productDetail(id)) }
                        )
                    }
                    composable(KenaKataRoutes.ADMIN) {
                        AdminDashboardScreen(
                            vm = vm,
                            onBackToStore = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
