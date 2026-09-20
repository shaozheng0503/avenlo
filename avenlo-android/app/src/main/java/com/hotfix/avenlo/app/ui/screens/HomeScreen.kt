package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.R
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.components.WaveIconTile
import com.hotfix.avenlo.app.ui.components.tagTone
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.domain.model.CardStatus
import com.hotfix.avenlo.domain.model.IdeaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** S1 首页：问候头 + 搜索入口 + 今天/本周/更早 分组卡片列表（Demo 主舞台） */
@Composable
fun HomeScreen(nav: NavController) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())

    // 存在 QUEUED 占位卡时每 2s 轮询刷新（服务端 3s 状态机：queued → ok）
    LaunchedEffect(ideas.any { it.status == CardStatus.QUEUED }) {
        while (ideas.any { it.status == CardStatus.QUEUED }) {
            kotlinx.coroutines.delay(2000)
            repo.refresh()
        }
    }

    val today = SimpleDateFormat("M月d日", Locale.CHINESE).format(Date())
    val week = SimpleDateFormat("EEEE", Locale.CHINESE).format(Date())

    Column(Modifier.fillMaxSize()) {
        // ---- 问候头 ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hey, Runel ☀️", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text("${week}·$today", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
            }
            // 头像入口（右上）
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AvenloTokens.Primary.copy(alpha = 0.15f))
                    .clickable { nav.navigate(Routes.MINE) },
                contentAlignment = Alignment.Center,
            ) { Text("R", color = AvenloTokens.Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        // ---- 搜索框（点击进 S4）----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AvenloTokens.Surface)
                .clickable { nav.navigate(Routes.SEARCH) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = AvenloTokens.TextDisabled, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("搜索灵感、关键词、标签", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled)
            Spacer(Modifier.weight(1f))
            // 灵感集入口（阻断项 #1 建议：挂在首页顶部）
            Icon(Icons.Filled.CollectionsBookmark, "灵感集", tint = AvenloTokens.Success, modifier = Modifier.size(20.dp).clickable { nav.navigate(Routes.COLLECTIONS) })
        }
        Spacer(Modifier.height(12.dp))

        // ---- 分组列表 ----
        LazyColumn(Modifier.weight(1f)) {
            val todayCards = ideas.filter { isToday(it.createdAt) }
            val weekCards = ideas.filter { !isToday(it.createdAt) && it.createdAt > System.currentTimeMillis() - 7 * 86_400_000L }
            val earlierCards = ideas.filter { it.createdAt <= System.currentTimeMillis() - 7 * 86_400_000L }

            if (todayCards.isNotEmpty()) {
                item { SectionHeader("今天·${todayCards.size}条", nav) }
                items(todayCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
            }
            if (weekCards.isNotEmpty()) {
                item { SectionHeader("本周·${weekCards.size}条", nav) }
                items(weekCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
            }
            if (earlierCards.isNotEmpty()) {
                item { SectionHeader("更早", nav) }
                items(earlierCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    // FAB：长按捕捉（≥300ms 防误触，规格站交互）
    Box(Modifier.fillMaxSize()) {
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = { nav.navigate(Routes.CAPTURE) },
            containerColor = AvenloTokens.Primary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) { Text("轻捏", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun isToday(ts: Long): Boolean = ts > System.currentTimeMillis() - 86_400_000L

@Composable
private fun SectionHeader(title: String, nav: NavController) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
        Spacer(Modifier.weight(1f))
        Text("›", color = AvenloTokens.TextSecondary, fontSize = 20.sp)
    }
}

/** 首页 Idea Card（surface 卡 + 描边、左波形图标、标签 chips、元信息行） */
@Composable
fun HomeIdeaCard(card: IdeaCard, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(CardShape)
            .background(AvenloTokens.Surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaveIconTile(toneIndex = card.id.hashCode())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold, color = AvenloTokens.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.tags.take(2).forEach { TagChip(it, tone = tagTone(it)) }
            }
            if (card.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(card.summary, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled, maxLines = 2)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(card.createdAt), fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                Text(" · ${formatDuration(card.durationMs)}", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                Spacer(Modifier.weight(1f))
                Text("›", color = AvenloTokens.TextDisabled, fontSize = 16.sp)
            }
        }
    }
}

internal fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(ts))
internal fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}分${s % 60}秒"
}
