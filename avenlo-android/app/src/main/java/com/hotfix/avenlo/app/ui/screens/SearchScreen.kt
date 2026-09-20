package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.theme.AvenloTokens

/** S4 搜索/筛选：常用标签四色胶囊 + 筛选 pill + 近期搜索 */
@Composable
fun SearchScreen(nav: NavController) {
    var filter by remember { mutableStateOf(0) }
    val history = remember { mutableStateOf(SeedHistory.list) }

    Column(Modifier.fillMaxSize()) {
        // 搜索框 + 右侧筛选图标
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(AvenloTokens.Surface)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = AvenloTokens.TextDisabled, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("搜索灵感、主题或关键词", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled)
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Tune, "筛选", tint = AvenloTokens.TextSecondary, modifier = Modifier.size(22.dp))
        }

        LazyColumn {
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
