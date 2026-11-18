package com.example.data.ai

import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.remote.InsforgeRemoteDataSource

data class RecommendationBundle(
    val recommendedForYou: List<ProductEntity>,
    val frequentlyViewed: List<ProductEntity>,
    val trending: List<ProductEntity>,
    val reasonLabelEn: String,
    val reasonLabelBn: String,
    val isPlaceholderMode: Boolean = true,
    val noticeEn: String = "AI Recommendations use local smart ranking when offline.",
    val noticeBn: String = "অফলাইন থাকলে স্থানীয় স্মার্ট সাজেশন দেখানো হচ্ছে।"
)

interface AiRecommendationRepository {
    suspend fun getHomeRecommendations(
        allProducts: List<ProductEntity>,
        recentlyViewedIds: List<String>,
        purchasedItems: List<OrderItemEntity>,
        userConsented: Boolean
    ): RecommendationBundle

    suspend fun getSimilarProducts(
        target: ProductEntity,
        allProducts: List<ProductEntity>
    ): List<ProductEntity>
}

class SampleAiRecommendationRepository : AiRecommendationRepository {
    override suspend fun getHomeRecommendations(
        allProducts: List<ProductEntity>,
        recentlyViewedIds: List<String>,
        purchasedItems: List<OrderItemEntity>,
        userConsented: Boolean
    ): RecommendationBundle {
        val activeProducts = allProducts.filter { it.stockQty > 0 }
        val pool = activeProducts.ifEmpty { allProducts }

        val trending = pool.sortedByDescending { it.soldCount * 2 + it.reviewCount }.take(6)
        val frequentlyViewed = if (recentlyViewedIds.isNotEmpty() && userConsented) {
            val viewedSet = recentlyViewedIds.mapNotNull { id -> pool.find { it.id == id } }
            (viewedSet + pool.filter { it.isTrending }).distinctBy { it.id }.take(6)
        } else {
            pool.sortedByDescending { it.ratingAvg }.take(6)
        }

        val recommended = if (!userConsented) {
            pool.filter { it.isFeatured }.ifEmpty { pool }.take(6)
        } else {
            val preferredCategoryIds = buildSet {
                recentlyViewedIds.forEach { id ->
                    pool.find { it.id == id }?.categoryId?.let(::add)
                }
                purchasedItems.forEach { item ->
                    pool.find { it.id == item.productId }?.categoryId?.let(::add)
                }
            }
            if (preferredCategoryIds.isEmpty()) {
                pool.sortedByDescending { (it.ratingAvg * 20) + it.soldCount }.take(6)
            } else {
                pool.sortedByDescending { product ->
                    val catBonus = if (product.categoryId in preferredCategoryIds) 200.0 else 0.0
                    val discountBonus = product.discountPercent.toDouble()
                    catBonus + discountBonus + product.soldCount * 0.5 + product.ratingAvg * 10.0
                }.take(6)
            }
        }

        val reasonEn = if (userConsented && recentlyViewedIds.isNotEmpty()) {
            "Smart picks tailored from your recent browsing & top-rated BD items"
        } else {
            "Smart picks featuring storewide bestsellers & verified deals"
        }
        val reasonBn = if (userConsented && recentlyViewedIds.isNotEmpty()) {
            "আপনার সম্প্রতি দেখা পণ্য ও জনপ্রিয় ক্যাটাগরি থেকে স্মার্ট সাজেশন"
        } else {
            "স্টোরের সেরা বিক্রিত ও জনপ্রিয় পণ্য থেকে স্মার্ট সাজেশন"
        }

        return RecommendationBundle(
            recommendedForYou = recommended,
            frequentlyViewed = frequentlyViewed,
            trending = trending,
            reasonLabelEn = reasonEn,
            reasonLabelBn = reasonBn,
            isPlaceholderMode = true
        )
    }

    override suspend fun getSimilarProducts(
        target: ProductEntity,
        allProducts: List<ProductEntity>
    ): List<ProductEntity> {
        val targetTags = target.tagsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        return allProducts
            .filter { it.id != target.id }
            .sortedByDescending { candidate ->
                val sameCategory = if (candidate.categoryId == target.categoryId) 100 else 0
                val sameBrand = if (candidate.brand.equals(target.brand, ignoreCase = true)) 40 else 0
                val candidateTags = candidate.tagsCsv.split(",").map { it.trim().lowercase() }.toSet()
                val tagOverlap = targetTags.intersect(candidateTags).size * 15
                sameCategory + sameBrand + tagOverlap + candidate.ratingAvg.toInt()
            }
            .take(5)
    }
}

class InsforgeAiRecommendationRepository(
    private val remote: InsforgeRemoteDataSource?,
    private val fallback: SampleAiRecommendationRepository = SampleAiRecommendationRepository()
) : AiRecommendationRepository {

    override suspend fun getHomeRecommendations(
        allProducts: List<ProductEntity>,
        recentlyViewedIds: List<String>,
        purchasedItems: List<OrderItemEntity>,
        userConsented: Boolean
    ): RecommendationBundle {
        val localBundle = fallback.getHomeRecommendations(allProducts, recentlyViewedIds, purchasedItems, userConsented)
        if (remote == null || !userConsented || allProducts.isEmpty()) return localBundle

        val edgeResp = remote.fetchEdgeRecommendations(recentlyViewedIds).getOrNull()
        if (edgeResp == null || !edgeResp.ok) return localBundle

        val byId = allProducts.associateBy { it.id }
        val recList = edgeResp.recommendedIds.mapNotNull { byId[it] }.ifEmpty { localBundle.recommendedForYou }
        val trendList = edgeResp.trendingIds.mapNotNull { byId[it] }.ifEmpty { localBundle.trending }

        return localBundle.copy(
            recommendedForYou = recList,
            trending = trendList,
            isPlaceholderMode = false,
            noticeEn = "Personalized ranking synced via InsForge Edge Function (kena-kata-gateway).",
            noticeBn = "InsForge এজ ফাংশনের মাধ্যমে লাইভ স্মার্ট সাজেশন আপডেট হয়েছে।"
        )
    }

    override suspend fun getSimilarProducts(
        target: ProductEntity,
        allProducts: List<ProductEntity>
    ): List<ProductEntity> = fallback.getSimilarProducts(target, allProducts)
}
