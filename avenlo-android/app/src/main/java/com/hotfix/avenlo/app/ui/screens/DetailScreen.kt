package com.hotfix.avenlo.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.app.ui.theme.CardShape
import com.hotfix.avenlo.app.ui.theme.ThumbShape
import com.hotfix.avenlo.data.mock.SeedData
import com.hotfix.avenlo.domain.model.IdeaCard
import kotlinx.coroutines.launch

/** S2 灵感详情：AI 摘要 → 相关想法横滑 → 延展思路折叠 → 参考资源（纵向长页） */
@Composable
fun DetailScreen(nav: NavController, ideaId: String) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())
    val card = ideas.firstOrNull { it.id == ideaId }?.let { mergeDetail(it) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (card == null) {
        // 悬空引用（如 related 里的 idea_11/12/13 server 未落卡）——空态而非错误兜底
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AvenloTokens.TextPrimary)
                }
                Text("灵感详情", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("这条灵感还在整理中", fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold, color = AvenloTokens.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("相关引用暂未生成完整卡片", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextDisabled)
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        // ---- 顶栏 ----
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AvenloTokens.TextPrimary)
                }
                Text("灵感详情", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                // 待确认卡（识别失败兜底）：一键确认转 ok（契约 POST /ideas/{id}/confirm）
                if (card.status == com.hotfix.avenlo.domain.model.CardStatus.NEEDS_REVIEW) {
                    Text(
                        "待确认",
                        fontSize = AvenloTokens.FontSizeXs,
                        color = AvenloTokens.Warning,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(AvenloTokens.Warning.copy(alpha = 0.14f))
                            .clickable {
                                scope.launch {
                                    repo.confirmIdea(ideaId)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Filled.MoreHoriz, "更多", tint = AvenloTokens.TextSecondary)
                }
            }
        }

        // ---- 更多菜单（删除入口） ----
        if (showMenu) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .clip(CardShape).background(AvenloTokens.Surface).padding(6.dp),
                ) {
                    Text(
                        "删除这条灵感",
                        fontSize = AvenloTokens.FontSizeSm,
                        color = AvenloTokens.Error,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showMenu = false
                                showDeleteDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }

        // ---- 删除确认对话框 ----
        if (showDeleteDialog) {
            item {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("删除这条灵感？") },
                    text = { Text("删除后可在「最近删除」中保留 30 天（服务端软删）。") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                repo.deleteIdea(ideaId)
                                nav.popBackStack()
                            }
                        }) { Text("删除", color = AvenloTokens.Error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                    },
                )
            }
        }

        // ---- AI 摘要卡 ----
        item {
            Row(
                Modifier.padding(horizontal = 24.dp).clip(CardShape).background(AvenloTokens.Surface).padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Lightbulb, null, tint = AvenloTokens.Warning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AI 摘要", fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Bold, color = AvenloTokens.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(card.summary, fontSize = AvenloTokens.FontSizeSm, lineHeight = 22.sp, color = AvenloTokens.TextSecondary)
                    // 音频回听（通用圆钮+进度条，方案 8.2 既定）
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(AvenloTokens.Primary),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.PlayArrow, "播放", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(AvenloTokens.Border))
                        Spacer(Modifier.width(8.dp))
                        Text(formatDuration(card.durationMs), fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                    }
                }
            }
        }

        // ---- 相关想法（横滑）----
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("相关想法", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("共${card.related.size}条 ›", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(card.related, key = { it.id }) { rel ->
                    val photo = listOf(
                        com.hotfix.avenlo.app.R.drawable.photo_seed_08,
                        com.hotfix.avenlo.app.R.drawable.photo_seed_09,
                        com.hotfix.avenlo.app.R.drawable.photo_seed_03,
                    )[card.related.indexOf(rel) % 3]
                    Column(
                        Modifier
                            .width(104.dp)
                            .clip(ThumbShape)
                            .background(AvenloTokens.Surface)
                            .clickable { nav.navigate(com.hotfix.avenlo.app.ui.navigation.Routes.detail(rel.id)) }
                            .padding(10.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(64.dp).clip(ThumbShape)) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(photo),
                                contentDescription = rel.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(rel.title, fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rel.tag?.let { TagChip(it) }
                        }
                        rel.durationMs.takeIf { it > 0 }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(formatDuration(it), fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(relationLabel(rel.relation), fontSize = AvenloTokens.FontSizeXs, color = toneForRelated(rel.relation))
                    }
                }
            }
        }

        // ---- 延展思路（折叠列表）----
        item {
            Spacer(Modifier.height(24.dp))
            Text("延展思路", Modifier.padding(horizontal = 24.dp), fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            card.extension.perspectives.forEachIndexed { i, ext ->
                FoldItem(index = i, title = ext.title, desc = ext.desc)
            }
        }

        // ---- 参考资源 ----
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("参考资源", fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("立即全部 ›", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            card.extension.references.forEach { ref ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(CardShape).background(AvenloTokens.Surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(AvenloTokens.blueTone.bg), contentAlignment = Alignment.Center) {
                        Text("🔗", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ref.title, fontSize = AvenloTokens.FontSizeSm, fontWeight = FontWeight.Medium)
                        Text(ref.url, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextDisabled, maxLines = 1)
                    }
                    Text("›", color = AvenloTokens.TextDisabled)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

/** 列表态卡片 → 详情态：server 数据优先（有 extension/related 直接用）；
 *  本地占位卡（无网络提交的 local_ 前缀 id）或字段为空时回落种子延展数据 */
private fun mergeDetail(card: IdeaCard): IdeaCard {
    if (card.id == "idea_01") {
        // server 版 idea_01 自带完整 extension + related（seed.py 定义）
        val hasServerData = card.extension.perspectives.isNotEmpty() || card.related.isNotEmpty()
        return if (hasServerData) {
            card.copy(related = card.related.ifEmpty { SeedData.detailOfIdea01.related })
        } else {
            // 断网兜底：本地种子的 idea_01 没带 extension
            SeedData.detailOfIdea01.copy(related = card.related.ifEmpty { SeedData.detailOfIdea01.related })
        }
    }
    // 非种子卡：server 未产出 extension 时生成默认延展（新捕捉卡 pipeline 只填 title/summary/tags）
    return if (card.extension.perspectives.isEmpty() && card.extension.directions.isEmpty()) {
        card.copy(extension = defaultExtension(card))
    } else {
        card
    }
}

private fun defaultExtension(card: IdeaCard) = IdeaCard.Extension(
    perspectives = listOf(
        IdeaCard.ExtItem("${card.tags.firstOrNull() ?: "主题"} × 内容创作", "将这条灵感转化为栏目化的内容"),
        IdeaCard.ExtItem("${card.tags.lastOrNull() ?: "主题"}社群实践", "找到同好社群，把想法落地为活动"),
    ),
    references = listOf(
        IdeaCard.ReferenceItem("相关主题精选", "https://example.com/related"),
    ),
)

private fun toneForRelated(relation: String) = when (relation) {
    "same_collection" -> AvenloTokens.Success
    "similar_theme" -> AvenloTokens.Primary
    else -> AvenloTokens.Warning
}

/** 关联原因文案（与 toneForRelated 同源配色）：让「为什么关联」可解释 */
private fun relationLabel(relation: String) = when (relation) {
    "same_collection" -> "同灵感集"
    "similar_theme" -> "相似主题"
    "time_space" -> "时间与空间关联"
    else -> "关联"
}

/** 延展思路折叠项：数字圆标（绿/金/红轮换）+ 标题 + 展开箭头 + 说明 */
@Composable
private fun FoldItem(index: Int, title: String, desc: String) {
    var expanded by remember { mutableStateOf(false) }
    val tone = listOf(AvenloTokens.Success, AvenloTokens.Warning, AvenloTokens.Error)[index % 3]
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(CardShape).background(AvenloTokens.Surface).clickable { expanded = !expanded }.padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, if (expanded) "收起" else "展开", tint = AvenloTokens.TextDisabled)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 34.dp, top = 8.dp)) {
                Text(desc, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
            }
        }
    }
}
