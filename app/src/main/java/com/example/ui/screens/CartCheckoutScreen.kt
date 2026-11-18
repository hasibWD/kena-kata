package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.OrderEntity
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.BangladeshDistricts
import com.example.ui.components.ProductImageBox
import com.example.ui.components.StatusPill
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutScreen(
    vm: StoreViewModel,
    onBrowseShop: () -> Unit,
    onOrderPlacedNavigateToOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val cartItems by vm.cartItems.collectAsStateWithLifecycle()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val addresses by vm.addresses.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val merchantStatus by vm.merchantGatewayStatus.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val subtotal = remember(cartItems) {
        cartItems.sumOf { it.product.effectivePrice * it.cartItem.quantity }
    }

    var customerName by remember(currentUser) { mutableStateOf(currentUser?.name.orEmpty()) }
    var customerPhone by remember(currentUser) { mutableStateOf(currentUser?.phone.orEmpty()) }
    var selectedDistrict by remember { mutableStateOf("Dhaka") }
    var area by remember { mutableStateOf("Dhanmondi") }
    var streetAddress by remember { mutableStateOf("") }

    LaunchedEffect(addresses) {
        val def = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
        if (def != null && streetAddress.isBlank()) {
            customerName = def.fullName
            customerPhone = def.phone
            selectedDistrict = def.district
            area = def.area
            streetAddress = def.streetAddress
        }
    }

    var deliveryCharge by remember { mutableDoubleStateOf(60.0) }
    LaunchedEffect(selectedDistrict, subtotal) {
        deliveryCharge = if (cartItems.isEmpty()) 0.0 else vm.calculateDeliveryFee(selectedDistrict, subtotal)
    }

    var couponInput by remember { mutableStateOf("") }
    var appliedCouponCode by remember { mutableStateOf("") }
    var couponDiscount by remember { mutableDoubleStateOf(0.0) }
    var redeemPoints by remember { mutableStateOf(false) }

    val loyaltyDiscount = remember(redeemPoints, currentUser, subtotal, couponDiscount, settings) {
        if (!redeemPoints || currentUser == null) 0.0
        else {
            val pts = currentUser!!.loyaltyPoints
            val maxBdt = maxOf(0.0, (subtotal - couponDiscount) * 0.5)
            minOf(pts * settings.loyaltyPointValueBdt, maxBdt)
        }
    }

    val totalPayable = maxOf(0.0, subtotal + deliveryCharge - couponDiscount - loyaltyDiscount)

    var paymentMethod by remember { mutableStateOf("BKASH") }
    var senderNumber by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }
    var showDistrictMenu by remember { mutableStateOf(false) }
    var idempotencyKey by remember(cartItems.size) { mutableStateOf(UUID.randomUUID().toString()) }
    var confirmedOrder by remember { mutableStateOf<OrderEntity?>(null) }

    if (cartItems.isEmpty() && confirmedOrder == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SoftGrayBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(64.dp))
                Text(Strings.tr(lang, "Your Kena Kata cart is empty", "আপনার কার্ট খালি"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    Strings.tr(lang, "Explore Bengali & English books, electronics, and heritage fashion.", "সেরা বই, গ্যাজেট ও ফ্যাশন পণ্য দেখুন।"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onBrowseShop,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("empty_cart_browse_button")
                ) {
                    Text(Strings.tr(lang, "Start Shopping", "কেনাকাটা শুরু করুন"))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SoftGrayBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Strings.navCart(lang)} & ${Strings.tr(lang, "Checkout", "চেকআউট")}",
                    style = MaterialTheme.typography.headlineSmall
                )
                StatusPill(
                    text = if (currentUser == null) Strings.tr(lang, "Guest Checkout", "গেস্ট চেকআউট") else currentUser!!.name,
                    backgroundColor = DeepBlueContainer,
                    contentColor = DeepBlue
                )
            }
        }

        // Cart Items
        items(cartItems, key = { it.cartItem.id }) { display ->
            val p = display.product
            val ci = display.cartItem
            val title = if (lang == AppLanguage.BN) p.titleBn else p.titleEn
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductImageBox(
                        imageResKey = p.imageResKey,
                        imageUri = p.imageUri,
                        contentDescription = title,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(ci.variantLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            Strings.formatBdt(p.effectivePrice * ci.quantity, lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.updateCartQty(ci, ci.quantity - 1) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text("${ci.quantity}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(onClick = { vm.updateCartQty(ci, ci.quantity + 1) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase")
                        }
                        IconButton(onClick = { vm.removeCartItem(ci.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = ErrorRed)
                        }
                    }
                }
            }
        }

        // Coupon & Loyalty Points
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Strings.tr(lang, "Coupons & Loyalty Rewards", "কুপন ও লয়্যালটি পয়েন্ট"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it },
                            label = { Text("Coupon (e.g. KENA10, DHAKA50)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("coupon_input")
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    vm.validateCoupon(couponInput, subtotal).fold(
                                        onSuccess = { (c, disc) ->
                                            appliedCouponCode = c.code
                                            couponDiscount = disc
                                            vm.emitMessage("Applied ${c.code}: -৳${disc.toInt()}")
                                        },
                                        onFailure = {
                                            appliedCouponCode = ""
                                            couponDiscount = 0.0
                                            vm.emitMessage(it.message ?: "Invalid coupon")
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            modifier = Modifier.heightIn(min = 52.dp).testTag("apply_coupon_button")
                        ) {
                            Text(Strings.tr(lang, "Apply", "প্রয়োগ"))
                        }
                    }
                    if (currentUser != null && currentUser!!.loyaltyPoints > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    Strings.tr(lang, "Redeem Loyalty Points (${currentUser!!.loyaltyPoints} pts)", "লয়্যালটি পয়েন্ট ব্যবহার (${currentUser!!.loyaltyPoints})"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    Strings.tr(lang, "1 point = ৳${settings.loyaltyPointValueBdt.toInt()} (up to 50% of order)", "প্রতি পয়েন্ট = ৳${settings.loyaltyPointValueBdt.toInt()}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(checked = redeemPoints, onCheckedChange = { redeemPoints = it })
                        }
                    }
                }
            }
        }

        // Shipping Details (Bangladesh Districts)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Strings.tr(lang, "Delivery Address (Bangladesh)", "ডেলিভারি ঠিকানা"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(Strings.tr(lang, "Full Name", "পুরো নাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text(Strings.tr(lang, "Mobile Number (01XXX-XXXXXX)", "মোবাইল নম্বর")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedDistrict,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(Strings.tr(lang, "District", "জেলা")) },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().clickable { showDistrictMenu = true }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDistrictMenu = true })
                            DropdownMenu(expanded = showDistrictMenu, onDismissRequest = { showDistrictMenu = false }) {
                                BangladeshDistricts.all.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d) },
                                        onClick = {
                                            selectedDistrict = d
                                            showDistrictMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text(Strings.tr(lang, "Area / Thana", "এলাকা / থানা")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it },
                        label = { Text(Strings.tr(lang, "House, Road & Street Details", "বাসা, রোড ও বিস্তারিত ঠিকানা")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                    )
                    OutlinedTextField(
                        value = orderNotes,
                        onValueChange = { orderNotes = it },
                        label = { Text(Strings.tr(lang, "Delivery Note (Optional)", "অতিরিক্ত নির্দেশনা (ঐচ্ছিক)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Single-Vendor Payment Architecture: Manual bKash, Manual Nagad, Cash on Delivery
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Strings.tr(lang, "Payment Method", "পেমেন্ট পদ্ধতি"), style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PaymentOptionChip("BKASH", "bKash", BkashPink, paymentMethod == "BKASH", Modifier.weight(1f)) { paymentMethod = "BKASH" }
                        PaymentOptionChip("NAGAD", "Nagad", NagadOrange, paymentMethod == "NAGAD", Modifier.weight(1f)) { paymentMethod = "NAGAD" }
                        PaymentOptionChip("COD", "Cash on Delivery", CodGreen, paymentMethod == "COD", Modifier.weight(1f)) { paymentMethod = "COD" }
                    }

                    if (paymentMethod == "BKASH" || paymentMethod == "NAGAD") {
                        val merchantNumber = if (paymentMethod == "BKASH") settings.bkashMerchantNumber else settings.nagadMerchantNumber
                        val brandColor = if (paymentMethod == "BKASH") BkashPink else NagadOrange
                        Surface(
                            color = brandColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, brandColor.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Kena Kata Official $paymentMethod Account: $merchantNumber",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = brandColor
                                )
                                Text(
                                    Strings.tr(
                                        lang,
                                        "1. Send Payment / Cash-Out ৳${totalPayable.toInt()} to $merchantNumber.\n2. Enter your Sender Number and TrxID below.\n3. Admin will verify your TrxID (status stays Submitted until verified).",
                                        "১. $merchantNumber নম্বরে ৳${totalPayable.toInt()} পেমেন্ট করুন।\n২. নিচে আপনার নম্বর ও TrxID দিন।\n৩. অ্যাডমিন যাচাই না করা পর্যন্ত পেমেন্ট Submitted থাকবে।"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = merchantStatus?.message?.takeIf { it.isNotBlank() }
                                        ?: Strings.tr(
                                            lang,
                                            "Automated $paymentMethod Merchant API module ready (requires live merchant credentials).",
                                            "অটোমেটেড $paymentMethod মার্চেন্ট API প্রস্তুত (লাইভ ক্রেডেনশিয়াল প্রয়োজন)।"
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        OutlinedTextField(
                            value = senderNumber,
                            onValueChange = { senderNumber = it },
                            label = { Text("$paymentMethod Sender Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("payment_sender_input")
                        )
                        OutlinedTextField(
                            value = transactionId,
                            onValueChange = { transactionId = it },
                            label = { Text("$paymentMethod Transaction ID (TrxID)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("payment_trxid_input")
                        )
                    } else {
                        Surface(color = SuccessContainer.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = Strings.tr(
                                    lang,
                                    "Cash on Delivery: Pay ৳${totalPayable.toInt()} in cash to the courier upon delivery. Tracked as Unpaid until collected.",
                                    "ক্যাশ অন ডেলিভারি: পণ্য হাতে পেয়ে কুরিয়ারকে ৳${totalPayable.toInt()} পরিশোধ করুন।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Order Summary & Confirm
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(Strings.subtotal(lang), Strings.formatBdt(subtotal, lang))
                    SummaryRow("${Strings.deliveryFee(lang)} ($selectedDistrict)", if (deliveryCharge == 0.0) "FREE" else Strings.formatBdt(deliveryCharge, lang))
                    if (couponDiscount > 0) {
                        SummaryRow("${Strings.discount(lang)} ($appliedCouponCode)", "-${Strings.formatBdt(couponDiscount, lang)}", SuccessGreen)
                    }
                    if (loyaltyDiscount > 0) {
                        SummaryRow("Loyalty Points", "-${Strings.formatBdt(loyaltyDiscount, lang)}", SuccessGreen)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Strings.total(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(Strings.formatBdt(totalPayable, lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = DeepBlue)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            vm.placeOrder(
                                idempotencyKey = idempotencyKey,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                district = selectedDistrict,
                                area = area,
                                streetAddress = streetAddress,
                                couponCode = appliedCouponCode,
                                redeemLoyaltyPoints = redeemPoints,
                                paymentMethod = paymentMethod,
                                senderNumber = senderNumber,
                                transactionId = transactionId,
                                notes = orderNotes,
                                onPlaced = { order ->
                                    confirmedOrder = order
                                    idempotencyKey = UUID.randomUUID().toString()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("place_order_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.placeOrder(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    confirmedOrder?.let { order ->
        AlertDialog(
            onDismissRequest = {
                confirmedOrder = null
                onOrderPlacedNavigateToOrders()
            },
            icon = { Icon(Icons.Filled.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(44.dp)) },
            title = { Text("Order ${order.orderNumber} Confirmed!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total: ${Strings.formatBdt(order.totalAmount, lang)}", fontWeight = FontWeight.Bold)
                    Text("Payment Method: ${order.paymentMethod} (${order.paymentStatus})")
                    Text(
                        if (order.paymentMethod == "COD") "Status: Unpaid until cash collection on delivery."
                        else "Your TrxID is submitted and awaiting Owner Admin verification."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmedOrder = null
                        onOrderPlacedNavigateToOrders()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text(Strings.tr(lang, "Track Order", "অর্ডার ট্র্যাক করুন"))
                }
            }
        )
    }
}

@Composable
private fun PaymentOptionChip(
    code: String,
    label: String,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag("payment_method_$code"),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else SoftGrayBg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else CardBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
