package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    // 滚动状态 + 协程（「回到最新」胶囊：滚动列下滚后出现在底栏上方）
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
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
        Column(Modifier.weight(1f).verticalScroll(scrollState)) {
            // ---- 2×2 网格（非 lazy，固定 2 行 4 个）----
            collections.take(4).chunked(2).forEach { rowCols ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowCols.forEach { col ->
                        Box(Modifier.weight(1f)) { CollectionCard(col, nav) }
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

    // 「回到最新」胶囊（新设计稿 v3 fig 精确数据：灵感集屏底栏上方居中，
    // 米白底 #F7F5F1 + 深灰字 #4D4B47，188×66px@2x ≈ 94×33dp 内容盒，字 ≈16sp）
    if (scrollState.value > 120) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .shadow(3.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF7F5F1))
                    .clickable { scope.launch { scrollState.animateScrollTo(0) } }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp, null,
                    tint = Color(0xFF4D4B47), modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("回到最新", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4D4B47))
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

/** 单个灵感集卡片（新 Hotfix.fig 精确版）：整卡浅主题色纯色块（217×164px@3x ≈ 158×120dp 圆角12），
 *  左上主题名（22sp 加粗深字）+ 副题两行（16sp）+ 右上计数纯数字（22sp，无徽标无照片无箭头） */
@Composable
private fun CollectionCard(
    col: com.hotfix.avenlo.domain.model.Collection,
    nav: NavController,
) {
    val tone = when (col.tone) {
        com.hotfix.avenlo.domain.model.Collection.Tone.SAGE -> AvenloTokens.sageTone
        com.hotfix.avenlo.domain.model.Collection.Tone.PEACH -> AvenloTokens.peachTone
        com.hotfix.avenlo.domain.model.Collection.Tone.GOLD -> AvenloTokens.goldTone
        com.hotfix.avenlo.domain.model.Collection.Tone.BLUE -> AvenloTokens.blueTone
    }
    Box(
        Modifier.height(150.dp).fillMaxWidth().clip(CardShape).background(tone.bg).clickable {
            nav.navigate(com.hotfix.avenlo.app.ui.navigation.Routes.collectionDetail(col.id))
        },
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Text(col.name, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, color = tone.text)
                // 右上角计数：纯数字（fig：22sp 深字，无圆形底）
                Text(
                    "${col.count}", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold,
                    fontFamily = com.hotfix.avenlo.app.ui.theme.InterFont,
                    color = tone.text, modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Spacer(Modifier.height(6.dp))
            // 副题两行（fig：16sp，7×0.7 透明度副字色）
            Text(
                col.subtitle, fontSize = AvenloTokens.FontSizeBase, lineHeight = 22.sp,
                color = tone.text.copy(alpha = 0.7f), maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
