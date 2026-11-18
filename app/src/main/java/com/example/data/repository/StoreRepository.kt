package com.example.data.repository

import android.net.Uri
import com.example.data.local.SeedData
import com.example.data.local.dao.KenaKataDao
import com.example.data.local.entity.*
import com.example.data.remote.*
import com.example.util.BackupRestoreResult
import com.example.util.CsvAndBackupUtils
import com.example.util.CsvImportResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class RegisterOutcome {
    data class Authenticated(val user: UserEntity) : RegisterOutcome()
    data class VerificationRequired(val email: String) : RegisterOutcome()
}

data class AnalyticsSummary(
    val grossSales: Double,
    val totalDiscounts: Double,
    val totalRefunds: Double,
    val totalCogs: Double,
    val totalDeliveryExpenses: Double,
    val netRevenue: Double,
    val netProfit: Double,
    val todayRevenue: Double,
    val monthlyRevenue: Double,
    val totalOrders: Int,
    val pendingOrders: Int,
    val deliveredOrders: Int,
    val cancelledOrReturnedOrders: Int,
    val totalCustomers: Int,
    val totalProducts: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val bkashCount: Int,
    val nagadCount: Int,
    val codCount: Int,
    val dailyRevenueBars: List<Pair<String, Double>>
)

class StoreRepository(
    private val dao: KenaKataDao,
    private val remote: InsforgeRemoteDataSource? = null
) {

    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val banners: Flow<List<BannerEntity>> = dao.observeBanners()
    val flashSales: Flow<List<FlashSaleEntity>> = dao.observeFlashSales()
    val coupons: Flow<List<CouponEntity>> = dao.observeCoupons()
    val reviews: Flow<List<ReviewEntity>> = dao.observeAllReviews()
    val settings: Flow<WebsiteSettingsEntity?> = dao.observeWebsiteSettings()

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)
    fun observeAddresses(userId: String): Flow<List<AddressEntity>> = dao.observeAddresses(userId)
    fun observeCart(userId: String): Flow<List<CartItemEntity>> = dao.observeCart(userId)
    fun observeWishlist(userId: String): Flow<List<WishlistItemEntity>> = dao.observeWishlist(userId)
    fun observeComparison(userId: String): Flow<List<ComparisonItemEntity>> = dao.observeComparison(userId)
    fun observeUserOrders(userId: String): Flow<List<OrderEntity>> = dao.observeUserOrders(userId)
    fun observeLoyaltyTransactions(userId: String): Flow<List<LoyaltyTransactionEntity>> = dao.observeLoyaltyTransactions(userId)
    fun observeReferrals(userId: String): Flow<List<ReferralRecordEntity>> = dao.observeReferralsByUser(userId)
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = dao.observeNotifications(userId)
    fun observeUserSupportMessages(userId: String): Flow<List<SupportMessageEntity>> = dao.observeUserSupportMessages(userId)

    // Admin Observers
    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()
    fun observeAllOrders(): Flow<List<OrderEntity>> = dao.observeAllOrders()
    fun observeAllOrderItems(): Flow<List<OrderItemEntity>> = dao.observeAllOrderItems()
    fun observeAllPayments(): Flow<List<PaymentRecordEntity>> = dao.observeAllPayments()
    fun observeAllSupportMessages(): Flow<List<SupportMessageEntity>> = dao.observeAllSupportMessages()
    fun observeAllReferrals(): Flow<List<ReferralRecordEntity>> = dao.observeAllReferrals()
    fun observeAllCartItems(): Flow<List<CartItemEntity>> = dao.observeAllCartItems()
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>> = dao.observeActivityLogs()
    fun observeBackupLogs(): Flow<List<BackupLogEntity>> = dao.observeBackupLogs()

    private fun isCloudAuthenticatedId(userId: String): Boolean {
        return remote != null &&
            userId.isNotBlank() &&
            !userId.startsWith("guest_") &&
            !userId.startsWith("usr_")
    }

    // ============ INSFORGE CLOUD SYNC ============

    suspend fun syncStorefrontFromInsforge(): Result<Unit> {
        val r = remote ?: return Result.success(Unit)
        return r.fetchStorefrontBundle().mapCatching { bundle ->
            val existingCostById = dao.getProductsOnce().associate { it.id to it.costPrice }
            dao.syncStorefrontCatalog(
                settings = bundle.settings?.toEntity(),
                categories = bundle.categories.map { it.toEntity() },
                products = bundle.products.map { it.toEntity(costPrice = existingCostById[it.id] ?: 0.0) },
                banners = bundle.banners.map { it.toEntity() },
                flashSales = bundle.flashSales.map { it.toEntity() },
                coupons = bundle.coupons.map { it.toEntity() },
                reviews = bundle.reviews.map { it.toEntity() },
                publicNotifications = bundle.publicNotifications.map { it.toEntity() }
            )
        }
    }

    suspend fun restoreRemoteSession(): UserEntity? {
        val r = remote ?: return null
        val profile = r.restoreActiveSessionProfile() ?: return null
        val user = profile.toEntity()
        dao.upsertUser(user)
        syncUserBundle(user.id)
        if (user.role == "ADMIN") {
            syncAdminBundle()
        }
        return user
    }

    suspend fun syncUserBundle(userId: String): Result<UserEntity> {
        val r = remote ?: return Result.failure(IllegalStateException("Cloud backend unavailable"))
        return r.fetchUserBundle(userId).mapCatching { bundle ->
            val user = bundle.profile.toEntity()
            dao.syncUserWorkspace(
                profile = user,
                addresses = bundle.addresses.map { it.toEntity() },
                cartItems = bundle.cartItems.map { it.toEntity() },
                wishlistItems = bundle.wishlistItems.map { it.toEntity() },
                comparisonItems = bundle.comparisonItems.map { it.toEntity() },
                orders = bundle.orders.map { it.toEntity() },
                orderItems = bundle.orderItems.map { it.toEntity() },
                payments = bundle.payments.map { it.toEntity() },
                loyaltyTransactions = bundle.loyaltyTransactions.map { it.toEntity() },
                referrals = bundle.referrals.map { it.toEntity() },
                notifications = bundle.notifications.map { it.toEntity() },
                supportMessages = bundle.supportMessages.map { it.toEntity() }
            )
            user
        }
    }

    suspend fun syncAdminBundle(): Result<Unit> {
        val r = remote ?: return Result.success(Unit)
        return r.fetchAdminBundle().mapCatching { bundle ->
            val costMap = bundle.productCosts.associate { it.productId to it.costPrice }
            val updatedProducts = dao.getProductsOnce().map { prod ->
                prod.copy(costPrice = costMap[prod.id] ?: prod.costPrice)
            }
            if (updatedProducts.isNotEmpty()) {
                dao.upsertProducts(updatedProducts)
            }
            dao.upsertUsers(bundle.profiles.map { it.toEntity() })
            dao.upsertOrders(bundle.orders.map { it.toEntity() })
            dao.insertOrderItems(bundle.orderItems.map { it.toEntity() })
            dao.upsertPaymentRecords(bundle.payments.map { it.toEntity() })
            dao.upsertCartItems(bundle.cartItems.map { it.toEntity() })
            dao.upsertSupportMessages(bundle.supportMessages.map { it.toEntity() })
            dao.upsertActivityLogs(bundle.activityLogs.map { it.toEntity() })
            dao.upsertBackupLogs(bundle.backupLogs.map { it.toEntity() })
        }
    }

    suspend fun syncGuestOrders(guestSessionId: String) {
        val r = remote ?: return
        if (guestSessionId.isBlank()) return
        val guestOrders = r.fetchGuestOrders(guestSessionId)
        if (guestOrders.isNotEmpty()) {
            dao.upsertOrders(guestOrders.map { it.toEntity() })
            guestOrders.forEach { o ->
                val items = r.fetchOrderItemsRpc(o.id, guestSessionId)
                if (items.isNotEmpty()) {
                    dao.insertOrderItems(items.map { it.toEntity() })
                }
            }
        }
    }

    suspend fun fetchMerchantGatewayStatus(): Result<MerchantStatusResponse> {
        val r = remote ?: return Result.success(
            MerchantStatusResponse(
                ok = false,
                manualPaymentModeActive = true,
                message = "Offline local cache mode"
            )
        )
        return r.fetchMerchantGatewayStatus()
    }

    // ============ AUTHENTICATION & PROFILE ============

    suspend fun registerCustomerWithOutcome(
        name: String,
        email: String,
        phone: String,
        password: String,
        referralCodeInput: String
    ): Result<RegisterOutcome> {
        val cleanEmail = email.trim().lowercase()
        if (name.trim().length < 2) return Result.failure(IllegalArgumentException("Name is too short."))
        if (!cleanEmail.contains("@")) return Result.failure(IllegalArgumentException("Enter a valid email address."))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))

        if (remote != null) {
            return remote.signUpCustomer(name, cleanEmail, phone, password, referralCodeInput).map { authRes ->
                when (authRes) {
                    is RemoteAuthResult.VerificationRequired -> RegisterOutcome.VerificationRequired(authRes.email)
                    is RemoteAuthResult.Authenticated -> {
                        val user = authRes.profile.toEntity()
                        dao.upsertUser(user)
                        syncUserBundle(user.id)
                        RegisterOutcome.Authenticated(user)
                    }
                }
            }
        }

        // Offline / Room fallback
        if (dao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }

        val uid = "usr_${UUID.randomUUID().toString().take(10)}"
        val myRefCode = "KK${name.filter { it.isLetterOrDigit() }.uppercase().take(4)}${(1000..9999).random()}"
        val cleanRefInput = referralCodeInput.trim().uppercase()
        val referrer = if (cleanRefInput.isNotBlank()) dao.getUserByReferralCode(cleanRefInput) else null
        if (cleanRefInput.isNotBlank() && referrer == null) {
            return Result.failure(IllegalArgumentException("Invalid referral code."))
        }
        if (referrer != null && (referrer.email.equals(cleanEmail, ignoreCase = true) || referrer.phone == phone.trim() && phone.isNotBlank())) {
            return Result.failure(IllegalArgumentException("Self-referral is not allowed."))
        }

        val settings = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
        val newUser = UserEntity(
            id = uid,
            name = name.trim(),
            email = cleanEmail,
            phone = phone.trim(),
            passwordHash = SeedData.hashPassword(password),
            role = "CUSTOMER", // Single-vendor: registrations are strictly CUSTOMER only
            emailVerified = false,
            loyaltyPoints = if (referrer != null) settings.referralRewardPoints else 20,
            referralCode = myRefCode,
            referredByCode = referrer?.referralCode ?: ""
        )
        dao.upsertUser(newUser)

        if (referrer != null) {
            dao.upsertReferral(
                ReferralRecordEntity(
                    id = "ref_${UUID.randomUUID().toString().take(8)}",
                    referrerUserId = referrer.id,
                    referredUserId = newUser.id,
                    referredUserEmail = newUser.email,
                    referralCode = referrer.referralCode,
                    rewardPoints = settings.referralRewardPoints,
                    status = "COMPLETED"
                )
            )
            val key = "REFERRAL_${newUser.id}"
            if (dao.getLoyaltyByUniqueKey(key) == null) {
                dao.insertLoyaltyTransaction(
                    LoyaltyTransactionEntity(
                        id = "loy_${UUID.randomUUID().toString().take(8)}",
                        userId = referrer.id,
                        orderId = "",
                        uniqueAwardKey = key,
                        pointsDelta = settings.referralRewardPoints,
                        type = "REFERRAL",
                        description = "Referral reward for inviting ${newUser.name}"
                    )
                )
                dao.adjustUserLoyaltyPoints(referrer.id, settings.referralRewardPoints)
            }
        }
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = newUser.id,
                titleEn = "Welcome to Kena Kata!",
                titleBn = "কেনা কাটায় স্বাগতম!",
                messageEn = "Verify your email in Account to unlock full loyalty benefits.",
                messageBn = "লয়্যালটি সুবিধা পেতে আপনার ইমেইল ভেরিফাই করুন।",
                type = "ACCOUNT"
            )
        )
        return Result.success(RegisterOutcome.Authenticated(newUser))
    }

    suspend fun registerCustomer(
        name: String,
        email: String,
        phone: String,
        password: String,
        referralCodeInput: String
    ): Result<UserEntity> {
        return registerCustomerWithOutcome(name, email, phone, password, referralCodeInput).mapCatching { outcome ->
            when (outcome) {
                is RegisterOutcome.Authenticated -> outcome.user
                is RegisterOutcome.VerificationRequired -> throw IllegalStateException("Email verification code sent to ${outcome.email}")
            }
        }
    }

    suspend fun verifyEmailWithCode(email: String, code: String): Result<UserEntity?> {
        val r = remote
        if (r != null) {
            return r.verifyEmailCode(email, code).map { profileDto ->
                val user = profileDto?.toEntity()
                if (user != null) {
                    dao.upsertUser(user.copy(emailVerified = true))
                    syncUserBundle(user.id)
                    user.copy(emailVerified = true)
                } else {
                    val local = dao.getUserByEmail(email.trim())
                    if (local != null) {
                        val updated = local.copy(emailVerified = true)
                        dao.upsertUser(updated)
                        updated
                    } else null
                }
            }
        }
        val local = dao.getUserByEmail(email.trim()) ?: return Result.failure(IllegalStateException("User not found."))
        val updated = local.copy(emailVerified = true)
        dao.upsertUser(updated)
        return Result.success(updated)
    }

    suspend fun resendVerificationEmail(email: String): Result<Unit> {
        val r = remote ?: return Result.success(Unit)
        return r.resendVerificationEmail(email)
    }

    suspend fun login(email: String, password: String, requireAdmin: Boolean): Result<UserEntity> {
        if (remote != null) {
            return remote.signIn(email, password, requireAdmin).map { profile ->
                val entity = profile.toEntity()
                dao.upsertUser(entity)
                syncUserBundle(entity.id)
                syncStorefrontFromInsforge()
                if (entity.role == "ADMIN") {
                    syncAdminBundle()
                }
                entity
            }
        }

        val user = dao.getUserByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("No account found for this email."))
        if (user.passwordHash != SeedData.hashPassword(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        if (requireAdmin && user.role != "ADMIN") {
            return Result.failure(SecurityException("Access denied: Owner Admin role required."))
        }
        if (user.role == "ADMIN") {
            logActivity(user, "ADMIN_LOGIN", "AUTH", user.id, "Owner Admin signed in")
        }
        return Result.success(user)
    }

    suspend fun logout() {
        remote?.signOut()
    }

    suspend fun verifyCustomerEmail(userId: String): Result<Unit> {
        val user = dao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found."))
        if (remote != null) {
            return remote.resendVerificationEmail(user.email)
        }
        dao.upsertUser(user.copy(emailVerified = true))
        return Result.success(Unit)
    }

    suspend fun sendPasswordResetCode(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (!clean.contains("@")) return Result.failure(IllegalArgumentException("Enter a valid email address."))
        val r = remote ?: return Result.success(Unit)
        return r.sendPasswordReset(clean)
    }

    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        if (remote != null) {
            if (code.trim().length < 4) return Result.failure(IllegalArgumentException("Enter the 6-digit verification code sent to your email."))
            return remote.resetPasswordWithCode(email, code, newPassword)
        }
        return resetPassword(email, newPassword)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        val user = dao.getUserByEmail(email.trim()) ?: return Result.failure(IllegalArgumentException("Email not found."))
        if (user.role == "ADMIN") return Result.failure(SecurityException("Owner Admin password cannot be reset via public form."))
        dao.upsertUser(user.copy(passwordHash = SeedData.hashPassword(newPassword)))
        return Result.success(Unit)
    }

    suspend fun updateProfile(userId: String, name: String, phone: String, avatarUri: String? = null): Result<Unit> {
        if (name.trim().length < 2) return Result.failure(IllegalArgumentException("Name too short."))
        if (isCloudAuthenticatedId(userId)) {
            var finalAvatarUrl: String? = avatarUri
            var avatarKey: String? = null
            if (!avatarUri.isNullOrBlank() && avatarUri.startsWith("content://")) {
                val uploaded = remote!!.uploadImageFromUri("avatars", Uri.parse(avatarUri), "avatar_${userId.take(8)}")
                if (uploaded.isFailure) return Result.failure(uploaded.exceptionOrNull()!!)
                val pair = uploaded.getOrThrow()
                finalAvatarUrl = pair.first
                avatarKey = pair.second
            }
            return remote!!.updateProfile(userId, name, phone, finalAvatarUrl, avatarKey).map { updated ->
                dao.upsertUser(updated.toEntity())
            }
        }

        val user = dao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found."))
        dao.upsertUser(
            user.copy(
                name = name.trim(),
                phone = phone.trim(),
                avatarUri = avatarUri ?: user.avatarUri
            )
        )
        return Result.success(Unit)
    }

    suspend fun saveAddress(userId: String, label: String, fullName: String, phone: String, district: String, area: String, street: String) {
        val entity = AddressEntity(
            id = "addr_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            label = label.ifBlank { "Home" },
            fullName = fullName,
            phone = phone,
            district = district,
            area = area,
            streetAddress = street,
            isDefault = true
        )
        if (isCloudAuthenticatedId(userId)) {
            remote!!.insertAddress(
                AddressDto(
                    id = entity.id,
                    userId = entity.userId,
                    label = entity.label,
                    fullName = entity.fullName,
                    phone = entity.phone,
                    district = entity.district,
                    area = entity.area,
                    streetAddress = entity.streetAddress,
                    isDefault = entity.isDefault
                )
            )
        }
        dao.upsertAddress(entity)
    }

    suspend fun deleteAddress(addressId: String, userId: String = "") {
        if (isCloudAuthenticatedId(userId) || (remote != null && !addressId.startsWith("addr_seed"))) {
            remote?.deleteAddress(addressId)
        }
        dao.deleteAddress(addressId)
    }

    // ============ CART, WISHLIST, COMPARE ============

    suspend fun addToCart(userId: String, product: ProductEntity, variant: String, qty: Int = 1): Result<Unit> {
        if (product.stockQty <= 0) return Result.failure(IllegalStateException("Product is out of stock."))
        val existing = dao.findCartItem(userId, product.id, variant)
        val newQty = (existing?.quantity ?: 0) + qty
        if (newQty > product.stockQty) {
            return Result.failure(IllegalStateException("Only ${product.stockQty} left in stock."))
        }
        val targetEntity = existing?.copy(quantity = newQty, addedAtMillis = System.currentTimeMillis())
            ?: CartItemEntity(
                id = "crt_${UUID.randomUUID().toString().take(10)}",
                userId = userId,
                productId = product.id,
                variantLabel = variant,
                quantity = qty
            )
        if (isCloudAuthenticatedId(userId)) {
            val remoteRes = remote!!.upsertCartItem(
                CartItemDto(
                    id = targetEntity.id,
                    userId = targetEntity.userId,
                    productId = targetEntity.productId,
                    variantLabel = targetEntity.variantLabel,
                    quantity = targetEntity.quantity
                ),
                exists = existing != null
            )
            if (remoteRes.isFailure) return Result.failure(remoteRes.exceptionOrNull()!!)
        }
        dao.upsertCartItem(targetEntity)
        return Result.success(Unit)
    }

    suspend fun updateCartQty(item: CartItemEntity, newQty: Int): Result<Unit> {
        if (newQty <= 0) {
            if (isCloudAuthenticatedId(item.userId)) {
                val res = remote!!.deleteCartItem(item.id)
                if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
            }
            dao.deleteCartItem(item.id)
            return Result.success(Unit)
        }
        val product = dao.getProductById(item.productId) ?: return Result.failure(IllegalStateException("Product unavailable."))
        if (newQty > product.stockQty) {
            return Result.failure(IllegalStateException("Only ${product.stockQty} available."))
        }
        val updated = item.copy(quantity = newQty)
        if (isCloudAuthenticatedId(item.userId)) {
            val res = remote!!.upsertCartItem(
                CartItemDto(updated.id, updated.userId, updated.productId, updated.variantLabel, updated.quantity),
                exists = true
            )
            if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
        }
        dao.upsertCartItem(updated)
        return Result.success(Unit)
    }

    suspend fun removeCartItem(id: String, userId: String = "") {
        if (isCloudAuthenticatedId(userId)) {
            remote?.deleteCartItem(id)
        }
        dao.deleteCartItem(id)
    }

    suspend fun toggleWishlist(userId: String, productId: String, currentlySaved: Boolean) {
        if (isCloudAuthenticatedId(userId)) {
            remote?.setWishlist(userId, productId, save = !currentlySaved)
        }
        if (currentlySaved) dao.removeWishlistItem(userId, productId)
        else dao.upsertWishlistItem(WishlistItemEntity("w_${userId}_$productId", userId, productId))
    }

    suspend fun toggleCompare(userId: String, productId: String, currentlyCompared: Boolean, currentCount: Int): Result<Unit> {
        if (currentlyCompared) {
            if (isCloudAuthenticatedId(userId)) {
                remote?.setCompare(userId, productId, save = false)
            }
            dao.removeComparisonItem(userId, productId)
            return Result.success(Unit)
        }
        if (currentCount >= 4) {
            return Result.failure(IllegalStateException("You can compare up to 4 products at once."))
        }
        if (isCloudAuthenticatedId(userId)) {
            remote?.setCompare(userId, productId, save = true)
        }
        dao.upsertComparisonItem(ComparisonItemEntity("cmp_${userId}_$productId", userId, productId))
        return Result.success(Unit)
    }

    // ============ CHECKOUT & ORDERS ============

    suspend fun calculateDeliveryFee(district: String, subtotal: Double): Double {
        val settings = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
        if (district.equals("Dhaka", ignoreCase = true) && subtotal >= 1500.0) return 0.0
        return when {
            district.equals("Dhaka", ignoreCase = true) -> settings.dhakaDeliveryFee
            district.equals("Gazipur", ignoreCase = true) || district.equals("Narayanganj", ignoreCase = true) || district.equals("Savar", ignoreCase = true) -> settings.dhakaDeliveryFee + 30.0
            else -> settings.outsideDhakaDeliveryFee
        }
    }

    suspend fun validateCoupon(code: String, subtotal: Double): Result<Pair<CouponEntity, Double>> {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Enter a coupon code."))
        val coupon = dao.getCouponByCode(clean) ?: return Result.failure(IllegalArgumentException("Coupon '$clean' not found."))
        if (!coupon.isActive) return Result.failure(IllegalStateException("This coupon is inactive."))
        if (coupon.expiresAtMillis < System.currentTimeMillis()) return Result.failure(IllegalStateException("This coupon has expired."))
        if (subtotal < coupon.minOrderAmount) {
            return Result.failure(IllegalStateException("Minimum order ৳${coupon.minOrderAmount.toInt()} required for $clean."))
        }
        val discount = if (coupon.discountType == "PERCENT") {
            minOf(subtotal * (coupon.discountValue / 100.0), coupon.maxDiscount)
        } else {
            minOf(coupon.discountValue, subtotal)
        }
        return Result.success(coupon to discount)
    }

    suspend fun placeOrder(
        activeUserId: String,
        isGuest: Boolean,
        idempotencyKey: String,
        customerName: String,
        customerPhone: String,
        district: String,
        area: String,
        streetAddress: String,
        couponCode: String,
        redeemLoyaltyPoints: Boolean,
        paymentMethod: String,
        senderNumber: String,
        transactionId: String,
        notes: String
    ): Result<OrderEntity> {
        if (customerName.trim().length < 2) return Result.failure(IllegalArgumentException("Customer name is required."))
        if (customerPhone.trim().length < 10) return Result.failure(IllegalArgumentException("Valid Bangladesh phone number is required."))
        if (streetAddress.trim().length < 5) return Result.failure(IllegalArgumentException("Full delivery address is required."))
        if ((paymentMethod == "BKASH" || paymentMethod == "NAGAD") && transactionId.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid $paymentMethod Transaction ID (TrxID) or select Cash on Delivery."))
        }

        val cartItems = dao.getCartOnce(activeUserId)
        if (cartItems.isEmpty()) return Result.failure(IllegalStateException("Your cart is empty."))

        if (remote != null) {
            val itemSpecs = cartItems.map { Triple(it.productId, it.variantLabel, it.quantity) }
            val guestSid = if (isGuest) activeUserId else ""
            return remote.placeOrderRpc(
                idempotencyKey = idempotencyKey,
                guestSessionId = guestSid,
                items = itemSpecs,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                district = district,
                area = area.trim(),
                streetAddress = streetAddress.trim(),
                couponCode = couponCode.trim(),
                redeemLoyalty = redeemLoyaltyPoints && !isGuest,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber.trim(),
                transactionId = transactionId.trim().uppercase(),
                notes = notes.trim()
            ).map { orderDto ->
                val orderEntity = orderDto.toEntity()
                dao.upsertOrders(listOf(orderEntity))
                val items = remote.fetchOrderItemsRpc(orderDto.id, guestSid)
                if (items.isNotEmpty()) {
                    dao.insertOrderItems(items.map { it.toEntity() })
                }
                dao.clearCart(activeUserId)
                syncStorefrontFromInsforge()
                if (!isGuest && isCloudAuthenticatedId(activeUserId)) {
                    syncUserBundle(activeUserId)
                }
                orderEntity
            }
        }

        // Offline Room atomic fallback
        val settings = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
        var subtotal = 0.0
        val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
        val orderNumber = "KK-2026-${(10100..99999).random()}"
        val orderItems = mutableListOf<OrderItemEntity>()

        for (ci in cartItems) {
            val freshProduct = dao.getProductById(ci.productId)
                ?: return Result.failure(IllegalStateException("A product in your cart no longer exists."))
            if (freshProduct.stockQty < ci.quantity) {
                return Result.failure(IllegalStateException("Insufficient stock for ${freshProduct.titleEn} (${freshProduct.stockQty} left)."))
            }
            val backendPrice = freshProduct.effectivePrice
            subtotal += backendPrice * ci.quantity
            orderItems.add(
                OrderItemEntity(
                    id = "oi_${UUID.randomUUID().toString().take(8)}",
                    orderId = orderId,
                    productId = freshProduct.id,
                    productTitleEn = freshProduct.titleEn,
                    productTitleBn = freshProduct.titleBn,
                    variantLabel = ci.variantLabel,
                    unitPrice = backendPrice,
                    costPrice = freshProduct.costPrice,
                    quantity = ci.quantity
                )
            )
        }

        val deliveryFee = calculateDeliveryFee(district, subtotal)
        var couponDiscount = 0.0
        var validCouponCode = ""
        if (couponCode.isNotBlank()) {
            val couponRes = validateCoupon(couponCode, subtotal)
            if (couponRes.isFailure) return Result.failure(couponRes.exceptionOrNull()!!)
            val pair = couponRes.getOrThrow()
            validCouponCode = pair.first.code
            couponDiscount = pair.second
        }

        var pointsUsed = 0
        var loyaltyDiscount = 0.0
        if (redeemLoyaltyPoints && !isGuest) {
            val user = dao.getUserById(activeUserId)
            val available = user?.loyaltyPoints ?: 0
            if (available > 0) {
                val maxRedeemableBdt = maxOf(0.0, (subtotal - couponDiscount) * 0.5)
                val maxPoints = (maxRedeemableBdt / settings.loyaltyPointValueBdt).toInt()
                pointsUsed = minOf(available, maxPoints)
                loyaltyDiscount = pointsUsed * settings.loyaltyPointValueBdt
            }
        }

        val total = maxOf(0.0, subtotal + deliveryFee - couponDiscount - loyaltyDiscount)
        // CRITICAL PAYMENT RULE: Manual bKash/Nagad starts as SUBMITTED (never VERIFIED automatically!). COD starts as PENDING.
        val initialPaymentStatus = if (paymentMethod == "COD") "PENDING" else "SUBMITTED"

        val order = OrderEntity(
            id = orderId,
            orderNumber = orderNumber,
            idempotencyKey = idempotencyKey,
            userId = activeUserId,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            shippingAddress = streetAddress.trim(),
            district = district,
            area = area.trim(),
            subtotal = subtotal,
            deliveryCharge = deliveryFee,
            deliveryCostActual = if (district.equals("Dhaka", true)) 45.0 else 95.0,
            discountAmount = couponDiscount,
            loyaltyPointsUsed = pointsUsed,
            loyaltyDiscount = loyaltyDiscount,
            couponCode = validCouponCode,
            totalAmount = total,
            paymentMethod = paymentMethod,
            paymentStatus = initialPaymentStatus,
            orderStatus = "PENDING",
            courierName = "Pending Assignment",
            trackingNumber = "",
            isGuestOrder = isGuest,
            notes = notes.trim()
        )
        val payment = PaymentRecordEntity(
            id = "pay_${UUID.randomUUID().toString().take(8)}",
            orderId = orderId,
            orderNumber = orderNumber,
            userId = activeUserId,
            method = paymentMethod,
            amount = total,
            senderNumber = senderNumber.trim(),
            transactionId = transactionId.trim().uppercase(),
            status = initialPaymentStatus,
            adminNote = if (paymentMethod == "COD") "Collect cash on delivery" else "Submitted TrxID awaiting Admin verification"
        )

        return try {
            val created = dao.placeOrderAtomic(
                order = order,
                items = orderItems,
                payment = payment,
                cartUserId = activeUserId,
                couponCodeUsed = validCouponCode,
                loyaltyPointsRedeemed = pointsUsed
            )
            if (!created) {
                return Result.failure(IllegalStateException("Duplicate submission prevented. Your order was already placed."))
            }
            dao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    userId = activeUserId,
                    titleEn = "Order $orderNumber Placed",
                    titleBn = "অর্ডার $orderNumber গৃহীত হয়েছে",
                    messageEn = if (paymentMethod == "COD") "Cash on Delivery order received. We will confirm shortly." else "$paymentMethod TrxID submitted. Owner verification pending.",
                    messageBn = if (paymentMethod == "COD") "ক্যাশ অন ডেলিভারি অর্ডার সফল হয়েছে।" else "$paymentMethod TrxID জমা হয়েছে। অ্যাডমিন যাচাইয়ের অপেক্ষায়।",
                    type = "ORDER"
                )
            )
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderItems(orderId: String, guestSessionId: String = ""): List<OrderItemEntity> {
        val cached = dao.getOrderItemsOnce(orderId)
        if (cached.isNotEmpty() || remote == null) return cached
        val remoteItems = remote.fetchOrderItemsRpc(orderId, guestSessionId).map { it.toEntity() }
        if (remoteItems.isNotEmpty()) dao.insertOrderItems(remoteItems)
        return remoteItems
    }

    suspend fun customerCancelOrReturnOrder(order: OrderEntity, userId: String, isReturn: Boolean): Result<Unit> {
        if (order.userId != userId) return Result.failure(SecurityException("Unauthorized order action."))
        if (!isReturn && order.orderStatus !in listOf("PENDING", "CONFIRMED")) {
            return Result.failure(IllegalStateException("Orders already packed or shipped cannot be cancelled directly. Request a return after delivery."))
        }
        if (isReturn && order.orderStatus != "DELIVERED") {
            return Result.failure(IllegalStateException("Only delivered orders can be requested for return."))
        }
        val targetStatus = if (isReturn) "RETURNED" else "CANCELLED"
        if (remote != null) {
            val guestSid = if (order.isGuestOrder) userId else ""
            return remote.cancelOrReturnOrderRpc(order.id, isReturn, guestSid).map {
                dao.updateOrder(order.copy(orderStatus = targetStatus, updatedAtMillis = System.currentTimeMillis()))
                syncStorefrontFromInsforge()
                if (isCloudAuthenticatedId(userId)) syncUserBundle(userId)
            }
        }

        dao.updateOrder(order.copy(orderStatus = targetStatus, updatedAtMillis = System.currentTimeMillis()))
        val items = dao.getOrderItemsOnce(order.id)
        items.forEach { dao.restoreStock(it.productId, it.quantity) }
        reverseEarnedLoyaltyIfAny(order)
        return Result.success(Unit)
    }

    suspend fun addReview(productId: String, userId: String, userName: String, rating: Int, comment: String): Result<Unit> {
        if (rating !in 1..5) return Result.failure(IllegalArgumentException("Select rating between 1 and 5."))
        if (comment.trim().length < 4) return Result.failure(IllegalArgumentException("Please write a short review."))

        val reviewId = "rev_${UUID.randomUUID().toString().take(8)}"
        if (remote != null) {
            val cloudUserId = if (isCloudAuthenticatedId(userId)) userId else null
            return remote.addReview(
                ReviewDto(
                    id = reviewId,
                    productId = productId,
                    userId = cloudUserId,
                    userName = userName,
                    rating = rating,
                    comment = comment.trim(),
                    isApproved = true
                )
            ).map {
                dao.upsertReview(
                    ReviewEntity(
                        id = reviewId,
                        productId = productId,
                        userId = userId,
                        userName = userName,
                        rating = rating,
                        comment = comment.trim(),
                        isApproved = true
                    )
                )
                syncStorefrontFromInsforge()
            }
        }

        dao.upsertReview(
            ReviewEntity(
                id = reviewId,
                productId = productId,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment.trim(),
                isApproved = true
            )
        )
        val approved = dao.getApprovedReviewsForProduct(productId)
        val avg = if (approved.isEmpty()) 5.0 else approved.map { it.rating }.average()
        dao.updateProductRating(productId, avg, approved.size)
        return Result.success(Unit)
    }

    suspend fun markNotificationsRead(userId: String) {
        if (isCloudAuthenticatedId(userId)) {
            remote?.markNotificationsRead(userId)
        }
        dao.markAllNotificationsRead(userId)
    }

    suspend fun sendSupportMessage(userId: String, name: String, email: String, subject: String, message: String): Result<Unit> {
        if (subject.trim().isEmpty() || message.trim().length < 4) {
            return Result.failure(IllegalArgumentException("Please enter a subject and message."))
        }
        val msgId = "sup_${UUID.randomUUID().toString().take(8)}"
        val cleanName = name.ifBlank { "Guest Customer" }
        val cleanEmail = email.ifBlank { "guest@kenakata.bd" }
        if (remote != null) {
            val cloudUid = if (isCloudAuthenticatedId(userId)) userId else null
            return remote.insertSupportMessage(
                SupportMessageDto(
                    id = msgId,
                    userId = cloudUid,
                    senderName = cleanName,
                    senderEmail = cleanEmail,
                    subject = subject.trim(),
                    message = message.trim()
                )
            ).map {
                dao.upsertSupportMessage(
                    SupportMessageEntity(
                        id = msgId,
                        userId = userId,
                        senderName = cleanName,
                        senderEmail = cleanEmail,
                        subject = subject.trim(),
                        message = message.trim()
                    )
                )
            }
        }

        dao.upsertSupportMessage(
            SupportMessageEntity(
                id = msgId,
                userId = userId,
                senderName = cleanName,
                senderEmail = cleanEmail,
                subject = subject.trim(),
                message = message.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun subscribeNewsletter() {
        if (remote != null) {
            remote.subscribeNewsletterRpc().onSuccess { count ->
                val current = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
                dao.upsertWebsiteSettings(current.copy(newsletterSubscribersCount = count))
                return
            }
        }
        val current = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
        dao.upsertWebsiteSettings(current.copy(newsletterSubscribersCount = current.newsletterSubscribersCount + 1))
    }

    // ============ ADMIN ONLY OPERATIONS (RBAC ENFORCED) ============

    private suspend fun requireAdmin(actorId: String): UserEntity {
        val actor = dao.getUserById(actorId)
        if (actor == null || actor.role != "ADMIN") {
            throw SecurityException("Admin authorization required.")
        }
        return actor
    }

    private suspend fun logActivity(actor: UserEntity, actionType: String, targetType: String, targetId: String, summary: String) {
        dao.insertActivityLog(
            ActivityLogEntity(
                id = "log_${UUID.randomUUID().toString().take(10)}",
                actorId = actor.id,
                actorEmail = actor.email,
                actionType = actionType,
                targetType = targetType,
                targetId = targetId,
                summary = summary
            )
        )
    }

    suspend fun adminSaveProduct(actorId: String, product: ProductEntity, isNew: Boolean): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        require(product.titleEn.isNotBlank() && product.sku.isNotBlank()) { "Title and SKU are required." }
        require(product.regularPrice > 0) { "Price must be positive." }

        var finalProduct = product
        var uploadedImageKey = ""
        if (remote != null) {
            if (product.imageUri.startsWith("content://")) {
                val (url, key) = remote.uploadImageFromUri("product-images", Uri.parse(product.imageUri), product.sku).getOrThrow()
                finalProduct = product.copy(imageUri = url)
                uploadedImageKey = key
            }
            remote.adminSaveProduct(
                adminId = admin.id,
                adminEmail = admin.email,
                dto = finalProduct.toDto(imageKey = uploadedImageKey),
                costPrice = finalProduct.costPrice,
                isNew = isNew
            ).getOrThrow()
            dao.upsertProduct(finalProduct)
            syncAdminBundle()
        } else {
            dao.upsertProduct(finalProduct)
            logActivity(admin, if (isNew) "PRODUCT_CREATE" else "PRODUCT_UPDATE", "PRODUCT", finalProduct.id, "${if (isNew) "Added" else "Updated"} ${finalProduct.titleEn} (${finalProduct.sku})")
        }
    }

    suspend fun adminDeleteProduct(actorId: String, product: ProductEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminDeleteProduct(admin.id, admin.email, product.id, product.titleEn).getOrThrow()
            dao.deleteProduct(product.id)
            syncAdminBundle()
        } else {
            dao.deleteProduct(product.id)
            logActivity(admin, "PRODUCT_DELETE", "PRODUCT", product.id, "Deleted ${product.titleEn}")
        }
    }

    suspend fun adminSaveCategory(actorId: String, category: CategoryEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminSaveCategory(
                admin.id,
                admin.email,
                CategoryDto(category.id, category.nameEn, category.nameBn, category.slug, category.iconKey, category.sortOrder, category.isFeatured)
            ).getOrThrow()
            dao.upsertCategory(category)
            syncAdminBundle()
        } else {
            dao.upsertCategory(category)
            logActivity(admin, "CATEGORY_SAVE", "CATEGORY", category.id, "Saved category ${category.nameEn}")
        }
    }

    suspend fun adminSaveCoupon(actorId: String, coupon: CouponEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        val normalized = coupon.copy(code = coupon.code.trim().uppercase())
        if (remote != null) {
            remote.adminSaveCoupon(
                admin.id,
                admin.email,
                CouponDto(normalized.id, normalized.code, normalized.discountType, normalized.discountValue, normalized.minOrderAmount, normalized.maxDiscount, normalized.isActive, normalized.expiresAtMillis, normalized.usageCount)
            ).getOrThrow()
            dao.upsertCoupon(normalized)
            syncAdminBundle()
        } else {
            dao.upsertCoupon(normalized)
            logActivity(admin, "COUPON_SAVE", "COUPON", normalized.id, "Saved coupon ${normalized.code}")
        }
    }

    suspend fun adminToggleFlashSale(actorId: String, sale: FlashSaleEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminToggleFlashSale(
                admin.id,
                admin.email,
                FlashSaleDto(sale.id, sale.titleEn, sale.titleBn, sale.discountPercent, sale.startTimeMillis, sale.endTimeMillis, sale.productIdsCsv, sale.isActive)
            ).getOrThrow()
            dao.upsertFlashSale(sale)
            syncAdminBundle()
        } else {
            dao.upsertFlashSale(sale)
            logActivity(admin, "FLASH_SALE_UPDATE", "FLASH_SALE", sale.id, "Updated flash sale ${sale.titleEn} (active=${sale.isActive})")
        }
    }

    suspend fun adminVerifyPayment(actorId: String, payment: PaymentRecordEntity, approve: Boolean, note: String): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminVerifyPaymentRpc(payment.id, approve, note).getOrThrow()
            syncAdminBundle()
            return@runCatching
        }

        val order = dao.getOrderById(payment.orderId) ?: error("Order not found.")
        val newPayStatus = if (approve) "VERIFIED" else "FAILED"
        dao.upsertPaymentRecord(
            payment.copy(
                status = newPayStatus,
                adminNote = note.ifBlank { if (approve) "Verified by Owner Admin" else "Rejected: TrxID mismatch" },
                verifiedAtMillis = System.currentTimeMillis()
            )
        )
        val nextOrderStatus = if (approve && order.orderStatus == "PENDING") "CONFIRMED" else order.orderStatus
        dao.updateOrder(order.copy(paymentStatus = newPayStatus, orderStatus = nextOrderStatus, updatedAtMillis = System.currentTimeMillis()))

        if (approve) {
            awardEarnedLoyaltyIfEligible(order)
        }
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = order.userId,
                titleEn = "Payment $newPayStatus (${order.orderNumber})",
                titleBn = "পেমেন্ট স্ট্যাটাস: $newPayStatus (${order.orderNumber})",
                messageEn = if (approve) "Your ${payment.method} payment of ৳${order.totalAmount.toInt()} is verified." else "Payment verification failed: $note",
                messageBn = if (approve) "আপনার ৳${order.totalAmount.toInt()} পেমেন্ট যাচাই সম্পন্ন হয়েছে।" else "পেমেন্ট যাচাই ব্যর্থ হয়েছে: $note",
                type = "PAYMENT"
            )
        )
        logActivity(admin, "PAYMENT_VERIFY", "PAYMENT", payment.id, "${if (approve) "Verified" else "Rejected"} ${payment.method} for ${order.orderNumber}")
    }

    suspend fun adminMarkRefunded(actorId: String, payment: PaymentRecordEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminRefundPaymentRpc(payment.id).getOrThrow()
            syncAdminBundle()
            return@runCatching
        }

        val order = dao.getOrderById(payment.orderId) ?: error("Order not found.")
        dao.upsertPaymentRecord(payment.copy(status = "REFUNDED", adminNote = "Refunded by Owner Admin"))
        dao.updateOrder(order.copy(paymentStatus = "REFUNDED", orderStatus = "RETURNED", updatedAtMillis = System.currentTimeMillis()))
        reverseEarnedLoyaltyIfAny(order)
        logActivity(admin, "PAYMENT_REFUND", "PAYMENT", payment.id, "Refunded order ${order.orderNumber}")
    }

    suspend fun adminUpdateOrderAndCourier(
        actorId: String,
        order: OrderEntity,
        newStatus: String,
        courierName: String,
        trackingNumber: String
    ): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminUpdateOrderRpc(order.id, newStatus, courierName, trackingNumber).getOrThrow()
            syncAdminBundle()
            syncStorefrontFromInsforge()
            return@runCatching
        }

        val updatedPayStatus = if (newStatus == "DELIVERED" && order.paymentMethod == "COD" && order.paymentStatus == "PENDING") {
            "VERIFIED"
        } else order.paymentStatus

        val updatedOrder = order.copy(
            orderStatus = newStatus,
            paymentStatus = updatedPayStatus,
            courierName = courierName.ifBlank { order.courierName },
            trackingNumber = trackingNumber.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.updateOrder(updatedOrder)

        if (newStatus == "DELIVERED" && updatedPayStatus == "VERIFIED") {
            dao.getPaymentByOrderId(order.id)?.let { pay ->
                dao.upsertPaymentRecord(pay.copy(status = "VERIFIED", verifiedAtMillis = System.currentTimeMillis(), adminNote = "Collected & Delivered"))
            }
            awardEarnedLoyaltyIfEligible(updatedOrder)
        }
        if (newStatus == "CANCELLED" || newStatus == "RETURNED") {
            if (order.orderStatus != "CANCELLED" && order.orderStatus != "RETURNED") {
                dao.getOrderItemsOnce(order.id).forEach { dao.restoreStock(it.productId, it.quantity) }
            }
            reverseEarnedLoyaltyIfAny(updatedOrder)
        }

        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = order.userId,
                titleEn = "Order ${order.orderNumber}: $newStatus",
                titleBn = "অর্ডার ${order.orderNumber}: $newStatus",
                messageEn = if (trackingNumber.isNotBlank()) "Courier: $courierName | Tracking: $trackingNumber" else "Order status updated to $newStatus.",
                messageBn = if (trackingNumber.isNotBlank()) "কুরিয়ার: $courierName | ট্র্যাকিং: $trackingNumber" else "অর্ডার স্ট্যাটাস আপডেট: $newStatus",
                type = "SHIPPING"
            )
        )
        logActivity(admin, "ORDER_UPDATE", "ORDER", order.id, "Order ${order.orderNumber} -> $newStatus ($courierName $trackingNumber)")
    }

    private suspend fun awardEarnedLoyaltyIfEligible(order: OrderEntity) {
        if (order.isGuestOrder) return
        val settings = dao.getWebsiteSettingsOnce() ?: WebsiteSettingsEntity()
        val earned = ((order.subtotal / 100.0) * settings.loyaltyEarnPer100Bdt).toInt()
        if (earned <= 0) return
        val awardKey = "EARN_${order.id}"
        val inserted = dao.insertLoyaltyTransaction(
            LoyaltyTransactionEntity(
                id = "loy_${UUID.randomUUID().toString().take(8)}",
                userId = order.userId,
                orderId = order.id,
                uniqueAwardKey = awardKey,
                pointsDelta = earned,
                type = "EARNED",
                description = "Earned on verified order ${order.orderNumber}"
            )
        )
        if (inserted > 0) {
            dao.adjustUserLoyaltyPoints(order.userId, earned)
        }
    }

    private suspend fun reverseEarnedLoyaltyIfAny(order: OrderEntity) {
        if (order.isGuestOrder) return
        val earnTx = dao.getLoyaltyByUniqueKey("EARN_${order.id}") ?: return
        val reverseKey = "REV_${order.id}"
        val inserted = dao.insertLoyaltyTransaction(
            LoyaltyTransactionEntity(
                id = "loy_${UUID.randomUUID().toString().take(8)}",
                userId = order.userId,
                orderId = order.id,
                uniqueAwardKey = reverseKey,
                pointsDelta = -earnTx.pointsDelta,
                type = "REVERSED",
                description = "Reversed loyalty points for ${order.orderNumber}"
            )
        )
        if (inserted > 0) {
            dao.adjustUserLoyaltyPoints(order.userId, -earnTx.pointsDelta)
        }
    }

    suspend fun adminReplySupport(actorId: String, msg: SupportMessageEntity, reply: String): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            val cloudUid = if (isCloudAuthenticatedId(msg.userId)) msg.userId else null
            remote.adminReplySupport(admin.id, admin.email, msg.id, cloudUid, msg.senderEmail, msg.subject, reply).getOrThrow()
            dao.upsertSupportMessage(msg.copy(status = "REPLIED", adminReply = reply.trim()))
            syncAdminBundle()
            return@runCatching
        }

        dao.upsertSupportMessage(msg.copy(status = "REPLIED", adminReply = reply.trim()))
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = msg.userId,
                titleEn = "Support Reply: ${msg.subject}",
                titleBn = "সাপোর্ট উত্তর: ${msg.subject}",
                messageEn = reply.trim(),
                messageBn = reply.trim(),
                type = "SUPPORT"
            )
        )
        logActivity(admin, "SUPPORT_REPLY", "SUPPORT", msg.id, "Replied to ${msg.senderEmail}")
    }

    suspend fun adminDeleteReview(actorId: String, review: ReviewEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminDeleteReview(admin.id, admin.email, review.id, review.productId).getOrThrow()
            dao.deleteReview(review.id)
            syncStorefrontFromInsforge()
            syncAdminBundle()
            return@runCatching
        }

        dao.deleteReview(review.id)
        val remaining = dao.getApprovedReviewsForProduct(review.productId)
        val avg = if (remaining.isEmpty()) 4.5 else remaining.map { it.rating }.average()
        dao.updateProductRating(review.productId, avg, remaining.size)
        logActivity(admin, "REVIEW_DELETE", "REVIEW", review.id, "Removed review on ${review.productId}")
    }

    suspend fun adminSaveSettings(actorId: String, settings: WebsiteSettingsEntity): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null) {
            remote.adminSaveSettings(admin.id, admin.email, settings.toDto()).getOrThrow()
            dao.upsertWebsiteSettings(settings)
            syncAdminBundle()
        } else {
            dao.upsertWebsiteSettings(settings)
            logActivity(admin, "SETTINGS_UPDATE", "SETTINGS", "1", "Updated website, payment, delivery & SEO settings")
        }
    }

    suspend fun adminImportCsvProducts(actorId: String, csvText: String): Result<CsvImportResult> = runCatching {
        val admin = requireAdmin(actorId)
        val categoryIds = dao.getCategoriesOnce().map { it.id }.toSet()
        val parsed = CsvAndBackupUtils.parseProductsFromCsv(csvText, categoryIds)
        if (parsed.importedProducts.isNotEmpty()) {
            if (remote != null) {
                val existingIds = dao.getProductsOnce().map { it.id }.toSet()
                parsed.importedProducts.forEach { prod ->
                    remote.adminSaveProduct(admin.id, admin.email, prod.toDto(), prod.costPrice, isNew = prod.id !in existingIds).getOrThrow()
                }
                remote.logAdminActivity(admin.id, admin.email, "CSV_IMPORT", "PRODUCT", "BULK", "Imported ${parsed.importedProducts.size} products (${parsed.errors.size} errors)")
                dao.upsertProducts(parsed.importedProducts)
                syncAdminBundle()
            } else {
                dao.upsertProducts(parsed.importedProducts)
                logActivity(admin, "CSV_IMPORT", "PRODUCT", "BULK", "Imported ${parsed.importedProducts.size} products (${parsed.errors.size} errors)")
            }
        }
        parsed
    }

    suspend fun adminExportCsvProducts(actorId: String): Result<String> = runCatching {
        val admin = requireAdmin(actorId)
        val products = dao.getProductsOnce()
        if (remote != null) {
            remote.logAdminActivity(admin.id, admin.email, "CSV_EXPORT", "PRODUCT", "BULK", "Exported ${products.size} products to CSV")
        } else {
            logActivity(admin, "CSV_EXPORT", "PRODUCT", "BULK", "Exported ${products.size} products to CSV")
        }
        CsvAndBackupUtils.exportProductsToCsv(products)
    }

    suspend fun adminCreateBackupJson(actorId: String, fileName: String): Result<String> = runCatching {
        val admin = requireAdmin(actorId)
        val categories = dao.getCategoriesOnce()
        val products = dao.getProductsOnce()
        val orders = dao.getAllOrdersOnce()
        val settings = dao.getWebsiteSettingsOnce()
        val coupons = listOfNotNull(dao.getCouponByCode("KENA10"), dao.getCouponByCode("DHAKA50"), dao.getCouponByCode("BOOK20"))
        val json = CsvAndBackupUtils.exportDatabaseBackupJson(categories, products, coupons, orders, settings)
        val logId = "bkp_${UUID.randomUUID().toString().take(8)}"
        val count = categories.size + products.size + orders.size
        if (remote != null) {
            remote.adminInsertBackupLog(BackupLogDto(logId, fileName, count, "EXPORTED", admin.email))
            remote.logAdminActivity(admin.id, admin.email, "BACKUP_EXPORT", "BACKUP", fileName, "Exported store backup ($fileName)")
            syncAdminBundle()
        } else {
            dao.insertBackupLog(BackupLogEntity(logId, fileName, count, "EXPORTED", admin.email))
            logActivity(admin, "BACKUP_EXPORT", "BACKUP", fileName, "Exported store backup ($fileName)")
        }
        json
    }

    suspend fun adminRestoreBackupJson(actorId: String, fileName: String, jsonText: String): Result<BackupRestoreResult> = runCatching {
        val admin = requireAdmin(actorId)
        val parsed = CsvAndBackupUtils.parseBackupJson(jsonText).getOrThrow()
        if (remote != null) {
            val existingProdIds = dao.getProductsOnce().map { it.id }.toSet()
            parsed.categories.forEach { c ->
                remote.adminSaveCategory(admin.id, admin.email, CategoryDto(c.id, c.nameEn, c.nameBn, c.slug, c.iconKey, c.sortOrder, c.isFeatured)).getOrThrow()
            }
            parsed.products.forEach { p ->
                remote.adminSaveProduct(admin.id, admin.email, p.toDto(), p.costPrice, isNew = p.id !in existingProdIds).getOrThrow()
            }
            parsed.coupons.forEach { c ->
                remote.adminSaveCoupon(admin.id, admin.email, CouponDto(c.id, c.code, c.discountType, c.discountValue, c.minOrderAmount, c.maxDiscount, c.isActive, c.expiresAtMillis, c.usageCount)).getOrThrow()
            }
            val logId = "bkp_${UUID.randomUUID().toString().take(8)}"
            remote.adminInsertBackupLog(BackupLogDto(logId, fileName, parsed.recordCount, "RESTORED", admin.email))
            remote.logAdminActivity(admin.id, admin.email, "BACKUP_RESTORE", "BACKUP", fileName, "Restored ${parsed.recordCount} records from $fileName")
            syncStorefrontFromInsforge()
            syncAdminBundle()
            return@runCatching parsed
        }

        if (parsed.categories.isNotEmpty()) dao.upsertCategories(parsed.categories)
        if (parsed.products.isNotEmpty()) dao.upsertProducts(parsed.products)
        if (parsed.coupons.isNotEmpty()) dao.upsertCoupons(parsed.coupons)
        dao.insertBackupLog(
            BackupLogEntity(
                id = "bkp_${UUID.randomUUID().toString().take(8)}",
                fileName = fileName,
                recordCount = parsed.recordCount,
                status = "RESTORED",
                actorEmail = admin.email
            )
        )
        logActivity(admin, "BACKUP_RESTORE", "BACKUP", fileName, "Restored ${parsed.recordCount} records from $fileName")
        parsed
    }

    suspend fun adminSendCartRecoveryReminder(actorId: String, targetUserId: String): Result<Unit> = runCatching {
        val admin = requireAdmin(actorId)
        if (remote != null && isCloudAuthenticatedId(targetUserId)) {
            remote.adminSendCartRecovery(admin.id, admin.email, targetUserId).getOrThrow()
            syncAdminBundle()
            return@runCatching
        }

        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = targetUserId,
                titleEn = "Items waiting in your Kena Kata Cart!",
                titleBn = "আপনার কার্টে পণ্য অপেক্ষা করছে!",
                messageEn = "Complete your order today and apply code KENA10 for 10% OFF.",
                messageBn = "আজই অর্ডার সম্পন্ন করুন এবং KENA10 কোডে ১০% ছাড় পান।",
                type = "CART_RECOVERY"
            )
        )
        logActivity(admin, "CART_RECOVERY_SEND", "CART", targetUserId, "Sent recovery reminder to $targetUserId")
    }

    fun computeAnalytics(
        orders: List<OrderEntity>,
        orderItems: List<OrderItemEntity>,
        products: List<ProductEntity>,
        users: List<UserEntity>
    ): AnalyticsSummary {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val monthMs = dayMs * 30

        val validOrders = orders.filter { it.orderStatus != "CANCELLED" && it.orderStatus != "RETURNED" }
        val refundedOrders = orders.filter { it.paymentStatus == "REFUNDED" || it.orderStatus == "RETURNED" }

        val grossSales = validOrders.sumOf { it.subtotal + it.deliveryCharge }
        val totalDiscounts = validOrders.sumOf { it.discountAmount + it.loyaltyDiscount }
        val totalRefunds = refundedOrders.sumOf { it.totalAmount }
        val netRevenue = validOrders.sumOf { it.totalAmount }

        val validOrderIds = validOrders.map { it.id }.toSet()
        val totalCogs = orderItems.filter { it.orderId in validOrderIds }.sumOf { it.costPrice * it.quantity }
        val totalDeliveryExpenses = validOrders.sumOf { it.deliveryCostActual }
        val netProfit = netRevenue - totalCogs - totalDeliveryExpenses - totalRefunds

        val todayRevenue = validOrders.filter { now - it.createdAtMillis <= dayMs }.sumOf { it.totalAmount }
        val monthlyRevenue = validOrders.filter { now - it.createdAtMillis <= monthMs }.sumOf { it.totalAmount }

        val dailyBars = (6 downTo 0).map { offset ->
            val start = now - (offset + 1) * dayMs
            val end = now - offset * dayMs
            val label = if (offset == 0) "Today" else "D-$offset"
            val rev = validOrders.filter { it.createdAtMillis in start..end }.sumOf { it.totalAmount }
            label to rev
        }

        return AnalyticsSummary(
            grossSales = grossSales,
            totalDiscounts = totalDiscounts,
            totalRefunds = totalRefunds,
            totalCogs = totalCogs,
            totalDeliveryExpenses = totalDeliveryExpenses,
            netRevenue = netRevenue,
            netProfit = netProfit,
            todayRevenue = todayRevenue,
            monthlyRevenue = monthlyRevenue,
            totalOrders = orders.size,
            pendingOrders = orders.count { it.orderStatus in listOf("PENDING", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED") },
            deliveredOrders = orders.count { it.orderStatus == "DELIVERED" },
            cancelledOrReturnedOrders = orders.count { it.orderStatus in listOf("CANCELLED", "RETURNED") },
            totalCustomers = users.count { it.role == "CUSTOMER" },
            totalProducts = products.size,
            lowStockCount = products.count { it.stockQty in 1..it.lowStockThreshold },
            outOfStockCount = products.count { it.stockQty <= 0 },
            bkashCount = orders.count { it.paymentMethod == "BKASH" },
            nagadCount = orders.count { it.paymentMethod == "NAGAD" },
            codCount = orders.count { it.paymentMethod == "COD" },
            dailyRevenueBars = dailyBars
        )
    }
}
