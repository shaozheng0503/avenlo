package com.hotfix.avenlo.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.domain.model.UserProfile
import kotlinx.coroutines.launch

/** S6 我的：头像 + 戒指卡（92% 电量环）+ 统计三格（真实数据）+ 设置四项 */
@Composable
fun MineScreen(nav: NavController) {
    val profile = UserProfile()
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())

    // 统计三格吃真实数据：灵感数 / 灵感集数（server 或种子）/ 录音总分钟
    var statsIdeas by remember { mutableStateOf(profile.statsIdeas) }
    var statsCollections by remember { mutableStateOf(profile.statsCollections) }
    var statsMinutes by remember { mutableStateOf(profile.statsMinutes) }
    LaunchedEffect(ideas) {
        if (ideas.isNotEmpty()) {
            statsIdeas = ideas.count { it.status != com.hotfix.avenlo.domain.model.CardStatus.DELETED }
            statsMinutes = (ideas.sumOf { it.durationMs } / 60000).toInt()
        }
    }
    LaunchedEffect(Unit) {
        ServiceLocator.collectionsRepo.getCollections()
            .onSuccess { if (it.isNotEmpty()) statsCollections = it.size }
    }

    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val onToast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        // 头部
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(AvenloTokens.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text("R", color = AvenloTokens.Primary, fontWeight = FontWeight.Bold, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Hey, ${profile.name}", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(profile.slogan, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
            }
            Icon(Icons.Filled.Settings, "设置", tint = AvenloTokens.TextSecondary)
        }

        LazyColumn {
            // 我的戒指卡
            item {
                Row(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                        .clip(CardShape).background(AvenloTokens.Surface).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Watch, null, tint = AvenloTokens.TextSecondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("我的戒指", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(AvenloTokens.Success))
                            Spacer(Modifier.width(4.dp))
                            Text("已连接", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.Success)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("轻捏两次撤销上次记录", fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                    }
                    // 电量环 92%
                    Box(Modifier.size(44.dp).clip(CircleShape).background(AvenloTokens.Bg), contentAlignment = Alignment.Center) {
                        Text("${profile.ringBattery}%", fontSize = AvenloTokens.FontSizeXs, fontWeight = FontWeight.Bold, color = AvenloTokens.Success)
                    }
                }
            }

            // 统计三格（标签 ⚠️ 源文件损坏，按语义重建：灵感/灵感集/分钟）
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                        .clip(CardShape).background(AvenloTokens.Surface).padding(vertical = 16.dp),
                ) {
                    StatCell("💡", "$statsIdeas", "灵感", Modifier.weight(1f)) { nav.navigate(Routes.RECORDS) }
                    Box(Modifier.width(1.dp).height(32.dp).background(AvenloTokens.Border))
                    StatCell("🗂", "$statsCollections", "灵感集", Modifier.weight(1f)) { nav.navigate(Routes.COLLECTIONS) }
                    Box(Modifier.width(1.dp).height(32.dp).background(AvenloTokens.Border))
                    StatCell("⏱", "$statsMinutes", "分钟", Modifier.weight(1f)) { nav.navigate(Routes.RECORDS) }
                }
            }

            // 设置四项（彩色圆角图标：桃/蓝/紫/绿 + 标题 + 副文）
            item {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                        .clip(CardShape).background(AvenloTokens.Surface).padding(6.dp),
                ) {
                    SettingRow("⏰", "提醒时间", "每日 21:00，回顾你的旅程", AvenloTokens.peachTone.badge, onToast)
                    SettingRow("📤", "导出数据", "导出你的记录与灵感", AvenloTokens.blueTone.badge, onToast)
                    SettingRow("✨", "AI整理偏好", "个性化你的灵感分类方式", AvenloTokens.Primary.copy(alpha = 0.6f), onToast)
                    SettingRow("💬", "帮助与反馈", "我们一直在倾听", AvenloTokens.Success, onToast)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
    androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}

@Composable
private fun StatCell(icon: String, value: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = AvenloTokens.FontSize2xl, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
            Spacer(Modifier.width(2.dp))
            Text(label, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun SettingRow(icon: String, title: String, sub: String, tone: Color, onToast: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).clickable { onToast(title + "：正式版提供（Demo 预览）") }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(1.dp))
            Text(sub, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
        }
        Text("›", color = AvenloTokens.TextDisabled, fontSize = 18.sp)
    }
}
