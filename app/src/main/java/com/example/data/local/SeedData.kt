package com.example.data.local

import com.example.data.local.entity.*
import java.security.MessageDigest

object SeedData {
    fun hashPassword(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun initialUsers(): List<UserEntity> = listOf(
        UserEntity(
            id = "usr_admin_owner",
            name = "Kena Kata Owner",
            email = "owner@kenakata.bd",
            phone = "01711-009988",
            passwordHash = hashPassword("Admin@2026"),
            role = "ADMIN",
            emailVerified = true,
            loyaltyPoints = 0,
            referralCode = "KENAOWNER"
        ),
        UserEntity(
            id = "usr_customer_hasib",
            name = "Hasibul Hasan",
            email = "hasibulhasan2087@gmail.com",
            phone = "01712-345678",
            passwordHash = hashPassword("customer123"),
            role = "CUSTOMER",
            emailVerified = true,
            loyaltyPoints = 180,
            referralCode = "HASIB2087"
        )
    )

    fun initialAddresses(): List<AddressEntity> = listOf(
        AddressEntity(
            id = "addr_1",
            userId = "usr_customer_hasib",
            label = "Home (Dhanmondi)",
            fullName = "Hasibul Hasan",
            phone = "01712-345678",
            district = "Dhaka",
            area = "Dhanmondi",
            streetAddress = "House 14, Road 6, Dhanmondi, Dhaka 1205",
            isDefault = true
        )
    )

    fun initialCategories(): List<CategoryEntity> = listOf(
        CategoryEntity("cat_books", "Books & Literature", "বই ও সাহিত্য", "books", "book", 1, true),
        CategoryEntity("cat_electronics", "Gadgets & Tech", "গ্যাজেট ও টেক", "electronics", "devices", 2, true),
        CategoryEntity("cat_fashion", "Deshi Fashion", "দেশি ফ্যাশন", "fashion", "checkroom", 3, true),
        CategoryEntity("cat_home", "Home & Living", "ঘর ও লাইফস্টাইল", "home", "weekend", 4, true),
        CategoryEntity("cat_stationery", "Study & Stationery", "পড়াশোনা ও স্টেশনারি", "stationery", "edit", 5, true)
    )

    fun initialProducts(): List<ProductEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ProductEntity(
                id = "prod_himu_book",
                sku = "KK-BK-101",
                barcode = "8901234500101",
                titleEn = "Himu Shomogro (Vol. 1) Hardcover",
                titleBn = "হিমু সমগ্র (প্রথম খণ্ড) হার্ডকভার",
                brand = "Ananya Prokashoni",
                categoryId = "cat_books",
                descriptionEn = "Collector's edition hardcover compilation of classic Himu novels with crisp cream paper and gold-foil spine.",
                descriptionBn = "হুমায়ূন আহমেদের কালজয়ী হিমু সিরিজের প্রথম খণ্ডের প্রিমিয়াম হার্ডকভার সংস্করণ। উন্নত মানের ক্রিম কাগজ ও দৃষ্টিনন্দন বাঁধাই।",
                regularPrice = 750.0,
                salePrice = 560.0,
                costPrice = 410.0,
                stockQty = 38,
                lowStockThreshold = 6,
                variantsCsv = "Hardcover Collector's,Paperback Standard",
                colorsCsv = "Gold Foil Navy",
                specsPipe = "Author: Humayun Ahmed;;Language: Bengali;;Pages: 520;;Binding: Hardcover;;Publisher: Ananya",
                tagsCsv = "book,himu,bengali novel,rokomari style,bestseller",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewArrival = false,
                isTodayDeal = true,
                ratingAvg = 4.9,
                reviewCount = 24,
                soldCount = 195,
                seoTitle = "Himu Shomogro Vol 1 Hardcover | Kena Kata",
                seoDescription = "Buy original Himu Shomogro Vol 1 hardcover in Bangladesh with fast Dhaka delivery.",
                imageResKey = "hero_rokomari_reads",
                galleryResCsv = "hero_rokomari_reads,fashion_craft",
                createdAtMillis = now - 86400000L * 14
            ),
            ProductEntity(
                id = "prod_anc_earbuds",
                sku = "KK-EL-201",
                barcode = "8901234500201",
                titleEn = "QCY MeloBuds ANC Wireless Earbuds",
                titleBn = "কিউসিওয়াই মেলোবাডস এএনসি ওয়্যারলেস ইয়ারবাডস",
                brand = "QCY Bangladesh",
                categoryId = "cat_electronics",
                descriptionEn = "40dB hybrid active noise cancellation, 6-mic ENC crystal call quality, Bluetooth 5.3, and 30-hour USB-C battery life.",
                descriptionBn = "৪০ ডিবি অ্যাক্টিভ নয়েজ ক্যান্সেলেশন, ৬-মাইক ক্লিয়ার কল কোয়ালিটি এবং ৩০ ঘণ্টা ব্যাটারি ব্যাকআপ।",
                regularPrice = 3450.0,
                salePrice = 2690.0,
                costPrice = 2050.0,
                stockQty = 19,
                lowStockThreshold = 5,
                variantsCsv = "Standard Edition,With Silicone Case",
                colorsCsv = "Midnight Blue,Pearl White",
                specsPipe = "ANC: 40dB Hybrid;;Bluetooth: v5.3;;Battery: 30 Hours;;Waterproof: IPX5;;Warranty: 6 Months Official",
                tagsCsv = "earbuds,anc,audio,bluetooth,gadget",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewArrival = true,
                isTodayDeal = true,
                ratingAvg = 4.8,
                reviewCount = 18,
                soldCount = 142,
                seoTitle = "QCY MeloBuds ANC Earbuds Price in BD | Kena Kata",
                seoDescription = "Authentic QCY MeloBuds ANC earbuds in Bangladesh with warranty and bKash/COD checkout.",
                imageResKey = "electronics",
                galleryResCsv = "electronics,hero_mega_sale",
                createdAtMillis = now - 86400000L * 6
            ),
            ProductEntity(
                id = "prod_jamdani_panjabi",
                sku = "KK-FS-301",
                barcode = "8901234500301",
                titleEn = "Tangail Woven Cotton Panjabi",
                titleBn = "টাঙ্গাইল হ্যান্ডলুম কটন পাঞ্জাবি",
                brand = "Kena Kata Heritage",
                categoryId = "cat_fashion",
                descriptionEn = "Breathable 100% combed Bangladeshi cotton panjabi tailored for Dhaka humidity with subtle jamdani collar embroidery.",
                descriptionBn = "খাঁটি টাঙ্গাইল সুতির আরামদায়ক পাঞ্জাবি, সূক্ষ্ম জামদানি কারুকাজ ও আধুনিক ফিটিং।",
                regularPrice = 2200.0,
                salePrice = 1650.0,
                costPrice = 1100.0,
                stockQty = 24,
                lowStockThreshold = 5,
                variantsCsv = "M (40),L (42),XL (44)",
                colorsCsv = "Royal Blue,Ivory Cream,Charcoal",
                specsPipe = "Fabric: 100% Tangail Cotton;;Fit: Regular Comfort;;Collar: Mandarin Jamdani;;Origin: Tangail, BD",
                tagsCsv = "panjabi,cotton,eid,fashion,bangladesh",
                isFeatured = true,
                isTrending = true,
                isBestSeller = false,
                isNewArrival = true,
                isTodayDeal = false,
                ratingAvg = 4.7,
                reviewCount = 12,
                soldCount = 87,
                seoTitle = "Tangail Cotton Panjabi | Kena Kata Bangladesh",
                seoDescription = "Handloom Tangail cotton panjabi with authentic jamdani detailing.",
                imageResKey = "fashion_craft",
                galleryResCsv = "fashion_craft,hero_rokomari_reads",
                createdAtMillis = now - 86400000L * 4
            ),
            ProductEntity(
                id = "prod_mech_keyboard",
                sku = "KK-EL-204",
                barcode = "8901234500204",
                titleEn = "Keychron K2 Hot-Swappable Mechanical Keyboard",
                titleBn = "কীক্রোন কে২ মেকানিক্যাল কীবোর্ড",
                brand = "Keychron",
                categoryId = "cat_electronics",
                descriptionEn = "Compact 75% wireless/wired mechanical keyboard with Gateron Brown switches, Mac/Windows keycaps, and white backlight.",
                descriptionBn = "৭৫% কমপ্যাক্ট ওয়্যারলেস ও ওয়্যার্ড মেকানিক্যাল কীবোর্ড, গ্যাটারন ব্রাউন সুইচসহ।",
                regularPrice = 8900.0,
                salePrice = 7490.0,
                costPrice = 6100.0,
                stockQty = 4, // Intentional Low Stock alert example!
                lowStockThreshold = 5,
                variantsCsv = "Gateron Brown,Gateron Red",
                colorsCsv = "Slate Gray & Orange",
                specsPipe = "Layout: 75% (84 Keys);;Connectivity: Bluetooth 5.1 & USB-C;;Battery: 4000mAh;;Hot-Swappable: Yes",
                tagsCsv = "keyboard,mechanical,developer,gaming,tech",
                isFeatured = true,
                isTrending = false,
                isBestSeller = true,
                isNewArrival = false,
                isTodayDeal = true,
                ratingAvg = 4.9,
                reviewCount = 15,
                soldCount = 63,
                seoTitle = "Keychron K2 Mechanical Keyboard BD | Kena Kata",
                seoDescription = "Official Keychron K2 hot-swappable keyboard in Bangladesh.",
                imageResKey = "electronics",
                galleryResCsv = "electronics,hero_mega_sale",
                createdAtMillis = now - 86400000L * 10
            ),
            ProductEntity(
                id = "prod_powerbank_20k",
                sku = "KK-EL-208",
                barcode = "8901234500208",
                titleEn = "Anker PowerCore 20,000mAh 22.5W Fast Power Bank",
                titleBn = "অ্যাঙ্কার ২০,০০০ এমএএইচ ফাস্ট পাওয়ার ব্যাংক",
                brand = "Anker",
                categoryId = "cat_electronics",
                descriptionEn = "High-capacity 20,000mAh portable charger with USB-C PD 22.5W fast charging and dual USB-A ports.",
                descriptionBn = "২২.৫ ওয়াট ফাস্ট চার্জিং সুবিধাসহ ২০,০০০ এমএএইচ পাওয়ার ব্যাংক।",
                regularPrice = 3200.0,
                salePrice = 2750.0,
                costPrice = 2150.0,
                stockQty = 16,
                lowStockThreshold = 5,
                variantsCsv = "20,000mAh 22.5W",
                colorsCsv = "Matte Black,Navy Blue",
                specsPipe = "Capacity: 20,000mAh;;Fast Charge: 22.5W PD;;Ports: 1x USB-C, 2x USB-A;;Warranty: 12 Months",
                tagsCsv = "powerbank,anker,charger,travel",
                isFeatured = false,
                isTrending = true,
                isBestSeller = true,
                isNewArrival = false,
                isTodayDeal = false,
                ratingAvg = 4.8,
                reviewCount = 29,
                soldCount = 210,
                imageResKey = "hero_mega_sale",
                galleryResCsv = "hero_mega_sale,electronics",
                createdAtMillis = now - 86400000L * 18
            ),
            ProductEntity(
                id = "prod_atomic_habits_bn",
                sku = "KK-BK-105",
                barcode = "8901234500105",
                titleEn = "Atomic Habits (Bengali Authorized Edition)",
                titleBn = "অ্যাটমিক হ্যাবিটস (বাংলা অনুবাদ সংস্করণ)",
                brand = "Adarsha Prokashoni",
                categoryId = "cat_books",
                descriptionEn = "James Clear's transformative guide to building good habits and breaking bad ones, cleanly translated into natural Bengali.",
                descriptionBn = "জেমস ক্লিয়ারের বিশ্বখ্যাত বেস্টসেলার বইয়ের প্রাঞ্জল বাংলা অনুবাদ সংস্করণ।",
                regularPrice = 480.0,
                salePrice = 340.0,
                costPrice = 220.0,
                stockQty = 52,
                lowStockThreshold = 8,
                variantsCsv = "Paperback,Hardcover",
                colorsCsv = "Classic White",
                specsPipe = "Author: James Clear;;Pages: 312;;Language: Bengali;;Binding: Quality Paperback",
                tagsCsv = "books,self help,habits,bengali,study",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewArrival = true,
                isTodayDeal = true,
                ratingAvg = 4.9,
                reviewCount = 41,
                soldCount = 320,
                imageResKey = "hero_rokomari_reads",
                galleryResCsv = "hero_rokomari_reads",
                createdAtMillis = now - 86400000L * 2
            ),
            ProductEntity(
                id = "prod_study_lamp",
                sku = "KK-HM-402",
                barcode = "8901234500402",
                titleEn = "Nordic Brass Eye-Care Study Desk Lamp",
                titleBn = "আই-কেয়ার পিতলের স্টাডি ডেস্ক ল্যাম্প",
                brand = "Alo Craft",
                categoryId = "cat_home",
                descriptionEn = "Flicker-free 3-tone dimmable LED reading lamp with adjustable brass arm for late-night study and reading.",
                descriptionBn = "চোখের আরামদায়ক ৩-টোন লাইটিং ও এডজাস্টেবল স্ট্যান্ডসহ প্রিমিয়াম স্টাডি ল্যাম্প।",
                regularPrice = 1850.0,
                salePrice = 1390.0,
                costPrice = 920.0,
                stockQty = 11,
                lowStockThreshold = 4,
                variantsCsv = "USB Rechargeable,AC Plug-in",
                colorsCsv = "Brushed Brass,Matte Black",
                specsPipe = "Color Temp: 3000K-6000K;;Power: 10W Eye-Care LED;;Material: Aluminum & Brass",
                tagsCsv = "lamp,study,desk,home,decor",
                isFeatured = false,
                isTrending = false,
                isBestSeller = false,
                isNewArrival = true,
                isTodayDeal = false,
                ratingAvg = 4.6,
                reviewCount = 9,
                soldCount = 41,
                imageResKey = "hero_rokomari_reads",
                galleryResCsv = "hero_rokomari_reads",
                createdAtMillis = now - 86400000L * 3
            ),
            ProductEntity(
                id = "prod_leather_wallet",
                sku = "KK-FS-309",
                barcode = "8901234500309",
                titleEn = "Full-Grain Bangladeshi Leather RFID Wallet",
                titleBn = "খাঁটি চামড়ার আরএফআইডি প্রোটেক্টেড ওয়ালেট",
                brand = "Tannery Craft BD",
                categoryId = "cat_fashion",
                descriptionEn = "Hand-stitched full-grain cowhide bifold wallet crafted in Dhaka with RFID shielding and 8 card slots.",
                descriptionBn = "ঢাকায় তৈরি খাঁটি গরুর চামড়ার হ্যান্ড-স্টিচড ওয়ালেট, ৮টি কার্ড স্লট ও আরএফআইডি সুরক্ষাসহ।",
                regularPrice = 1250.0,
                salePrice = 890.0,
                costPrice = 540.0,
                stockQty = 0, // Intentional Out-Of-Stock alert example!
                lowStockThreshold = 5,
                variantsCsv = "Standard Bifold,Gift Box Edition",
                colorsCsv = "Espresso Brown,Jet Black",
                specsPipe = "Material: 100% Full-Grain Leather;;RFID Shield: Yes;;Slots: 8 Cards + 2 Cash",
                tagsCsv = "wallet,leather,gift,fashion",
                isFeatured = false,
                isTrending = false,
                isBestSeller = true,
                isNewArrival = false,
                isTodayDeal = false,
                ratingAvg = 4.7,
                reviewCount = 19,
                soldCount = 115,
                imageResKey = "fashion_craft",
                galleryResCsv = "fashion_craft",
                createdAtMillis = now - 86400000L * 25
            ),
            ProductEntity(
                id = "prod_journal_kit",
                sku = "KK-ST-501",
                barcode = "8901234500501",
                titleEn = "Kena Kata Dotted Bullet Journal & Fountain Pen Kit",
                titleBn = "কেনা কাটা ডটেড জার্নাল ও ফাউন্টেন পেন সেট",
                brand = "PaperTrail BD",
                categoryId = "cat_stationery",
                descriptionEn = "120gsm fountain-pen friendly dotted A5 notebook paired with a smooth brass-nib fountain pen and 2 ink cartridges.",
                descriptionBn = "১২০ জিএসএম প্রিমিয়াম কাগজের ডটেড নোটবুক ও মসৃণ ফাউন্টেন পেন কম্বো সেট।",
                regularPrice = 950.0,
                salePrice = 720.0,
                costPrice = 440.0,
                stockQty = 27,
                lowStockThreshold = 5,
                variantsCsv = "A5 Dotted Kit,A5 Ruled Kit",
                colorsCsv = "Deep Blue Cover,Burnt Orange Cover",
                specsPipe = "Paper: 120gsm Acid-Free;;Pages: 192;;Pen Nib: Fine 0.5mm",
                tagsCsv = "notebook,journal,pen,stationery,study",
                isFeatured = true,
                isTrending = true,
                isBestSeller = false,
                isNewArrival = true,
                isTodayDeal = true,
                ratingAvg = 4.8,
                reviewCount = 14,
                soldCount = 74,
                imageResKey = "hero_rokomari_reads",
                galleryResCsv = "hero_rokomari_reads",
                createdAtMillis = now - 86400000L * 1
            )
        )
    }

    fun initialBanners(): List<BannerEntity> = listOf(
        BannerEntity(
            id = "ban_1",
            titleEn = "Winter Mega Tech & Book Fair",
            titleBn = "উইন্টার মেগা টেক ও বই মেলা",
            subtitleEn = "Up to 30% OFF + 10% Extra with code KENA10",
            subtitleBn = "৩০% পর্যন্ত ছাড় + KENA10 কোডে আরও ১০% সাশ্রয়",
            badgeText = "HOT DEAL",
            targetCategoryId = "cat_electronics",
            imageResKey = "hero_mega_sale",
            isActive = true,
            sortOrder = 1
        ),
        BannerEntity(
            id = "ban_2",
            titleEn = "Curated Bengali Classics & Study Kits",
            titleBn = "সেরা বাংলা বই ও স্টাডি কালেকশন",
            subtitleEn = "Authentic editions delivered across 64 districts",
            subtitleBn = "সারা দেশের ৬৪ জেলায় দ্রুত হোম ডেলিভারি",
            badgeText = "ROKOMARI PICK",
            targetCategoryId = "cat_books",
            imageResKey = "hero_rokomari_reads",
            isActive = true,
            sortOrder = 2
        )
    )

    fun initialFlashSales(): List<FlashSaleEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            FlashSaleEntity(
                id = "fs_winter_1",
                titleEn = "Midnight Lightning Deals",
                titleBn = "মিডনাইট ফ্ল্যাশ সেল",
                discountPercent = 25,
                startTimeMillis = now - 3600_000L,
                endTimeMillis = now + 8 * 3600_000L,
                productIdsCsv = "prod_himu_book,prod_anc_earbuds,prod_mech_keyboard,prod_atomic_habits_bn,prod_journal_kit",
                isActive = true
            )
        )
    }

    fun initialCoupons(): List<CouponEntity> {
        val future = System.currentTimeMillis() + 30L * 86400_000L
        return listOf(
            CouponEntity("cpn_1", "KENA10", "PERCENT", 10.0, 500.0, 400.0, true, future, 14),
            CouponEntity("cpn_2", "DHAKA50", "FIXED", 50.0, 600.0, 50.0, true, future, 8),
            CouponEntity("cpn_3", "BOOK20", "PERCENT", 20.0, 400.0, 250.0, true, future, 21)
        )
    }

    fun initialReviews(): List<ReviewEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ReviewEntity(
                id = "rev_1",
                productId = "prod_himu_book",
                userId = "usr_customer_hasib",
                userName = "Hasibul Hasan",
                rating = 5,
                comment = "Print quality and packaging were spotless. Arrived in Dhanmondi within 24 hours!",
                isApproved = true,
                createdAtMillis = now - 86400_000L * 3
            ),
            ReviewEntity(
                id = "rev_2",
                productId = "prod_anc_earbuds",
                userId = "usr_demo_sadia",
                userName = "Sadia Afrin",
                rating = 5,
                comment = "ANC blocks Dhaka traffic noise really well. Verified bKash payment smoothly.",
                isApproved = true,
                createdAtMillis = now - 86400_000L * 2
            ),
            ReviewEntity(
                id = "rev_3",
                productId = "prod_jamdani_panjabi",
                userId = "usr_demo_rafiq",
                userName = "Rafiqul Islam",
                rating = 5,
                comment = "Soft Tangail cotton, accurate size chart, and great value in BDT.",
                isApproved = true,
                createdAtMillis = now - 86400_000L * 5
            )
        )
    }

    fun initialSampleOrders(): List<OrderEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            OrderEntity(
                id = "ord_seed_1",
                orderNumber = "KK-2026-10041",
                idempotencyKey = "seed_order_1",
                userId = "usr_customer_hasib",
                customerName = "Hasibul Hasan",
                customerPhone = "01712-345678",
                shippingAddress = "House 14, Road 6, Dhanmondi, Dhaka 1205",
                district = "Dhaka",
                area = "Dhanmondi",
                subtotal = 3250.0,
                deliveryCharge = 60.0,
                deliveryCostActual = 45.0,
                discountAmount = 325.0,
                loyaltyPointsUsed = 0,
                loyaltyDiscount = 0.0,
                couponCode = "KENA10",
                totalAmount = 2985.0,
                paymentMethod = "BKASH",
                paymentStatus = "VERIFIED",
                orderStatus = "DELIVERED",
                courierName = "Steadfast Courier",
                trackingNumber = "SF-BD-882941",
                createdAtMillis = now - 86400_000L * 4,
                updatedAtMillis = now - 86400_000L * 2
            ),
            OrderEntity(
                id = "ord_seed_2",
                orderNumber = "KK-2026-10042",
                idempotencyKey = "seed_order_2",
                userId = "usr_customer_hasib",
                customerName = "Hasibul Hasan",
                customerPhone = "01712-345678",
                shippingAddress = "House 14, Road 6, Dhanmondi, Dhaka 1205",
                district = "Dhaka",
                area = "Dhanmondi",
                subtotal = 1650.0,
                deliveryCharge = 60.0,
                deliveryCostActual = 45.0,
                discountAmount = 0.0,
                loyaltyPointsUsed = 0,
                loyaltyDiscount = 0.0,
                couponCode = "",
                totalAmount = 1710.0,
                paymentMethod = "NAGAD",
                paymentStatus = "SUBMITTED",
                orderStatus = "PENDING",
                courierName = "Pathao Courier",
                trackingNumber = "",
                createdAtMillis = now - 3600_000L * 5,
                updatedAtMillis = now - 3600_000L * 5
            )
        )
    }

    fun initialSampleOrderItems(): List<OrderItemEntity> = listOf(
        OrderItemEntity("oi_1", "ord_seed_1", "prod_anc_earbuds", "QCY MeloBuds ANC Wireless Earbuds", "কিউসিওয়াই মেলোবাডস এএনসি ওয়্যারলেস ইয়ারবাডস", "Midnight Blue", 2690.0, 2050.0, 1),
        OrderItemEntity("oi_2", "ord_seed_1", "prod_himu_book", "Himu Shomogro (Vol. 1) Hardcover", "হিমু সমগ্র (প্রথম খণ্ড) হার্ডকভার", "Hardcover Collector's", 560.0, 410.0, 1),
        OrderItemEntity("oi_3", "ord_seed_2", "prod_jamdani_panjabi", "Tangail Woven Cotton Panjabi", "টাঙ্গাইল হ্যান্ডলুম কটন পাঞ্জাবি", "L (42) / Royal Blue", 1650.0, 1100.0, 1)
    )

    fun initialSamplePayments(): List<PaymentRecordEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            PaymentRecordEntity(
                id = "pay_1",
                orderId = "ord_seed_1",
                orderNumber = "KK-2026-10041",
                userId = "usr_customer_hasib",
                method = "BKASH",
                amount = 2985.0,
                senderNumber = "01712345678",
                transactionId = "BKS9X72LMN",
                status = "VERIFIED",
                adminNote = "Verified against bKash merchant statement",
                createdAtMillis = now - 86400_000L * 4,
                verifiedAtMillis = now - 86400_000L * 4 + 1800_000L
            ),
            PaymentRecordEntity(
                id = "pay_2",
                orderId = "ord_seed_2",
                orderNumber = "KK-2026-10042",
                userId = "usr_customer_hasib",
                method = "NAGAD",
                amount = 1710.0,
                senderNumber = "01712345678",
                transactionId = "NGD44K81PQ",
                status = "SUBMITTED",
                adminNote = "Awaiting owner TrxID verification",
                createdAtMillis = now - 3600_000L * 5,
                verifiedAtMillis = null
            )
        )
    }

    fun initialLoyaltyTransactions(): List<LoyaltyTransactionEntity> = listOf(
        LoyaltyTransactionEntity(
            id = "loy_seed_1",
            userId = "usr_customer_hasib",
            orderId = "ord_seed_1",
            uniqueAwardKey = "EARN_ord_seed_1",
            pointsDelta = 60,
            type = "EARNED",
            description = "Earned from verified order KK-2026-10041",
            createdAtMillis = System.currentTimeMillis() - 86400_000L * 2
        ),
        LoyaltyTransactionEntity(
            id = "loy_seed_2",
            userId = "usr_customer_hasib",
            orderId = "",
            uniqueAwardKey = "WELCOME_usr_customer_hasib",
            pointsDelta = 120,
            type = "EARNED",
            description = "Welcome loyalty bonus",
            createdAtMillis = System.currentTimeMillis() - 86400_000L * 10
        )
    )

    fun initialNotifications(): List<NotificationEntity> = listOf(
        NotificationEntity(
            id = "notif_1",
            userId = "ALL",
            titleEn = "Midnight Flash Sale Live!",
            titleBn = "মিডনাইট ফ্ল্যাশ সেল শুরু হয়েছে!",
            messageEn = "Save up to 25% on books and ANC gadgets. Use KENA10 at checkout.",
            messageBn = "বই ও গ্যাজেটে ২৫% পর্যন্ত ছাড়! চেকআউটে KENA10 কোড ব্যবহার করুন।",
            type = "FLASH_SALE"
        ),
        NotificationEntity(
            id = "notif_2",
            userId = "usr_customer_hasib",
            titleEn = "Order KK-2026-10041 Delivered",
            titleBn = "অর্ডার KK-2026-10041 ডেলিভারি সম্পন্ন",
            messageEn = "You earned 60 Loyalty Points. Download your PDF invoice anytime.",
            messageBn = "আপনি ৬০ লয়্যালটি পয়েন্ট অর্জন করেছেন। যেকোনো সময় PDF ইনভয়েস ডাউনলোড করুন।",
            type = "ORDER"
        )
    )
}
