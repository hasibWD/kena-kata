package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductImageBox
import com.example.ui.components.StatusPill
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    vm: StoreViewModel,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onGoToCart: () -> Unit
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val allReviews by vm.reviews.collectAsStateWithLifecycle()
    val wishlistIds by vm.wishlistProductIds.collectAsStateWithLifecycle()
    val compareList by vm.compareProducts.collectAsStateWithLifecycle()

    val product = products.find { it.id == productId }
    if (product == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Product") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Product not found.")
            }
        }
        return
    }

    LaunchedEffect(productId) {
        vm.recordProductView(productId)
    }

    val similarProducts by produceState<List<ProductEntity>>(initialValue = emptyList(), productId, products) {
        value = vm.getSimilarProducts(product)
    }
    val productReviews = remember(allReviews, productId) { allReviews.filter { it.productId == productId && it.isApproved } }

    val variants = remember(product.variantsCsv) {
        product.variantsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Standard") }
    }
    val colors = remember(product.colorsCsv) {
        product.colorsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val galleryKeys = remember(product.imageResKey, product.galleryResCsv) {
        (listOf(product.imageResKey) + product.galleryResCsv.split(",").map { it.trim() }).filter { it.isNotBlank() }.distinct()
    }

    var selectedImageKey by remember(product.id) { mutableStateOf(galleryKeys.first()) }
    var selectedVariant by remember(product.id) { mutableStateOf(variants.first()) }
    var selectedColor by remember(product.id) { mutableStateOf(colors.firstOrNull().orEmpty()) }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val fullVariantLabel = if (selectedColor.isNotBlank()) "$selectedVariant / $selectedColor" else selectedVariant
    val title = if (lang == AppLanguage.BN) product.titleBn else product.titleEn
    val description = if (lang == AppLanguage.BN) product.descriptionBn else product.descriptionEn
    val isWishlisted = product.id in wishlistIds
    val isCompared = compareList.any { it.id == product.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.brand, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleCompare(product.id) }) {
                        Icon(
                            Icons.Filled.CompareArrows,
                            contentDescription = "Compare",
                            tint = if (isCompared) VibrantOrange else DeepBlue
                        )
                    }
                    IconButton(onClick = { vm.toggleWishlist(product.id) }) {
                        Icon(
                            if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) ErrorRed else DeepBlue
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = PureWhite, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.addToCart(product, fullVariantLabel, quantity) },
                        enabled = product.stockQty > 0,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("detail_add_to_cart_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.addToCart(lang))
                    }
                    Button(
                        onClick = {
                            vm.addToCart(product, fullVariantLabel, quantity)
                            onGoToCart()
                        },
                        enabled = product.stockQty > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("detail_buy_now_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.buyNow(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftGrayBg)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gallery
            item {
                Column(modifier = Modifier.fillMaxWidth().background(PureWhite).padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftGrayBg)
                    ) {
                        ProductImageBox(
                            imageResKey = selectedImageKey,
                            imageUri = product.imageUri,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (product.discountPercent > 0) {
                            Surface(
                                color = VibrantOrange,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                            ) {
                                Text(
                                    "-${product.discountPercent}% OFF",
                                    color = PureWhite,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (galleryKeys.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            galleryKeys.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable { selectedImageKey = key },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(if (selectedImageKey == key) 2.dp else 1.dp, if (selectedImageKey == key) DeepBlue else CardBorder)
                                ) {
                                    ProductImageBox(
                                        imageResKey = key,
                                        imageUri = "",
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Title, Price, Stock, Variants
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(product.brand, style = MaterialTheme.typography.labelLarge, color = DeepBlue)
                            val (bg, fg) = when {
                                product.stockQty <= 0 -> ErrorContainer to ErrorRed
                                product.stockQty <= product.lowStockThreshold -> WarningContainer to VibrantOrangeDark
                                else -> SuccessContainer to SuccessGreen
                            }
                            StatusPill(
                                text = when {
                                    product.stockQty <= 0 -> Strings.outOfStock(lang)
                                    product.stockQty <= product.lowStockThreshold -> "${Strings.lowStock(lang)} (${product.stockQty})"
                                    else -> "${Strings.inStock(lang)} (${product.stockQty})"
                                },
                                backgroundColor = bg,
                                contentColor = fg
                            )
                        }

                        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Text(
                                "${String.format("%.1f", product.ratingAvg)} (${product.reviewCount} reviews) • ${product.soldCount} sold",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = Strings.formatBdt(product.effectivePrice, lang),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = DeepBlue
                            )
                            if (product.discountPercent > 0) {
                                Text(
                                    text = Strings.formatBdt(product.regularPrice, lang),
                                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                                    color = TextMuted
                                )
                            }
                        }

                        Text("SKU: ${product.sku}" + if (product.barcode.isNotBlank()) " • Barcode: ${product.barcode}" else "", style = MaterialTheme.typography.labelSmall, color = TextMuted)

                        HorizontalDivider()

                        // Variant Selector
                        Text(Strings.tr(lang, "Select Edition / Size", "ভ্যারিয়েন্ট নির্বাচন করুন"), style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(variants) { v ->
                                FilterChip(
                                    selected = selectedVariant == v,
                                    onClick = { selectedVariant = v },
                                    label = { Text(v) }
                                )
                            }
                        }

                        if (colors.isNotEmpty()) {
                            Text(Strings.tr(lang, "Color", "রঙ"), style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(colors) { c ->
                                    FilterChip(
                                        selected = selectedColor == c,
                                        onClick = { selectedColor = c },
                                        label = { Text(c) }
                                    )
                                }
                            }
                        }

                        // Quantity Stepper
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(Strings.tr(lang, "Quantity", "পরিমাণ"), style = MaterialTheme.typography.titleSmall)
                            OutlinedIconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(38.dp)
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
                            Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedIconButton(
                                onClick = { if (quantity < product.stockQty) quantity++ },
                                modifier = Modifier.size(38.dp)
                            ) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
                        }
                    }
                }
            }

            // Description & Specs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Strings.tr(lang, "Description & Specifications", "বিবরণ ও বৈশিষ্ট্য"), style = MaterialTheme.typography.titleMedium)
                        Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        val specs = product.specsPipe.split(";;").mapNotNull {
                            val parts = it.split(":")
                            if (parts.size >= 2) parts[0].trim() to parts.drop(1).joinToString(":").trim() else null
                        }
                        if (specs.isNotEmpty()) {
                            HorizontalDivider()
                            specs.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(k, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Customer Reviews
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${Strings.customerReviews(lang)} (${productReviews.size})", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { showReviewDialog = true }, modifier = Modifier.testTag("write_review_button")) {
                                Text(Strings.tr(lang, "Write Review", "রিভিউ লিখুন"), color = VibrantOrange)
                            }
                        }
                        if (productReviews.isEmpty()) {
                            Text(Strings.tr(lang, "Be the first to review this product.", "প্রথম রিভিউ দিন।"), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        } else {
                            productReviews.forEach { rev ->
                                Surface(color = SurfaceElevated, shape = RoundedCornerShape(10.dp)) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(rev.userName, style = MaterialTheme.typography.labelLarge, color = DeepBlue)
                                            Row {
                                                repeat(rev.rating) {
                                                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                        Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Similar Products
            if (similarProducts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = Strings.tr(lang, "Similar Products", "অনুরূপ পণ্য"),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(similarProducts, key = { "sim_${it.id}" }) { sim ->
                                ProductCard(
                                    product = sim,
                                    language = lang,
                                    isWishlisted = sim.id in wishlistIds,
                                    isCompared = compareList.any { it.id == sim.id },
                                    onClick = { onOpenProduct(sim.id) },
                                    onAddToCart = { vm.addToCart(sim) },
                                    onToggleWishlist = { vm.toggleWishlist(sim.id) },
                                    onToggleCompare = { vm.toggleCompare(sim.id) },
                                    modifier = Modifier.width(172.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text(Strings.tr(lang, "Rate & Review", "রেটিং ও রিভিউ")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = WarningAmber
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(Strings.tr(lang, "Your experience", "আপনার মতামত")) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.addReview(product.id, rating, comment)
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) { Text(Strings.tr(lang, "Submit", "জমা দিন")) }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text(Strings.tr(lang, "Cancel", "বাতিল")) }
            }
        )
    }
}
