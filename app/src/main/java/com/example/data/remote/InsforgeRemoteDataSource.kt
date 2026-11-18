package com.example.data.remote

import android.content.Context
import android.net.Uri
import dev.insforge.InsforgeClient
import dev.insforge.auth.auth
import dev.insforge.database.database
import dev.insforge.exceptions.InsforgeHttpException
import dev.insforge.functions.functions
import dev.insforge.storage.storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

sealed class RemoteAuthResult {
    data class Authenticated(val profile: ProfileDto) : RemoteAuthResult()
    data class VerificationRequired(val email: String) : RemoteAuthResult()
}

data class StorefrontBundle(
    val settings: WebsiteSettingsDto?,
    val categories: List<CategoryDto>,
    val products: List<ProductDto>,
    val banners: List<BannerDto>,
    val flashSales: List<FlashSaleDto>,
    val coupons: List<CouponDto>,
    val reviews: List<ReviewDto>,
    val publicNotifications: List<NotificationDto>
)

data class UserSyncBundle(
    val profile: ProfileDto,
    val addresses: List<AddressDto>,
    val cartItems: List<CartItemDto>,
    val wishlistItems: List<WishlistItemDto>,
    val comparisonItems: List<ComparisonItemDto>,
    val orders: List<OrderDto>,
    val orderItems: List<OrderItemDto>,
    val payments: List<PaymentRecordDto>,
    val loyaltyTransactions: List<LoyaltyTransactionDto>,
    val referrals: List<ReferralRecordDto>,
    val notifications: List<NotificationDto>,
    val supportMessages: List<SupportMessageDto>
)

data class AdminSyncBundle(
    val productCosts: List<ProductCostDto>,
    val profiles: List<ProfileDto>,
    val orders: List<OrderDto>,
    val orderItems: List<OrderItemDto>,
    val payments: List<PaymentRecordDto>,
    val cartItems: List<CartItemDto>,
    val supportMessages: List<SupportMessageDto>,
    val activityLogs: List<ActivityLogDto>,
    val backupLogs: List<BackupLogDto>
)

@Serializable
data class GatewayRecsRequest(
    val action: String = "recommendations",
    val recentlyViewedIds: List<String> = emptyList()
)

class InsforgeRemoteDataSource(
    private val context: Context,
    private val client: InsforgeClient
) {
    private fun readableError(t: Throwable): Throwable = when (t) {
        is InsforgeHttpException -> IllegalStateException(t.message ?: t.error ?: "Server request failed (${t.statusCode})")
        else -> t
    }

    suspend fun fetchStorefrontBundle(): Result<StorefrontBundle> = runCatching {
        val settings = client.database.from("website_settings").select().execute<WebsiteSettingsDto>().firstOrNull()
        val categories = client.database.from("categories").select().order("sort_order", ascending = true).execute<CategoryDto>()
        val products = client.database.from("products").select().order("created_at", ascending = false).execute<ProductDto>()
        val banners = client.database.from("banners").select().order("sort_order", ascending = true).execute<BannerDto>()
        val flashSales = client.database.from("flash_sales").select().order("end_time_millis", ascending = false).execute<FlashSaleDto>()
        val coupons = client.database.from("coupons").select().order("expires_at_millis", ascending = false).execute<CouponDto>()
        val reviews = client.database.from("reviews").select().order("created_at", ascending = false).execute<ReviewDto>()
        val publicNotifs = client.database.from("notifications").select().eq("target_scope", "ALL").order("created_at", ascending = false).execute<NotificationDto>()
        StorefrontBundle(settings, categories, products, banners, flashSales, coupons, reviews, publicNotifs)
    }.recoverCatching { throw readableError(it) }

    suspend fun signUpCustomer(
        name: String,
        email: String,
        phone: String,
        password: String,
        referralCodeInput: String
    ): Result<RemoteAuthResult> = runCatching {
        val cleanEmail = email.trim().lowercase()
        val res = client.auth.signUp(email = cleanEmail, password = password, name = name.trim())
        if (res.requireEmailVerification || res.accessToken == null) {
            RemoteAuthResult.VerificationRequired(cleanEmail)
        } else {
            val profile = client.database.rpc<ProfileDto>(
                "ensure_customer_profile_rpc",
                mapOf(
                    "p_name" to name.trim(),
                    "p_phone" to phone.trim(),
                    "p_referral_code_input" to referralCodeInput.trim().uppercase()
                )
            )
            RemoteAuthResult.Authenticated(profile)
        }
    }.recoverCatching { throw readableError(it) }

    suspend fun verifyEmailCode(email: String, code: String): Result<ProfileDto?> = runCatching {
        client.auth.verifyEmail(email = email.trim().lowercase(), otp = code.trim())
        val current = runCatching { client.auth.getCurrentUser() }.getOrNull()
        if (current != null) {
            client.database.rpc<ProfileDto>(
                "ensure_customer_profile_rpc",
                mapOf("p_name" to "", "p_phone" to "", "p_referral_code_input" to "")
            )
        } else {
            null
        }
    }.recoverCatching { throw readableError(it) }

    suspend fun resendVerificationEmail(email: String): Result<Unit> = runCatching {
        client.auth.sendVerificationEmail(email = email.trim().lowercase())
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun signIn(email: String, password: String, requireAdmin: Boolean): Result<ProfileDto> = runCatching {
        client.auth.signIn(email = email.trim().lowercase(), password = password)
        val profile = client.database.rpc<ProfileDto>(
            "ensure_customer_profile_rpc",
            mapOf("p_name" to "", "p_phone" to "", "p_referral_code_input" to "")
        )
        if (requireAdmin && profile.role != "ADMIN") {
            client.auth.signOut()
            throw SecurityException("Access denied: Owner Admin role required.")
        }
        if (profile.role == "ADMIN") {
            logAdminActivity(profile.id, profile.email, "ADMIN_LOGIN", "AUTH", profile.id, "Owner Admin signed in")
        }
        profile
    }.recoverCatching { throw readableError(it) }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        client.auth.sendPasswordReset(email = email.trim().lowercase())
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> = runCatching {
        val tokenResp = client.auth.exchangeResetPasswordToken(
            email = email.trim().lowercase(),
            code = code.trim()
        )
        client.auth.resetPassword(newPassword = newPassword, otp = tokenResp.token)
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun signOut() {
        runCatching { client.auth.signOut() }
    }

    suspend fun restoreActiveSessionProfile(): ProfileDto? = runCatching {
        client.auth.getCurrentUser()
        client.database.rpc<ProfileDto>(
            "ensure_customer_profile_rpc",
            mapOf("p_name" to "", "p_phone" to "", "p_referral_code_input" to "")
        )
    }.getOrNull()

    suspend fun fetchUserBundle(userId: String): Result<UserSyncBundle> = runCatching {
        val profile = client.database.from("profiles").select().eq("id", userId).execute<ProfileDto>().first()
        val addresses = client.database.from("addresses").select().eq("user_id", userId).execute<AddressDto>()
        val carts = client.database.from("cart_items").select().eq("user_id", userId).execute<CartItemDto>()
        val wish = client.database.from("wishlist_items").select().eq("user_id", userId).execute<WishlistItemDto>()
        val cmp = client.database.from("comparison_items").select().eq("user_id", userId).execute<ComparisonItemDto>()
        val orders = client.database.from("orders").select().eq("user_id", userId).order("created_at", ascending = false).execute<OrderDto>()
        val orderItems = client.database.from("order_items").select().execute<OrderItemDto>()
        val payments = client.database.from("payment_records").select().eq("user_id", userId).order("created_at", ascending = false).execute<PaymentRecordDto>()
        val loyalty = client.database.from("loyalty_transactions").select().eq("user_id", userId).order("created_at", ascending = false).execute<LoyaltyTransactionDto>()
        val referrals = client.database.from("referral_records").select().eq("referrer_user_id", userId).order("created_at", ascending = false).execute<ReferralRecordDto>()
        val notifs = client.database.from("notifications").select().order("created_at", ascending = false).execute<NotificationDto>()
        val support = client.database.from("support_messages").select().eq("user_id", userId).order("created_at", ascending = false).execute<SupportMessageDto>()
        UserSyncBundle(profile, addresses, carts, wish, cmp, orders, orderItems, payments, loyalty, referrals, notifs, support)
    }.recoverCatching { throw readableError(it) }

    suspend fun fetchAdminBundle(): Result<AdminSyncBundle> = runCatching {
        val costs = client.database.from("product_costs").select().execute<ProductCostDto>()
        val profiles = client.database.from("profiles").select().order("created_at", ascending = false).execute<ProfileDto>()
        val orders = client.database.from("orders").select().order("created_at", ascending = false).execute<OrderDto>()
        val items = client.database.from("order_items").select().execute<OrderItemDto>()
        val payments = client.database.from("payment_records").select().order("created_at", ascending = false).execute<PaymentRecordDto>()
        val carts = client.database.from("cart_items").select().execute<CartItemDto>()
        val support = client.database.from("support_messages").select().order("created_at", ascending = false).execute<SupportMessageDto>()
        val logs = client.database.from("activity_logs").select().order("created_at", ascending = false).limit(200).execute<ActivityLogDto>()
        val backups = client.database.from("backup_logs").select().order("created_at", ascending = false).execute<BackupLogDto>()
        AdminSyncBundle(costs, profiles, orders, items, payments, carts, support, logs, backups)
    }.recoverCatching { throw readableError(it) }

    suspend fun fetchGuestOrders(guestSessionId: String): List<OrderDto> = runCatching {
        client.database.rpc<List<OrderDto>>("get_guest_orders_rpc", mapOf("p_guest_session_id" to guestSessionId))
    }.getOrDefault(emptyList())

    suspend fun fetchOrderItemsRpc(orderId: String, guestSessionId: String): List<OrderItemDto> = runCatching {
        client.database.rpc<List<OrderItemDto>>(
            "get_order_items_rpc",
            mapOf("p_order_id" to orderId, "p_guest_session_id" to guestSessionId)
        )
    }.getOrDefault(emptyList())

    // Storage uploads (Android Photo Picker Uri -> InsForge Storage bucket)
    suspend fun uploadImageFromUri(bucket: String, uri: Uri, defaultName: String): Result<Pair<String, String>> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Unable to read selected image.")
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val uploaded = client.storage.from(bucket).uploadWithAutoKey("${defaultName}_${System.currentTimeMillis()}.$ext", bytes) {
            contentType = mime
        }
        uploaded.url to uploaded.key
    }.recoverCatching { throw readableError(it) }

    suspend fun updateProfile(userId: String, name: String, phone: String, avatarUrl: String?, avatarKey: String?): Result<ProfileDto> = runCatching {
        val body = buildJsonObject {
            put("name", name.trim())
            put("phone", phone.trim())
            if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
            if (!avatarKey.isNullOrBlank()) put("avatar_key", avatarKey)
        }
        val updated = client.database.from("profiles").update(body).eq("id", userId).returning().execute<ProfileDto>().first()
        runCatching {
            client.auth.updateProfile(
                buildMap {
                    put("name", name.trim())
                    if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
                }
            )
        }
        updated
    }.recoverCatching { throw readableError(it) }

    suspend fun insertAddress(dto: AddressDto): Result<Unit> = runCatching {
        client.database.from("addresses").insertTyped(listOf(dto)).returning().execute<AddressDto>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun deleteAddress(addressId: String): Result<Unit> = runCatching {
        client.database.from("addresses").delete().eq("id", addressId).execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun upsertCartItem(dto: CartItemDto, exists: Boolean): Result<Unit> = runCatching {
        if (exists) {
            client.database.from("cart_items")
                .update(buildJsonObject { put("quantity", dto.quantity) })
                .eq("id", dto.id)
                .execute<JsonObject>()
        } else {
            client.database.from("cart_items").insertTyped(listOf(dto)).execute<JsonObject>()
        }
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun deleteCartItem(id: String): Result<Unit> = runCatching {
        client.database.from("cart_items").delete().eq("id", id).execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun setWishlist(userId: String, productId: String, save: Boolean): Result<Unit> = runCatching {
        val id = "w_${userId}_$productId"
        if (save) {
            client.database.from("wishlist_items").insertTyped(listOf(WishlistItemDto(id, userId, productId))).execute<JsonObject>()
        } else {
            client.database.from("wishlist_items").delete().eq("user_id", userId).eq("product_id", productId).execute<JsonObject>()
        }
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun setCompare(userId: String, productId: String, save: Boolean): Result<Unit> = runCatching {
        val id = "cmp_${userId}_$productId"
        if (save) {
            client.database.from("comparison_items").insertTyped(listOf(ComparisonItemDto(id, userId, productId))).execute<JsonObject>()
        } else {
            client.database.from("comparison_items").delete().eq("user_id", userId).eq("product_id", productId).execute<JsonObject>()
        }
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun placeOrderRpc(
        idempotencyKey: String,
        guestSessionId: String,
        items: List<Triple<String, String, Int>>,
        customerName: String,
        customerPhone: String,
        district: String,
        area: String,
        streetAddress: String,
        couponCode: String,
        redeemLoyalty: Boolean,
        paymentMethod: String,
        senderNumber: String,
        transactionId: String,
        notes: String
    ): Result<OrderDto> = runCatching {
        val itemsJson: JsonArray = buildJsonArray {
            items.forEach { (pid, variant, qty) ->
                add(
                    buildJsonObject {
                        put("product_id", pid)
                        put("variant_label", variant)
                        put("quantity", qty)
                    }
                )
            }
        }
        client.database.rpc<OrderDto>(
            "place_order_rpc",
            mapOf(
                "p_idempotency_key" to idempotencyKey,
                "p_guest_session_id" to guestSessionId,
                "p_items" to itemsJson,
                "p_customer_name" to customerName,
                "p_customer_phone" to customerPhone,
                "p_district" to district,
                "p_area" to area,
                "p_street_address" to streetAddress,
                "p_coupon_code" to couponCode,
                "p_redeem_loyalty" to redeemLoyalty,
                "p_payment_method" to paymentMethod,
                "p_sender_number" to senderNumber,
                "p_transaction_id" to transactionId,
                "p_notes" to notes
            )
        )
    }.recoverCatching { throw readableError(it) }

    suspend fun cancelOrReturnOrderRpc(orderId: String, isReturn: Boolean, guestSessionId: String): Result<Unit> = runCatching {
        client.database.rpcRaw(
            "customer_cancel_or_return_order_rpc",
            mapOf("p_order_id" to orderId, "p_is_return" to isReturn, "p_guest_session_id" to guestSessionId)
        )
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun addReview(dto: ReviewDto): Result<Unit> = runCatching {
        client.database.from("reviews").insertTyped(listOf(dto)).execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun markNotificationsRead(userId: String): Result<Unit> = runCatching {
        client.database.from("notifications")
            .update(buildJsonObject { put("is_read", true) })
            .eq("user_id", userId)
            .execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun insertSupportMessage(dto: SupportMessageDto): Result<Unit> = runCatching {
        client.database.from("support_messages").insertTyped(listOf(dto)).execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun subscribeNewsletterRpc(): Result<Int> = runCatching {
        client.database.rpc<Int>("subscribe_newsletter_rpc")
    }.recoverCatching { throw readableError(it) }

    // ============ ADMIN REMOTE OPERATIONS ============

    suspend fun adminSaveProduct(adminId: String, adminEmail: String, dto: ProductDto, costPrice: Double, isNew: Boolean): Result<Unit> = runCatching {
        if (isNew) {
            client.database.from("products").insertTyped(listOf(dto)).returning().execute<ProductDto>()
            client.database.from("product_costs").insertTyped(listOf(ProductCostDto(dto.id, costPrice))).execute<JsonObject>()
        } else {
            val payload = buildJsonObject {
                put("sku", dto.sku)
                put("barcode", dto.barcode)
                put("title_en", dto.titleEn)
                put("title_bn", dto.titleBn)
                put("brand", dto.brand)
                put("category_id", dto.categoryId)
                put("description_en", dto.descriptionEn)
                put("description_bn", dto.descriptionBn)
                put("regular_price", dto.regularPrice)
                put("sale_price", dto.salePrice)
                put("stock_qty", dto.stockQty)
                put("low_stock_threshold", dto.lowStockThreshold)
                put("variants_csv", dto.variantsCsv)
                put("colors_csv", dto.colorsCsv)
                put("specs_pipe", dto.specsPipe)
                put("tags_csv", dto.tagsCsv)
                put("is_featured", dto.isFeatured)
                put("is_trending", dto.isTrending)
                put("is_best_seller", dto.isBestSeller)
                put("is_new_arrival", dto.isNewArrival)
                put("is_today_deal", dto.isTodayDeal)
                put("seo_title", dto.seoTitle)
                put("seo_description", dto.seoDescription)
                put("image_res_key", dto.imageResKey)
                put("image_url", dto.imageUrl)
                if (dto.imageKey.isNotBlank()) put("image_key", dto.imageKey)
            }
            client.database.from("products").update(payload).eq("id", dto.id).execute<JsonObject>()
            val existingCost = client.database.from("product_costs").select().eq("product_id", dto.id).execute<ProductCostDto>()
            if (existingCost.isEmpty()) {
                client.database.from("product_costs").insertTyped(listOf(ProductCostDto(dto.id, costPrice))).execute<JsonObject>()
            } else {
                client.database.from("product_costs").update(buildJsonObject { put("cost_price", costPrice) }).eq("product_id", dto.id).execute<JsonObject>()
            }
        }
        logAdminActivity(adminId, adminEmail, if (isNew) "PRODUCT_CREATE" else "PRODUCT_UPDATE", "PRODUCT", dto.id, "${if (isNew) "Added" else "Updated"} ${dto.titleEn} (${dto.sku})")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminDeleteProduct(adminId: String, adminEmail: String, productId: String, title: String): Result<Unit> = runCatching {
        client.database.from("products").delete().eq("id", productId).execute<JsonObject>()
        logAdminActivity(adminId, adminEmail, "PRODUCT_DELETE", "PRODUCT", productId, "Deleted $title")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminSaveCategory(adminId: String, adminEmail: String, dto: CategoryDto): Result<Unit> = runCatching {
        val existing = client.database.from("categories").select().eq("id", dto.id).execute<CategoryDto>()
        if (existing.isEmpty()) {
            client.database.from("categories").insertTyped(listOf(dto)).execute<JsonObject>()
        } else {
            client.database.from("categories").update(
                buildJsonObject {
                    put("name_en", dto.nameEn)
                    put("name_bn", dto.nameBn)
                    put("slug", dto.slug)
                    put("icon_key", dto.iconKey)
                    put("sort_order", dto.sortOrder)
                    put("is_featured", dto.isFeatured)
                }
            ).eq("id", dto.id).execute<JsonObject>()
        }
        logAdminActivity(adminId, adminEmail, "CATEGORY_SAVE", "CATEGORY", dto.id, "Saved category ${dto.nameEn}")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminSaveCoupon(adminId: String, adminEmail: String, dto: CouponDto): Result<Unit> = runCatching {
        val existing = client.database.from("coupons").select().eq("id", dto.id).execute<CouponDto>()
        if (existing.isEmpty()) {
            client.database.from("coupons").insertTyped(listOf(dto)).execute<JsonObject>()
        } else {
            client.database.from("coupons").update(
                buildJsonObject {
                    put("code", dto.code)
                    put("discount_type", dto.discountType)
                    put("discount_value", dto.discountValue)
                    put("min_order_amount", dto.minOrderAmount)
                    put("max_discount", dto.maxDiscount)
                    put("is_active", dto.isActive)
                    put("expires_at_millis", dto.expiresAtMillis)
                }
            ).eq("id", dto.id).execute<JsonObject>()
        }
        logAdminActivity(adminId, adminEmail, "COUPON_SAVE", "COUPON", dto.id, "Saved coupon ${dto.code}")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminToggleFlashSale(adminId: String, adminEmail: String, dto: FlashSaleDto): Result<Unit> = runCatching {
        client.database.from("flash_sales")
            .update(buildJsonObject { put("is_active", dto.isActive) })
            .eq("id", dto.id)
            .execute<JsonObject>()
        logAdminActivity(adminId, adminEmail, "FLASH_SALE_UPDATE", "FLASH_SALE", dto.id, "Updated flash sale ${dto.titleEn} (active=${dto.isActive})")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminVerifyPaymentRpc(paymentId: String, approve: Boolean, note: String): Result<Unit> = runCatching {
        client.database.rpcRaw(
            "admin_verify_payment_rpc",
            mapOf("p_payment_id" to paymentId, "p_approve" to approve, "p_note" to note)
        )
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminRefundPaymentRpc(paymentId: String): Result<Unit> = runCatching {
        client.database.rpcRaw("admin_refund_payment_rpc", mapOf("p_payment_id" to paymentId))
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminUpdateOrderRpc(orderId: String, status: String, courier: String, tracking: String): Result<Unit> = runCatching {
        client.database.rpcRaw(
            "admin_update_order_rpc",
            mapOf(
                "p_order_id" to orderId,
                "p_new_status" to status,
                "p_courier_name" to courier,
                "p_tracking_number" to tracking
            )
        )
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminReplySupport(adminId: String, adminEmail: String, msgId: String, targetUserId: String?, senderEmail: String, subject: String, reply: String): Result<Unit> = runCatching {
        client.database.from("support_messages")
            .update(buildJsonObject { put("status", "REPLIED"); put("admin_reply", reply.trim()) })
            .eq("id", msgId)
            .execute<JsonObject>()
        if (!targetUserId.isNullOrBlank() && targetUserId != "guest_local_device") {
            client.database.from("notifications").insertTyped(
                listOf(
                    NotificationDto(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        userId = targetUserId,
                        targetScope = "USER",
                        titleEn = "Support Reply: $subject",
                        titleBn = "সাপোর্ট উত্তর: $subject",
                        messageEn = reply.trim(),
                        messageBn = reply.trim(),
                        type = "SUPPORT"
                    )
                )
            ).execute<JsonObject>()
        }
        logAdminActivity(adminId, adminEmail, "SUPPORT_REPLY", "SUPPORT", msgId, "Replied to $senderEmail")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminDeleteReview(adminId: String, adminEmail: String, reviewId: String, productId: String): Result<Unit> = runCatching {
        client.database.from("reviews").delete().eq("id", reviewId).execute<JsonObject>()
        logAdminActivity(adminId, adminEmail, "REVIEW_DELETE", "REVIEW", reviewId, "Removed review on $productId")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminSaveSettings(adminId: String, adminEmail: String, dto: WebsiteSettingsDto): Result<Unit> = runCatching {
        client.database.from("website_settings").update(
            buildJsonObject {
                put("announcement_en", dto.announcementEn)
                put("announcement_bn", dto.announcementBn)
                put("bkash_merchant_number", dto.bkashMerchantNumber)
                put("nagad_merchant_number", dto.nagadMerchantNumber)
                put("whatsapp_number", dto.whatsappNumber)
                put("seo_home_title", dto.seoHomeTitle)
                put("seo_home_description", dto.seoHomeDescription)
                put("show_flash_sale", dto.showFlashSale)
                put("show_recommended", dto.showRecommended)
                put("dhaka_delivery_fee", dto.dhakaDeliveryFee)
                put("outside_dhaka_delivery_fee", dto.outsideDhakaDeliveryFee)
            }
        ).eq("id", 1).execute<JsonObject>()
        logAdminActivity(adminId, adminEmail, "SETTINGS_UPDATE", "SETTINGS", "1", "Updated website, payment, delivery & SEO settings")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminSendCartRecovery(adminId: String, adminEmail: String, targetUserId: String): Result<Unit> = runCatching {
        client.database.from("notifications").insertTyped(
            listOf(
                NotificationDto(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    userId = targetUserId,
                    targetScope = "USER",
                    titleEn = "Items waiting in your Kena Kata Cart!",
                    titleBn = "আপনার কার্টে পণ্য অপেক্ষা করছে!",
                    messageEn = "Complete your order today and apply code KENA10 for 10% OFF.",
                    messageBn = "আজই অর্ডার সম্পন্ন করুন এবং KENA10 কোডে ১০% ছাড় পান।",
                    type = "CART_RECOVERY"
                )
            )
        ).execute<JsonObject>()
        logAdminActivity(adminId, adminEmail, "CART_RECOVERY_SEND", "CART", targetUserId, "Sent recovery reminder to $targetUserId")
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun adminInsertBackupLog(log: BackupLogDto): Result<Unit> = runCatching {
        client.database.from("backup_logs").insertTyped(listOf(log)).execute<JsonObject>()
        Unit
    }.recoverCatching { throw readableError(it) }

    suspend fun logAdminActivity(actorId: String, actorEmail: String, actionType: String, targetType: String, targetId: String, summary: String) {
        runCatching {
            client.database.from("activity_logs").insertTyped(
                listOf(
                    ActivityLogDto(
                        id = "log_${UUID.randomUUID().toString().take(10)}",
                        actorId = actorId,
                        actorEmail = actorEmail,
                        actionType = actionType,
                        targetType = targetType,
                        targetId = targetId,
                        summary = summary
                    )
                )
            ).execute<JsonObject>()
            Unit
        }
    }

    // Edge Functions (kena-kata-gateway)
    suspend fun fetchMerchantGatewayStatus(): Result<MerchantStatusResponse> = runCatching {
        client.functions.invoke<MerchantStatusResponse>(
            slug = "kena-kata-gateway",
            body = mapOf("action" to "merchant_status")
        )
    }

    suspend fun fetchEdgeRecommendations(recentlyViewedIds: List<String>): Result<EdgeRecommendationsResponse> = runCatching {
        client.functions.invoke<EdgeRecommendationsResponse>(
            slug = "kena-kata-gateway",
            body = GatewayRecsRequest(action = "recommendations", recentlyViewedIds = recentlyViewedIds)
        )
    }
}
