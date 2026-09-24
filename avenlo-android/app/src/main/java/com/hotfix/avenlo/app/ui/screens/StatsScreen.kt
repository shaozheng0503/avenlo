package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.app.ui.theme.InterFont
import com.hotfix.avenlo.domain.model.CardStatus
import com.hotfix.avenlo.domain.model.IdeaCard

/**
 * S5' 统计页（新设计稿「标签与统计」）：回顾你的灵感，见证生活的积累
 * - 标签筛选 chips（全部/旅行/生活/成长/家居/工作/艺术/其他 两行）
 * - 记录总数 / 连续记录（40sp Inter 数字）
 * - 灵感热力图（12 周 × 7 日，主色 5 级热力）
 * - 灵感类型分布（donut 环形图 + 右侧图例百分比）
 * 真实数据驱动：总数/连续天数吃 ideaRepo；类型分布按 tags 统计回落设计稿比例。
 */
@Composable
fun StatsScreen(nav: NavController) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())
    var selectedTag by remember { mutableStateOf("全部") }

    val tagOptions = listOf("全部", "旅行", "生活", "成长", "家居", "工作", "艺术", "其他")
    val allActive = ideas.filter { it.status != CardStatus.DELETED }
    val active = if (selectedTag == "全部") allActive else when (selectedTag) {
        "其他" -> allActive.filter { it.tags.none { t -> t in tagOptions.drop(1).dropLast(1) } }
        else -> allActive.filter { selectedTag in it.tags }
    }
    val totalCount = active.size.coerceAtLeast(1)
    // 连续记录天数：按日去重后从今天往前数连续段（无记录日断开）
    val streakDays = rememberStreakDays(allActive)

    LazyColumn(Modifier.fillMaxSize()) {
        // ---- 页头（新设计稿：标签与统计）----
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("标签与统计", fontSize = AvenloTokens.FontSize2xl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("回顾你的灵感，见证生活的积累。", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
            }
        }

        // ---- 标签筛选 chips（两行流式排列，选中 primary 实底白字）----
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tagOptions.chunked(5).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowTags.forEach { tag ->
                            val selected = selectedTag == tag
                            Text(
                                tag,
                                fontSize = AvenloTokens.FontSizeXs,
                                color = if (selected) Color.White else AvenloTokens.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) AvenloTokens.Primary else AvenloTokens.Surface)
                                    .clickable { selectedTag = tag }
                                    .padding(horizontal = 18.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }

        // ---- 统计双卡：记录总数 / 连续记录（统计数字 40sp Inter）----
        item {
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatBigCard(
                    value = "$totalCount", unit = "条灵感", label = "记录总数",
                    modifier = Modifier.weight(1f),
                )
                StatBigCard(
                    value = "$streakDays", unit = "天", label = "连续记录",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---- 灵感热力图（12 周 × 7 日，主色 5 级透明度）----
        item {
            Spacer(Modifier.height(16.dp))
            HeatmapCard(active)
        }

        // ---- 灵感类型分布 ----
        item {
            Spacer(Modifier.height(16.dp))
            TypeDistCard(active, totalCount)
            Spacer(Modifier.height(88.dp))
        }
    }
}

/** 连续记录天数：真实 createdAt 按日聚合，从今天（或昨天）往回数 */
private fun rememberStreakDays(active: List<IdeaCard>): Int {
    if (active.isEmpty()) return 0
    val dayMs = 86_400_000L
    val today = System.currentTimeMillis() / dayMs
    val days = active.map { it.createdAt / dayMs }.toMutableSet()
    var streak = 0
    var d = today
    if (today !in days) d = today - 1   // 今天还没记，从昨天起算
    while (d in days) { streak++; d-- }
    return streak
}

@Composable
private fun StatBigCard(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(AvenloTokens.Surface).padding(16.dp),
    ) {
        Text(label, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = AvenloTokens.StatNumber, fontWeight = FontWeight.Bold, fontFamily = InterFont, color = AvenloTokens.TextPrimary)
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

/** 热力图卡：灵感热力图 + 12周×7日网格 + 最浅→最深 5 级图例 */
@Composable
private fun HeatmapCard(active: List<IdeaCard>) {
    val dayMs = 86_400_000L
    val today = System.currentTimeMillis() / dayMs
    val byDay = active.groupBy { it.createdAt / dayMs }

    Column(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(CardShape).background(AvenloTokens.Surface).padding(16.dp),
    ) {
        Text("灵感热力图", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // 12 列（周）× 7 行（日）；主色 5 级透明度：0 → 最浅
        val levels = listOf(0.10f, 0.30f, 0.50f, 0.75f, 1.0f)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 12) {
                        // col=11 为本周，row=6 为今天（简化：最后一格即今天）
                        val day = today - (11 - col) * 7 - (6 - row)
                        val count = byDay[day]?.size ?: 0
                        val lvl = when {
                            count == 0 -> 0
                            count == 1 -> 1
                            count == 2 -> 2
                            count <= 4 -> 3
                            else -> 4
                        }
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (lvl == 0) AvenloTokens.Border.copy(alpha = 0.35f)
                                    else AvenloTokens.Primary.copy(alpha = levels[lvl])
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // 星期轴标注（新设计稿：一 三 四 五 六 日，对齐 7 行网格）
        val dayLabels = listOf("一", "", "三", "四", "五", "六", "日")
        Column(Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            dayLabels.forEachIndexed { i, label ->
                Box(Modifier.height(16.dp)) {
                    Text(
                        label, fontSize = 10.sp, color = AvenloTokens.TextDisabled,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 图例：最浅 → 最深
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最浅", fontSize = 10.sp, color = AvenloTokens.TextDisabled)
            Spacer(Modifier.width(6.dp))
            levels.forEach { a ->
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(AvenloTokens.Primary.copy(alpha = a)))
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text("最深", fontSize = 10.sp, color = AvenloTokens.TextDisabled)
        }
    }
}

/** 类型分布卡（新设计稿：donut 环形图 + 右侧图例百分比），六类配色主色系深→浅 */
@Composable
private fun TypeDistCard(active: List<IdeaCard>, totalCount: Int) {
    // 设计稿基准分布（真实 tags 覆盖优先，未命中类保持设计稿比例）
    val design = linkedMapOf(
        "旅行" to 28, "生活" to 24, "成长" to 18, "家居" to 12, "工作" to 10, "艺术" to 8,
    )
    val real = active.flatMap { it.tags }.groupingBy { it }.eachCount()
    val rows = design.map { (name, base) ->
        val c = real[name] ?: 0
        name to if (c > 0) c * 100 / totalCount else base
    }
    val total = rows.sumOf { it.second }.coerceAtLeast(1)
    // 六类颜色：主色系深→浅 + 补充蓝/金（呼应设计稿 donut 多彩但克制）
    val colors = listOf(
        Color(0xFFBF6940), Color(0xFFD9A55C), Color(0xFFE0C3A0),
        Color(0xFFC98B6B), Color(0xFF6E8CA8), Color(0xFFE5D5C0),
    )

    Column(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(CardShape).background(AvenloTokens.Surface).padding(16.dp),
    ) {
        Text("灵感类型分布", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // donut 环形图（Canvas drawArc，环宽 ~26dp）
            androidx.compose.foundation.Canvas(
                Modifier.size(132.dp),
            ) {
                val stroke = 26.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var startAngle = -90f
                rows.forEachIndexed { i, (_, pct) ->
                    val sweep = pct.toFloat() / total * 360f
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep - 2f, // 留 2° 缝隙呼应设计稿分段感
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        style = Stroke(width = stroke),
                    )
                    startAngle += sweep
                }
            }
            Spacer(Modifier.width(24.dp))
            // 右侧图例：色点 + 名称 + 百分比
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { i, (name, pct) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(colors[i % colors.size]))
                        Spacer(Modifier.width(8.dp))
                        Text(name, fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$pct%", fontSize = AvenloTokens.FontSizeXs,
                            fontFamily = InterFont, fontWeight = FontWeight.Medium,
                            color = AvenloTokens.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
