package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRecommendationRepository
import com.example.data.ai.InsforgeAiRecommendationRepository
import com.example.data.ai.RecommendationBundle
import com.example.data.local.entity.*
import com.example.data.preferences.SessionPreferences
import com.example.data.remote.InsforgeRemoteDataSource
import com.example.data.remote.MerchantStatusResponse
import com.example.data.repository.AnalyticsSummary
import com.example.data.repository.RegisterOutcome
import com.example.data.repository.StoreRepository
import com.example.i18n.AppLanguage
import com.example.util.BackupRestoreResult
import com.example.util.CsvImportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val labelEn: String, val labelBn: String) {
    POPULARITY("Popular", "জনপ্রিয়"),
    PRICE_LOW("Price: Low to High", "দাম: কম থেকে বেশি"),
    PRICE_HIGH("Price: High to Low", "দাম: বেশি থেকে কম"),
    NEWEST("Newest", "নতুন")
}

data class SearchFilterState(
    val query: String = "",
    val selectedCategoryId: String? = null,
    val selectedBrand: String? = null,
    val minPrice: Double = 0.0,
    val maxPrice: Double = 25000.0,
    val minRating: Double = 0.0,
    val inStockOnly: Boolean = false,
    val sortBy: SortOption = SortOption.POPULARITY,
    val visibleCount: Int = 6
)

data class CartDisplayItem(
    val cartItem: CartItemEntity,
    val product: ProductEntity
)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repo: StoreRepository,
    private val prefs: SessionPreferences,
    private val remote: InsforgeRemoteDataSource? = null,
    private val aiRepo: AiRecommendationRepository = InsforgeAiRecommendationRepository(remote)
) : ViewModel() {

    private val _merchantGatewayStatus = MutableStateFlow<MerchantStatusResponse?>(null)
    val merchantGatewayStatus: StateFlow<MerchantStatusResponse?> = _merchantGatewayStatus.asStateFlow()

    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail: StateFlow<String?> = _pendingVerificationEmail.asStateFlow()

    init {
        if (remote != null) {
            viewModelScope.launch {
                repo.syncStorefrontFromInsforge()
                val restored = repo.restoreRemoteSession()
                if (restored != null) {
                    prefs.setCurrentUserId(restored.id)
                } else {
                    val guestId = prefs.guestIdFlow.first()
                    repo.syncGuestOrders(guestId)
                }
                refreshMerchantGatewayStatus()
            }
        }
    }

    fun refreshMerchantGatewayStatus() {
        viewModelScope.launch {
            repo.fetchMerchantGatewayStatus().onSuccess { status ->
                _merchantGatewayStatus.value = status
            }
        }
    }

    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.EN)

    val currentUserId: StateFlow<String?> = prefs.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val effectiveSessionId: StateFlow<String> = combine(prefs.currentUserIdFlow, prefs.guestIdFlow) { uid, guest ->
        uid ?: guest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "guest_local_device")

    val currentUser: StateFlow<UserEntity?> = prefs.currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null) else repo.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<CategoryEntity>> = repo.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repo.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banners: StateFlow<List<BannerEntity>> = repo.banners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashSales: StateFlow<List<FlashSaleEntity>> = repo.flashSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<CouponEntity>> = repo.coupons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repo.reviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<WebsiteSettingsEntity> = repo.settings
        .map { it ?: WebsiteSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WebsiteSettingsEntity())

    val searchHistory: StateFlow<List<String>> = prefs.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewedIds: StateFlow<List<String>> = prefs.recentlyViewedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiConsent: StateFlow<Boolean> = prefs.aiConsentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationConsent: StateFlow<Boolean> = prefs.notificationConsentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cartItems: StateFlow<List<CartDisplayItem>> = combine(
        effectiveSessionId.flatMapLatest { repo.observeCart(it) },
        products
    ) { items, prods ->
        items.mapNotNull { ci -> prods.find { it.id == ci.productId }?.let { CartDisplayItem(ci, it) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistProductIds: StateFlow<Set<String>> = effectiveSessionId
        .flatMapLatest { repo.observeWishlist(it) }
        .map { list -> list.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val compareProducts: StateFlow<List<ProductEntity>> = combine(
        effectiveSessionId.flatMapLatest { repo.observeComparison(it) },
        products
    ) { cmpList, prods ->
        cmpList.mapNotNull { c -> prods.find { it.id == c.productId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userOrders: StateFlow<List<OrderEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeUserOrders(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addresses: StateFlow<List<AddressEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeAddresses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loyaltyTransactions: StateFlow<List<LoyaltyTransactionEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeLoyaltyTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referrals: StateFlow<List<ReferralRecordEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeReferrals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeNotifications(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSupportMessages: StateFlow<List<SupportMessageEntity>> = effectiveSessionId
        .flatMapLatest { repo.observeUserSupportMessages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live countdown ticker for Flash Sale
    val currentTimeMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // Search & Filter
    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(products, _filterState) { all, f ->
        val q = f.query.trim().lowercase()
        all.filter { p ->
            val matchesQuery = q.isBlank() ||
                p.titleEn.lowercase().contains(q) ||
                p.titleBn.lowercase().contains(q) ||
                p.brand.lowercase().contains(q) ||
                p.tagsCsv.lowercase().contains(q) ||
                p.sku.lowercase().contains(q)
            val matchesCat = f.selectedCategoryId == null || p.categoryId == f.selectedCategoryId
            val matchesBrand = f.selectedBrand == null || p.brand.equals(f.selectedBrand, ignoreCase = true)
            val matchesPrice = p.effectivePrice in f.minPrice..f.maxPrice
            val matchesRating = p.ratingAvg >= f.minRating
            val matchesStock = !f.inStockOnly || p.stockQty > 0
            matchesQuery && matchesCat && matchesBrand && matchesPrice && matchesRating && matchesStock
        }.let { list ->
            when (f.sortBy) {
                SortOption.POPULARITY -> list.sortedByDescending { it.soldCount }
                SortOption.PRICE_LOW -> list.sortedBy { it.effectivePrice }
                SortOption.PRICE_HIGH -> list.sortedByDescending { it.effectivePrice }
                SortOption.NEWEST -> list.sortedByDescending { it.createdAtMillis }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Recommendations State
    val recommendations: StateFlow<RecommendationBundle?> = combine(
        products,
        recentlyViewedIds,
        aiConsent
    ) { prods, recent, consent ->
        if (prods.isEmpty()) null
        else aiRepo.getHomeRecommendations(prods, recent, emptyList(), consent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Admin Flows (Gated by role == "ADMIN" in UI and Repository)
    val allUsers: StateFlow<List<UserEntity>> = repo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allOrders: StateFlow<List<OrderEntity>> = repo.observeAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allOrderItems: StateFlow<List<OrderItemEntity>> = repo.observeAllOrderItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPayments: StateFlow<List<PaymentRecordEntity>> = repo.observeAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSupportMessages: StateFlow<List<SupportMessageEntity>> = repo.observeAllSupportMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCartItems: StateFlow<List<CartItemEntity>> = repo.observeAllCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = repo.observeActivityLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val backupLogs: StateFlow<List<BackupLogEntity>> = repo.observeBackupLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<AnalyticsSummary> = combine(
        allOrders, allOrderItems, products, allUsers
    ) { orders, items, prods, users ->
        repo.computeAnalytics(orders, items, prods, users)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repo.computeAnalytics(emptyList(), emptyList(), emptyList(), emptyList())
    )

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun emitMessage(msg: String) {
        _toastMessage.tryEmit(msg)
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
            prefs.setLanguage(next)
        }
    }

    fun updateSearchQuery(q: String) {
        _filterState.update { it.copy(query = q, visibleCount = 6) }
    }

    fun submitSearch(q: String) {
        _filterState.update { it.copy(query = q, visibleCount = 6) }
        viewModelScope.launch { prefs.addSearchQuery(q) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun selectCategoryFilter(categoryId: String?) {
        _filterState.update { it.copy(selectedCategoryId = categoryId, visibleCount = 6) }
    }

    fun updateFilter(transform: (SearchFilterState) -> SearchFilterState) {
        _filterState.update(transform)
    }

    fun loadMoreProducts() {
        _filterState.update { it.copy(visibleCount = it.visibleCount + 6) }
    }

    fun recordProductView(productId: String) {
        viewModelScope.launch { prefs.recordProductView(productId) }
    }

    suspend fun getSimilarProducts(product: ProductEntity): List<ProductEntity> =
        aiRepo.getSimilarProducts(product, products.value)

    fun setAiConsent(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiConsent(enabled) }
    }

    fun setNotificationConsent(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationConsent(enabled) }
    }

    // Auth Actions
    fun loginCustomer(email: String, password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.login(email, password, requireAdmin = false).fold(
                onSuccess = {
                    prefs.setCurrentUserId(it.id)
                    emitMessage("Signed in as ${it.name}")
                    onSuccess()
                },
                onFailure = { emitMessage(it.message ?: "Login failed") }
            )
        }
    }

    fun loginAdmin(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.login(email, password, requireAdmin = true).fold(
                onSuccess = {
                    prefs.setCurrentUserId(it.id)
                    emitMessage("Owner Admin authenticated")
                    onSuccess()
                },
                onFailure = { emitMessage(it.message ?: "Admin login failed") }
            )
        }
    }

    fun registerCustomer(name: String, email: String, phone: String, password: String, referralCode: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.registerCustomerWithOutcome(name, email, phone, password, referralCode).fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is RegisterOutcome.Authenticated -> {
                            prefs.setCurrentUserId(outcome.user.id)
                            emitMessage("Welcome to Kena Kata, ${outcome.user.name}!")
                            onSuccess()
                        }
                        is RegisterOutcome.VerificationRequired -> {
                            _pendingVerificationEmail.value = outcome.email
                            emitMessage("6-digit verification code sent to ${outcome.email}")
                        }
                    }
                },
                onFailure = { emitMessage(it.message ?: "Registration failed") }
            )
        }
    }

    fun verifyEmailCode(email: String, code: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.verifyEmailWithCode(email, code).fold(
                onSuccess = { user ->
                    _pendingVerificationEmail.value = null
                    if (user != null) {
                        prefs.setCurrentUserId(user.id)
                    }
                    emitMessage("Email verified successfully!")
                    onSuccess()
                },
                onFailure = { emitMessage(it.message ?: "Verification failed") }
            )
        }
    }

    fun resendVerificationCode(email: String) {
        viewModelScope.launch {
            repo.resendVerificationEmail(email).fold(
                onSuccess = { emitMessage("Verification code sent to $email") },
                onFailure = { emitMessage(it.message ?: "Unable to send code") }
            )
        }
    }

    fun dismissVerificationPrompt() {
        _pendingVerificationEmail.value = null
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            prefs.setCurrentUserId(null)
            emitMessage("Signed out")
        }
    }

    fun verifyEmail() {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            val userEmail = currentUser.value?.email.orEmpty()
            repo.verifyCustomerEmail(uid).fold(
                onSuccess = {
                    if (remote != null && userEmail.isNotBlank()) {
                        _pendingVerificationEmail.value = userEmail
                        emitMessage("Verification code sent to $userEmail")
                    } else {
                        emitMessage("Email verified!")
                    }
                },
                onFailure = { emitMessage(it.message ?: "Unable to verify email") }
            )
        }
    }

    fun sendPasswordResetCode(email: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            repo.sendPasswordResetCode(email).fold(
                onSuccess = {
                    emitMessage("Reset code sent to $email")
                    onSent()
                },
                onFailure = { emitMessage(it.message ?: "Failed to send reset code") }
            )
        }
    }

    fun resetPasswordWithCode(email: String, code: String, newPassword: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.resetPasswordWithCode(email, code, newPassword).fold(
                onSuccess = {
                    emitMessage("Password updated. You can now log in.")
                    onSuccess()
                },
                onFailure = { emitMessage(it.message ?: "Reset failed") }
            )
        }
    }

    fun resetPassword(email: String, newPassword: String) {
        viewModelScope.launch {
            repo.resetPassword(email, newPassword).fold(
                onSuccess = { emitMessage("Password updated. You can now log in.") },
                onFailure = { emitMessage(it.message ?: "Reset failed") }
            )
        }
    }

    fun updateProfile(name: String, phone: String, avatarUri: String? = null) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            repo.updateProfile(uid, name, phone, avatarUri).fold(
                onSuccess = { emitMessage("Profile saved") },
                onFailure = { emitMessage(it.message ?: "Update failed") }
            )
        }
    }

    fun uploadAndSaveAvatar(name: String, phone: String, avatarUri: String) {
        updateProfile(name, phone, avatarUri)
    }

    fun saveAddress(label: String, fullName: String, phone: String, district: String, area: String, street: String) {
        viewModelScope.launch {
            repo.saveAddress(effectiveSessionId.value, label, fullName, phone, district, area, street)
            emitMessage("Address saved")
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch { repo.deleteAddress(id, effectiveSessionId.value) }
    }

    // Cart, Wishlist, Compare
    fun addToCart(product: ProductEntity, variant: String = product.variantsCsv.split(",").firstOrNull()?.trim().orEmpty().ifBlank { "Standard" }, qty: Int = 1) {
        viewModelScope.launch {
            repo.addToCart(effectiveSessionId.value, product, variant, qty).fold(
                onSuccess = { emitMessage("Added to cart") },
                onFailure = { emitMessage(it.message ?: "Unable to add") }
            )
        }
    }

    fun updateCartQty(item: CartItemEntity, qty: Int) {
        viewModelScope.launch {
            repo.updateCartQty(item, qty).onFailure { emitMessage(it.message ?: "Stock limit reached") }
        }
    }

    fun removeCartItem(id: String) {
        viewModelScope.launch { repo.removeCartItem(id, effectiveSessionId.value) }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch {
            val saved = productId in wishlistProductIds.value
            repo.toggleWishlist(effectiveSessionId.value, productId, saved)
            emitMessage(if (saved) "Removed from wishlist" else "Saved to wishlist")
        }
    }

    fun toggleCompare(productId: String) {
        viewModelScope.launch {
            val compared = compareProducts.value.any { it.id == productId }
            repo.toggleCompare(effectiveSessionId.value, productId, compared, compareProducts.value.size).fold(
                onSuccess = { emitMessage(if (compared) "Removed from comparison" else "Added to compare") },
                onFailure = { emitMessage(it.message ?: "Limit reached") }
            )
        }
    }

    suspend fun calculateDeliveryFee(district: String, subtotal: Double): Double =
        repo.calculateDeliveryFee(district, subtotal)

    suspend fun validateCoupon(code: String, subtotal: Double) =
        repo.validateCoupon(code, subtotal)

    fun placeOrder(
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
        notes: String,
        onPlaced: (OrderEntity) -> Unit
    ) {
        viewModelScope.launch {
            val isGuest = currentUser.value == null
            repo.placeOrder(
                activeUserId = effectiveSessionId.value,
                isGuest = isGuest,
                idempotencyKey = idempotencyKey,
                customerName = customerName,
                customerPhone = customerPhone,
                district = district,
                area = area,
                streetAddress = streetAddress,
                couponCode = couponCode,
                redeemLoyaltyPoints = redeemLoyaltyPoints,
                paymentMethod = paymentMethod,
                senderNumber = senderNumber,
                transactionId = transactionId,
                notes = notes
            ).fold(
                onSuccess = {
                    emitMessage("Order ${it.orderNumber} confirmed!")
                    onPlaced(it)
                },
                onFailure = { emitMessage(it.message ?: "Checkout failed") }
            )
        }
    }

    suspend fun getOrderItems(orderId: String): List<OrderItemEntity> {
        val guestSid = if (currentUser.value == null) effectiveSessionId.value else ""
        return repo.getOrderItems(orderId, guestSid)
    }

    fun cancelOrReturnOrder(order: OrderEntity, isReturn: Boolean) {
        viewModelScope.launch {
            repo.customerCancelOrReturnOrder(order, effectiveSessionId.value, isReturn).fold(
                onSuccess = { emitMessage(if (isReturn) "Return processed" else "Order cancelled") },
                onFailure = { emitMessage(it.message ?: "Action failed") }
            )
        }
    }

    fun addReview(productId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val user = currentUser.value
            repo.addReview(productId, effectiveSessionId.value, user?.name ?: "Verified Guest", rating, comment).fold(
                onSuccess = { emitMessage("Review published!") },
                onFailure = { emitMessage(it.message ?: "Unable to post review") }
            )
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch { repo.markNotificationsRead(effectiveSessionId.value) }
    }

    fun sendSupportMessage(name: String, email: String, subject: String, message: String) {
        viewModelScope.launch {
            repo.sendSupportMessage(effectiveSessionId.value, name, email, subject, message).fold(
                onSuccess = { emitMessage("Support message sent to Kena Kata team") },
                onFailure = { emitMessage(it.message ?: "Failed to send") }
            )
        }
    }

    fun subscribeNewsletter() {
        viewModelScope.launch {
            repo.subscribeNewsletter()
            emitMessage("Subscribed to Kena Kata updates!")
        }
    }

    // ============ ADMIN ACTIONS ============
    fun adminSaveProduct(product: ProductEntity, isNew: Boolean) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminSaveProduct(adminId, product, isNew).fold(
                onSuccess = { emitMessage(if (isNew) "Product created" else "Product updated") },
                onFailure = { emitMessage(it.message ?: "Failed to save product") }
            )
        }
    }

    fun adminDeleteProduct(product: ProductEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminDeleteProduct(adminId, product).fold(
                onSuccess = { emitMessage("Product deleted") },
                onFailure = { emitMessage(it.message ?: "Delete failed") }
            )
        }
    }

    fun adminSaveCategory(category: CategoryEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminSaveCategory(adminId, category).fold(
                onSuccess = { emitMessage("Category saved") },
                onFailure = { emitMessage(it.message ?: "Error saving category") }
            )
        }
    }

    fun adminSaveCoupon(coupon: CouponEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminSaveCoupon(adminId, coupon).fold(
                onSuccess = { emitMessage("Coupon ${coupon.code} saved") },
                onFailure = { emitMessage(it.message ?: "Error saving coupon") }
            )
        }
    }

    fun adminToggleFlashSale(sale: FlashSaleEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminToggleFlashSale(adminId, sale).fold(
                onSuccess = { emitMessage("Flash Sale updated") },
                onFailure = { emitMessage(it.message ?: "Failed") }
            )
        }
    }

    fun adminVerifyPayment(payment: PaymentRecordEntity, approve: Boolean, note: String) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminVerifyPayment(adminId, payment, approve, note).fold(
                onSuccess = { emitMessage(if (approve) "Payment verified!" else "Payment rejected") },
                onFailure = { emitMessage(it.message ?: "Verification error") }
            )
        }
    }

    fun adminMarkRefunded(payment: PaymentRecordEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminMarkRefunded(adminId, payment).fold(
                onSuccess = { emitMessage("Refund recorded & loyalty reversed") },
                onFailure = { emitMessage(it.message ?: "Refund error") }
            )
        }
    }

    fun adminUpdateOrderAndCourier(order: OrderEntity, status: String, courier: String, tracking: String) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminUpdateOrderAndCourier(adminId, order, status, courier, tracking).fold(
                onSuccess = { emitMessage("Order ${order.orderNumber} updated to $status") },
                onFailure = { emitMessage(it.message ?: "Update failed") }
            )
        }
    }

    fun adminReplySupport(msg: SupportMessageEntity, reply: String) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminReplySupport(adminId, msg, reply).fold(
                onSuccess = { emitMessage("Reply sent") },
                onFailure = { emitMessage(it.message ?: "Failed") }
            )
        }
    }

    fun adminDeleteReview(review: ReviewEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminDeleteReview(adminId, review).onSuccess { emitMessage("Review deleted") }
        }
    }

    fun adminSaveSettings(newSettings: WebsiteSettingsEntity) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminSaveSettings(adminId, newSettings).onSuccess { emitMessage("Store settings saved") }
        }
    }

    fun adminSendCartRecovery(userId: String) {
        val adminId = currentUserId.value ?: return
        viewModelScope.launch {
            repo.adminSendCartRecoveryReminder(adminId, userId).onSuccess { emitMessage("Recovery reminder sent") }
        }
    }

    suspend fun adminImportCsv(csvText: String): Result<CsvImportResult> {
        val adminId = currentUserId.value ?: return Result.failure(SecurityException("Admin login required"))
        return repo.adminImportCsvProducts(adminId, csvText)
    }

    suspend fun adminExportCsv(): Result<String> {
        val adminId = currentUserId.value ?: return Result.failure(SecurityException("Admin login required"))
        return repo.adminExportCsvProducts(adminId)
    }

    suspend fun adminCreateBackup(fileName: String): Result<String> {
        val adminId = currentUserId.value ?: return Result.failure(SecurityException("Admin login required"))
        return repo.adminCreateBackupJson(adminId, fileName)
    }

    suspend fun adminRestoreBackup(fileName: String, jsonText: String): Result<BackupRestoreResult> {
        val adminId = currentUserId.value ?: return Result.failure(SecurityException("Admin login required"))
        return repo.adminRestoreBackupJson(adminId, fileName, jsonText)
    }
}

class StoreViewModelFactory(
    private val repo: StoreRepository,
    private val prefs: SessionPreferences,
    private val remote: InsforgeRemoteDataSource? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StoreViewModel(repo, prefs, remote) as T
    }
}
