package com.hotfix.avenlo.data.mock

import com.hotfix.avenlo.domain.model.*
// 命名冲突规避：kotlin.collections.Collection 是默认导入（优先级高于 star import），
// 灵感集模型必须走别名。
import com.hotfix.avenlo.domain.model.Collection as IdeaCollection
import kotlin.math.abs

/**
 * 预灌种子数据 —— 文案全部来自 .fig 源文件提取（方案 1.4.2），
 * 演示数据 ≠ 造假：链路真实，数据为赛前调优的种子。
 */
object SeedData {

    private val now = System.currentTimeMillis()

    val ideas: List<IdeaCard> = listOf(
        idea(
            id = "idea_01", title = "关于旅行的灵感",
            summary = "在陌生城市中通过观察、聆听和日常邂逅取得新的视角。",
            tags = listOf("旅行", "生活"),
            time = "08:22", durationMs = 136_000, iconTone = 0,
            related = listOf(
                IdeaCard.RelatedRef("idea_11", "清晨的露水", "similar_theme", durationMs = 22_000, tag = "自然"),
                IdeaCard.RelatedRef("idea_12", "咖啡馆与工作", "same_collection", durationMs = 560_000, tag = "创作"),
                IdeaCard.RelatedRef("idea_13", "山野徒步", "similar_theme", tag = "户外"),
            ),
        ),
        idea(
            id = "idea_02", title = "写作素材：时间与记忆",
            summary = "时间的流逝感与记忆的选择性保存，可以作为叙事的双线结构。",
            tags = listOf("灵感", "日记"),
            time = "10:38", durationMs = 232_000, iconTone = 1,
        ),
        idea(
            id = "idea_03", title = "生活片段·咖啡馆",
            summary = "咖啡馆里的声音层次：磨豆、低语、杯碟——城市白噪音的样本。",
            tags = listOf("创作", "美食"),
            time = "11:17", durationMs = 96_000, iconTone = 2,
        ),
        idea(
            id = "idea_04", title = "城市与人",
            summary = "城市废墟中的纹理与光影，人与空间的相互塑造。",
            tags = listOf("旅行", "摄影"),
            time = "前天", durationMs = 321_000, iconTone = 0, daysAgo = 2,
        ),
        // 更早分组
        idea("idea_05", "夜骑的城市观察", "晚间骑行时城市灯光的节奏变化。", listOf("城市", "观察"), "3天前", 154_000, 1, daysAgo = 3),
        idea("idea_06", "播客选题：慢生活", "与「慢生活」主张者对谈的系列选题。", listOf("播客", "创作"), "5天前", 208_000, 2, daysAgo = 5),
        idea("idea_07", "菜市场的人类学", "菜市场作为城市公共空间的样本。", listOf("生活", "观察"), "上周", 187_000, 0, daysAgo = 8),
        idea("idea_08", "一张照片的配色", "废墟场景的低饱和暖色配色方案。", listOf("摄影", "灵感"), "上周", 45_000, 1, daysAgo = 9),
        // related 引用落卡（与 server seed.py 对齐；idea_01 的 RELATED_TRAVEL 指向这三张）
        idea("idea_11", "清晨的露水", "晨间露珠的微观摄影与光影观察。", listOf("自然", "摄影"), "2天前", 22_000, 2, daysAgo = 2),
        idea("idea_12", "咖啡馆与工作", "咖啡馆作为第三空间的工位选择与观察笔记。", listOf("创作", "生活"), "2天前", 560_000, 0, daysAgo = 2),
        idea("idea_13", "山野徒步", "城郊徒步路线的体力分配与风景节奏。", listOf("户外", "旅行"), "3天前", 315_000, 1, daysAgo = 3),
    )

    /** 详情页（idea_01）的 AI 摘要与延展 —— .fig 实读文案 */
    val detailOfIdea01 = IdeaCard(
        id = "idea_01", title = "关于旅行的灵感",
        summary = "这条灵感表达了「旅行中的慢生活」的体验。强调在陌生城市中通过观察、聆听和日常邂逅取得新的视角，它可以延展到产品设计、生活方式内容与品牌故事的方向。",
        transcript = "今天在巷子里走了很久，观察当地人的日常……",
        tags = listOf("旅行", "生活"),
        status = CardStatus.OK,
        related = ideas[0].related,
        extension = IdeaCard.Extension(
            perspectives = listOf(
                IdeaCard.ExtItem("旅行 × 当地文化体验", "与当地人交流并参与文化活动，打破游客视角的旅行方式"),
                IdeaCard.ExtItem("旅行与内容创作结合", "将旅行中的体验转化为图文、视频、播客等内容的可能"),
                IdeaCard.ExtItem("轻量化旅行用品设计", "面向「轻旅行」人群，设计更轻便、更灵活的旅行用品"),
            ),
            references = listOf(
                IdeaCard.ReferenceItem("The Way We Travel", "https://example.com/way-we-travel"),
                IdeaCard.ReferenceItem("慢城市主义：以人的尺度重建街道", "https://example.com/slow-urbanism"),
                IdeaCard.ReferenceItem("设计中的观察法", "https://example.com/observation-in-design"),
            ),
            directions = listOf(
                IdeaCard.ExtItem("城市观察笔记系列内容", "把「观察—记录—延展」做成栏目"),
                IdeaCard.ExtItem("旅行主题灵感集运营", "沉淀为可分享的灵感集合"),
            ),
        ),
        collectionId = "col_02f",
        createdAt = now - 6 * 3600_000,
        durationMs = 136_000,
    )

    val collections = listOf(
        IdeaCollection("col_01", "旅行灵感", "关于远方、路上与不同的生活方式", 12, tone = IdeaCollection.Tone.SAGE),
        IdeaCollection("col_02", "项目构思", "创意想法与产品方向", 5, tone = IdeaCollection.Tone.PEACH),
        IdeaCollection("col_03", "晨间随想", "清晨的灵感与碎片", 15, tone = IdeaCollection.Tone.GOLD),
        IdeaCollection("col_04", "阅读笔记", "书籍、文章与知识沉淀", 7, tone = IdeaCollection.Tone.BLUE),
    )

    val dailyReview = DailyReview(
        date = java.text.SimpleDateFormat("yyyy年M月d日·EEE", java.util.Locale.CHINESE).format(java.util.Date()),
        bestIdea = DailyReview.BestIdea(
            quote = "真正的放松不是躺平，而是把注意力收回来，转回当下的感受。",
            tags = listOf("生活", "心情", "旅行"),
            ideaId = "idea_01",  // 关于旅行的灵感（与 server 口径一致）
        ),
        serendipity = DailyReview.Serendipity(
            desc = "两个想法在时间与空间上形成了有趣的关联",
            left = DailyReview.Serendipity.PairCard("城市与人", "城市废墟 × 纹理光影", "旅行", ideaId = "idea_04"),
            right = DailyReview.Serendipity.PairCard("夜骑的城市观察", "夜骑 × 街头观察", "城市", ideaId = "idea_05"),
        ),
        tomorrowDirections = listOf(
            DailyReview.Direction("城市中的自然疗愈空间", "在高密度城市里创造自然疗愈的角落"),
            DailyReview.Direction("旅行与工作方式的融合", "是否可以设计一种新的远程工作场景？"),
        ),
    )

    val searchHistory = listOf(
        SearchHistoryEntry("东东西设计灵感", "00:28", listOf("#设计", "#旅行灵感")),
        SearchHistoryEntry("人像摄影构图技巧", "07:25", listOf("#摄影", "#人物")),
        SearchHistoryEntry("明天的待办事项", "07:02", listOf("#待办", "#生活")),
    )

    data class SearchHistoryEntry(val text: String, val time: String, val tags: List<String>)

    private fun idea(
        id: String, title: String, summary: String, tags: List<String>,
        time: String, durationMs: Long, iconTone: Int, daysAgo: Long = 0,
        related: List<IdeaCard.RelatedRef> = emptyList(),
    ) = IdeaCard(
        id = id, title = title, summary = summary, transcript = summary,
        tags = tags, status = CardStatus.OK, related = related,
        createdAt = now - daysAgo * 86_400_000 - abs(title.hashCode()) % 3600_000,
        durationMs = durationMs,
    )
}
