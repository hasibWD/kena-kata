package com.example.util

import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject

data class CsvImportResult(
    val importedProducts: List<ProductEntity>,
    val errors: List<String>
)

data class BackupRestoreResult(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val coupons: List<CouponEntity>,
    val settings: WebsiteSettingsEntity?,
    val recordCount: Int
)

object CsvAndBackupUtils {
    private val CSV_HEADER = "sku,titleEn,titleBn,brand,categoryId,regularPrice,salePrice,costPrice,stockQty,lowStockThreshold,variantsCsv,descriptionEn"

    fun exportProductsToCsv(products: List<ProductEntity>): String {
        val sb = StringBuilder().appendLine(CSV_HEADER)
        for (p in products) {
            sb.appendLine(
                listOf(
                    escapeCsv(p.sku),
                    escapeCsv(p.titleEn),
                    escapeCsv(p.titleBn),
                    escapeCsv(p.brand),
                    escapeCsv(p.categoryId),
                    p.regularPrice.toString(),
                    p.salePrice.toString(),
                    p.costPrice.toString(),
                    p.stockQty.toString(),
                    p.lowStockThreshold.toString(),
                    escapeCsv(p.variantsCsv),
                    escapeCsv(p.descriptionEn)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun parseProductsFromCsv(csvText: String, existingCategoryIds: Set<String>): CsvImportResult {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvImportResult(emptyList(), listOf("CSV file is empty."))
        val imported = mutableListOf<ProductEntity>()
        val errors = mutableListOf<String>()
        val startIndex = if (lines.first().lowercase().startsWith("sku,")) 1 else 0

        for (i in startIndex until lines.size) {
            val rowNum = i + 1
            val cols = parseCsvLine(lines[i])
            if (cols.size < 9) {
                errors.add("Row $rowNum: Expected at least 9 columns, found ${cols.size}.")
                continue
            }
            val sku = cols[0].trim()
            val titleEn = cols[1].trim()
            val titleBn = cols[2].trim().ifBlank { titleEn }
            val brand = cols[3].trim().ifBlank { "Kena Kata" }
            val categoryId = cols[4].trim()
            val regularPrice = cols[5].toDoubleOrNull()
            val salePrice = cols[6].toDoubleOrNull() ?: 0.0
            val costPrice = cols[7].toDoubleOrNull() ?: 0.0
            val stockQty = cols[8].toIntOrNull()
            val lowThreshold = cols.getOrNull(9)?.toIntOrNull() ?: 5
            val variants = cols.getOrNull(10)?.trim().orEmpty().ifBlank { "Standard" }
            val desc = cols.getOrNull(11)?.trim().orEmpty().ifBlank { titleEn }

            when {
                sku.isBlank() -> errors.add("Row $rowNum: SKU cannot be blank.")
                titleEn.length < 2 -> errors.add("Row $rowNum ($sku): Title too short.")
                categoryId !in existingCategoryIds -> errors.add("Row $rowNum ($sku): Unknown categoryId '$categoryId'.")
                regularPrice == null || regularPrice <= 0 -> errors.add("Row $rowNum ($sku): Invalid regularPrice.")
                stockQty == null || stockQty < 0 -> errors.add("Row $rowNum ($sku): Invalid stockQty.")
                else -> {
                    val safeSale = if (salePrice > 0 && salePrice < regularPrice) salePrice else regularPrice
                    imported.add(
                        ProductEntity(
                            id = "prod_${sku.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
                            sku = sku,
                            titleEn = titleEn,
                            titleBn = titleBn,
                            brand = brand,
                            categoryId = categoryId,
                            descriptionEn = desc,
                            descriptionBn = desc,
                            regularPrice = regularPrice,
                            salePrice = safeSale,
                            costPrice = costPrice,
                            stockQty = stockQty,
                            lowStockThreshold = lowThreshold,
                            variantsCsv = variants,
                            isNewArrival = true,
                            imageResKey = when (categoryId) {
                                "cat_books" -> "hero_rokomari_reads"
                                "cat_fashion" -> "fashion_craft"
                                else -> "electronics"
                            }
                        )
                    )
                }
            }
        }
        return CsvImportResult(imported, errors)
    }

    fun exportDatabaseBackupJson(
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        coupons: List<CouponEntity>,
        orders: List<OrderEntity>,
        settings: WebsiteSettingsEntity?
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("platform", "Kena Kata Single-Vendor Store")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val catArr = JSONArray()
        categories.forEach { c ->
            catArr.put(JSONObject().apply {
                put("id", c.id); put("nameEn", c.nameEn); put("nameBn", c.nameBn)
                put("slug", c.slug); put("iconKey", c.iconKey); put("sortOrder", c.sortOrder)
                put("isFeatured", c.isFeatured)
            })
        }
        root.put("categories", catArr)

        val prodArr = JSONArray()
        products.forEach { p ->
            prodArr.put(JSONObject().apply {
                put("id", p.id); put("sku", p.sku); put("titleEn", p.titleEn); put("titleBn", p.titleBn)
                put("brand", p.brand); put("categoryId", p.categoryId)
                put("descriptionEn", p.descriptionEn); put("descriptionBn", p.descriptionBn)
                put("regularPrice", p.regularPrice); put("salePrice", p.salePrice); put("costPrice", p.costPrice)
                put("stockQty", p.stockQty); put("lowStockThreshold", p.lowStockThreshold)
                put("variantsCsv", p.variantsCsv); put("imageResKey", p.imageResKey)
            })
        }
        root.put("products", prodArr)

        val cpnArr = JSONArray()
        coupons.forEach { c ->
            cpnArr.put(JSONObject().apply {
                put("id", c.id); put("code", c.code); put("discountType", c.discountType)
                put("discountValue", c.discountValue); put("minOrderAmount", c.minOrderAmount)
                put("maxDiscount", c.maxDiscount); put("isActive", c.isActive)
                put("expiresAtMillis", c.expiresAtMillis); put("usageCount", c.usageCount)
            })
        }
        root.put("coupons", cpnArr)
        root.put("ordersCount", orders.size)

        settings?.let { s ->
            root.put("settings", JSONObject().apply {
                put("storeName", s.storeName)
                put("announcementEn", s.announcementEn)
                put("announcementBn", s.announcementBn)
                put("bkashMerchantNumber", s.bkashMerchantNumber)
                put("nagadMerchantNumber", s.nagadMerchantNumber)
                put("whatsappNumber", s.whatsappNumber)
                put("dhakaDeliveryFee", s.dhakaDeliveryFee)
                put("outsideDhakaDeliveryFee", s.outsideDhakaDeliveryFee)
            })
        }
        return root.toString(2)
    }

    fun parseBackupJson(jsonText: String): Result<BackupRestoreResult> {
        return try {
            val root = JSONObject(jsonText)
            if (!root.has("schemaVersion") || !root.has("products") || !root.has("categories")) {
                return Result.failure(IllegalArgumentException("Invalid Kena Kata backup schema."))
            }
            val categories = mutableListOf<CategoryEntity>()
            val catArr = root.getJSONArray("categories")
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.getString("id"),
                        nameEn = o.getString("nameEn"),
                        nameBn = o.optString("nameBn", o.getString("nameEn")),
                        slug = o.optString("slug", o.getString("id")),
                        iconKey = o.optString("iconKey", "devices"),
                        sortOrder = o.optInt("sortOrder", i + 1),
                        isFeatured = o.optBoolean("isFeatured", true)
                    )
                )
            }
            val products = mutableListOf<ProductEntity>()
            val prodArr = root.getJSONArray("products")
            for (i in 0 until prodArr.length()) {
                val o = prodArr.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = o.getString("id"),
                        sku = o.getString("sku"),
                        titleEn = o.getString("titleEn"),
                        titleBn = o.optString("titleBn", o.getString("titleEn")),
                        brand = o.optString("brand", "Kena Kata"),
                        categoryId = o.getString("categoryId"),
                        descriptionEn = o.optString("descriptionEn", ""),
                        descriptionBn = o.optString("descriptionBn", ""),
                        regularPrice = o.getDouble("regularPrice"),
                        salePrice = o.optDouble("salePrice", o.getDouble("regularPrice")),
                        costPrice = o.optDouble("costPrice", 0.0),
                        stockQty = o.getInt("stockQty"),
                        lowStockThreshold = o.optInt("lowStockThreshold", 5),
                        variantsCsv = o.optString("variantsCsv", "Standard"),
                        imageResKey = o.optString("imageResKey", "electronics")
                    )
                )
            }
            val coupons = mutableListOf<CouponEntity>()
            val cpnArr = root.optJSONArray("coupons") ?: JSONArray()
            for (i in 0 until cpnArr.length()) {
                val o = cpnArr.getJSONObject(i)
                coupons.add(
                    CouponEntity(
                        id = o.getString("id"),
                        code = o.getString("code"),
                        discountType = o.getString("discountType"),
                        discountValue = o.getDouble("discountValue"),
                        minOrderAmount = o.optDouble("minOrderAmount", 0.0),
                        maxDiscount = o.optDouble("maxDiscount", 500.0),
                        isActive = o.optBoolean("isActive", true),
                        expiresAtMillis = o.optLong("expiresAtMillis", System.currentTimeMillis() + 86400_000L * 30),
                        usageCount = o.optInt("usageCount", 0)
                    )
                )
            }
            Result.success(
                BackupRestoreResult(
                    categories = categories,
                    products = products,
                    coupons = coupons,
                    settings = null,
                    recordCount = categories.size + products.size + coupons.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else clean
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
