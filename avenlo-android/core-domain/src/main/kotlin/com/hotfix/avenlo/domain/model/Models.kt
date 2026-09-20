package com.hotfix.avenlo.domain.model

import kotlinx.serialization.Serializable

/** 灵感集（AI 自动聚合 + 手动新建，阻断项 #2：collection_name 字段定稿） */
@Serializable
data class Collection(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val count: Int = 0,
    val coverUrl: String? = null,
    /** 四色轮换：sage(绿) / peach(桃) / gold(金) / blue(蓝)，对应 S3 卡片上半部主题色 */
    val tone: Tone = Tone.SAGE,
    val manual: Boolean = false,   // true = 用户手动新建（右上「+」）
) {
    @kotlinx.serialization.Serializable
    enum class Tone {
        @kotlinx.serialization.SerialName("sage") SAGE,
        @kotlinx.serialization.SerialName("peach") PEACH,
        @kotlinx.serialization.SerialName("gold") GOLD,
        @kotlinx.serialization.SerialName("blue") BLUE,
    }
}

/** 今日回顾（S5）—— Demo 叙事收尾页 */
@Serializable
data class DailyReview(
    val date: String,                       // 「2025年9月29日·周一」
    val bestIdea: BestIdea,
    val serendipity: Serendipity,
    val tomorrowDirections: List<Direction>,
) {
    @Serializable
    data class BestIdea(
        val quote: String,                  // 白色金句
        val tags: List<String>,
        val coverUrl: String? = null,
    )

    @Serializable
    data class Serendipity(
        val desc: String,                   // 「两个想法在时间与空间上形成了有趣的关联」
        val left: PairCard,
        val right: PairCard,
    ) {
        @Serializable
        data class PairCard(
            val title: String,
            val subtitle: String,
            val tag: String,
            val coverUrl: String? = null,
        )
    }

    @Serializable
    data class Direction(val title: String, val desc: String)
}

/** 我的页（S6）统计与戒指状态 */
data class UserProfile(
    val name: String = "Runel",
    val slogan: String = "记录，让生活更有方向",
    val ringBattery: Int = 92,
    val ringConnected: Boolean = true,
    val statsIdeas: Int = 12,
    val statsCollections: Int = 86,
    val statsMinutes: Int = 5,
)
