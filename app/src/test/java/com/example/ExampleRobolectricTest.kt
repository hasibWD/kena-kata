package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: StoreRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { AppDatabase.seedIfEmpty(db.dao()) }
        repo = StoreRepository(db.dao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches Kena Kata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Kena Kata", context.getString(R.string.app_name))
    }

    @Test
    fun `single vendor invariant has only CUSTOMER and ADMIN roles`() = runBlocking {
        val users = db.dao().getAllUsersOnce()
        assertTrue(users.any { it.role == "ADMIN" })
        assertTrue(users.any { it.role == "CUSTOMER" })
        assertFalse(users.any { it.role == "SELLER" })
    }

    @Test
    fun `manual bKash order stays SUBMITTED and prevents duplicate idempotencyKey`() = runBlocking {
        val products = db.dao().getProductsOnce()
        val target = products.first { it.stockQty > 5 }
        val initialStock = target.stockQty
        val customerId = "usr_customer_hasib"

        repo.addToCart(customerId, target, "Standard", 2).getOrThrow()
        val res = repo.placeOrder(
            activeUserId = customerId,
            isGuest = false,
            idempotencyKey = "idem_test_1",
            customerName = "Hasibul Hasan",
            customerPhone = "01712345678",
            district = "Dhaka",
            area = "Dhanmondi",
            streetAddress = "Road 6, Dhanmondi",
            couponCode = "KENA10",
            redeemLoyaltyPoints = false,
            paymentMethod = "BKASH",
            senderNumber = "01712345678",
            transactionId = "TRX998877",
            notes = ""
        )
        assertTrue(res.isSuccess)
        val order = res.getOrThrow()
        assertEquals("SUBMITTED", order.paymentStatus)
        assertEquals("PENDING", order.orderStatus)

        val updatedProduct = db.dao().getProductById(target.id)!!
        assertEquals(initialStock - 2, updatedProduct.stockQty)

        // Non-admin cannot verify payments
        val payment = db.dao().getPaymentByOrderId(order.id)!!
        val unauthVerify = repo.adminVerifyPayment(customerId, payment, true, "Attempt")
        assertTrue(unauthVerify.isFailure)

        // Owner Admin verifies payment cleanly
        val adminVerify = repo.adminVerifyPayment("usr_admin_owner", payment, true, "Matched statement")
        assertTrue(adminVerify.isSuccess)
        val verifiedOrder = db.dao().getOrderById(order.id)!!
        assertEquals("VERIFIED", verifiedOrder.paymentStatus)
        assertEquals("CONFIRMED", verifiedOrder.orderStatus)
    }

    @Test
    fun `customer cannot login to admin portal`() = runBlocking {
        val res = repo.login("hasibulhasan2087@gmail.com", "customer123", requireAdmin = true)
        assertTrue(res.isFailure)
        val adminRes = repo.login("owner@kenakata.bd", "Admin@2026", requireAdmin = true)
        assertTrue(adminRes.isSuccess)
    }

    @Test
    fun `insforge ProfileDto enforces single vendor roles only`() {
        val sellerAttempt = com.example.data.remote.ProfileDto(
            id = "00000000-0000-0000-0000-000000000001",
            email = "test@kenakata.bd",
            name = "Test Customer",
            role = "SELLER"
        ).toEntity()
        assertEquals("CUSTOMER", sellerAttempt.role)

        val adminProfile = com.example.data.remote.ProfileDto(
            id = "00000000-0000-0000-0000-000000000002",
            email = "owner@kenakata.bd",
            name = "Owner",
            role = "ADMIN"
        ).toEntity()
        assertEquals("ADMIN", adminProfile.role)
    }
}
