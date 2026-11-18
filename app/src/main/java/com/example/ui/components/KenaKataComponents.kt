package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.ProductEntity
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.*

object BangladeshDistricts {
    val all = listOf(
        "Dhaka", "Gazipur", "Narayanganj", "Savar", "Chattogram",
        "Sylhet", "Rajshahi", "Khulna", "Barishal", "Rangpur",
        "Mymensingh", "Cumilla", "Bogura", "Cox's Bazar", "Jessore"
    )
}

fun drawableResForKey(key: String): Int = when (key) {
    "hero_mega_sale" -> R.drawable.img_hero_mega_sale
    "hero_rokomari_reads" -> R.drawable.img_hero_rokomari_reads
    "fashion_craft" -> R.drawable.img_cat_fashion_craft
    else -> R.drawable.img_cat_electronics
}

@Composable
fun KenaKataLogo(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(if (compact) 28.dp else 34.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = DeepBlue,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
            )
            // Stylized Shopping Bag
            val bagPath = Path().apply {
                moveTo(w * 0.26f, h * 0.38f)
                lineTo(w * 0.74f, h * 0.38f)
                lineTo(w * 0.80f, h * 0.80f)
                lineTo(w * 0.20f, h * 0.80f)
                close()
            }
            drawPath(bagPath, color = Color.White, style = Stroke(width = w * 0.08f))
            // Handle
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.36f, h * 0.18f),
                size = Size(w * 0.28f, h * 0.28f),
                style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
            )
            // Orange Kena Kata spark
            drawCircle(
                color = VibrantOrange,
                radius = w * 0.14f,
                center = Offset(w * 0.70f, h * 0.34f)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kena",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 16.sp else 19.sp
                    ),
                    color = DeepBlue
                )
                Text(
                    text = "Kata",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 16.sp else 19.sp
                    ),
                    color = VibrantOrange
                )
            }
            if (!compact) {
                Text(
                    text = "Shop Smart, Buy Better.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ProductImageBox(
    imageResKey: String,
    imageUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUri.isNotBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = drawableResForKey(imageResKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    language: AppLanguage,
    isWishlisted: Boolean,
    isCompared: Boolean,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleWishlist: () -> Unit,
    onToggleCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (language == AppLanguage.BN) product.titleBn else product.titleEn
    Card(
        modifier = modifier
            .testTag("product_card_${product.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .background(SoftGrayBg)
            ) {
                ProductImageBox(
                    imageResKey = product.imageResKey,
                    imageUri = product.imageUri,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
                // Discount or Stock Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (product.discountPercent > 0) {
                        Surface(
                            color = VibrantOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "-${product.discountPercent}%",
                                color = PureWhite,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (product.stockQty <= 0) {
                        Surface(color = ErrorRed, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = Strings.outOfStock(language),
                                color = PureWhite,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (product.stockQty <= product.lowStockThreshold) {
                        Surface(color = WarningAmber, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = Strings.lowStock(language),
                                color = PureWhite,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Wishlist & Compare icons
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PureWhite.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onToggleWishlist,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("wishlist_btn_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (isWishlisted) ErrorRed else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isCompared) DeepBlue else PureWhite.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onToggleCompare,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("compare_btn_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CompareArrows,
                                contentDescription = "Compare",
                                tint = if (isCompared) PureWhite else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format("%.1f (%d)", product.ratingAvg, product.reviewCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Strings.formatBdt(product.effectivePrice, language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue
                    )
                    if (product.discountPercent > 0) {
                        Text(
                            text = Strings.formatBdt(product.regularPrice, language),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = onAddToCart,
                    enabled = product.stockQty > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .testTag("add_cart_btn_${product.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepBlue,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.stockQty > 0) Strings.addToCart(language) else Strings.outOfStock(language),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ProductSkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFE2E8F0).copy(alpha = alpha))
            )
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0).copy(alpha = alpha)))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0).copy(alpha = alpha)))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0).copy(alpha = alpha)))
                Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE2E8F0).copy(alpha = alpha)))
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, color = VibrantOrange, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StatusPill(text: String, backgroundColor: Color, contentColor: Color) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun orderStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "DELIVERED", "VERIFIED", "COMPLETED", "REPLIED" -> SuccessContainer to Color(0xFF065F46)
    "SHIPPED", "CONFIRMED", "PROCESSING", "PACKED", "SUBMITTED" -> DeepBlueContainer to DeepBlue
    "CANCELLED", "RETURNED", "FAILED", "REFUNDED" -> ErrorContainer to Color(0xFF991B1B)
    else -> WarningContainer to Color(0xFF92400E)
}
