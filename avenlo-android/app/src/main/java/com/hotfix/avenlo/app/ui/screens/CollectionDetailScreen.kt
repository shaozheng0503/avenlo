package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.components.TagChip
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.domain.model.IdeaCard
import kotlinx.coroutines.flow.first

/**
 * 灵感集内页（P2 补全，第三十三轮）：列该集的卡。
 * 归属规则（与 mock server 口径一致）：collectionId 精确匹配优先；
 * 无归属数据的卡按语义标签映射兜底（旅行灵感→旅行/户外；项目构思→创作/灵感；
 * 晨间随想→自然/观察；阅读笔记→日记/摄影）——种子 10/11 卡无 collectionId，
 * 映射保证每个灵感集点开都非空。
 */
@Composable
fun CollectionDetailScreen(nav: NavController, collectionId: String) {
    var title by remember { mutableStateOf("灵感集") }
    var subtitle by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<IdeaCard>>(emptyList()) }

    LaunchedEffect(collectionId) {
        ServiceLocator.collectionsRepo.getCollections()
            .onSuccess { cols ->
                cols.firstOrNull { it.id == collectionId }?.let {
                    title = it.name
                    subtitle = it.subtitle
                }
            }
        val all = ServiceLocator.ideaRepo.observeIdeas().first()
        cards = filterForCollection(all, collectionId)
    }

    Column(Modifier.fillMaxSize()) {
        // 顶栏（二级页：带返回）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AvenloTokens.TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = AvenloTokens.FontSizeXs, color = AvenloTokens.TextSecondary)
                }
            }
            Text("${cards.size}条", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary, modifier = Modifier.padding(end = 16.dp))
        }

        LazyColumn {
            if (cards.isEmpty()) {
                item {
                    Text(
                        "还没有归属这个灵感集的想法",
                        Modifier.fillMaxWidth().padding(48.dp),
                        fontSize = AvenloTokens.FontSizeSm,
                        color = AvenloTokens.TextDisabled,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                items(cards.size) { i ->
                    HomeIdeaCard(cards[i]) { nav.navigate(Routes.detail(cards[i].id)) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/** collectionId 精确匹配 + 语义标签兜底（种子数据无归属时的 Demo 完整性） */
private fun filterForCollection(all: List<IdeaCard>, collectionId: String): List<IdeaCard> {
    val exact = all.filter { it.collectionId == collectionId }
    val tags = when (collectionId) {
        "col_01" -> listOf("旅行", "户外")
        "col_02" -> listOf("创作", "灵感")
        "col_03" -> listOf("自然", "观察", "城市")
        "col_04" -> listOf("日记", "摄影", "播客")
        else -> return exact
    }
    val semantic = all.filter { card -> card.tags.any { it in tags } && card.collectionId == null }
    return (exact + semantic).distinctBy { it.id }.sortedByDescending { it.createdAt }
}
