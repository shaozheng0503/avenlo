package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.components.tagTone
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.data.mock.SeedData

/** S3 灵感集：2×2 网格（浅主题色底 + 彩色圆形计数徽标 + 照片下半部 + 白圆箭头）+ 最近收录卡 +「+」新建；server 优先断网回落种子 */
@Composable
fun CollectionsScreen(nav: NavController) {
    var collections by remember { mutableStateOf(SeedData.collections) }
    val ideas by ServiceLocator.ideaRepo.observeIdeas().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ServiceLocator.collectionsRepo.getCollections()
            .onSuccess { if (it.isNotEmpty()) collections = it }
        ServiceLocator.ideaRepo.refresh()
    }
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
                Modifier.size(36.dp).clip(CircleShape).background(AvenloTokens.Primary).clickable {
                    newName = ""
                    showCreateDialog = true
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "新建灵感集", tint = Color.White) }
        }

        // 2×2 网格（固定高度项）+ 最近收录卡：用普通 Column 包 LazyRow 结构外滚动
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // ---- 2×2 网格（非 lazy，固定 2 行 4 个）----
            collections.take(4).chunked(2).forEach { rowCols ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowCols.forEach { col ->
                        Box(Modifier.weight(1f)) { CollectionCard(col, collections.indexOf(col), nav) }
                    }
                    // 补位保持网格形状
                    repeat(2 - rowCols.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // ---- 最近收录卡（新设计稿：最近收录·标题 + #tag chips，点击进对应灵感详情）----
            val latest = ideas.filter { it.status != com.hotfix.avenlo.domain.model.CardStatus.DELETED }
                .maxByOrNull { it.createdAt }
            if (latest != null) {
                Row(
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth()
                        .clip(CardShape).background(AvenloTokens.Surface)
                        .clickable { nav.navigate(com.hotfix.avenlo.app.ui.navigation.Routes.detail(latest.id)) }
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            "最近收录·${latest.title}",
                            fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold,
                            color = AvenloTokens.TextPrimary, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            latest.tags.take(2).forEach { TagChip(it, tone = tagTone(it)) }
                        }
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

    // 新建灵感集对话框（POST /collections，失败静默——Demo 容错）
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!creating) showCreateDialog = false },
            title = { Text("新建灵感集", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("名称") },
                    enabled = !creating,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            creating = true
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                ServiceLocator.collectionsRepo.createCollection(newName.trim())
                                    .onSuccess { col ->
                                        collections = listOf(col) + collections
                                        showCreateDialog = false
                                    }
                                creating = false
                            }
                        }
                    },
                    enabled = newName.isNotBlank() && !creating,
                ) { Text(if (creating) "创建中…" else "创建") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { if (!creating) showCreateDialog = false },
                    enabled = !creating,
                ) { Text("取消") }
            },
        )
    }
}

/** 单个灵感集卡片：上半浅主题色底（名称+副题+计数徽标）+ 下半照片 + 白圆箭头 */
@Composable
private fun CollectionCard(
    col: com.hotfix.avenlo.domain.model.Collection,
    index: Int,
    nav: NavController,
) {
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
    )[index % 4]
    Box(
        Modifier.height(160.dp).clip(CardShape).background(AvenloTokens.Surface).clickable {
            nav.navigate(com.hotfix.avenlo.app.ui.navigation.Routes.collectionDetail(col.id))
        },
    ) {
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
