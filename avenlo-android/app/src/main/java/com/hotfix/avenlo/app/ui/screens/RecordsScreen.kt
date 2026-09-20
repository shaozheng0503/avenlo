package com.hotfix.avenlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.ui.navigation.Routes
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 记录 Tab（S1 复用）：全部灵感按日期分组的时间线视图（方案 8.1 #1 定稿） */
@Composable
fun RecordsScreen(nav: NavController) {
    val repo = ServiceLocator.ideaRepo
    val ideas by repo.observeIdeas().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { repo.refresh() }

    // 按自然日分组（降序）
    val grouped = ideas
        .filter { it.status != com.hotfix.avenlo.domain.model.CardStatus.DELETED }
        .sortedByDescending { it.createdAt }
        .groupBy { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date(it.createdAt)) }

    Column(Modifier.fillMaxSize()) {
        // 页头
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column {
                Text("记录", fontSize = AvenloTokens.FontSize2xl, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("按日期回看你的灵感", fontSize = AvenloTokens.FontSizeSm, color = AvenloTokens.TextSecondary)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            grouped.forEach { (dateLabel, cards) ->
                item(key = "header_$dateLabel") {
                    Text(
                        "${dateLabel} · ${cards.size}条",
                        Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        fontSize = AvenloTokens.FontSizeSm,
                        fontWeight = FontWeight.Bold,
                        color = AvenloTokens.TextSecondary,
                    )
                }
                items(cards, key = { it.id }) {
                    HomeIdeaCard(it) { nav.navigate(Routes.detail(it.id)) }
                }
            }
        }
    }
}
