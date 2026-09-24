package com.hotfix.avenlo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotfix.avenlo.app.R

/**
 * Avenlo 设计 Token —— Figma variables Collection 1 源文件级精确值（方案 4.1）
 * 第五十轮（新 Hotfix.fig）：
 *  - 数字/西文用 Inter（Medium/Regular/Bold 三字重，res/font），中文继续系统 Microsoft YaHei 回退
 *  - 统计数字字号：设计规范「原34-46收敛取40」→ StatNumber = 40sp
 *  - 小字号规范「原11-13取12」→ FontSizeXs = 12sp（维持）
 */
object AvenloTokens {
    // ---- 颜色（11 色）——第四十六轮按设计稿样机 PNG 像素实测校准 ----
    val Primary = Color(0xFFBF6940)        // 陶土橘棕：设计稿实测 #BF6940（原 #BE693F 微调）
    val PrimaryHover = Color(0xFFA25935)
    val Bg = Color(0xFFFAF5EB)             // 全局米白：设计稿众数 #FAF5EB~#FBF6EE（原 #FAF8EB 偏黄已修）
    val Surface = Color(0xFFFCF9F3)        // 卡片表面：设计稿实测 #FCF9F1/#FCF9F4
    val TextPrimary = Color(0xFF2F2C28)
    val TextSecondary = Color(0xFF5A564D)
    val TextDisabled = Color(0xFFB0A294)
    val Border = Color(0xFFDAD0C1)
    val Success = Color(0xFF749378)        // 鼠尾草绿：第二主色（Laurel 确认）
    val Warning = Color(0xFFD9A55C)        // 琥珀金
    val Error = Color(0xFFC05A4E)
    val CaptureDarkBg = Color(0xFF1C1A18)  // S7 锁屏捕捉背景（规格站）

    // 卡片图标三色轮换（紫无 token → primary 透明度变体，方案 4.4 降级策略）
    val IconTones = listOf(Primary, Success, Primary.copy(alpha = 0.55f))

    // S3 灵感集四色（浅主题色底）——badge/peach 同步 #BF6940
    data class ToneSet(val bg: Color, val badge: Color, val text: Color)
    val sageTone = ToneSet(Color(0xFFE8EDE6), Color(0xFF749378), Color(0xFF2F2C28))
    val peachTone = ToneSet(Color(0xFFF5E3D8), Color(0xFFBF6940), Color(0xFF2F2C28))
    val goldTone = ToneSet(Color(0xFFF6E8CF), Color(0xFFD9A55C), Color(0xFF2F2C28))
    val blueTone = ToneSet(Color(0xFFDDE5EC), Color(0xFF6E8CA8), Color(0xFF2F2C28))

    // ---- 字号（sp）：12/14/16/18/22/28/36 + 统计数字 40（新设计稿规范）----
    val FontSizeXs = 12.sp
    val FontSizeSm = 14.sp
    val FontSizeBase = 16.sp
    val FontSizeLg = 18.sp
    val FontSizeXl = 22.sp
    val FontSize2xl = 28.sp
    val FontSize3xl = 36.sp
    val StatNumber = 40.sp           // 设计规范「统计数字(原34-46收敛取40)」

    // ---- 间距（dp）：4/8/16/24/32/48 ----
    val Spacing1 = 4.dp
    val Spacing2 = 8.dp
    val Spacing3 = 16.dp
    val Spacing4 = 24.dp
    val Spacing5 = 32.dp
    val Spacing6 = 48.dp

    // ---- 圆角（第四十六轮设计稿样机实测：720px 宽切图上卡片圆角弧约 16px → 8dp；大卡 24px → 12dp）----
    val RadiusSm = 4.dp
    val RadiusMd = 8.dp
    val RadiusLg = 12.dp    // 大卡（灵感集/今日最佳照片卡）
    val RadiusFull = 999.dp
}

/** Inter 数字/西文字体族（新设计稿：Inter Medium 20 / Regular 14 / Bold 6 处引用） */
val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold),
)
/** 常用文字样式（正文基准 14sp，页面标题 22sp） */
val AvenloTypography = Typography(
    headlineLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.4.sp),   // 行高 1.6
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

/** 常用形状 */
val CardShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(999.dp)
val ThumbShape = RoundedCornerShape(8.dp)
