package com.hotfix.avenlo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotfix.avenlo.app.ui.theme.AvenloTokens

/** 左 44dp 圆角方形波形图标（三色轮换：陶土/鼠尾草绿/淡紫变体） */
@Composable
fun WaveIconTile(toneIndex: Int, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val tone = AvenloTokens.IconTones[Math.floorMod(toneIndex, AvenloTokens.IconTones.size)]
    Box(
        modifier = modifier
            .size(44.dp)
            .background(tone.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(22.dp))
        } else {
            // 波形符号（音频意象）
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(8, 14, 20, 12).forEach { h ->
                    Box(
                        Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(3.dp, h.dp)
                            .background(tone, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

/** 浅色胶囊标签 chip（新设计稿：#前缀 + 浅底深字）；onClick 非空时可点（如搜索屏标签点击触发搜索） */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier, tone: Color? = null, onClick: (() -> Unit)? = null, showHash: Boolean = true) {
    val bg = tone?.copy(alpha = 0.16f) ?: AvenloTokens.Primary.copy(alpha = 0.12f)
    val fg = tone ?: AvenloTokens.Primary
    Text(
        text = if (showHash) "#$text" else text,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .let { m -> if (onClick != null) m.clip(RoundedCornerShape(999.dp)).clickable { onClick() } else m }
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 标签 → 配色（方案 4.4：生活/待办=绿、旅行/心情=粉橙、创作/美食=金、摄影/日记=蓝） */
fun tagTone(tag: String): Color = when (tag) {
    "生活", "待办" -> AvenloTokens.Success
    "旅行", "心情" -> AvenloTokens.Primary
    "创作", "美食" -> AvenloTokens.Warning
    "摄影", "日记" -> AvenloTokens.blueTone.badge
    else -> AvenloTokens.Success
}
