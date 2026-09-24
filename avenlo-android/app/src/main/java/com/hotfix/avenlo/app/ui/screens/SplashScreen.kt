package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * S0 启动/欢迎页 —— 1:1 还原设计稿 mock3_phone1：
 * 手写体 Logo + 标语「把灵感装进一次轻捏」+ 居中戒指插画（环带/宝石/光线/投影）
 * + 底部陶土色胶囊 CTA「开始记录 →」，角落浅陶土色块与游走细线装饰。
 */
@Composable
fun SplashScreen(onStart: () -> Unit) {
    // 分镜脚本：Splash 1.5s 自动跳首页（Demo 节奏）；CTA 点击可提前跳，先到先得
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onStart()
    }
    Box(Modifier.fillMaxSize().background(AvenloTokens.Bg)) {
        SplashDecoration(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1.05f))
            // 手写体 Logo（衬线斜体近似手写）
            Text(
                "Avenlo",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                color = AvenloTokens.Primary,
            )
            Spacer(Modifier.height(14.dp))
            Text("把灵感装进一次轻捏", fontSize = 18.sp, color = AvenloTokens.TextPrimary)
            Spacer(Modifier.weight(0.85f))
            RingIllustration(Modifier.size(280.dp, 255.dp))
            Spacer(Modifier.weight(1.1f))
            // 陶土色胶囊 CTA（设计稿：284.5dp 宽 × 53.5dp 高，居中）
            Row(
                Modifier
                    .padding(bottom = 72.dp)
                    .padding(horizontal = 52.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AvenloTokens.Primary)
                    .clickable { onStart() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("开始记录", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Text("→", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 背景装饰：右上/左下浅陶土色块 + 主色低透明度游走细线（设计稿意象） */
@Composable
private fun SplashDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // 右上色块
        drawOval(Color(0xFFF6E7DA), topLeft = Offset(w * 0.74f, -h * 0.07f), size = Size(w * 0.42f, h * 0.17f))
        // 左下色块
        drawOval(Color(0xFFF6E7DA), topLeft = Offset(-w * 0.20f, h * 0.80f), size = Size(w * 0.52f, h * 0.30f))
        // 右下细弧色带
        drawOval(Color(0xFFF3E1D2), topLeft = Offset(w * 0.70f, h * 0.72f), size = Size(w * 0.55f, h * 0.42f))
        // 游走细线 ×2
        val line = Path()
        line.moveTo(w * 0.50f, h * 0.55f)
        line.quadraticBezierTo(w * 0.98f, h * 0.60f, w * 0.86f, h * 0.86f)
        drawPath(line, Color(0x40BF6940), style = Stroke(1.5.dp.toPx()))
        val line2 = Path()
        line2.moveTo(w * 0.08f, h * 0.64f)
        line2.quadraticBezierTo(w * 0.46f, h * 0.76f, w * 0.32f, h * 0.985f)
        drawPath(line2, Color(0x38BF6940), style = Stroke(1.5.dp.toPx()))
    }
}

/** 戒指插画：椭圆环带 + 顶部橙宝石 + 右上三道光线 + 地面投影（Canvas 矢量绘制） */
@Composable
private fun RingIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = Color(0xFF4A4038)

        // ---- 地面投影 ----
        drawOval(
            Color(0xFFEDE1CF),
            topLeft = Offset(w * 0.16f, h * 0.885f),
            size = Size(w * 0.68f, h * 0.075f),
        )

        // ---- 环带：外椭圆 + 内椭圆 even-odd 成环 ----
        val ring = Path().apply {
            addOval(Rect(Offset(w * 0.14f, h * 0.155f), Size(w * 0.72f, h * 0.625f)))
            addOval(Rect(Offset(w * 0.265f, h * 0.295f), Size(w * 0.47f, h * 0.345f)))
            fillType = PathFillType.EvenOdd
        }
        drawPath(ring, Color(0xFFFDF9F1))
        drawPath(ring, ink, style = Stroke(3.dp.toPx()))

        // ---- 环带中径参数（用于细节刻线定位）----
        val cx = w * 0.5f
        val cy = h * 0.4675f
        val mrx = w * 0.2975f
        val mry = h * 0.24f

        // 左侧缝线：4 条切向短线
        val stitch = Path()
        for (deg in intArrayOf(140, 160, 180, 200)) {
            val a = Math.toRadians(deg.toDouble())
            val px = cx + (cos(a) * mrx).toFloat()
            val py = cy + (sin(a) * mry).toFloat()
            val tx = (-sin(a) * mrx).toFloat()
            val ty = (cos(a) * mry).toFloat()
            val n = sqrt(tx * tx + ty * ty)
            val half = 3.5.dp.toPx()
            stitch.moveTo(px - tx / n * half, py - ty / n * half)
            stitch.lineTo(px + tx / n * half, py + ty / n * half)
        }
        drawPath(stitch, ink, style = Stroke(2.dp.toPx()))

        // 右侧反光：2 条切向短线（浅色）
        val shine = Path()
        for (deg in intArrayOf(-35, -15)) {
            val a = Math.toRadians(deg.toDouble())
            val px = cx + (cos(a) * mrx).toFloat()
            val py = cy + (sin(a) * mry).toFloat()
            val tx = (-sin(a) * mrx).toFloat()
            val ty = (cos(a) * mry).toFloat()
            val n = sqrt(tx * tx + ty * ty)
            val half = 4.5.dp.toPx()
            shine.moveTo(px - tx / n * half, py - ty / n * half)
            shine.lineTo(px + tx / n * half, py + ty / n * half)
        }
        drawPath(shine, Color(0xFFD9CCB8), style = Stroke(2.5.dp.toPx()))

        // ---- 宝石：顶部橙石 + 描边 + 高光（坐在环带顶部）----
        drawOval(
            Color(0xFFD98A4E),
            topLeft = Offset(cx - w * 0.055f, h * 0.10f),
            size = Size(w * 0.11f, h * 0.09f),
        )
        drawOval(
            Color(0xFF7A4E28),
            topLeft = Offset(cx - w * 0.055f, h * 0.10f),
            size = Size(w * 0.11f, h * 0.09f),
            style = Stroke(2.5.dp.toPx()),
        )
        drawOval(
            Color(0xCCF2B27E),
            topLeft = Offset(cx - w * 0.032f, h * 0.115f),
            size = Size(w * 0.028f, h * 0.02f),
        )

        // ---- 右上光线 ×3 ----
        val ray = Path()
        ray.moveTo(w * 0.60f, h * 0.075f); ray.lineTo(w * 0.645f, h * 0.038f)
        ray.moveTo(w * 0.655f, h * 0.115f); ray.lineTo(w * 0.715f, h * 0.092f)
        ray.moveTo(w * 0.575f, h * 0.045f); ray.lineTo(w * 0.590f, h * 0.008f)
        drawPath(ray, Color(0xFFC77B4A), style = Stroke(3.dp.toPx()))
    }
}
