package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.StatusPill
import com.example.ui.components.orderStatusColors
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AdminProductsPanel(vm: StoreViewModel, lang: AppLanguage) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by vm.products.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var csvReport by remember { mutableStateOf<String?>(null) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                vm.adminExportCsv().onSuccess { csv ->
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                    vm.emitMessage("Exported ${products.size} products to CSV")
                }
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                vm.adminImportCsv(text).fold(
                    onSuccess = { res ->
                        csvReport = "Imported ${res.importedProducts.size} products.\n" +
                            if (res.errors.isEmpty()) "0 validation errors." else "Errors:\n" + res.errors.joinToString("\n")
                    },
                    onFailure = { vm.emitMessage(it.message ?: "Import failed") }
                )
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        editingProduct = null
                        showEditor = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.weight(1f).testTag("admin_add_product_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Product")
                }
                OutlinedButton(
                    onClick = { csvExportLauncher.launch("kenakata_products.csv") },
                    modifier = Modifier.testTag("admin_csv_export_button")
                ) { Text("Export CSV") }
                OutlinedButton(
                    onClick = { csvImportLauncher.launch(arrayOf("text/*", "text/csv")) },
                    modifier = Modifier.testTag("admin_csv_import_button")
                ) { Text("Import CSV") }
            }
        }

        csvReport?.let { rep ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DeepBlueContainer), shape = RoundedCornerShape(12.dp)) {
                    Text(rep, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
        }

        items(products, key = { it.id }) { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(p.titleEn, style = MaterialTheme.typography.titleSmall)
                        Text("SKU: ${p.sku} • ${Strings.formatBdt(p.effectivePrice, lang)} (Cost: ৳${p.costPrice.toInt()})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "Stock: ${p.stockQty} (Alert <= ${p.lowStockThreshold})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (p.stockQty <= p.lowStockThreshold) ErrorRed else SuccessGreen
                        )
                    }
                    Row {
                        IconButton(onClick = { editingProduct = p; showEditor = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = DeepBlue)
                        }
                        IconButton(onClick = { vm.adminDeleteProduct(p) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            initial = editingProduct,
            categories = categories,
            onDismiss = { showEditor = false },
            onSave = { prod, isNew ->
                vm.adminSaveProduct(prod, isNew)
                showEditor = false
            }
        )
    }
}

@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Boolean) -> Unit
) {
    val isNew = initial == null
    var sku by remember { mutableStateOf(initial?.sku ?: "KK-${(1000..9999).random()}") }
    var titleEn by remember { mutableStateOf(initial?.titleEn.orEmpty()) }
    var titleBn by remember { mutableStateOf(initial?.titleBn.orEmpty()) }
    var brand by remember { mutableStateOf(initial?.brand ?: "Kena Kata") }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }
    var regularPrice by remember { mutableStateOf(initial?.regularPrice?.toInt()?.toString() ?: "990") }
    var salePrice by remember { mutableStateOf(initial?.salePrice?.toInt()?.toString() ?: "850") }
    var costPrice by remember { mutableStateOf(initial?.costPrice?.toInt()?.toString() ?: "600") }
    var stockQty by remember { mutableStateOf(initial?.stockQty?.toString() ?: "15") }
    var variantsCsv by remember { mutableStateOf(initial?.variantsCsv ?: "Standard") }
    var descEn by remember { mutableStateOf(initial?.descriptionEn.orEmpty()) }
    var imageUri by remember { mutableStateOf(initial?.imageUri.orEmpty()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Product" else "Edit ${initial?.sku}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imageUri.isBlank()) "Pick Product Image (Photo Picker)" else "Image Selected ✓")
                }
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (English)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("Title (Bengali)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = regularPrice, onValueChange = { regularPrice = it }, label = { Text("Regular ৳") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("Sale ৳") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost ৳") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = stockQty, onValueChange = { stockQty = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = variantsCsv, onValueChange = { variantsCsv = it }, label = { Text("Variants (comma separated)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reg = regularPrice.toDoubleOrNull() ?: 0.0
                    val sale = salePrice.toDoubleOrNull() ?: reg
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val stk = stockQty.toIntOrNull() ?: 0
                    val prod = (initial ?: ProductEntity(
                        id = "prod_${UUID.randomUUID().toString().take(8)}",
                        sku = sku,
                        titleEn = titleEn,
                        titleBn = titleBn.ifBlank { titleEn },
                        brand = brand,
                        categoryId = categoryId,
                        descriptionEn = descEn.ifBlank { titleEn },
                        descriptionBn = descEn.ifBlank { titleEn },
                        regularPrice = reg,
                        salePrice = sale,
                        costPrice = cost,
                        stockQty = stk
                    )).copy(
                        sku = sku.trim(),
                        titleEn = titleEn.trim(),
                        titleBn = titleBn.trim().ifBlank { titleEn.trim() },
                        brand = brand.trim(),
                        categoryId = categoryId,
                        regularPrice = reg,
                        salePrice = sale,
                        costPrice = cost,
                        stockQty = stk,
                        variantsCsv = variantsCsv,
                        descriptionEn = descEn.trim().ifBlank { titleEn.trim() },
                        imageUri = imageUri
                    )
                    onSave(prod, isNew)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminOrdersPanel(vm: StoreViewModel, lang: AppLanguage) {
    val orders by vm.allOrders.collectAsStateWithLifecycle()
    val statuses = listOf("PENDING", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED")
    val couriers = listOf("Steadfast Courier", "Pathao Courier", "RedX Bangladesh", "Sundarban Courier")

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(orders, key = { it.id }) { order ->
            var selectedStatus by remember(order.id, order.orderStatus) { mutableStateOf(order.orderStatus) }
            var selectedCourier by remember(order.id, order.courierName) { mutableStateOf(order.courierName) }
            var trackingInput by remember(order.id, order.trackingNumber) { mutableStateOf(order.trackingNumber) }
            var showStatusMenu by remember { mutableStateOf(false) }
            var showCourierMenu by remember { mutableStateOf(false) }
            val (bg, fg) = orderStatusColors(order.orderStatus)

            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("#${order.orderNumber} • ${order.customerName}", style = MaterialTheme.typography.titleSmall)
                            Text("${order.district} • ${order.paymentMethod} (${order.paymentStatus}) • ${Strings.formatBdt(order.totalAmount, lang)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        StatusPill(order.orderStatus, bg, fg)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showStatusMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedStatus, maxLines = 1)
                            }
                            DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                statuses.forEach { st ->
                                    DropdownMenuItem(text = { Text(st) }, onClick = { selectedStatus = st; showStatusMenu = false })
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showCourierMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedCourier, maxLines = 1)
                            }
                            DropdownMenu(expanded = showCourierMenu, onDismissRequest = { showCourierMenu = false }) {
                                couriers.forEach { c ->
                                    DropdownMenuItem(text = { Text(c) }, onClick = { selectedCourier = c; showCourierMenu = false })
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = trackingInput,
                            onValueChange = { trackingInput = it },
                            label = { Text("Tracking Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { vm.adminUpdateOrderAndCourier(order, selectedStatus, selectedCourier, trackingInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            modifier = Modifier.heightIn(min = 50.dp)
                        ) { Text("Update") }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPaymentsPanel(vm: StoreViewModel, lang: AppLanguage) {
    val payments by vm.allPayments.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Manual bKash / Nagad & COD Verification", style = MaterialTheme.typography.titleMedium)
            Text("TrxIDs are never auto-verified. Owner Admin verifies against merchant statement.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        items(payments, key = { it.id }) { pay ->
            val (bg, fg) = orderStatusColors(pay.status)
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${pay.orderNumber} • ${pay.method}", style = MaterialTheme.typography.titleSmall, color = DeepBlue)
                        StatusPill(pay.status, bg, fg)
                    }
                    Text("Amount: ${Strings.formatBdt(pay.amount, lang)}", fontWeight = FontWeight.Bold)
                    if (pay.transactionId.isNotBlank()) {
                        Text("Sender: ${pay.senderNumber} | TrxID: ${pay.transactionId}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Text(pay.adminNote, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pay.status == "SUBMITTED" || pay.status == "PENDING") {
                            Button(
                                onClick = { vm.adminVerifyPayment(pay, true, "Verified by Owner Admin") },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1f).testTag("verify_payment_${pay.id}")
                            ) { Text(if (pay.method == "COD") "Mark Collected" else "Verify TrxID") }
                            OutlinedButton(
                                onClick = { vm.adminVerifyPayment(pay, false, "TrxID not found in statement") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                modifier = Modifier.weight(1f)
                            ) { Text("Reject") }
                        } else if (pay.status == "VERIFIED") {
                            OutlinedButton(
                                onClick = { vm.adminMarkRefunded(pay) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) { Text("Issue Refund & Reverse Loyalty") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPromosAndSupportPanel(vm: StoreViewModel, lang: AppLanguage) {
    val flashSales by vm.flashSales.collectAsStateWithLifecycle()
    val coupons by vm.coupons.collectAsStateWithLifecycle()
    val supportMessages by vm.allSupportMessages.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val allCarts by vm.allCartItems.collectAsStateWithLifecycle()

    var newCouponCode by remember { mutableStateOf("") }
    var newCouponValue by remember { mutableStateOf("15") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Flash Sales & Coupons", style = MaterialTheme.typography.titleMedium)
        }
        items(flashSales, key = { it.id }) { sale ->
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sale.titleEn, style = MaterialTheme.typography.titleSmall)
                        Text("${sale.discountPercent}% OFF", style = MaterialTheme.typography.bodySmall, color = VibrantOrange)
                    }
                    Switch(checked = sale.isActive, onCheckedChange = { vm.adminToggleFlashSale(sale.copy(isActive = it)) })
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coupons (${coupons.size})", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newCouponCode, onValueChange = { newCouponCode = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newCouponValue, onValueChange = { newCouponValue = it }, label = { Text("% Off") }, modifier = Modifier.width(86.dp))
                        Button(
                            onClick = {
                                if (newCouponCode.isNotBlank()) {
                                    vm.adminSaveCoupon(
                                        CouponEntity(
                                            id = "cpn_${UUID.randomUUID().toString().take(6)}",
                                            code = newCouponCode.trim().uppercase(),
                                            discountType = "PERCENT",
                                            discountValue = newCouponValue.toDoubleOrNull() ?: 10.0,
                                            minOrderAmount = 300.0,
                                            maxDiscount = 500.0,
                                            isActive = true,
                                            expiresAtMillis = System.currentTimeMillis() + 30L * 86400_000L
                                        )
                                    )
                                    newCouponCode = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                        ) { Text("Add") }
                    }
                    coupons.forEach { c ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${c.code} (${c.discountValue.toInt()}% • used ${c.usageCount}x)", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = c.isActive, onCheckedChange = { vm.adminSaveCoupon(c.copy(isActive = it)) })
                        }
                    }
                }
            }
        }

        // Abandoned Cart Recovery
        item {
            val cartByUser = allCarts.groupBy { it.userId }
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Abandoned Cart Recovery (${cartByUser.size} active shoppers)", style = MaterialTheme.typography.titleSmall)
                    if (cartByUser.isEmpty()) {
                        Text("No pending carts right now.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        cartByUser.forEach { (uid, items) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("$uid (${items.sumOf { it.quantity }} items)", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { vm.adminSendCartRecovery(uid) }) {
                                    Text("Send In-App Reminder", color = VibrantOrange)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customer Support Tickets
        item { Text("Customer Support Tickets (${supportMessages.size})", style = MaterialTheme.typography.titleMedium) }
        items(supportMessages, key = { it.id }) { msg ->
            var reply by remember(msg.id) { mutableStateOf(msg.adminReply) }
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${msg.subject} — ${msg.senderName}", style = MaterialTheme.typography.titleSmall)
                    Text(msg.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Reply") }, modifier = Modifier.weight(1f))
                        Button(onClick = { vm.adminReplySupport(msg, reply) }, colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)) {
                            Text("Send")
                        }
                    }
                }
            }
        }

        // Review Moderation
        item { Text("Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium) }
        items(reviews.take(10), key = { it.id }) { rev ->
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${rev.userName} (${rev.rating}★)", style = MaterialTheme.typography.labelLarge)
                        Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = { vm.adminDeleteReview(rev) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove review", tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsBackupLogsPanel(vm: StoreViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val backupLogs by vm.backupLogs.collectAsStateWithLifecycle()
    val activityLogs by vm.activityLogs.collectAsStateWithLifecycle()

    var bkashNum by remember(settings) { mutableStateOf(settings.bkashMerchantNumber) }
    var nagadNum by remember(settings) { mutableStateOf(settings.nagadMerchantNumber) }
    var waNum by remember(settings) { mutableStateOf(settings.whatsappNumber) }
    var announcementEn by remember(settings) { mutableStateOf(settings.announcementEn) }
    var seoTitle by remember(settings) { mutableStateOf(settings.seoHomeTitle) }
    var showFlash by remember(settings) { mutableStateOf(settings.showFlashSale) }
    var showRec by remember(settings) { mutableStateOf(settings.showRecommended) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val fileName = "kenakata_backup_${System.currentTimeMillis()}.json"
                vm.adminCreateBackup(fileName).onSuccess { json ->
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    vm.emitMessage("Database backup exported!")
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                vm.adminRestoreBackup("restore.json", jsonText).fold(
                    onSuccess = { vm.emitMessage("Restored ${it.recordCount} records cleanly!") },
                    onFailure = { vm.emitMessage(it.message ?: "Restore validation failed") }
                )
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Website, Payment & SEO Settings", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = announcementEn, onValueChange = { announcementEn = it }, label = { Text("Announcement Bar") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bkashNum, onValueChange = { bkashNum = it }, label = { Text("bKash Merchant Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nagadNum, onValueChange = { nagadNum = it }, label = { Text("Nagad Merchant Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = waNum, onValueChange = { waNum = it }, label = { Text("WhatsApp Support Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = seoTitle, onValueChange = { seoTitle = it }, label = { Text("SEO Home Title") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Flash Sale on Home")
                        Switch(checked = showFlash, onCheckedChange = { showFlash = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show AI Recommendations")
                        Switch(checked = showRec, onCheckedChange = { showRec = it })
                    }
                    Button(
                        onClick = {
                            vm.adminSaveSettings(
                                settings.copy(
                                    announcementEn = announcementEn,
                                    bkashMerchantNumber = bkashNum,
                                    nagadMerchantNumber = nagadNum,
                                    whatsappNumber = waNum,
                                    seoHomeTitle = seoTitle,
                                    showFlashSale = showFlash,
                                    showRecommended = showRec
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Website Settings") }
                }
            }
        }

        // Database Backup & Restore
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Database Backup & Restore", style = MaterialTheme.typography.titleMedium)
                    Text("Admin-triggered JSON export & validated restore synced with InsForge Postgres & local cache.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { backupExportLauncher.launch("kenakata_backup.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            modifier = Modifier.weight(1f).testTag("admin_backup_export_button")
                        ) { Text("Export Backup") }
                        OutlinedButton(
                            onClick = { backupRestoreLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f).testTag("admin_backup_restore_button")
                        ) { Text("Restore JSON") }
                    }
                    backupLogs.take(5).forEach { b ->
                        Text("• ${b.status}: ${b.fileName} (${b.recordCount} records)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        // Immutable Admin Activity Logs
        item {
            Text("Admin Activity Audit Logs (${activityLogs.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(activityLogs.take(25), key = { it.id }) { log ->
            val ts = SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(log.createdAtMillis))
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.actionType, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                        Text(ts, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Text(log.summary, style = MaterialTheme.typography.bodySmall)
                    Text("Actor: ${log.actorEmail}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}
