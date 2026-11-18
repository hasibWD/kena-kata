package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KenaKataDao {
    // Users
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAtMillis DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET loyaltyPoints = loyaltyPoints + :delta WHERE id = :userId")
    suspend fun adjustUserLoyaltyPoints(userId: String, delta: Int)

    // Addresses
    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY isDefault DESC")
    fun observeAddresses(userId: String): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddress(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :addressId")
    suspend fun deleteAddress(addressId: String)

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // Products
    @Query("SELECT * FROM products ORDER BY createdAtMillis DESC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAtMillis DESC")
    suspend fun getProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun observeProduct(id: String): Flow<ProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("UPDATE products SET stockQty = stockQty - :qty, soldCount = soldCount + :qty WHERE id = :productId AND stockQty >= :qty")
    suspend fun deductStockIfAvailable(productId: String, qty: Int): Int

    @Query("UPDATE products SET stockQty = stockQty + :qty, soldCount = MAX(0, soldCount - :qty) WHERE id = :productId")
    suspend fun restoreStock(productId: String, qty: Int)

    @Query("UPDATE products SET ratingAvg = :ratingAvg, reviewCount = :reviewCount WHERE id = :productId")
    suspend fun updateProductRating(productId: String, ratingAvg: Double, reviewCount: Int)

    // Banners & Flash Sales & Coupons
    @Query("SELECT * FROM banners ORDER BY sortOrder ASC")
    fun observeBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBanners(banners: List<BannerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: String)

    @Query("SELECT * FROM flash_sales ORDER BY endTimeMillis DESC")
    fun observeFlashSales(): Flow<List<FlashSaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashSales(sales: List<FlashSaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashSale(sale: FlashSaleEntity)

    @Query("SELECT * FROM coupons ORDER BY expiresAtMillis DESC")
    fun observeCoupons(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getCouponByCode(code: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoupons(coupons: List<CouponEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoupon(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteCoupon(id: String)

    // Cart, Wishlist, Compare
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY addedAtMillis DESC")
    fun observeCart(userId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartOnce(userId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId AND variantLabel = :variant LIMIT 1")
    suspend fun findCartItem(userId: String, productId: String, variant: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE addedAtMillis < :cutoffMillis")
    suspend fun getAbandonedCartsBefore(cutoffMillis: Long): List<CartItemEntity>

    @Query("SELECT * FROM cart_items")
    fun observeAllCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: String)

    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY addedAtMillis DESC")
    fun observeWishlist(userId: String): Flow<List<WishlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWishlistItem(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND productId = :productId")
    suspend fun removeWishlistItem(userId: String, productId: String)

    @Query("SELECT * FROM comparison_items WHERE userId = :userId ORDER BY addedAtMillis ASC")
    fun observeComparison(userId: String): Flow<List<ComparisonItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComparisonItem(item: ComparisonItemEntity)

    @Query("DELETE FROM comparison_items WHERE userId = :userId AND productId = :productId")
    suspend fun removeComparisonItem(userId: String, productId: String)

    // Orders & OrderItems & Payments
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeUserOrders(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    suspend fun getAllOrdersOnce(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getOrderByIdempotencyKey(key: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsOnce(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    fun observeAllOrderItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM payment_records ORDER BY createdAtMillis DESC")
    fun observeAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE orderId = :orderId LIMIT 1")
    suspend fun getPaymentByOrderId(orderId: String): PaymentRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentRecord(payment: PaymentRecordEntity)

    // Reviews
    @Query("SELECT * FROM reviews ORDER BY createdAtMillis DESC")
    fun observeAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE productId = :productId AND isApproved = 1 ORDER BY createdAtMillis DESC")
    fun observeProductReviews(productId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE productId = :productId AND isApproved = 1")
    suspend fun getApprovedReviewsForProduct(productId: String): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteReview(reviewId: String)

    // Loyalty & Referrals
    @Query("SELECT * FROM loyalty_transactions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeLoyaltyTransactions(userId: String): Flow<List<LoyaltyTransactionEntity>>

    @Query("SELECT * FROM loyalty_transactions WHERE uniqueAwardKey = :key LIMIT 1")
    suspend fun getLoyaltyByUniqueKey(key: String): LoyaltyTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLoyaltyTransaction(tx: LoyaltyTransactionEntity): Long

    @Query("SELECT * FROM referral_records WHERE referrerUserId = :userId ORDER BY createdAtMillis DESC")
    fun observeReferralsByUser(userId: String): Flow<List<ReferralRecordEntity>>

    @Query("SELECT * FROM referral_records ORDER BY createdAtMillis DESC")
    fun observeAllReferrals(): Flow<List<ReferralRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReferral(record: ReferralRecordEntity)

    // Notifications & Support
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY createdAtMillis DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 'ALL'")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("SELECT * FROM support_messages WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeUserSupportMessages(userId: String): Flow<List<SupportMessageEntity>>

    @Query("SELECT * FROM support_messages ORDER BY createdAtMillis DESC")
    fun observeAllSupportMessages(): Flow<List<SupportMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupportMessage(message: SupportMessageEntity)

    // Activity Logs & Settings & Backups
    @Query("SELECT * FROM activity_logs ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    @Query("SELECT * FROM website_settings WHERE id = 1 LIMIT 1")
    fun observeWebsiteSettings(): Flow<WebsiteSettingsEntity?>

    @Query("SELECT * FROM website_settings WHERE id = 1 LIMIT 1")
    suspend fun getWebsiteSettingsOnce(): WebsiteSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWebsiteSettings(settings: WebsiteSettingsEntity)

    @Query("SELECT * FROM backup_logs ORDER BY createdAtMillis DESC")
    fun observeBackupLogs(): Flow<List<BackupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLog(log: BackupLogEntity)

    // Batch upsert helpers for InsForge cloud sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddresses(addresses: List<AddressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItems(items: List<CartItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWishlistItems(items: List<WishlistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComparisonItems(items: List<ComparisonItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentRecords(payments: List<PaymentRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviews(reviews: List<ReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyTransactions(items: List<LoyaltyTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReferrals(items: List<ReferralRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(items: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupportMessages(items: List<SupportMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityLogs(items: List<ActivityLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackupLogs(items: List<BackupLogEntity>)

    @Query("DELETE FROM products WHERE id NOT IN (:ids)")
    suspend fun deleteProductsNotIn(ids: List<String>)

    @Query("DELETE FROM categories WHERE id NOT IN (:ids)")
    suspend fun deleteCategoriesNotIn(ids: List<String>)

    @Query("DELETE FROM banners WHERE id NOT IN (:ids)")
    suspend fun deleteBannersNotIn(ids: List<String>)

    @Query("DELETE FROM coupons WHERE id NOT IN (:ids)")
    suspend fun deleteCouponsNotIn(ids: List<String>)

    @Query("DELETE FROM flash_sales WHERE id NOT IN (:ids)")
    suspend fun deleteFlashSalesNotIn(ids: List<String>)

    @Query("DELETE FROM reviews WHERE id NOT IN (:ids)")
    suspend fun deleteReviewsNotIn(ids: List<String>)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId")
    suspend fun clearWishlistForUser(userId: String)

    @Query("DELETE FROM comparison_items WHERE userId = :userId")
    suspend fun clearComparisonForUser(userId: String)

    @Query("DELETE FROM addresses WHERE userId = :userId")
    suspend fun clearAddressesForUser(userId: String)

    @Transaction
    suspend fun syncStorefrontCatalog(
        settings: WebsiteSettingsEntity?,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        banners: List<BannerEntity>,
        flashSales: List<FlashSaleEntity>,
        coupons: List<CouponEntity>,
        reviews: List<ReviewEntity>,
        publicNotifications: List<NotificationEntity>
    ) {
        if (settings != null) upsertWebsiteSettings(settings)
        if (categories.isNotEmpty()) {
            deleteCategoriesNotIn(categories.map { it.id })
            upsertCategories(categories)
        }
        if (products.isNotEmpty()) {
            deleteProductsNotIn(products.map { it.id })
            upsertProducts(products)
        }
        if (banners.isNotEmpty()) {
            deleteBannersNotIn(banners.map { it.id })
            upsertBanners(banners)
        }
        if (flashSales.isNotEmpty()) {
            deleteFlashSalesNotIn(flashSales.map { it.id })
            upsertFlashSales(flashSales)
        }
        if (coupons.isNotEmpty()) {
            deleteCouponsNotIn(coupons.map { it.id })
            upsertCoupons(coupons)
        }
        if (reviews.isNotEmpty()) {
            deleteReviewsNotIn(reviews.map { it.id })
        }
        upsertReviews(reviews)
        upsertNotifications(publicNotifications)
    }

    @Transaction
    suspend fun syncUserWorkspace(
        profile: UserEntity,
        addresses: List<AddressEntity>,
        cartItems: List<CartItemEntity>,
        wishlistItems: List<WishlistItemEntity>,
        comparisonItems: List<ComparisonItemEntity>,
        orders: List<OrderEntity>,
        orderItems: List<OrderItemEntity>,
        payments: List<PaymentRecordEntity>,
        loyaltyTransactions: List<LoyaltyTransactionEntity>,
        referrals: List<ReferralRecordEntity>,
        notifications: List<NotificationEntity>,
        supportMessages: List<SupportMessageEntity>
    ) {
        upsertUser(profile)
        clearAddressesForUser(profile.id)
        upsertAddresses(addresses)
        clearCart(profile.id)
        upsertCartItems(cartItems)
        clearWishlistForUser(profile.id)
        upsertWishlistItems(wishlistItems)
        clearComparisonForUser(profile.id)
        upsertComparisonItems(comparisonItems)
        upsertOrders(orders)
        insertOrderItems(orderItems)
        upsertPaymentRecords(payments)
        upsertLoyaltyTransactions(loyaltyTransactions)
        upsertReferrals(referrals)
        upsertNotifications(notifications)
        upsertSupportMessages(supportMessages)
    }

    @Transaction
    suspend fun placeOrderAtomic(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        payment: PaymentRecordEntity,
        cartUserId: String,
        couponCodeUsed: String,
        loyaltyPointsRedeemed: Int
    ): Boolean {
        if (getOrderByIdempotencyKey(order.idempotencyKey) != null) {
            return false
        }
        for (item in items) {
            val updated = deductStockIfAvailable(item.productId, item.quantity)
            if (updated == 0) {
                throw IllegalStateException("Insufficient stock for ${item.productTitleEn}")
            }
        }
        insertOrder(order)
        insertOrderItems(items)
        upsertPaymentRecord(payment)
        clearCart(cartUserId)
        if (couponCodeUsed.isNotBlank()) {
            val coupon = getCouponByCode(couponCodeUsed)
            if (coupon != null) {
                upsertCoupon(coupon.copy(usageCount = coupon.usageCount + 1))
            }
        }
        if (loyaltyPointsRedeemed > 0 && !order.isGuestOrder) {
            val redeemKey = "REDEEM_${order.id}"
            val inserted = insertLoyaltyTransaction(
                LoyaltyTransactionEntity(
                    id = "loy_${System.currentTimeMillis()}",
                    userId = order.userId,
                    orderId = order.id,
                    uniqueAwardKey = redeemKey,
                    pointsDelta = -loyaltyPointsRedeemed,
                    type = "REDEEMED",
                    description = "Redeemed on order ${order.orderNumber}"
                )
            )
            if (inserted > 0) {
                adjustUserLoyaltyPoints(order.userId, -loyaltyPointsRedeemed)
            }
        }
        return true
    }
}
