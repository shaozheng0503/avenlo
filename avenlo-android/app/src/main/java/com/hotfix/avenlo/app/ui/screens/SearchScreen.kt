package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.theme.AvenloTokens

/** S4 搜索/筛选：常用标签四色胶囊 + 筛选 pill + 近期搜索；输入关键词实时搜（title/summary/tags 匹配） */
@Composable
fun SearchScreen(nav: NavController) {
    var filter by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    val history = remember { mutableStateOf(SeedHistory.list) }
    val repo = ServiceLocator.ideaRepo
    val allIdeas by repo.observeIdeas().collectAsState(initial = emptyList())

    // 筛选 pill 对应 range：全部=all 今天=today 本周=week 收藏=favorite
    val rangeOf = listOf("all", "today", "week", "favorite")
    val results = remember(query, filter, allIdeas) {
        val q = query.trim()
        val dayMs = 24 * 3600 * 1000L
        val weekMs = 7 * dayMs
        val now = System.currentTimeMillis()
        allIdeas
            .filter { it.status != com.hotfix.avenlo.domain.model.CardStatus.DELETED }
            .filter { card ->
                when (rangeOf[filter]) {
                    "today" -> now - card.createdAt < dayMs
                    "week" -> now - card.createdAt < weekMs
                    "favorite" -> card.tags.contains("收藏")
                    else -> true
                }
            }
            .filter { card ->
                q.isEmpty() ||
                    card.title.contains(q, ignoreCase = true) ||
                    card.summary.contains(q, ignoreCase = true) ||
                    card.tags.any { it.contains(q, ignoreCase = true) }
            }
    }

    Column(Modifier.fillMaxSize()) {
        // 搜索框 + 右侧筛选图标（真实输入）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索灵感、主题或关键词", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = AvenloTokens.TextDisabled, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(Icons.Filled.Close, "清空", tint = AvenloTokens.TextDisabled, modifier = Modifier.size(16.dp).clickable { query = "" })
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* 实时搜索，无需额外动作 */ }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AvenloTokens.Surface,
                    unfocusedContainerColor = AvenloTokens.Surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Tune, "筛选", tint = AvenloTokens.TextSecondary, modifier = Modifier.size(22.dp))
        }

        LazyColumn {
            if (query.isNotBlank()) {
                // ---- 搜索结果 ----
                item {
                    Text(
                        "找到 ${results.size} 条",
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary,
                    )
                }
                if (results.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("没有找到相关灵感", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled)
                            Text("换个关键词试试", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                        }
                    }
                }
                items(results, key = { it.id }) { card ->
                    SearchResultRow(card, onClick = { nav.navigate(com.hotfix.avenlo.app.ui.navigation.Routes.detail(card.id)) })
                }
            } else {
            // 常用标签
            item {
                Text("常用标签", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeedHistory.hotTags.forEach { (tag, tone) -> TagChip(tag, tone = tone) }
                }
            }
            // 筛选 pill
            item {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "今天", "本周", "收藏").forEachIndexed { i, label ->
                        val selected = filter == i
                        Text(
                            label,
                            fontSize = AvenloTokens.FontSizeXs,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else AvenloTokens.TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) AvenloTokens.TextPrimary else AvenloTokens.Surface)
                                .clickable { filter = i }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            // 近期搜索
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("近期搜索", fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("清空", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary,
                        modifier = Modifier.clickable { history.value = emptyList() })
                }
            }
            items(history.value, key = { it.text }) { h ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp)).background(AvenloTokens.Surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(AvenloTokens.Bg))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.text, fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            h.tags.forEach { TagChip(it) }
                        }
                    }
                    Text(h.time, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                }
            }
            } // end else（无 query 时显示常用标签/近期搜索）
        }
    }
}

/** 搜索结果行：点击进详情 */
@Composable
private fun SearchResultRow(card: com.hotfix.avenlo.domain.model.IdeaCard, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp)).background(AvenloTokens.Surface).clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.hotfix.avenlo.app.ui.components.WaveIconTile(toneIndex = card.id.hashCode())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(card.summary, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.tags.take(2).forEach { TagChip(it) }
            }
        }
    }
}

private object SeedHistory {
    val hotTags = listOf(
        "#设计" to AvenloTokens.Success,
        "#摄影" to AvenloTokens.Primary,
        "#待办" to AvenloTokens.Warning,
        "#人物" to AvenloTokens.blueTone.badge,
    )
    val list = listOf(
        com.hotfix.avenlo.data.mock.SeedData.searchHistory[0],
        com.hotfix.avenlo.data.mock.SeedData.searchHistory[1],
        com.hotfix.avenlo.data.mock.SeedData.searchHistory[2],
    )
}
