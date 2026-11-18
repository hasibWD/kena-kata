package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.AnalyticsSummary
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.StatusPill
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*

enum class AdminTab(val label: String) {
    OVERVIEW("Overview & Analytics"),
    PRODUCTS("Products & CSV"),
    ORDERS("Orders & Courier"),
    PAYMENTS("Payments"),
    PROMOS("Promos & Support"),
    SETTINGS("Settings, Backup & Logs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    vm: StoreViewModel,
    onBackToStore: () -> Unit
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()

    if (currentUser?.role != "ADMIN") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Restricted Area") },
                    navigationIcon = {
                        IconButton(onClick = onBackToStore) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(56.dp))
                    Text("Owner Admin Authorization Required", style = MaterialTheme.typography.titleLarge)
                    Text("Only the single Kena Kata store owner can access this dashboard.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Button(onClick = onBackToStore, colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)) {
                        Text("Return to Store")
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    val analytics by vm.analytics.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue, titleContentColor = PureWhite, navigationIconContentColor = PureWhite),
                title = {
                    Column {
                        Text("Kena Kata Owner Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Single-Vendor Store Control • ${currentUser?.email}", style = MaterialTheme.typography.labelSmall, color = DeepBlueContainer)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToStore, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Store")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftGrayBg)
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(PureWhite).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AdminTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                AdminTab.OVERVIEW -> AdminOverviewPanel(analytics = analytics, products = products, lang = lang)
                AdminTab.PRODUCTS -> AdminProductsPanel(vm = vm, lang = lang)
                AdminTab.ORDERS -> AdminOrdersPanel(vm = vm, lang = lang)
                AdminTab.PAYMENTS -> AdminPaymentsPanel(vm = vm, lang = lang)
                AdminTab.PROMOS -> AdminPromosAndSupportPanel(vm = vm, lang = lang)
                AdminTab.SETTINGS -> AdminSettingsBackupLogsPanel(vm = vm)
            }
        }
    }
}

@Composable
private fun AdminOverviewPanel(
    analytics: AnalyticsSummary,
    products: List<ProductEntity>,
    lang: AppLanguage
) {
    val lowOrOutProducts = remember(products) { products.filter { it.stockQty <= it.lowStockThreshold } }
    val bestSellers = remember(products) { products.sortedByDescending { it.soldCount }.take(5) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Store KPIs & True Net Profit", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Net Revenue", Strings.formatBdt(analytics.netRevenue, lang), DeepBlue, Modifier.weight(1f))
                KpiCard("Net Profit", Strings.formatBdt(analytics.netProfit, lang), if (analytics.netProfit >= 0) SuccessGreen else ErrorRed, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Today Revenue", Strings.formatBdt(analytics.todayRevenue, lang), VibrantOrange, Modifier.weight(1f))
                KpiCard("30d Revenue", Strings.formatBdt(analytics.monthlyRevenue, lang), DeepBlueDark, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Orders (Pend/Del)", "${analytics.totalOrders} (${analytics.pendingOrders}/${analytics.deliveredOrders})", DeepBlue, Modifier.weight(1f))
                KpiCard("Customers / Prods", "${analytics.totalCustomers} / ${analytics.totalProducts}", SlateTeal, Modifier.weight(1f))
            }
        }

        // Profit Breakdown (Gross Sales vs Net Profit)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Accounting Breakdown (Gross vs Net)", style = MaterialTheme.typography.titleMedium)
                    MetricLine("Gross Sales (incl. delivery)", Strings.formatBdt(analytics.grossSales, lang))
                    MetricLine("Coupon & Loyalty Discounts", "-${Strings.formatBdt(analytics.totalDiscounts, lang)}", WarningAmber)
                    MetricLine("Net Order Revenue", Strings.formatBdt(analytics.netRevenue, lang), DeepBlue)
                    MetricLine("Cost of Goods Sold (COGS)", "-${Strings.formatBdt(analytics.totalCogs, lang)}", ErrorRed)
                    MetricLine("Actual Delivery Expenses", "-${Strings.formatBdt(analytics.totalDeliveryExpenses, lang)}", ErrorRed)
                    MetricLine("Refunds", "-${Strings.formatBdt(analytics.totalRefunds, lang)}", ErrorRed)
                    HorizontalDivider()
                    MetricLine("Net Profit", Strings.formatBdt(analytics.netProfit, lang), SuccessGreen)
                }
            }
        }

        // 7-Day Revenue Bar Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("7-Day Revenue Trend", style = MaterialTheme.typography.titleMedium)
                    val maxBar = (analytics.dailyRevenueBars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(100.0)
                    Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                        val bars = analytics.dailyRevenueBars
                        val step = size.width / bars.size.coerceAtLeast(1)
                        val barW = step * 0.55f
                        bars.forEachIndexed { idx, (_, value) ->
                            val hRatio = (value / maxBar).toFloat().coerceIn(0.05f, 1f)
                            val barH = size.height * hRatio
                            drawRoundRect(
                                color = if (idx == bars.lastIndex) VibrantOrange else DeepBlue,
                                topLeft = Offset(idx * step + (step - barW) / 2f, size.height - barH),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        analytics.dailyRevenueBars.forEach { (lbl, _) ->
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    HorizontalDivider()
                    Text(
                        "Payment Mix: bKash (${analytics.bkashCount}) • Nagad (${analytics.nagadCount}) • COD (${analytics.codCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Low & Out-of-Stock Alerts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stock Alerts (${analytics.lowStockCount} Low, ${analytics.outOfStockCount} Out)",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (lowOrOutProducts.isNotEmpty()) ErrorRed else SuccessGreen
                    )
                    if (lowOrOutProducts.isEmpty()) {
                        Text("All inventory levels are healthy.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        lowOrOutProducts.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${p.titleEn} (${p.sku})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                StatusPill(
                                    text = if (p.stockQty <= 0) "OUT OF STOCK" else "${p.stockQty} left",
                                    backgroundColor = if (p.stockQty <= 0) ErrorContainer else WarningContainer,
                                    contentColor = if (p.stockQty <= 0) ErrorRed else VibrantOrangeDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Best Selling Products
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Best-Selling Products", style = MaterialTheme.typography.titleMedium)
                    bestSellers.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.titleEn, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${p.soldCount} sold • ${Strings.formatBdt(p.effectivePrice, lang)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String, color: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}
