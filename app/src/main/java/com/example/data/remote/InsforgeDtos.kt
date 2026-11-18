package com.example.data.remote

import com.example.data.local.entity.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun parseIsoToMillis(iso: String?): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    val clean = iso.substringBefore(".").substringBefore("+").substringBefore("Z")
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(clean)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
        }
    }
    return System.currentTimeMillis()
}

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val name: String,
    val phone: String = "",
    val role: String = "CUSTOMER",
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("loyalty_points") val loyaltyPoints: Int = 0,
    @SerialName("referral_code") val referralCode: String = "",
    @SerialName("referred_by_code") val referredByCode: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("avatar_key") val avatarKey: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = UserEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        passwordHash = "",
        role = if (role == "ADMIN") "ADMIN" else "CUSTOMER",
        emailVerified = emailVerified,
        loyaltyPoints = loyaltyPoints,
        referralCode = referralCode,
        referredByCode = referredByCode,
        avatarUri = avatarUrl,
        createdAtMillis = parseIsoToMillis(createdAt)
    )
}

@Serializable
data class AddressDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val label: String = "Home",
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val district: String,
    val area: String = "",
    @SerialName("street_address") val streetAddress: String,
    @SerialName("is_default") val isDefault: Boolean = false
) {
    fun toEntity() = AddressEntity(id, userId, label, fullName, phone, district, area, streetAddress, isDefault)
}

@Serializable
data class CategoryDto(
    val id: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_bn") val nameBn: String,
    val slug: String,
    @SerialName("icon_key") val iconKey: String = "category",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_featured") val isFeatured: Boolean = true
) {
    fun toEntity() = CategoryEntity(id, nameEn, nameBn, slug, iconKey, sortOrder, isFeatured)
}

@Serializable
data class ProductDto(
    val id: String,
    val sku: String,
    val barcode: String = "",
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_bn") val titleBn: String,
    val brand: String = "Kena Kata",
    @SerialName("category_id") val categoryId: String,
    @SerialName("description_en") val descriptionEn: String = "",
    @SerialName("description_bn") val descriptionBn: String = "",
    @SerialName("regular_price") val regularPrice: Double,
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("stock_qty") val stockQty: Int = 0,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int = 5,
    @SerialName("variants_csv") val variantsCsv: String = "Standard",
    @SerialName("colors_csv") val colorsCsv: String = "",
    @SerialName("specs_pipe") val specsPipe: String = "",
    @SerialName("tags_csv") val tagsCsv: String = "",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_trending") val isTrending: Boolean = false,
    @SerialName("is_best_seller") val isBestSeller: Boolean = false,
    @SerialName("is_new_arrival") val isNewArrival: Boolean = false,
    @SerialName("is_today_deal") val isTodayDeal: Boolean = false,
    @SerialName("rating_avg") val ratingAvg: Double = 4.6,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("sold_count") val soldCount: Int = 0,
    @SerialName("seo_title") val seoTitle: String = "",
    @SerialName("seo_description") val seoDescription: String = "",
    @SerialName("image_res_key") val imageResKey: String = "electronics",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("image_key") val imageKey: String = "",
    @SerialName("gallery_res_csv") val galleryResCsv: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity(costPrice: Double = 0.0) = ProductEntity(
        id = id,
        sku = sku,
        barcode = barcode,
        titleEn = titleEn,
        titleBn = titleBn,
        brand = brand,
        categoryId = categoryId,
        descriptionEn = descriptionEn,
        descriptionBn = descriptionBn,
        regularPrice = regularPrice,
        salePrice = salePrice,
        costPrice = costPrice,
        stockQty = stockQty,
        lowStockThreshold = lowStockThreshold,
        variantsCsv = variantsCsv,
        colorsCsv = colorsCsv,
        specsPipe = specsPipe,
        tagsCsv = tagsCsv,
        isFeatured = isFeatured,
        isTrending = isTrending,
        isBestSeller = isBestSeller,
        isNewArrival = isNewArrival,
        isTodayDeal = isTodayDeal,
        ratingAvg = ratingAvg,
        reviewCount = reviewCount,
        soldCount = soldCount,
        seoTitle = seoTitle,
        seoDescription = seoDescription,
        imageResKey = imageResKey,
        imageUri = imageUrl,
        galleryResCsv = galleryResCsv,
        createdAtMillis = parseIsoToMillis(createdAt)
    )
}

@Serializable
data class ProductCostDto(
    @SerialName("product_id") val productId: String,
    @SerialName("cost_price") val costPrice: Double
)

@Serializable
data class BannerDto(
    val id: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_bn") val titleBn: String,
    @SerialName("subtitle_en") val subtitleEn: String = "",
    @SerialName("subtitle_bn") val subtitleBn: String = "",
    @SerialName("badge_text") val badgeText: String = "",
    @SerialName("target_category_id") val targetCategoryId: String = "",
    @SerialName("image_res_key") val imageResKey: String = "hero_mega_sale",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0
) {
    fun toEntity() = BannerEntity(id, titleEn, titleBn, subtitleEn, subtitleBn, badgeText, targetCategoryId, imageResKey, isActive, sortOrder)
}

@Serializable
data class FlashSaleDto(
    val id: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_bn") val titleBn: String,
    @SerialName("discount_percent") val discountPercent: Int = 10,
    @SerialName("start_time_millis") val startTimeMillis: Long,
    @SerialName("end_time_millis") val endTimeMillis: Long,
    @SerialName("product_ids_csv") val productIdsCsv: String = "",
    @SerialName("is_active") val isActive: Boolean = true
) {
    fun toEntity() = FlashSaleEntity(id, titleEn, titleBn, discountPercent, startTimeMillis, endTimeMillis, productIdsCsv, isActive)
}

@Serializable
data class CouponDto(
    val id: String,
    val code: String,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: Double,
    @SerialName("min_order_amount") val minOrderAmount: Double,
    @SerialName("max_discount") val maxDiscount: Double,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("expires_at_millis") val expiresAtMillis: Long,
    @SerialName("usage_count") val usageCount: Int = 0
) {
    fun toEntity() = CouponEntity(id, code, discountType, discountValue, minOrderAmount, maxDiscount, isActive, expiresAtMillis, usageCount)
}

@Serializable
data class CartItemDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("variant_label") val variantLabel: String = "Standard",
    val quantity: Int = 1,
    @SerialName("added_at") val addedAt: String? = null
) {
    fun toEntity() = CartItemEntity(id, userId, productId, variantLabel, quantity, parseIsoToMillis(addedAt))
}

@Serializable
data class WishlistItemDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("added_at") val addedAt: String? = null
) {
    fun toEntity() = WishlistItemEntity(id, userId, productId, parseIsoToMillis(addedAt))
}

@Serializable
data class ComparisonItemDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("added_at") val addedAt: String? = null
) {
    fun toEntity() = ComparisonItemEntity(id, userId, productId, parseIsoToMillis(addedAt))
}

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("guest_session_id") val guestSessionId: String = "",
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_phone") val customerPhone: String,
    @SerialName("shipping_address") val shippingAddress: String,
    val district: String,
    val area: String = "",
    val subtotal: Double,
    @SerialName("delivery_charge") val deliveryCharge: Double,
    @SerialName("delivery_cost_actual") val deliveryCostActual: Double = 60.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("loyalty_points_used") val loyaltyPointsUsed: Int = 0,
    @SerialName("loyalty_discount") val loyaltyDiscount: Double = 0.0,
    @SerialName("coupon_code") val couponCode: String = "",
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("order_status") val orderStatus: String,
    @SerialName("courier_name") val courierName: String = "Pending Assignment",
    @SerialName("tracking_number") val trackingNumber: String = "",
    @SerialName("is_guest_order") val isGuestOrder: Boolean = false,
    val notes: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toEntity() = OrderEntity(
        id = id,
        orderNumber = orderNumber,
        idempotencyKey = idempotencyKey,
        userId = userId ?: guestSessionId.ifBlank { "guest_local_device" },
        customerName = customerName,
        customerPhone = customerPhone,
        shippingAddress = shippingAddress,
        district = district,
        area = area,
        subtotal = subtotal,
        deliveryCharge = deliveryCharge,
        deliveryCostActual = deliveryCostActual,
        discountAmount = discountAmount,
        loyaltyPointsUsed = loyaltyPointsUsed,
        loyaltyDiscount = loyaltyDiscount,
        couponCode = couponCode,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        orderStatus = orderStatus,
        courierName = courierName,
        trackingNumber = trackingNumber,
        isGuestOrder = isGuestOrder,
        notes = notes,
        createdAtMillis = parseIsoToMillis(createdAt),
        updatedAtMillis = parseIsoToMillis(updatedAt)
    )
}

@Serializable
data class OrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_title_en") val productTitleEn: String,
    @SerialName("product_title_bn") val productTitleBn: String,
    @SerialName("variant_label") val variantLabel: String = "Standard",
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("cost_price") val costPrice: Double = 0.0,
    val quantity: Int
) {
    fun toEntity() = OrderItemEntity(id, orderId, productId, productTitleEn, productTitleBn, variantLabel, unitPrice, costPrice, quantity)
}

@Serializable
data class PaymentRecordDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("guest_session_id") val guestSessionId: String = "",
    val method: String,
    val amount: Double,
    @SerialName("sender_number") val senderNumber: String = "",
    @SerialName("transaction_id") val transactionId: String = "",
    val status: String,
    @SerialName("admin_note") val adminNote: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null
) {
    fun toEntity() = PaymentRecordEntity(
        id = id,
        orderId = orderId,
        orderNumber = orderNumber,
        userId = userId ?: guestSessionId.ifBlank { "guest_local_device" },
        method = method,
        amount = amount,
        senderNumber = senderNumber,
        transactionId = transactionId,
        status = status,
        adminNote = adminNote,
        createdAtMillis = parseIsoToMillis(createdAt),
        verifiedAtMillis = verifiedAt?.let { parseIsoToMillis(it) }
    )
}

@Serializable
data class ReviewDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String,
    val rating: Int,
    val comment: String,
    @SerialName("is_approved") val isApproved: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = ReviewEntity(id, productId, userId ?: "guest", userName, rating, comment, isApproved, parseIsoToMillis(createdAt))
}

@Serializable
data class LoyaltyTransactionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("order_id") val orderId: String = "",
    @SerialName("unique_award_key") val uniqueAwardKey: String,
    @SerialName("points_delta") val pointsDelta: Int,
    val type: String,
    val description: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = LoyaltyTransactionEntity(id, userId, orderId, uniqueAwardKey, pointsDelta, type, description, parseIsoToMillis(createdAt))
}

@Serializable
data class ReferralRecordDto(
    val id: String,
    @SerialName("referrer_user_id") val referrerUserId: String,
    @SerialName("referred_user_id") val referredUserId: String,
    @SerialName("referred_user_email") val referredUserEmail: String,
    @SerialName("referral_code") val referralCode: String,
    @SerialName("reward_points") val rewardPoints: Int,
    val status: String = "COMPLETED",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = ReferralRecordEntity(id, referrerUserId, referredUserId, referredUserEmail, referralCode, rewardPoints, status, parseIsoToMillis(createdAt))
}

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("target_scope") val targetScope: String = "USER",
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_bn") val titleBn: String,
    @SerialName("message_en") val messageEn: String,
    @SerialName("message_bn") val messageBn: String,
    val type: String = "INFO",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = NotificationEntity(
        id = id,
        userId = if (targetScope == "ALL") "ALL" else (userId ?: "ALL"),
        titleEn = titleEn,
        titleBn = titleBn,
        messageEn = messageEn,
        messageBn = messageBn,
        type = type,
        isRead = isRead,
        createdAtMillis = parseIsoToMillis(createdAt)
    )
}

@Serializable
data class SupportMessageDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_email") val senderEmail: String,
    val subject: String,
    val message: String,
    val status: String = "OPEN",
    @SerialName("admin_reply") val adminReply: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = SupportMessageEntity(id, userId ?: "guest_local_device", senderName, senderEmail, subject, message, status, adminReply, parseIsoToMillis(createdAt))
}

@Serializable
data class ActivityLogDto(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_email") val actorEmail: String,
    @SerialName("action_type") val actionType: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val summary: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = ActivityLogEntity(id, actorId ?: "", actorEmail, actionType, targetType, targetId, summary, parseIsoToMillis(createdAt))
}

@Serializable
data class WebsiteSettingsDto(
    val id: Int = 1,
    @SerialName("store_name") val storeName: String = "Kena Kata",
    @SerialName("tagline_en") val taglineEn: String = "Shop Smart, Buy Better.",
    @SerialName("tagline_bn") val taglineBn: String = "বুদ্ধিমান কেনাকাটা, সেরা মানের নিশ্চয়তা।",
    @SerialName("announcement_en") val announcementEn: String = "",
    @SerialName("announcement_bn") val announcementBn: String = "",
    @SerialName("bkash_merchant_number") val bkashMerchantNumber: String = "01711-009988 (Merchant)",
    @SerialName("nagad_merchant_number") val nagadMerchantNumber: String = "01811-009988 (Merchant)",
    @SerialName("whatsapp_number") val whatsappNumber: String = "8801711009988",
    @SerialName("support_email") val supportEmail: String = "support@kenakata.bd",
    @SerialName("dhaka_delivery_fee") val dhakaDeliveryFee: Double = 60.0,
    @SerialName("outside_dhaka_delivery_fee") val outsideDhakaDeliveryFee: Double = 120.0,
    @SerialName("loyalty_earn_per_100_bdt") val loyaltyEarnPer100Bdt: Int = 2,
    @SerialName("loyalty_point_value_bdt") val loyaltyPointValueBdt: Double = 1.0,
    @SerialName("referral_reward_points") val referralRewardPoints: Int = 50,
    @SerialName("newsletter_subscribers_count") val newsletterSubscribersCount: Int = 1420,
    @SerialName("show_flash_sale") val showFlashSale: Boolean = true,
    @SerialName("show_trending") val showTrending: Boolean = true,
    @SerialName("show_best_selling") val showBestSelling: Boolean = true,
    @SerialName("show_new_arrivals") val showNewArrivals: Boolean = true,
    @SerialName("show_today_deals") val showTodayDeals: Boolean = true,
    @SerialName("show_recommended") val showRecommended: Boolean = true,
    @SerialName("show_reviews") val showReviews: Boolean = true,
    @SerialName("seo_home_title") val seoHomeTitle: String = "",
    @SerialName("seo_home_description") val seoHomeDescription: String = ""
) {
    fun toEntity() = WebsiteSettingsEntity(
        id = id,
        storeName = storeName,
        taglineEn = taglineEn,
        taglineBn = taglineBn,
        announcementEn = announcementEn,
        announcementBn = announcementBn,
        bkashMerchantNumber = bkashMerchantNumber,
        nagadMerchantNumber = nagadMerchantNumber,
        whatsappNumber = whatsappNumber,
        supportEmail = supportEmail,
        dhakaDeliveryFee = dhakaDeliveryFee,
        outsideDhakaDeliveryFee = outsideDhakaDeliveryFee,
        loyaltyEarnPer100Bdt = loyaltyEarnPer100Bdt,
        loyaltyPointValueBdt = loyaltyPointValueBdt,
        referralRewardPoints = referralRewardPoints,
        newsletterSubscribersCount = newsletterSubscribersCount,
        showFlashSale = showFlashSale,
        showTrending = showTrending,
        showBestSelling = showBestSelling,
        showNewArrivals = showNewArrivals,
        showTodayDeals = showTodayDeals,
        showRecommended = showRecommended,
        showReviews = showReviews,
        seoHomeTitle = seoHomeTitle,
        seoHomeDescription = seoHomeDescription
    )
}

@Serializable
data class BackupLogDto(
    val id: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("record_count") val recordCount: Int = 0,
    val status: String,
    @SerialName("actor_email") val actorEmail: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toEntity() = BackupLogEntity(id, fileName, recordCount, status, actorEmail, parseIsoToMillis(createdAt))
}

@Serializable
data class MerchantStatusResponse(
    val ok: Boolean = false,
    val vendorMode: String = "SINGLE_VENDOR",
    val manualPaymentModeActive: Boolean = true,
    val bkashMerchantApiConfigured: Boolean = false,
    val nagadMerchantApiConfigured: Boolean = false,
    val message: String = ""
)

@Serializable
data class EdgeRecommendationsResponse(
    val ok: Boolean = false,
    val recommendedIds: List<String> = emptyList(),
    val trendingIds: List<String> = emptyList(),
    val source: String = "insforge-edge-function"
)

fun WebsiteSettingsEntity.toDto() = WebsiteSettingsDto(
    id = id,
    storeName = storeName,
    taglineEn = taglineEn,
    taglineBn = taglineBn,
    announcementEn = announcementEn,
    announcementBn = announcementBn,
    bkashMerchantNumber = bkashMerchantNumber,
    nagadMerchantNumber = nagadMerchantNumber,
    whatsappNumber = whatsappNumber,
    supportEmail = supportEmail,
    dhakaDeliveryFee = dhakaDeliveryFee,
    outsideDhakaDeliveryFee = outsideDhakaDeliveryFee,
    loyaltyEarnPer100Bdt = loyaltyEarnPer100Bdt,
    loyaltyPointValueBdt = loyaltyPointValueBdt,
    referralRewardPoints = referralRewardPoints,
    newsletterSubscribersCount = newsletterSubscribersCount,
    showFlashSale = showFlashSale,
    showTrending = showTrending,
    showBestSelling = showBestSelling,
    showNewArrivals = showNewArrivals,
    showTodayDeals = showTodayDeals,
    showRecommended = showRecommended,
    showReviews = showReviews,
    seoHomeTitle = seoHomeTitle,
    seoHomeDescription = seoHomeDescription
)

fun ProductEntity.toDto(imageKey: String = "") = ProductDto(
    id = id,
    sku = sku,
    barcode = barcode,
    titleEn = titleEn,
    titleBn = titleBn,
    brand = brand,
    categoryId = categoryId,
    descriptionEn = descriptionEn,
    descriptionBn = descriptionBn,
    regularPrice = regularPrice,
    salePrice = salePrice,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    variantsCsv = variantsCsv,
    colorsCsv = colorsCsv,
    specsPipe = specsPipe,
    tagsCsv = tagsCsv,
    isFeatured = isFeatured,
    isTrending = isTrending,
    isBestSeller = isBestSeller,
    isNewArrival = isNewArrival,
    isTodayDeal = isTodayDeal,
    ratingAvg = ratingAvg,
    reviewCount = reviewCount,
    soldCount = soldCount,
    seoTitle = seoTitle,
    seoDescription = seoDescription,
    imageResKey = imageResKey,
    imageUrl = imageUri,
    imageKey = imageKey,
    galleryResCsv = galleryResCsv
)
