package com.hotfix.avenlo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avenlo 设计 Token —— Figma variables Collection 1 源文件级精确值（方案 4.1）
 * 字体：Microsoft YaHei → Android 系统中文字体回退即可，不打包。
 */
object AvenloTokens {
    // ---- 颜色（11 色） ----
    val Primary = Color(0xFFBE693F)        // 陶土橘棕：CTA、选中 Tab、波形图标
    val PrimaryHover = Color(0xFFA25935)
    val Bg = Color(0xFFFAF8EB)             // 全局米白偏暖黄
    val Surface = Color(0xFFFCF9F3)        // 卡片表面
    val TextPrimary = Color(0xFF2F2C28)
    val TextSecondary = Color(0xFF5A564D)
    val TextDisabled = Color(0xFFB0A294)
    val Border = Color(0xFFDAD0C1)
    val Success = Color(0xFF749378)        // 鼠尾草绿：第二主色
    val Warning = Color(0xFFD9A55C)        // 琥珀金
    val Error = Color(0xFFC05A4E)
    val CaptureDarkBg = Color(0xFF1C1A18)  // S7 锁屏捕捉背景（规格站）

    // 卡片图标三色轮换（紫无 token → primary 透明度变体，方案 4.4 降级策略）
    val IconTones = listOf(Primary, Success, Primary.copy(alpha = 0.55f))

    // S3 灵感集四色（浅主题色底）
    data class ToneSet(val bg: Color, val badge: Color, val text: Color)
    val sageTone = ToneSet(Color(0xFFE8EDE6), Color(0xFF749378), Color(0xFF2F2C28))
    val peachTone = ToneSet(Color(0xFFF5E3D8), Color(0xFFBE693F), Color(0xFF2F2C28))
    val goldTone = ToneSet(Color(0xFFF6E8CF), Color(0xFFD9A55C), Color(0xFF2F2C28))
    val blueTone = ToneSet(Color(0xFFDDE5EC), Color(0xFF6E8CA8), Color(0xFF2F2C28))

    // ---- 字号（sp）：12/14/16/18/22/28/36 ----
    val FontSizeXs = 12.sp
    val FontSizeSm = 14.sp
    val FontSizeBase = 16.sp
    val FontSizeLg = 18.sp
    val FontSizeXl = 22.sp
    val FontSize2xl = 28.sp
    val FontSize3xl = 36.sp

    // ---- 间距（dp）：4/8/16/24/32/48 ----
    val Spacing1 = 4.dp
    val Spacing2 = 8.dp
    val Spacing3 = 16.dp
    val Spacing4 = 24.dp
    val Spacing5 = 32.dp
    val Spacing6 = 48.dp

    // ---- 圆角 ----
    val RadiusSm = 4.dp
    val RadiusMd = 8.dp
    val RadiusLg = 12.dp    // 卡片实际圆角（比目视 20 小，.fig 确认）
    val RadiusFull = 999.dp
}

/** 典型文字样式（正文基准 14sp，页面标题 22sp） */
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
