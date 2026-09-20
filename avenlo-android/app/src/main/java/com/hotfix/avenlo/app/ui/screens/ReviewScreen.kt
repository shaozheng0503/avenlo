package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.data.mock.SeedData

/** S5 今日回顾：今日最佳 → 意外关联（评审记忆点）→ 明日待延展 → 睡前提醒；server 优先断网回落种子 */
@Composable
fun ReviewScreen(nav: NavController) {
    var review by remember { mutableStateOf(SeedData.dailyReview) }
    LaunchedEffect(Unit) {
        ServiceLocator.reviewRepo.getDailyReview()
            .onSuccess { review = it }
    }
    var remindOn by remember { mutableStateOf(true) }

    LazyColumn(Modifier.fillMaxSize()) {
        // 顶栏
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AvenloTokens.TextPrimary)
                }
                Text("今日回顾", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(review.date, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary, modifier = Modifier.padding(end = 16.dp))
            }
        }

        // 今日最佳灵感（大照片卡：底部压暗 + 白色金句 + 标签行）
        item {
            Text("今日最佳灵感", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold, color = AvenloTokens.TextSecondary)
            Box(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(200.dp).clip(CardShape)
                    .then(if (review.bestIdea.ideaId.isNotEmpty()) Modifier.clickable { nav.navigate(Routes.detail(review.bestIdea.ideaId)) } else Modifier),
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.hotfix.avenlo.app.R.drawable.photo_seed_05),
                    contentDescription = "今日最佳灵感",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC33402F)))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text("“${review.bestIdea.quote}”", color = Color.White, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        review.bestIdea.tags.forEach { TagChip(it, tone = Color.White.copy(alpha = 0.85f)) }
                    }
                }
            }
        }

        // 意外关联（两卡配对 + 中间 🔗）
        item {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Link, null, tint = AvenloTokens.Success, modifier = Modifier.padding(start = 24.dp).size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("意外关联", fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold, color = AvenloTokens.TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(review.serendipity.desc, Modifier.padding(horizontal = 24.dp), fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SerendipityCard(review.serendipity.left, Modifier.weight(1f), tone = AvenloTokens.sageTone) { nav.navigate(Routes.detail(review.serendipity.left.ideaId)) }
                // 中间链条
                Box(Modifier.size(28.dp).align(Alignment.CenterVertically).clip(CircleShape).background(AvenloTokens.Warning), contentAlignment = Alignment.Center) {
                    Text("🔗", fontSize = 12.sp)
                }
                SerendipityCard(review.serendipity.right, Modifier.weight(1f), tone = AvenloTokens.blueTone) { nav.navigate(Routes.detail(review.serendipity.right.ideaId)) }
            }
        }

        // 明日待延展
        item {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = AvenloTokens.Primary, modifier = Modifier.padding(start = 24.dp).size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("明日待延展", fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold, color = AvenloTokens.TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            review.tomorrowDirections.forEachIndexed { i, d ->
                Row(
                    Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth()
                        .clip(CardShape).background(AvenloTokens.Surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(AvenloTokens.Primary), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(d.desc, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AvenloTokens.TextDisabled)
                }
            }
        }

        // 底部提醒开关
        item {
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(CardShape).background(AvenloTokens.Surface).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.NightsStay, null, tint = AvenloTokens.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("每日 21:00 提醒回顾", fontSize = AvenloTokens.FontSizeSm, modifier = Modifier.weight(1f))
                Switch(
                    checked = remindOn,
                    onCheckedChange = { remindOn = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AvenloTokens.Success),
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SerendipityCard(
    card: com.hotfix.avenlo.domain.model.DailyReview.Serendipity.PairCard,
    modifier: Modifier = Modifier,
    tone: com.hotfix.avenlo.app.ui.theme.AvenloTokens.ToneSet,
    onOpen: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(AvenloTokens.Surface)
            .then(if (card.ideaId.isNotEmpty()) Modifier.clickable { onOpen() } else Modifier),
    ) {
        Box(Modifier.fillMaxWidth().height(76.dp)) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    if (card.title == "城市与人") com.hotfix.avenlo.app.R.drawable.photo_seed_06
                    else com.hotfix.avenlo.app.R.drawable.photo_seed_07
                ),
                contentDescription = card.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.padding(10.dp)) {
            Text(card.title, fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(card.subtitle, fontSize = 11.sp, color = AvenloTokens.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(card.tag, fontSize = 10.sp, color = tone.badge,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(tone.bg).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}
