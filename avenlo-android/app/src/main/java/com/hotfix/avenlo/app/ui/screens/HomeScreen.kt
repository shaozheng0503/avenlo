package com.hotfix.avenlo.app.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.R
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.components.tagTone
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.domain.capture.CaptureSpec
import com.hotfix.avenlo.domain.capture.HapticEvent
import com.hotfix.avenlo.domain.model.CardStatus
import com.hotfix.avenlo.domain.model.IdeaCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** S1 首页：问候头 + 搜索入口 + 今天/本周/更早 分组卡片列表（Demo 主舞台） */
@Composable
fun HomeScreen(nav: NavController) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())

    // 冷启动即拉 server 列表（merge 保留本地种子；断网时 runCatching 静默回落本地数据）
    LaunchedEffect(Unit) { repo.refresh() }

    // 存在 QUEUED 占位卡时每 2s 轮询刷新（服务端 3s 状态机：queued → ok）
    LaunchedEffect(ideas.any { it.status == CardStatus.QUEUED }) {
        while (ideas.any { it.status == CardStatus.QUEUED }) {
            kotlinx.coroutines.delay(2000)
            repo.refresh()
        }
    }

    val today = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE).format(Date())
    val week = SimpleDateFormat("EEEE", Locale.CHINESE).format(Date())

    Column(Modifier.fillMaxSize()) {
        // ---- 问候头（新设计稿：日期一行完整显示「2025年9月22日星期一」）----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hey, Runel ☀️", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text("$today$week", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
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
            Text("搜索灵感、关键词、标签..", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled)
            Spacer(Modifier.weight(1f))
            // 灵感集入口（阻断项 #1 建议：挂在首页顶部）
            Icon(Icons.Filled.CollectionsBookmark, "灵感集", tint = AvenloTokens.Success, modifier = Modifier.size(20.dp).clickable { nav.navigate(Routes.COLLECTIONS) })
        }
        Spacer(Modifier.height(12.dp))

        // ---- Tab chips（新设计稿：三枚独立胶囊并排，今天选中 primary 实底白字，其余米色底）----
        var tab by remember { mutableStateOf(0) }
        Row(
            Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("今天", "本周", "更早").forEachIndexed { i, label ->
                val selected = tab == i
                Text(
                    label,
                    fontSize = AvenloTokens.FontSizeXs,
                    color = if (selected) Color.White else AvenloTokens.TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) AvenloTokens.Primary
                            else AvenloTokens.Surface
                        )
                        .clickable { tab = i }
                        .padding(horizontal = 22.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // ---- 分组列表 ----
        LazyColumn(modifier = Modifier.weight(1f)) {
            val todayCards = ideas.filter { isToday(it.createdAt) }
            val weekCards = ideas.filter { !isToday(it.createdAt) && it.createdAt > System.currentTimeMillis() - 7 * 86_400_000L }
            val earlierCards = ideas.filter { it.createdAt <= System.currentTimeMillis() - 7 * 86_400_000L }

            when (tab) {
                0 -> {
                    if (todayCards.isNotEmpty()) {
                        item { SectionHeader("今天·${todayCards.size}条", nav) }
                        items(todayCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
                    }
                }
                1 -> {
                    if (weekCards.isNotEmpty()) {
                        item { SectionHeader("本周·${weekCards.size}条", nav) }
                        items(weekCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
                    }
                }
                else -> {
                    if (earlierCards.isNotEmpty()) {
                        item { SectionHeader("更早", nav) }
                        items(earlierCards, key = { it.id }) { HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) } }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    // FAB：胶囊形「⏺ 记录灵感」（新设计稿）；长按 300ms 模拟戒指轻捏（PINCH_MIN_MS 防误触）带按压进度环
    val view = LocalView.current
    Box(Modifier.fillMaxSize()) {
        var holdProgress by remember { mutableStateOf(0f) }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 按压进度环
            if (holdProgress > 0f && holdProgress < 1f) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(56.dp),
                )
            }
            Row(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(AvenloTokens.Primary)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = System.currentTimeMillis()
                            var finished = false
                            holdProgress = 0f
                            // 时间驱动长按：按 32ms 时间片轮询，超时无事件 = 仍在按住（≥300ms 触发）。
                            // adb input swipe 同点长按不派发中间 MOVE，纯事件驱动轮询会漏判成短按
                            while (true) {
                                val elapsed = System.currentTimeMillis() - start
                                val remaining = CaptureSpec.PINCH_MIN_MS - elapsed
                                if (remaining <= 0) { finished = true; break }
                                holdProgress = (elapsed.toFloat() / CaptureSpec.PINCH_MIN_MS).coerceIn(0f, 1f)
                                val event = withTimeoutOrNull(minOf(remaining, 32)) { awaitPointerEvent() }
                                if (event == null) continue           // 本时间片无事件：仍在按住，继续计时
                                if (!event.changes.any { it.pressed }) break  // 提前松手 → 短按
                            }
                            holdProgress = 0f
                            if (finished) {
                                // 消费后续 up 事件（手指可能仍按着）
                                withTimeoutOrNull(80) { awaitPointerEvent() }
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                nav.navigate(Routes.capture(autoStart = true))
                            } else {
                                // 短按（<300ms）：进 Ready 态（单击兜底）
                                nav.navigate(Routes.CAPTURE)
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("记录灵感", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
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

/** 首页 Idea Card（新设计稿：无左侧图标，标题/tags/摘要顶格；时长跟在摘要行尾） */
@Composable
fun HomeIdeaCard(card: IdeaCard, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(CardShape)
            .background(AvenloTokens.Surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(card.title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold, color = AvenloTokens.TextPrimary)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            card.tags.take(2).forEach { TagChip(it, tone = tagTone(it)) }
        }
        if (card.summary.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    card.summary, fontSize = AvenloTokens.FontSizeXs,
                    color = AvenloTokens.TextDisabled, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text("${formatTime(card.createdAt)}·${formatDuration(card.durationMs)}", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
            }
        } else {
            Spacer(Modifier.height(7.dp))
            Text("${formatTime(card.createdAt)} · ${formatDuration(card.durationMs)}", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
        }
    }
}

internal fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(ts))
internal fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}分${s % 60}秒"
}
