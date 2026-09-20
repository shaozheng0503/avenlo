package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.data.mock.SeedData

/** S3 灵感集：2×2 网格（浅主题色底 + 彩色圆形计数徽标 + 照片下半部 + 白圆箭头）+「+」新建 */
@Composable
fun CollectionsScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        // 页头（顶级页：无返回箭头）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("灵感集", fontSize = AvenloTokens.FontSize2xl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("AI自动归类的主题", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
            }
            // 右上「+」手动新建（阻断项 #2 定稿：POST /collections）
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AvenloTokens.Primary).clickable { /* TODO: 新建对话框 */ },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "新建灵感集", tint = Color.White) }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(SeedData.collections, key = { it.id }) { col ->
                val tone = when (col.tone) {
                    com.hotfix.avenlo.domain.model.Collection.Tone.SAGE -> AvenloTokens.sageTone
                    com.hotfix.avenlo.domain.model.Collection.Tone.PEACH -> AvenloTokens.peachTone
                    com.hotfix.avenlo.domain.model.Collection.Tone.GOLD -> AvenloTokens.goldTone
                    com.hotfix.avenlo.domain.model.Collection.Tone.BLUE -> AvenloTokens.blueTone
                }
                val photoRes = listOf(
                    com.hotfix.avenlo.app.R.drawable.photo_seed_01,
                    com.hotfix.avenlo.app.R.drawable.photo_seed_02,
                    com.hotfix.avenlo.app.R.drawable.photo_seed_03,
                    com.hotfix.avenlo.app.R.drawable.photo_seed_04,
                )[SeedData.collections.indexOf(col) % 4]
                Box(Modifier.height(160.dp).clip(CardShape).background(AvenloTokens.Surface)) {
                    Column {
                        // 上半部：浅主题色底 + 主题名 + 副题
                        Column(
                            Modifier.fillMaxWidth().weight(1.2f).background(tone.bg).padding(12.dp)
                        ) {
                            Box(Modifier.fillMaxWidth()) {
                                Text(col.name, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold, color = tone.text)
                                // 右上角彩色圆形计数徽标
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape).background(tone.badge),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${col.count}", color = Color.White, fontSize = AvenloTokens.FontSizeXs, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(col.subtitle, fontSize = AvenloTokens.FontSizeXs, color = tone.text.copy(alpha = 0.7f), maxLines = 1)
                        }
                        // 下半部：照片区（.fig 内嵌实景照片）
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(photoRes),
                            contentDescription = col.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                    // 右下白色圆形 → 按钮
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(10.dp).size(26.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, "打开", tint = tone.badge, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        // 页尾标语 + 叶子装饰
        Row(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Eco, null, tint = AvenloTokens.Success, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("收藏生活中的每一个灵感", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
        }
    }
}
