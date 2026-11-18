package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["referralCode"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val passwordHash: String,
    val role: String = "CUSTOMER", // Only CUSTOMER or ADMIN (single-vendor owner)
    val emailVerified: Boolean = false,
    val loyaltyPoints: Int = 0,
    val referralCode: String,
    val referredByCode: String = "",
    val avatarUri: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "addresses", indices = [Index("userId")])
data class AddressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val label: String,
    val fullName: String,
    val phone: String,
    val district: String,
    val area: String,
    val streetAddress: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameBn: String,
    val slug: String,
    val iconKey: String,
    val sortOrder: Int = 0,
    val isFeatured: Boolean = true
)

@Entity(
    tableName = "products",
    indices = [Index("categoryId"), Index(value = ["sku"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val barcode: String = "",
    val titleEn: String,
    val titleBn: String,
    val brand: String,
    val categoryId: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val regularPrice: Double,
    val salePrice: Double,
    val costPrice: Double,
    val stockQty: Int,
    val lowStockThreshold: Int = 5,
    val variantsCsv: String = "Standard",
    val colorsCsv: String = "",
    val specsPipe: String = "", // Key:Value;;Key:Value
    val tagsCsv: String = "",
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNewArrival: Boolean = false,
    val isTodayDeal: Boolean = false,
    val ratingAvg: Double = 4.6,
    val reviewCount: Int = 0,
    val soldCount: Int = 0,
    val seoTitle: String = "",
    val seoDescription: String = "",
    val imageResKey: String = "electronics",
    val imageUri: String = "",
    val galleryResCsv: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val effectivePrice: Double
        get() = if (salePrice > 0.0 && salePrice < regularPrice) salePrice else regularPrice

    val discountPercent: Int
        get() = if (salePrice > 0.0 && salePrice < regularPrice && regularPrice > 0.0) {
            (((regularPrice - salePrice) / regularPrice) * 100).toInt()
        } else 0
}

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val badgeText: String,
    val targetCategoryId: String = "",
    val imageResKey: String = "hero_mega_sale",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "flash_sales")
data class FlashSaleEntity(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleBn: String,
    val discountPercent: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val productIdsCsv: String,
    val isActive: Boolean = true
)

@Entity(tableName = "coupons", indices = [Index(value = ["code"], unique = true)])
data class CouponEntity(
    @PrimaryKey val id: String,
    val code: String,
    val discountType: String, // PERCENT or FIXED
    val discountValue: Double,
    val minOrderAmount: Double,
    val maxDiscount: Double,
    val isActive: Boolean = true,
    val expiresAtMillis: Long,
    val usageCount: Int = 0
)

@Entity(tableName = "cart_items", indices = [Index("userId"), Index("productId")])
data class CartItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String,
    val variantLabel: String,
    val quantity: Int,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist_items", indices = [Index("userId"), Index("productId")])
data class WishlistItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "comparison_items", indices = [Index("userId"), Index("productId")])
data class ComparisonItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "orders",
    indices = [Index("userId"), Index(value = ["orderNumber"], unique = true), Index(value = ["idempotencyKey"], unique = true)]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val idempotencyKey: String,
    val userId: String,
    val customerName: String,
    val customerPhone: String,
    val shippingAddress: String,
    val district: String,
    val area: String,
    val subtotal: Double,
    val deliveryCharge: Double,
    val deliveryCostActual: Double = 60.0,
    val discountAmount: Double,
    val loyaltyPointsUsed: Int = 0,
    val loyaltyDiscount: Double = 0.0,
    val couponCode: String = "",
    val totalAmount: Double,
    val paymentMethod: String, // BKASH, NAGAD, COD
    val paymentStatus: String, // PENDING, SUBMITTED, VERIFIED, FAILED, REFUNDED
    val orderStatus: String, // PENDING, CONFIRMED, PROCESSING, PACKED, SHIPPED, DELIVERED, CANCELLED, RETURNED
    val courierName: String = "Unassigned",
    val trackingNumber: String = "",
    val isGuestOrder: Boolean = false,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items", indices = [Index("orderId"), Index("productId")])
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productTitleEn: String,
    val productTitleBn: String,
    val variantLabel: String,
    val unitPrice: Double,
    val costPrice: Double,
    val quantity: Int
)

@Entity(tableName = "payment_records", indices = [Index("orderId"), Index("userId")])
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val orderNumber: String,
    val userId: String,
    val method: String, // BKASH, NAGAD, COD
    val amount: Double,
    val senderNumber: String = "",
    val transactionId: String = "",
    val status: String, // PENDING, SUBMITTED, VERIFIED, FAILED, REFUNDED
    val adminNote: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val verifiedAtMillis: Long? = null
)

@Entity(tableName = "reviews", indices = [Index("productId")])
data class ReviewEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val isApproved: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loyalty_transactions",
    indices = [Index("userId"), Index(value = ["uniqueAwardKey"], unique = true)]
)
data class LoyaltyTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val orderId: String,
    val uniqueAwardKey: String,
    val pointsDelta: Int,
    val type: String, // EARNED, REDEEMED, REVERSED, REFERRAL
    val description: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "referral_records", indices = [Index("referrerUserId"), Index("referredUserId")])
data class ReferralRecordEntity(
    @PrimaryKey val id: String,
    val referrerUserId: String,
    val referredUserId: String,
    val referredUserEmail: String,
    val referralCode: String,
    val rewardPoints: Int,
    val status: String, // PENDING, COMPLETED
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications", indices = [Index("userId")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val titleEn: String,
    val titleBn: String,
    val messageEn: String,
    val messageBn: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_messages", indices = [Index("userId")])
data class SupportMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val message: String,
    val status: String = "OPEN", // OPEN, REPLIED
    val adminReply: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorEmail: String,
    val actionType: String,
    val targetType: String,
    val targetId: String,
    val summary: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "website_settings")
data class WebsiteSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Kena Kata",
    val taglineEn: String = "Shop Smart, Buy Better.",
    val taglineBn: String = "বুদ্ধিমান কেনাকাটা, সেরা মানের নিশ্চয়তা।",
    val announcementEn: String = "Free Dhaka Delivery on orders over ৳1,500 | Code: KENA10 for 10% OFF",
    val announcementBn: String = "৳১,৫০০+ অর্ডারে ঢাকায় ফ্রি ডেলিভারি | ১০% ছাড়ে কোড: KENA10",
    val bkashMerchantNumber: String = "01711-009988 (Merchant)",
    val nagadMerchantNumber: String = "01811-009988 (Merchant)",
    val whatsappNumber: String = "8801711009988",
    val supportEmail: String = "support@kenakata.bd",
    val dhakaDeliveryFee: Double = 60.0,
    val outsideDhakaDeliveryFee: Double = 120.0,
    val loyaltyEarnPer100Bdt: Int = 2,
    val loyaltyPointValueBdt: Double = 1.0,
    val referralRewardPoints: Int = 50,
    val newsletterSubscribersCount: Int = 1420,
    val showFlashSale: Boolean = true,
    val showTrending: Boolean = true,
    val showBestSelling: Boolean = true,
    val showNewArrivals: Boolean = true,
    val showTodayDeals: Boolean = true,
    val showRecommended: Boolean = true,
    val showReviews: Boolean = true,
    val seoHomeTitle: String = "Kena Kata Bangladesh | Shop Smart, Buy Better",
    val seoHomeDescription: String = "Single-vendor online store in Bangladesh for curated books, tech gadgets, fashion & lifestyle in BDT."
)

@Entity(tableName = "backup_logs")
data class BackupLogEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val recordCount: Int,
    val status: String,
    val actorEmail: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
