package com.hotfix.avenlo.domain.model

import kotlinx.serialization.Serializable

/**
 * Idea Card V2.1 —— 与 Mock Server 契约一致（M1 冻结版）。
 *
 * status 五状态与戒指端状态机一一对应：
 *  - ok           正常，已整理完成
 *  - needs_review 识别置信度低，卡片显示「待确认」
 *  - queued       无网络/AI 排队中，灰态「整理中」
 *  - syncing      戒指本地 FIFO 待回传（端上状态，服务端不产生）
 *  - deleted      最近删除（30 天保留）
 */
@Serializable
data class IdeaCard(
    val id: String,
    val title: String,
    val summary: String,
    val transcript: String,
    val tags: List<String> = emptyList(),
    val status: CardStatus = CardStatus.OK,
    val related: List<RelatedRef> = emptyList(),
    val extension: Extension = Extension(),
    val collectionId: String? = null,
    val audioUrl: String? = null,
    val createdAt: Long = 0L,   // epoch millis
    val durationMs: Long = 0L,
) {
    @Serializable
    data class RelatedRef(
        val id: String,
        val title: String,
        val relation: String,          // same_collection / similar_theme / time_space
        val coverUrl: String? = null,
        val durationMs: Long = 0L,
        val tag: String? = null,
    )

    @Serializable
    data class Extension(
        val perspectives: List<ExtItem> = emptyList(),
        val references: List<ReferenceItem> = emptyList(),
        val directions: List<ExtItem> = emptyList(),
    )

    @Serializable
    data class ExtItem(val title: String, val desc: String = "")

    /** 阻断项 #2 定稿：references 明确为 {title, url} 数组 */
    @Serializable
    data class ReferenceItem(val title: String, val url: String)
}

@Serializable
enum class CardStatus { OK, NEEDS_REVIEW, QUEUED, SYNCING, DELETED }
