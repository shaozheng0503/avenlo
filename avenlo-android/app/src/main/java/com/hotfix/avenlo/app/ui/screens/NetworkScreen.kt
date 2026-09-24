package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.domain.model.CardStatus
import com.hotfix.avenlo.domain.model.IdeaCard
import kotlin.math.cos
import kotlin.math.sin

/**
 * 灵感脉络（新设计稿详情区）：发现灵感之间的连接与可能
 * 中心 = 当前灵感（主色，脉略中心），周围卫星 = 相关灵感，连线用「脉络连线」色。
 * 底部：这条灵感表达了「旅行中的慢生活」的体验 + 相关灵感列表。
 */
@Composable
fun NetworkScreen(nav: NavController, ideaId: String) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())
    val card = ideas.firstOrNull { it.id == ideaId }
    val relatedCards: List<IdeaCard> = card?.related?.mapNotNull { ref -> ideas.firstOrNull { it.id == ref.id } } ?: emptyList()

    Column(Modifier.fillMaxSize()) {
        // 顶栏（新设计稿 s5：返回箭头 + 当前灵感标题 + 副文「这条灵感表达了…」）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AvenloTokens.TextPrimary)
            }
            Text(
                card?.title ?: "灵感脉络",
                fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold,
                color = AvenloTokens.TextPrimary,
            )
        }
        Text(
            "这条灵感表达了「${card?.tags?.firstOrNull() ?: "旅行"}中的慢生活」的体验",
            Modifier.padding(horizontal = 24.dp),
            fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary,
        )

        LazyColumn(Modifier.fillMaxSize()) {
            // ---- 小节头：灵感脉络（参考样式）----
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("灵感脉络", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "参考样式",
                        fontSize = 10.sp, color = AvenloTokens.Primary,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(AvenloTokens.Primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    "发现灵感之间的连接与可能。",
                    Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled,
                )
            }

            // ---- 脉络图（中心 + 卫星 + 连线）----
            item {
                NetworkGraph(
                    centerTitle = card?.title ?: "灵感",
                    satellites = relatedCards.map { it.title },
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(280.dp),
                )
            }

            // ---- 相关灵感列表 ----
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("相关灵感", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("查看全部 ›", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                if (relatedCards.isEmpty()) {
                    Text(
                        "这条灵感还在整理中，暂无关联",
                        Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled,
                    )
                }
                relatedCards.forEachIndexed { idx, rel ->
                    Row(
                        Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth()
                            .clip(CardShape).background(AvenloTokens.Surface)
                            .clickable { nav.navigate(Routes.detail(rel.id)) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 新设计稿：56dp 圆角色块方图（橙/绿交替，呼应 mock5 相关灵感卡）
                        val blockColor = if (idx % 2 == 0) AvenloTokens.Primary else AvenloTokens.Success
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(blockColor.copy(alpha = 0.85f))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rel.title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(rel.tags.joinToString(" · "), fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
                        }
                        Text("›", color = AvenloTokens.TextDisabled, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}

/** 脉络图：中心圆（主色实底白字）+ 卫星圆 + 连线（Canvas 线，颜色=脉络连线） */
@Composable
private fun NetworkGraph(centerTitle: String, satellites: List<String>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) { drawNetwork(centerTitle, satellites) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNetwork(centerTitle: String, satellites: List<String>) {
    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
    val rx = size.width * 0.30f
    val ry = size.height * 0.28f
    val linkColor = AvenloTokens.Border
    val primary = AvenloTokens.Primary
    val labelColor = AvenloTokens.TextPrimary

    // 文字 Paint（卫星/中心标题标注）
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = labelColor.toArgb()
        textSize = 12.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    // 卫星节点（最多 6 个，围绕椭圆分布；n<=2 时改为左上/右上分布避免「哑铃」形态）
    val n = satellites.size.coerceAtMost(6)
    val pts: List<androidx.compose.ui.geometry.Offset> = when {
        n == 0 -> emptyList()
        n == 1 -> listOf(androidx.compose.ui.geometry.Offset(center.x + rx, center.y))
        n == 2 -> listOf(
            androidx.compose.ui.geometry.Offset(center.x - rx, center.y - ry * 0.55f),
            androidx.compose.ui.geometry.Offset(center.x + rx, center.y - ry * 0.55f),
        )
        else -> (0 until n).map { i ->
            // 从顶部开始，间隔均匀铺开（略去正下方让位给底部解读卡）
            val angleDeg = -90.0 + i * (300.0 / maxOf(n - 1, 1))
            val angle = Math.toRadians(angleDeg)
            androidx.compose.ui.geometry.Offset(
                (center.x + rx * cos(angle)).toFloat(),
                (center.y + ry * sin(angle)).toFloat(),
            )
        }
    }
    // 连线
    pts.forEach { p ->
        drawLine(linkColor, center, p, strokeWidth = 2.dp.toPx())
    }
    // 卫星间弧线（相邻连接，形成脉络网；n<3 时不画避免重复线）
    if (n >= 3) {
        for (i in pts.indices) {
            drawLine(linkColor.copy(alpha = 0.6f), pts[i], pts[(i + 1) % pts.size], strokeWidth = 1.dp.toPx())
        }
    }
    // 卫星节点
    pts.forEach { p ->
        drawCircle(AvenloTokens.Surface, radius = 22.dp.toPx(), center = p)
        drawCircle(primary.copy(alpha = 0.65f), radius = 22.dp.toPx(), center = p, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
    // 中心节点（主色实底：脉络中心）
    drawCircle(primary, radius = 34.dp.toPx(), center = center)
    drawCircle(Color.White.copy(alpha = 0.25f), radius = 42.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))

    // 文字标注：卫星标题在节点下方，中心标题在节点内部（白色粗体）
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        pts.forEachIndexed { i, p ->
            val title = satellites[i]
            val ellipsized = if (title.length > 8) title.take(7) + "…" else title
            native.drawText(ellipsized, p.x, p.y + 22.dp.toPx() + 16.sp.toPx(), textPaint)
        }
        val centerPaint = android.graphics.Paint(textPaint).apply {
            color = android.graphics.Color.WHITE
            textSize = 13.sp.toPx()
            isFakeBoldText = true
        }
        val centerTitleEllipsized = if (centerTitle.length > 4) centerTitle.take(3) + "…" else centerTitle
        val fm = centerPaint.fontMetrics
        val baseline = center.y - (fm.ascent + fm.descent) / 2f
        native.drawText(centerTitleEllipsized, center.x, baseline, centerPaint)
    }
}
