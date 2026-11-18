package com.example.i18n

import java.text.NumberFormat
import java.util.Locale

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "EN"),
    BN("bn", "বাংলা")
}

object Strings {
    fun tr(lang: AppLanguage, en: String, bn: String): String =
        if (lang == AppLanguage.BN) bn else en

    fun formatBdt(amount: Double, lang: AppLanguage = AppLanguage.EN): String {
        val rounded = kotlin.math.round(amount).toLong()
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).format(rounded)
        return if (lang == AppLanguage.BN) {
            "৳${toBengaliDigits(formatted)}"
        } else {
            "৳$formatted"
        }
    }

    fun toBengaliDigits(input: String): String {
        val map = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return buildString {
            for (ch in input) append(map[ch] ?: ch)
        }
    }

    fun navHome(l: AppLanguage) = tr(l, "Home", "হোম")
    fun navShop(l: AppLanguage) = tr(l, "Shop", "শপ")
    fun navCart(l: AppLanguage) = tr(l, "Cart", "কার্ট")
    fun navOrders(l: AppLanguage) = tr(l, "Orders", "অর্ডার")
    fun navAccount(l: AppLanguage) = tr(l, "Account", "অ্যাকাউন্ট")

    fun tagline(l: AppLanguage) = tr(l, "Shop Smart, Buy Better.", "বুদ্ধিমান কেনাকাটা, সেরা মানের নিশ্চয়তা।")
    fun searchHint(l: AppLanguage) = tr(l, "Search books, gadgets, fashion...", "বই, গ্যাজেট, ফ্যাশন খুঁজুন...")
    fun flashSale(l: AppLanguage) = tr(l, "Flash Sale", "ফ্ল্যাশ সেল")
    fun todaysDeals(l: AppLanguage) = tr(l, "Today's Deals", "আজকের ডিল")
    fun featuredCategories(l: AppLanguage) = tr(l, "Featured Categories", "জনপ্রিয় ক্যাটাগরি")
    fun trendingProducts(l: AppLanguage) = tr(l, "Trending Now", "ট্রেন্ডিং পণ্য")
    fun bestSelling(l: AppLanguage) = tr(l, "Best Sellers", "সর্বাধিক বিক্রিত")
    fun newArrivals(l: AppLanguage) = tr(l, "New Arrivals", "নতুন কালেকশন")
    fun recommendedForYou(l: AppLanguage) = tr(l, "Recommended for You", "আপনার জন্য সুপারিশ")
    fun recentlyViewed(l: AppLanguage) = tr(l, "Recently Viewed", "সম্প্রতি দেখা পণ্য")
    fun customerReviews(l: AppLanguage) = tr(l, "Customer Reviews", "গ্রাহকদের মতামত")
    fun newsletterTitle(l: AppLanguage) = tr(l, "Stay Updated with Kena Kata", "কেনা কাটার নতুন অফার পান")
    fun addToCart(l: AppLanguage) = tr(l, "Add to Cart", "কার্টে যোগ করুন")
    fun buyNow(l: AppLanguage) = tr(l, "Buy Now", "এখনই কিনুন")
    fun outOfStock(l: AppLanguage) = tr(l, "Out of Stock", "স্টক শেষ")
    fun lowStock(l: AppLanguage) = tr(l, "Low Stock", "স্টক সীমিত")
    fun inStock(l: AppLanguage) = tr(l, "In Stock", "স্টকে আছে")
    fun compare(l: AppLanguage) = tr(l, "Compare", "তুলনা")
    fun wishlist(l: AppLanguage) = tr(l, "Wishlist", "পছন্দের তালিকা")
    fun checkout(l: AppLanguage) = tr(l, "Proceed to Checkout", "চেকআউট করুন")
    fun placeOrder(l: AppLanguage) = tr(l, "Confirm Order", "অর্ডার নিশ্চিত করুন")
    fun subtotal(l: AppLanguage) = tr(l, "Subtotal", "উপমোট")
    fun deliveryFee(l: AppLanguage) = tr(l, "Delivery Charge", "ডেলিভারি চার্জ")
    fun discount(l: AppLanguage) = tr(l, "Discount", "ডিসকাউন্ট")
    fun total(l: AppLanguage) = tr(l, "Total Payable", "সর্বমোট")
    fun adminPortal(l: AppLanguage) = tr(l, "Owner Admin Dashboard", "অ্যাডমিন ড্যাশবোর্ড")
}
