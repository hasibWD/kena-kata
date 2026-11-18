package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.ProductCard
import com.example.ui.state.SortOption
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSearchScreen(
    vm: StoreViewModel,
    onOpenProduct: (String) -> Unit,
    onOpenCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by vm.language.collectAsStateWithLifecycle()
    val filter by vm.filterState.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allProducts by vm.products.collectAsStateWithLifecycle()
    val filteredProducts by vm.filteredProducts.collectAsStateWithLifecycle()
    val searchHistory by vm.searchHistory.collectAsStateWithLifecycle()
    val wishlistIds by vm.wishlistProductIds.collectAsStateWithLifecycle()
    val compareList by vm.compareProducts.collectAsStateWithLifecycle()

    var showFiltersSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val brands = remember(allProducts) { allProducts.map { it.brand }.distinct().sorted() }
    val suggestions = remember(filter.query, allProducts, lang) {
        val q = filter.query.trim().lowercase()
        if (q.length < 2) emptyList()
        else allProducts.filter {
            it.titleEn.lowercase().contains(q) || it.titleBn.lowercase().contains(q) || it.brand.lowercase().contains(q)
        }.take(4)
    }

    val pagedProducts = remember(filteredProducts, filter.visibleCount) {
        filteredProducts.take(filter.visibleCount)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftGrayBg)
    ) {
        // Search Header
        Surface(color = PureWhite, shadowElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { vm.updateSearchQuery(it) },
                        placeholder = { Text(Strings.searchHint(lang)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DeepBlue) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) {
                                IconButton(onClick = { vm.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.submitSearch(filter.query) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("catalog_search_input")
                    )
                    if (compareList.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = onOpenCompare,
                            modifier = Modifier.testTag("catalog_compare_shortcut")
                        ) {
                            BadgedBox(badge = { Badge(containerColor = VibrantOrange) { Text("${compareList.size}") } }) {
                                Icon(Icons.Filled.CompareArrows, contentDescription = "Compare", tint = DeepBlue)
                            }
                        }
                    }
                }

                // Autocomplete live suggestions
                if (suggestions.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            suggestions.forEach { s ->
                                val label = if (lang == AppLanguage.BN) s.titleBn else s.titleEn
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.submitSearch(label)
                                            onOpenProduct(s.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(Strings.formatBdt(s.effectivePrice, lang), style = MaterialTheme.typography.labelMedium, color = DeepBlue)
                                }
                            }
                        }
                    }
                }

                // Search History chips
                if (searchHistory.isNotEmpty() && filter.query.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchHistory) { hist ->
                                AssistChip(
                                    onClick = { vm.submitSearch(hist) },
                                    label = { Text(hist, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                        TextButton(onClick = { vm.clearSearchHistory() }) {
                            Text(Strings.tr(lang, "Clear", "মুছুন"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.selectedCategoryId == null,
                            onClick = { vm.selectCategoryFilter(null) },
                            label = { Text(Strings.tr(lang, "All", "সব")) }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = filter.selectedCategoryId == cat.id,
                            onClick = {
                                vm.selectCategoryFilter(if (filter.selectedCategoryId == cat.id) null else cat.id)
                            },
                            label = { Text(if (lang == AppLanguage.BN) cat.nameBn else cat.nameEn) }
                        )
                    }
                }

                // Filter + Sort Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showFiltersSheet = true },
                            modifier = Modifier.heightIn(min = 40.dp).testTag("open_filters_button")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.tr(lang, "Filters", "ফিল্টার"))
                        }
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.heightIn(min = 40.dp).testTag("open_sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (lang == AppLanguage.BN) filter.sortBy.labelBn else filter.sortBy.labelEn)
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(if (lang == AppLanguage.BN) option.labelBn else option.labelEn) },
                                        onClick = {
                                            vm.updateFilter { it.copy(sortBy = option) }
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${filteredProducts.size} ${Strings.tr(lang, "items", "টি পণ্য")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Paginated Grid
        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Text(Strings.tr(lang, "No products matched your filters.", "কোনো পণ্য পাওয়া যায়নি।"), style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { vm.updateFilter { com.example.ui.state.SearchFilterState() } },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                    ) {
                        Text(Strings.tr(lang, "Reset Filters", "ফিল্টার রিসেট করুন"))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pagedProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        language = lang,
                        isWishlisted = product.id in wishlistIds,
                        isCompared = compareList.any { it.id == product.id },
                        onClick = { onOpenProduct(product.id) },
                        onAddToCart = { vm.addToCart(product) },
                        onToggleWishlist = { vm.toggleWishlist(product.id) },
                        onToggleCompare = { vm.toggleCompare(product.id) }
                    )
                }
                if (pagedProducts.size < filteredProducts.size) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedButton(
                            onClick = { vm.loadMoreProducts() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .heightIn(min = 48.dp)
                                .testTag("load_more_products_button")
                        ) {
                            Text(Strings.tr(lang, "Load More (${filteredProducts.size - pagedProducts.size} remaining)", "আরও দেখুন"))
                        }
                    }
                }
            }
        }
    }

    if (showFiltersSheet) {
        ModalBottomSheet(onDismissRequest = { showFiltersSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(Strings.tr(lang, "Filter Products", "পণ্য ফিল্টার করুন"), style = MaterialTheme.typography.titleLarge)

                // Brand Filter
                Text(Strings.tr(lang, "Brand", "ব্র্যান্ড"), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.selectedBrand == null,
                            onClick = { vm.updateFilter { it.copy(selectedBrand = null) } },
                            label = { Text(Strings.tr(lang, "All Brands", "সব ব্র্যান্ড")) }
                        )
                    }
                    items(brands) { b ->
                        FilterChip(
                            selected = filter.selectedBrand == b,
                            onClick = { vm.updateFilter { it.copy(selectedBrand = if (it.selectedBrand == b) null else b) } },
                            label = { Text(b) }
                        )
                    }
                }

                // Price Ceiling Slider
                Text(
                    text = "${Strings.tr(lang, "Max Price", "সর্বোচ্চ দাম")}: ${Strings.formatBdt(filter.maxPrice, lang)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = filter.maxPrice.toFloat(),
                    onValueChange = { v -> vm.updateFilter { it.copy(maxPrice = v.toDouble()) } },
                    valueRange = 300f..15000f
                )

                // Minimum Rating
                Text(Strings.tr(lang, "Minimum Rating", "সর্বনিম্ন রেটিং"), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0, 4.0, 4.5, 4.8).forEach { r ->
                        FilterChip(
                            selected = filter.minRating == r,
                            onClick = { vm.updateFilter { it.copy(minRating = r) } },
                            label = { Text(if (r == 0.0) "Any" else "$r★+") }
                        )
                    }
                }

                // In Stock Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.tr(lang, "In Stock Only", "শুধুমাত্র স্টকে থাকা পণ্য"), style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = filter.inStockOnly,
                        onCheckedChange = { checked -> vm.updateFilter { it.copy(inStockOnly = checked) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            vm.updateFilter { com.example.ui.state.SearchFilterState() }
                            showFiltersSheet = false
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(Strings.tr(lang, "Reset", "রিসেট"))
                    }
                    Button(
                        onClick = { showFiltersSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(Strings.tr(lang, "Apply", "প্রয়োগ করুন"))
                    }
                }
            }
        }
    }
}
