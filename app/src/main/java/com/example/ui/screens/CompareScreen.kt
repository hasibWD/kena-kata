package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.ProductImageBox
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    vm: StoreViewModel,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val compareList by vm.compareProducts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${Strings.compare(lang)} (${compareList.size}/4)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("compare_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (compareList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(SoftGrayBg).padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(56.dp))
                    Text(
                        Strings.tr(lang, "No products selected for comparison.", "তুলনা করার জন্য কোনো পণ্য নির্বাচন করা হয়নি।"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        Strings.tr(lang, "Tap the compare icon on any product card to compare specs and prices side by side.", "দাম ও বৈশিষ্ট্য তুলনা করতে যেকোনো পণ্যের তুলনা আইকনে ট্যাপ করুন।"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftGrayBg)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(hScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    compareList.forEach { p ->
                        val title = if (lang == AppLanguage.BN) p.titleBn else p.titleEn
                        Card(
                            modifier = Modifier.width(196.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp))) {
                                    ProductImageBox(p.imageResKey, p.imageUri, title, Modifier.fillMaxSize())
                                    IconButton(
                                        onClick = { vm.toggleCompare(p.id) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(999.dp), color = PureWhite) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.padding(4.dp).size(16.dp))
                                        }
                                    }
                                }
                                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(Strings.formatBdt(p.effectivePrice, lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                                HorizontalDivider()
                                CompareRow("Brand", p.brand)
                                CompareRow("Savings", if (p.discountPercent > 0) "-${p.discountPercent}%" else "None")
                                CompareRow("Stock", if (p.stockQty > 0) "${p.stockQty} left" else "Out of Stock")
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                    Text("${String.format("%.1f", p.ratingAvg)} (${p.reviewCount})", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                                val specs = p.specsPipe.split(";;").take(4)
                                specs.forEach { line ->
                                    Text("• $line", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { vm.addToCart(p) },
                                    enabled = p.stockQty > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                                ) {
                                    Text(Strings.addToCart(lang), style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { onOpenProduct(p.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(Strings.tr(lang, "View Details", "বিস্তারিত"), color = VibrantOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
