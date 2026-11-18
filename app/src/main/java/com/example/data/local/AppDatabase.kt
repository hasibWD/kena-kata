package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.KenaKataDao
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        AddressEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        BannerEntity::class,
        FlashSaleEntity::class,
        CouponEntity::class,
        CartItemEntity::class,
        WishlistItemEntity::class,
        ComparisonItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentRecordEntity::class,
        ReviewEntity::class,
        LoyaltyTransactionEntity::class,
        ReferralRecordEntity::class,
        NotificationEntity::class,
        SupportMessageEntity::class,
        ActivityLogEntity::class,
        WebsiteSettingsEntity::class,
        BackupLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): KenaKataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kena_kata_store.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { seedIfEmpty(it.dao()) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedIfEmpty(dao: KenaKataDao) {
            if (dao.getCategoriesOnce().isEmpty()) {
                SeedData.initialUsers().forEach { dao.upsertUser(it) }
                SeedData.initialAddresses().forEach { dao.upsertAddress(it) }
                dao.upsertCategories(SeedData.initialCategories())
                dao.upsertProducts(SeedData.initialProducts())
                dao.upsertBanners(SeedData.initialBanners())
                dao.upsertFlashSales(SeedData.initialFlashSales())
                dao.upsertCoupons(SeedData.initialCoupons())
                SeedData.initialReviews().forEach { dao.upsertReview(it) }
                SeedData.initialSampleOrders().forEach { dao.insertOrder(it) }
                dao.insertOrderItems(SeedData.initialSampleOrderItems())
                SeedData.initialSamplePayments().forEach { dao.upsertPaymentRecord(it) }
                SeedData.initialLoyaltyTransactions().forEach { dao.insertLoyaltyTransaction(it) }
                SeedData.initialNotifications().forEach { dao.insertNotification(it) }
                dao.upsertWebsiteSettings(WebsiteSettingsEntity())
            }
        }
    }
}
