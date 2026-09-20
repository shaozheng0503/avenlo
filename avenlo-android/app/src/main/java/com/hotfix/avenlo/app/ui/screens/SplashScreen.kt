package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotfix.avenlo.app.ui.theme.AvenloTokens

/** S0 启动页：手写体 Logo + slogan + 戒指线稿意象 + 陶土 CTA */
@Composable
fun SplashScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(AvenloTokens.Bg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1.2f))
        // Logo
        Text("Avenlo", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = AvenloTokens.Primary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        Spacer(Modifier.height(10.dp))
        Text("把灵感装进一次轻捏", fontSize = AvenloTokens.FontSizeXl, color = AvenloTokens.TextPrimary)
        Spacer(Modifier.weight(1f))
        // 戒指线稿意象（圆环）
        Box(
            Modifier.size(140.dp).clip(CircleShape).background(Color.Transparent)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(AvenloTokens.Bg).padding(14.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(AvenloTokens.Surface))
            }
            Box(
                Modifier.align(Alignment.TopCenter).size(14.dp).clip(CircleShape).background(AvenloTokens.Primary)
            )
        }
        Spacer(Modifier.weight(1.4f))
        // CTA
        Box(
            Modifier
                .padding(bottom = 64.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AvenloTokens.Primary)
                .clickable { onStart() }
                .padding(horizontal = 48.dp, vertical = 16.dp),
        ) {
            Text("开始记录 →", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
