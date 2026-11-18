package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.StatusPill
import com.example.ui.components.orderStatusColors
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*
import com.example.util.PdfInvoiceGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersTrackingScreen(
    vm: StoreViewModel,
    onBrowseShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang by vm.language.collectAsStateWithLifecycle()
    val orders by vm.userOrders.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var pendingInvoiceOrder by remember { mutableStateOf<OrderEntity?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val targetOrder = pendingInvoiceOrder
        if (uri != null && targetOrder != null) {
            scope.launch {
                val items = vm.getOrderItems(targetOrder.id)
                PdfInvoiceGenerator.writeInvoicePdf(context, uri, targetOrder, items, settings.supportEmail).fold(
                    onSuccess = { vm.emitMessage("PDF Invoice saved for ${targetOrder.orderNumber}") },
                    onFailure = { vm.emitMessage(it.message ?: "Failed to save PDF") }
                )
            }
        }
        pendingInvoiceOrder = null
    }

    if (orders.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(SoftGrayBg).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(64.dp))
                Text(Strings.tr(lang, "No orders yet", "এখনো কোনো অর্ডার নেই"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    Strings.tr(lang, "Track live courier delivery, payment verification, and download PDF invoices here.", "এখানে অর্ডার ট্র্যাক করুন এবং PDF ইনভয়েস ডাউনলোড করুন।"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(onClick = onBrowseShop, colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)) {
                    Text(Strings.tr(lang, "Browse Products", "পণ্য দেখুন"))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(SoftGrayBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(Strings.tr(lang, "My Orders & Tracking", "আমার অর্ডার ও ট্র্যাকিং"), style = MaterialTheme.typography.headlineSmall)
        }
        items(orders, key = { it.id }) { order ->
            OrderTrackingCard(
                order = order,
                language = lang,
                loadItems = { vm.getOrderItems(order.id) },
                onDownloadInvoice = {
                    pendingInvoiceOrder = order
                    pdfLauncher.launch("Invoice_${order.orderNumber}.pdf")
                },
                onCancelOrReturn = { isReturn -> vm.cancelOrReturnOrder(order, isReturn) }
            )
        }
    }
}

@Composable
private fun OrderTrackingCard(
    order: OrderEntity,
    language: AppLanguage,
    loadItems: suspend () -> List<OrderItemEntity>,
    onDownloadInvoice: () -> Unit,
    onCancelOrReturn: (Boolean) -> Unit
) {
    val items by produceState<List<OrderItemEntity>>(initialValue = emptyList(), order.id) {
        value = loadItems()
    }
    val dateStr = remember(order.createdAtMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(order.createdAtMillis))
    }
    val (ordBg, ordFg) = orderStatusColors(order.orderStatus)
    val (payBg, payFg) = orderStatusColors(order.paymentStatus)

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("#${order.orderNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                StatusPill(order.orderStatus, ordBg, ordFg)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill("${order.paymentMethod}: ${order.paymentStatus}", payBg, payFg)
                Text(Strings.formatBdt(order.totalAmount, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Courier & Tracking Info
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Courier: ${order.courierName}", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (order.trackingNumber.isNotBlank()) "Tracking: ${order.trackingNumber}" else "Tracking ID will appear once shipped",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Timeline Stepper
            if (order.orderStatus !in listOf("CANCELLED", "RETURNED")) {
                val steps = listOf("PENDING", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED")
                val activeIdx = steps.indexOf(order.orderStatus).coerceAtLeast(0)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    steps.forEachIndexed { idx, step ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (idx <= activeIdx) DeepBlue else CardBorder)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                step.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (idx <= activeIdx) DeepBlue else TextMuted
                            )
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                HorizontalDivider()
                items.forEach { it ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.quantity}x ${if (language == AppLanguage.BN) it.productTitleBn else it.productTitleEn}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(Strings.formatBdt(it.unitPrice * it.quantity, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDownloadInvoice,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("download_invoice_${order.id}")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.tr(language, "PDF Invoice", "ইনভয়েস"))
                }
                if (order.orderStatus in listOf("PENDING", "CONFIRMED")) {
                    OutlinedButton(
                        onClick = { onCancelOrReturn(false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(Strings.tr(language, "Cancel", "বাতিল"))
                    }
                } else if (order.orderStatus == "DELIVERED") {
                    OutlinedButton(
                        onClick = { onCancelOrReturn(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibrantOrangeDark),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(Strings.tr(language, "Return", "রিটার্ন"))
                    }
                }
            }
        }
    }
}
