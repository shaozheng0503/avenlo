package com.hotfix.avenlo.domain.repository

import com.hotfix.avenlo.domain.model.IdeaCard
import com.hotfix.avenlo.domain.model.CardStatus
import kotlinx.coroutines.flow.Flow

/** 卡片仓库接口 —— UI 不直接触库/触网，iOS 移植时同接口换实现 */
interface IdeaRepository {
    fun observeIdeas(): Flow<List<IdeaCard>>
    suspend fun getIdea(id: String): IdeaCard?
    suspend fun refresh(): Result<Unit>
    /** 长按捕捉结束后触发（本地生成 QUEUED 占位卡 → 后端回填） */
    suspend fun submitCapture(audioPath: String?, durationMs: Long): Result<String>
    suspend fun confirmIdea(id: String): Result<Unit>          // needs_review -> ok
    suspend fun deleteIdea(id: String): Result<Unit>           // 软删，30 天保留
    suspend fun feedbackRelation(id: String, relatedId: String, good: Boolean): Result<Unit>
}
