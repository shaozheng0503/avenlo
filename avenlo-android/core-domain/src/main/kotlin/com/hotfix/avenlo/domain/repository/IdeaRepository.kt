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
    /**
     * 补交滞留的占位卡（第四十轮 M3-lite 重试队列）。
     * 断网捕捉的卡停在「整理中」——网络恢复后由 refresh() 自动调用，
     * 用留存的原始参数重新提交 server；返回补交成功数。
     * 已知边界：进程重启后队列丢失（Room 落地解决）。
     */
    suspend fun retryPending(): Result<Int>
    suspend fun confirmIdea(id: String): Result<Unit>          // needs_review -> ok
    suspend fun deleteIdea(id: String): Result<Unit>           // 软删，30 天保留
    suspend fun feedbackRelation(id: String, relatedId: String, good: Boolean): Result<Unit>
}
