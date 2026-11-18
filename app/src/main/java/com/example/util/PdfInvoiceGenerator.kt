package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {
    fun writeInvoicePdf(
        context: Context,
        uri: Uri,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        storeEmail: String
    ): Result<Unit> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.rgb(11, 43, 107) // Deep Blue
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

            val whiteTitle = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                isFakeBoldText = true
            }
            val whiteSub = Paint().apply {
                color = Color.rgb(255, 237, 213)
                textSize = 12f
            }
            canvas.drawText("KENA KATA", 36f, 48f, whiteTitle)
            canvas.drawText("Shop Smart, Buy Better | Official Single-Vendor Store (BD)", 36f, 70f, whiteSub)
            canvas.drawText("Support: $storeEmail", 36f, 90f, whiteSub)

            val invoiceTag = Paint().apply {
                color = Color.rgb(249, 115, 22)
                textSize = 18f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("INVOICE", 559f, 50f, invoiceTag)
            val whiteRight = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("#${order.orderNumber}", 559f, 70f, whiteRight)

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(order.createdAtMillis))
            canvas.drawText(dateStr, 559f, 90f, whiteRight)

            val bodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
            }
            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
                isFakeBoldText = true
            }
            val mutedPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 11f
            }

            var y = 145f
            canvas.drawText("Billed & Shipped To:", 36f, y, boldPaint)
            canvas.drawText("Payment & Order Status:", 330f, y, boldPaint)
            y += 18f
            canvas.drawText(order.customerName, 36f, y, bodyPaint)
            canvas.drawText("Method: ${order.paymentMethod}", 330f, y, bodyPaint)
            y += 16f
            canvas.drawText("Phone: ${order.customerPhone}", 36f, y, bodyPaint)
            canvas.drawText("Payment: ${order.paymentStatus}", 330f, y, bodyPaint)
            y += 16f
            canvas.drawText("${order.area}, ${order.district}", 36f, y, bodyPaint)
            canvas.drawText("Order: ${order.orderStatus}", 330f, y, bodyPaint)
            y += 16f
            canvas.drawText(order.shippingAddress.take(48), 36f, y, mutedPaint)
            if (order.trackingNumber.isNotBlank()) {
                canvas.drawText("Courier: ${order.courierName} (${order.trackingNumber})", 330f, y, mutedPaint)
            }

            y += 32f
            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }
            canvas.drawLine(36f, y, 559f, y, linePaint)
            y += 20f
            canvas.drawText("Item", 36f, y, boldPaint)
            canvas.drawText("Qty", 355f, y, boldPaint)
            canvas.drawText("Unit (BDT)", 405f, y, boldPaint)
            canvas.drawText("Total (BDT)", 490f, y, boldPaint)
            y += 10f
            canvas.drawLine(36f, y, 559f, y, linePaint)

            y += 20f
            for (item in items) {
                val title = "${item.productTitleEn.take(38)} (${item.variantLabel})"
                canvas.drawText(title, 36f, y, bodyPaint)
                canvas.drawText("${item.quantity}", 360f, y, bodyPaint)
                canvas.drawText("৳${item.unitPrice.toInt()}", 410f, y, bodyPaint)
                canvas.drawText("৳${(item.unitPrice * item.quantity).toInt()}", 495f, y, bodyPaint)
                y += 20f
                if (y > 720f) break
            }

            y += 12f
            canvas.drawLine(330f, y, 559f, y, linePaint)
            y += 20f
            canvas.drawText("Subtotal:", 350f, y, bodyPaint)
            canvas.drawText("৳${order.subtotal.toInt()}", 495f, y, bodyPaint)
            y += 18f
            canvas.drawText("Delivery (${order.district}):", 350f, y, bodyPaint)
            canvas.drawText("৳${order.deliveryCharge.toInt()}", 495f, y, bodyPaint)
            if (order.discountAmount > 0) {
                y += 18f
                canvas.drawText("Coupon (${order.couponCode}):", 350f, y, bodyPaint)
                canvas.drawText("-৳${order.discountAmount.toInt()}", 495f, y, bodyPaint)
            }
            if (order.loyaltyDiscount > 0) {
                y += 18f
                canvas.drawText("Loyalty Redeemed:", 350f, y, bodyPaint)
                canvas.drawText("-৳${order.loyaltyDiscount.toInt()}", 495f, y, bodyPaint)
            }
            y += 14f
            canvas.drawLine(330f, y, 559f, y, linePaint)
            y += 22f
            val totalPaint = Paint().apply {
                color = Color.rgb(11, 43, 107)
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText("Total Payable:", 350f, y, totalPaint)
            canvas.drawText("৳${order.totalAmount.toInt()}", 495f, y, totalPaint)

            canvas.drawText("Thank you for shopping with Kena Kata Bangladesh!", 36f, 805f, mutedPaint)

            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            } ?: error("Unable to open output stream for PDF invoice.")
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
