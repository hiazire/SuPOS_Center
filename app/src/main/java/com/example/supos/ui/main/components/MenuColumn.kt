package com.example.supos.ui.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supos.data.local.entity.MenuItemEntity
import kotlin.math.roundToInt

import com.example.supos.data.model.CartItem
import com.example.supos.data.local.entity.OptionItemEntity

// ─────────────────────────────────────────────
// iPad-style 三層點餐介面 + 底部客製化選項面板
//  上層: 分類按鍵格 (可左右滑動)
//  中層: 該分類的品項格 (可左右滑動)
//  下中: 翻頁控制 + 頁碼
//  底層: 客製化加料與選項面板
// ─────────────────────────────────────────────
@Composable
fun MenuColumn(
    allItems: List<MenuItemEntity>,
    selectedCategory: String?,
    categoryPage: Int,
    itemPage: Int,
    selectedCartItem: CartItem?,
    allOptions: List<OptionItemEntity>,
    onCategorySelect: (String) -> Unit,
    onCategoryPageChange: (Int) -> Unit,
    onItemPageChange: (Int) -> Unit,
    onItemClick: (MenuItemEntity) -> Unit,
    onOptionClick: (OptionItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = allItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    val filteredItems = if (selectedCategory != null) {
        allItems.filter { it.category == selectedCategory }
    } else emptyList()

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8F8F8))) {
        // ── 上層：分類格 (佔 22%) ──
        CategoryGrid(
            categories = categories,
            selectedCategory = selectedCategory,
            currentPage = categoryPage,
            onPageChange = onCategoryPageChange,
            onCategorySelect = onCategorySelect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.22f)
        )

        HorizontalDivider(color = Color(0xFFDDDDDD))

        // ── 中層：品項格 (佔 38%) ──
        ItemGrid(
            items = filteredItems,
            currentPage = itemPage,
            onPageChange = onItemPageChange,
            onItemClick = onItemClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
        )

        HorizontalDivider(color = Color(0xFFDDDDDD))

        // ── 底部：翻頁按鈕 (佔 12%) ──
        PageNavigationBar(
            items = filteredItems,
            currentPage = itemPage,
            itemsPerPage = ITEMS_PER_PAGE,
            onPrevPage = { if (itemPage > 0) onItemPageChange(itemPage - 1) },
            onNextPage = {
                val totalPages = calcTotalPages(filteredItems.size, ITEMS_PER_PAGE)
                if (itemPage < totalPages - 1) onItemPageChange(itemPage + 1)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.12f)
        )

        HorizontalDivider(color = Color(0xFFDDDDDD))

        // ── 最底層：客製化選項面板 (佔 28%) ──
        OptionsBottomPanel(
            selectedCartItem = selectedCartItem,
            allOptions = allOptions,
            onOptionClick = onOptionClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f)
        )
    }
}

private const val CATEGORIES_PER_PAGE = 10 // 5欄 × 2列
private const val ITEMS_PER_PAGE = 15       // 5欄 × 3列

private fun calcTotalPages(count: Int, perPage: Int) = maxOf(1, Math.ceil(count.toDouble() / perPage).toInt())

// ─── 分類格 ───────────────────────────────────
@Composable
private fun CategoryGrid(
    categories: List<String>,
    selectedCategory: String?,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = calcTotalPages(categories.size, CATEGORIES_PER_PAGE)
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .background(Color(0xFFF0F0F0))
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        // 分頁容器（虛擬水平捲動）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(totalPages, currentPage) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragAccumulator < -80f && currentPage < totalPages - 1 -> onPageChange(currentPage + 1)
                                dragAccumulator > 80f && currentPage > 0 -> onPageChange(currentPage - 1)
                            }
                            dragAccumulator = 0f
                        },
                        onHorizontalDrag = { _, delta -> dragAccumulator += delta }
                    )
                }
        ) {
            val offsetX by animateFloatAsState(
                targetValue = -currentPage * containerWidthPx,
                animationSpec = tween(250),
                label = "categoryOffset"
            )

            Row(modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }) {
                repeat(totalPages) { pageIdx ->
                    val start = pageIdx * CATEGORIES_PER_PAGE
                    val end = minOf(start + CATEGORIES_PER_PAGE, categories.size)
                    val pageCategories = categories.subList(start, end)

                    Box(
                        modifier = Modifier
                            .width(with(density) { containerWidthPx.toDp() })
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pageCategories) { cat ->
                                CategoryButton(
                                    label = cat,
                                    isSelected = cat == selectedCategory,
                                    onClick = { onCategorySelect(cat) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 小圓點頁碼
        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (idx == currentPage) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (idx == currentPage) Color(0xFF1976D2) else Color(0xFFCCCCCC))
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isSelected) {
        Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFBBDEFB), Color(0xFF90CAF9)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (isSelected) 4.dp else 1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(brush = bgBrush)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF0D47A1),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── 品項格 ───────────────────────────────────
@Composable
private fun ItemGrid(
    items: List<MenuItemEntity>,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onItemClick: (MenuItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = calcTotalPages(items.size, ITEMS_PER_PAGE)
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color(0xFFFAFAFA))
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(0.dp))
            .pointerInput(totalPages, currentPage) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragAccumulator < -80f && currentPage < totalPages - 1 -> onPageChange(currentPage + 1)
                            dragAccumulator > 80f && currentPage > 0 -> onPageChange(currentPage - 1)
                        }
                        dragAccumulator = 0f
                    },
                    onHorizontalDrag = { _, delta -> dragAccumulator += delta }
                )
            }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "請先選擇分類",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val offsetX by animateFloatAsState(
                targetValue = -currentPage * containerWidthPx,
                animationSpec = tween(250),
                label = "itemOffset"
            )

            Row(modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }) {
                repeat(totalPages) { pageIdx ->
                    val start = pageIdx * ITEMS_PER_PAGE
                    val end = minOf(start + ITEMS_PER_PAGE, items.size)
                    val pageItems = items.subList(start, end)

                    Box(
                        modifier = Modifier
                            .width(with(density) { containerWidthPx.toDp() })
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pageItems) { item ->
                                ItemButton(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemButton(
    item: MenuItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF212121)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$${item.price}",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── 底部翻頁控制欄 ────────────────────────────
@Composable
private fun PageNavigationBar(
    items: List<MenuItemEntity>,
    currentPage: Int,
    itemsPerPage: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = calcTotalPages(items.size, itemsPerPage)
    val canPrev = currentPage > 0
    val canNext = currentPage < totalPages - 1

    Row(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (canPrev) Color(0xFFEEEEEE) else Color.Transparent)
                .clickable(enabled = canPrev, onClick = onPrevPage)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "◀◀",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (canPrev) Color(0xFF424242) else Color(0xFFCCCCCC)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (items.isNotEmpty()) {
                Text(
                    text = "第 ${currentPage + 1} 頁 / 共 $totalPages 頁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575)
                )
            }
            // 小圓點
            if (totalPages > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(totalPages) { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == currentPage) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (idx == currentPage) Color(0xFF1976D2) else Color(0xFFCCCCCC))
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (canNext) Color(0xFFEEEEEE) else Color.Transparent)
                .clickable(enabled = canNext, onClick = onNextPage)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "▶▶",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (canNext) Color(0xFF424242) else Color(0xFFCCCCCC)
            )
        }
    }
}

@Composable
fun OptionsBottomPanel(
    selectedCartItem: CartItem?,
    allOptions: List<OptionItemEntity>,
    onOptionClick: (OptionItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedCartItem == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFEFEFEF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "等待選擇餐點...",
                color = Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val availableGroups = selectedCartItem.menuItem.optionsGroup
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() } ?: emptyList()

    val matchedOptions = allOptions.filter { it.groupName in availableGroups }
    val singleOptions = matchedOptions.filter { it.optionType.lowercase() == "single" }
    val multiOptions = matchedOptions.filter { it.optionType.lowercase() == "multi" }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {
        // Left 60%: Single Selection Options
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(Color(0xFFFFF3E0).copy(alpha = 0.4f)) // Light Orange tint matching single selection
                .padding(6.dp)
        ) {
            Text(
                text = "單選替換區",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(singleOptions) { opt ->
                    val isSelected = selectedCartItem.options.any { it.name == opt.name }
                    OptionButton(
                        option = opt,
                        isSelected = isSelected,
                        onClick = { onOptionClick(opt) }
                    )
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.LightGray)
        )

        // Right 40%: Multi Selection Options (Add-ons)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFFE3F2FD).copy(alpha = 0.4f)) // Light Blue tint matching multi selection
                .padding(6.dp)
        ) {
            Text(
                text = "加料複選區",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(multiOptions) { opt ->
                    val isSelected = selectedCartItem.options.any { it.name == opt.name }
                    OptionButton(
                        option = opt,
                        isSelected = isSelected,
                        onClick = { onOptionClick(opt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    option: OptionItemEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF1976D2) else Color.White
    val textCol = if (isSelected) Color.White else Color(0xFF333333)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = option.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textCol,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (option.price > 0) "+$${option.price}元" else if (option.price < 0) "-$${Math.abs(option.price)}元" else "免費",
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
