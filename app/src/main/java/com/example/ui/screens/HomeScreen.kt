package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.*
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    vm: StoreViewModel,
    onOpenSearch: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenCompare: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val banners by vm.banners.collectAsStateWithLifecycle()
    val flashSales by vm.flashSales.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val recommendations by vm.recommendations.collectAsStateWithLifecycle()
    val recentlyViewedIds by vm.recentlyViewedIds.collectAsStateWithLifecycle()
    val wishlistIds by vm.wishlistProductIds.collectAsStateWithLifecycle()
    val compareList by vm.compareProducts.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val nowMillis by vm.currentTimeMillis.collectAsStateWithLifecycle()

    val unreadNotifCount = notifications.count { !it.isRead }
    var newsletterEmail by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SoftGrayBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Announcement Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(DeepBlueDark, DeepBlue))
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalOffer,
                        contentDescription = null,
                        tint = VibrantOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (lang == AppLanguage.BN) settings.announcementBn else settings.announcementEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. Brand Header + Language Switch + Compare + Notifications + Wishlist
        item {
            Surface(color = PureWhite, shadowElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KenaKataLogo()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { vm.toggleLanguage() },
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("language_toggle_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DeepBlueContainer,
                                    contentColor = DeepBlue
                                )
                            ) {
                                Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (lang == AppLanguage.EN) "বাংলা" else "EN", style = MaterialTheme.typography.labelMedium)
                            }
                            BadgedBox(
                                badge = {
                                    if (compareList.isNotEmpty()) {
                                        Badge(containerColor = VibrantOrange) { Text("${compareList.size}") }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = onOpenCompare,
                                    modifier = Modifier.testTag("header_compare_button")
                                ) {
                                    Icon(Icons.Filled.CompareArrows, contentDescription = Strings.compare(lang), tint = DeepBlue)
                                }
                            }
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(containerColor = ErrorRed) { Text("$unreadNotifCount") }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = onOpenNotifications,
                                    modifier = Modifier.testTag("header_notifications_button")
                                ) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = DeepBlue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Bar Trigger (Rokomari/Daraz style)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("home_search_bar")
                            .clickable(onClick = onOpenSearch),
                        shape = RoundedCornerShape(14.dp),
                        color = SoftGrayBg,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = DeepBlue)
                            Text(
                                text = Strings.searchHint(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = VibrantOrange,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = Strings.tr(lang, "Search", "খুঁজুন"),
                                    color = PureWhite,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Quick Trust & Single-Vendor Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = Strings.tr(lang, "100% Official Kena Kata Store", "শতভাগ অফিসিয়াল কেনা কাটা স্টোর"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        TextButton(
                            onClick = onOpenWishlist,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${Strings.wishlist(lang)} (${wishlistIds.size})", style = MaterialTheme.typography.labelSmall, color = DeepBlue)
                        }
                    }
                }
            }
        }

        // 3. Hero Banner Carousel
        if (banners.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(banners.filter { it.isActive }, key = { it.id }) { banner ->
                        HeroBannerCard(
                            banner = banner,
                            language = lang,
                            onExplore = {
                                vm.selectCategoryFilter(banner.targetCategoryId.takeIf { it.isNotBlank() })
                                onOpenSearch()
                            }
                        )
                    }
                }
            }
        }

        // 4. Featured Categories
        if (categories.isNotEmpty()) {
            item {
                SectionHeader(
                    title = Strings.featuredCategories(lang),
                    actionLabel = Strings.tr(lang, "All", "সব"),
                    onActionClick = {
                        vm.selectCategoryFilter(null)
                        onOpenSearch()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryChipCard(
                            category = cat,
                            language = lang,
                            onClick = {
                                vm.selectCategoryFilter(cat.id)
                                onOpenSearch()
                            }
                        )
                    }
                }
            }
        }

        // 5. Flash Sale with Live Countdown Timer
        val activeFlashSale = flashSales.firstOrNull { it.isActive && it.endTimeMillis > nowMillis }
        if (settings.showFlashSale && activeFlashSale != null) {
            item {
                val saleIds = activeFlashSale.productIdsCsv.split(",").map { it.trim() }.toSet()
                val saleProducts = products.filter { it.id in saleIds }.ifEmpty { products.take(4) }
                FlashSaleSection(
                    sale = activeFlashSale,
                    remainingMillis = (activeFlashSale.endTimeMillis - nowMillis).coerceAtLeast(0L),
                    products = saleProducts,
                    language = lang,
                    wishlistIds = wishlistIds,
                    compareIds = compareList.map { it.id }.toSet(),
                    onProductClick = onOpenProduct,
                    onAddToCart = { vm.addToCart(it) },
                    onToggleWishlist = { vm.toggleWishlist(it.id) },
                    onToggleCompare = { vm.toggleCompare(it.id) }
                )
            }
        }

        // 6. Loading skeleton if DB is still seeding
        if (products.isEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) { ProductSkeletonCard(modifier = Modifier.width(178.dp)) }
                }
            }
        } else {
            // 7. Today's Deals
            if (settings.showTodayDeals) {
                val deals = products.filter { it.isTodayDeal }
                if (deals.isNotEmpty()) {
                    item {
                        ProductHorizontalSection(
                            title = Strings.todaysDeals(lang),
                            subtitle = Strings.tr(lang, "Limited-time BDT price drops", "সীমিত সময়ের বিশেষ ছাড়"),
                            products = deals,
                            language = lang,
                            wishlistIds = wishlistIds,
                            compareIds = compareList.map { it.id }.toSet(),
                            onSeeAll = onOpenSearch,
                            onProductClick = onOpenProduct,
                            onAddToCart = { vm.addToCart(it) },
                            onToggleWishlist = { vm.toggleWishlist(it.id) },
                            onToggleCompare = { vm.toggleCompare(it.id) }
                        )
                    }
                }
            }

            // 8. Trending Products
            if (settings.showTrending) {
                val trending = products.filter { it.isTrending }
                if (trending.isNotEmpty()) {
                    item {
                        ProductHorizontalSection(
                            title = Strings.trendingProducts(lang),
                            subtitle = Strings.tr(lang, "Most popular across Bangladesh", "সারা দেশে জনপ্রিয় পণ্য"),
                            products = trending,
                            language = lang,
                            wishlistIds = wishlistIds,
                            compareIds = compareList.map { it.id }.toSet(),
                            onSeeAll = onOpenSearch,
                            onProductClick = onOpenProduct,
                            onAddToCart = { vm.addToCart(it) },
                            onToggleWishlist = { vm.toggleWishlist(it.id) },
                            onToggleCompare = { vm.toggleCompare(it.id) }
                        )
                    }
                }
            }

            // 9. Best Sellers
            if (settings.showBestSelling) {
                val best = products.filter { it.isBestSeller }
                if (best.isNotEmpty()) {
                    item {
                        ProductHorizontalSection(
                            title = Strings.bestSelling(lang),
                            subtitle = Strings.tr(lang, "Top-rated customer favorites", "ক্রেতাদের সর্বাধিক পছন্দের পণ্য"),
                            products = best,
                            language = lang,
                            wishlistIds = wishlistIds,
                            compareIds = compareList.map { it.id }.toSet(),
                            onSeeAll = onOpenSearch,
                            onProductClick = onOpenProduct,
                            onAddToCart = { vm.addToCart(it) },
                            onToggleWishlist = { vm.toggleWishlist(it.id) },
                            onToggleCompare = { vm.toggleCompare(it.id) }
                        )
                    }
                }
            }

            // 10. New Arrivals
            if (settings.showNewArrivals) {
                val fresh = products.filter { it.isNewArrival }
                if (fresh.isNotEmpty()) {
                    item {
                        ProductHorizontalSection(
                            title = Strings.newArrivals(lang),
                            subtitle = Strings.tr(lang, "Just landed in Kena Kata warehouse", "নতুন স্টক যুক্ত হয়েছে"),
                            products = fresh,
                            language = lang,
                            wishlistIds = wishlistIds,
                            compareIds = compareList.map { it.id }.toSet(),
                            onSeeAll = onOpenSearch,
                            onProductClick = onOpenProduct,
                            onAddToCart = { vm.addToCart(it) },
                            onToggleWishlist = { vm.toggleWishlist(it.id) },
                            onToggleCompare = { vm.toggleCompare(it.id) }
                        )
                    }
                }
            }

            // 11. AI Recommended for You (with explicit Firebase AI Logic placeholder notice!)
            if (settings.showRecommended && recommendations != null) {
                val rec = recommendations!!
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(
                            title = Strings.recommendedForYou(lang),
                            subtitle = if (lang == AppLanguage.BN) rec.reasonLabelBn else rec.reasonLabelEn
                        )
                        if (rec.isPlaceholderMode) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = DeepBlueContainer,
                                border = BorderStroke(1.dp, DeepBlue.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (lang == AppLanguage.BN) rec.noticeBn else rec.noticeEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnDeepBlueContainer
                                    )
                                }
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(rec.recommendedForYou, key = { "rec_${it.id}" }) { product ->
                                ProductCard(
                                    product = product,
                                    language = lang,
                                    isWishlisted = product.id in wishlistIds,
                                    isCompared = compareList.any { it.id == product.id },
                                    onClick = { onOpenProduct(product.id) },
                                    onAddToCart = { vm.addToCart(product) },
                                    onToggleWishlist = { vm.toggleWishlist(product.id) },
                                    onToggleCompare = { vm.toggleCompare(product.id) },
                                    modifier = Modifier.width(178.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 12. Recently Viewed
            val recentProducts = recentlyViewedIds.mapNotNull { id -> products.find { it.id == id } }
            if (recentProducts.isNotEmpty()) {
                item {
                    ProductHorizontalSection(
                        title = Strings.recentlyViewed(lang),
                        subtitle = null,
                        products = recentProducts,
                        language = lang,
                        wishlistIds = wishlistIds,
                        compareIds = compareList.map { it.id }.toSet(),
                        onSeeAll = onOpenSearch,
                        onProductClick = onOpenProduct,
                        onAddToCart = { vm.addToCart(it) },
                        onToggleWishlist = { vm.toggleWishlist(it.id) },
                        onToggleCompare = { vm.toggleCompare(it.id) }
                    )
                }
            }
        }

        // 13. Customer Reviews Showcase
        if (settings.showReviews && reviews.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.customerReviews(lang))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reviews.take(6), key = { it.id }) { rev ->
                        Card(
                            modifier = Modifier.width(260.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(rev.userName, style = MaterialTheme.typography.titleSmall, color = DeepBlue)
                                    Row {
                                        repeat(rev.rating) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 14. Newsletter Subscription + Support CTA + Footer
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlue)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = Strings.newsletterTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Text(
                        text = Strings.tr(
                            lang,
                            "${settings.newsletterSubscribersCount} shoppers subscribed for exclusive coupons & new arrivals.",
                            "${settings.newsletterSubscribersCount} জন ক্রেতা অফার আপডেট পাচ্ছেন।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newsletterEmail,
                            onValueChange = { newsletterEmail = it },
                            placeholder = { Text("email@example.com") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedBorderColor = VibrantOrange
                            )
                        )
                        Button(
                            onClick = {
                                if (newsletterEmail.contains("@")) {
                                    vm.subscribeNewsletter()
                                    newsletterEmail = ""
                                } else {
                                    vm.emitMessage("Enter a valid email")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                            modifier = Modifier.heightIn(min = 52.dp).testTag("newsletter_subscribe_button")
                        ) {
                            Text(Strings.tr(lang, "Join", "যুক্ত হোন"))
                        }
                    }
                    HorizontalDivider(color = PureWhite.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kena Kata Bangladesh", style = MaterialTheme.typography.labelLarge, color = PureWhite)
                            Text("Single-Vendor Official Store • bKash • Nagad • COD", style = MaterialTheme.typography.labelSmall, color = DeepBlueContainer)
                        }
                        TextButton(onClick = onOpenSupport) {
                            Text(Strings.tr(lang, "Help & Chat", "সহায়তা"), color = VibrantOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBannerCard(banner: BannerEntity, language: AppLanguage, onExplore: () -> Unit) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(172.dp)
            .clickable(onClick = onExplore),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ProductImageBox(
                imageResKey = banner.imageResKey,
                imageUri = "",
                contentDescription = banner.titleEn,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(DeepBlueDark.copy(alpha = 0.90f), DeepBlue.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(color = VibrantOrange, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = banner.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) banner.titleBn else banner.titleEn,
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite,
                        maxLines = 2
                    )
                    Text(
                        text = if (language == AppLanguage.BN) banner.subtitleBn else banner.subtitleEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueContainer,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipCard(category: CategoryEntity, language: AppLanguage, onClick: () -> Unit) {
    val icon = when (category.iconKey) {
        "book" -> Icons.Filled.MenuBook
        "devices" -> Icons.Filled.Devices
        "checkroom" -> Icons.Filled.Checkroom
        "weekend" -> Icons.Filled.Weekend
        else -> Icons.Filled.Edit
    }
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("category_chip_${category.id}"),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DeepBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(18.dp))
            }
            Text(
                text = if (language == AppLanguage.BN) category.nameBn else category.nameEn,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun FlashSaleSection(
    sale: FlashSaleEntity,
    remainingMillis: Long,
    products: List<ProductEntity>,
    language: AppLanguage,
    wishlistIds: Set<String>,
    compareIds: Set<String>,
    onProductClick: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onToggleWishlist: (ProductEntity) -> Unit,
    onToggleCompare: (ProductEntity) -> Unit
) {
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val mins = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
    val secs = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
    val timerStr = String.format("%02d:%02d:%02d", hours, mins, secs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(OrangeContainer.copy(alpha = 0.55f), SoftGrayBg))
            )
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = VibrantOrange)
                Text(
                    text = if (language == AppLanguage.BN) sale.titleBn else sale.titleEn,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            Surface(
                color = DeepBlue,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${Strings.tr(language, "Ends in", "সময় বাকি")} $timerStr",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { "fs_${it.id}" }) { product ->
                ProductCard(
                    product = product,
                    language = language,
                    isWishlisted = product.id in wishlistIds,
                    isCompared = product.id in compareIds,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { onAddToCart(product) },
                    onToggleWishlist = { onToggleWishlist(product) },
                    onToggleCompare = { onToggleCompare(product) },
                    modifier = Modifier.width(178.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductHorizontalSection(
    title: String,
    subtitle: String?,
    products: List<ProductEntity>,
    language: AppLanguage,
    wishlistIds: Set<String>,
    compareIds: Set<String>,
    onSeeAll: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onToggleWishlist: (ProductEntity) -> Unit,
    onToggleCompare: (ProductEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            actionLabel = Strings.tr(language, "See All", "সব দেখুন"),
            onActionClick = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { "${title}_${it.id}" }) { product ->
                ProductCard(
                    product = product,
                    language = language,
                    isWishlisted = product.id in wishlistIds,
                    isCompared = product.id in compareIds,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { onAddToCart(product) },
                    onToggleWishlist = { onToggleWishlist(product) },
                    onToggleCompare = { onToggleCompare(product) },
                    modifier = Modifier.width(178.dp)
                )
            }
        }
    }
}
