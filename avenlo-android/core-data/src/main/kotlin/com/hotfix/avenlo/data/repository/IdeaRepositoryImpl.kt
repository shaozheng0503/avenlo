package com.hotfix.avenlo.data.repository

import com.hotfix.avenlo.data.remote.AvenloApi
import com.hotfix.avenlo.domain.model.CardStatus
import com.hotfix.avenlo.domain.model.IdeaCard
import com.hotfix.avenlo.domain.repository.IdeaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存态 Repository（M3 接 Room）。
 * 单用户低频写入，Demo 阶段不做冲突合并（方案 8.2 既定决策）。
 */
class IdeaRepositoryImpl(
    private val api: AvenloApi,
    seed: List<IdeaCard> = emptyList(),
) : IdeaRepository {

    private val mutex = Mutex()
    private val ideas = MutableStateFlow(seed.sortedByDescending { it.createdAt })

    companion object {
        private const val LOCAL_ID_PREFIX = "idea_local_"
    }

    override fun observeIdeas(): Flow<List<IdeaCard>> = ideas

    override suspend fun getIdea(id: String): IdeaCard? =
        ideas.value.firstOrNull { it.id == id } ?: runCatching { api.getIdea(id) }.getOrNull()

    override suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.listIdeas()
        mutex.withLock {
            // 合并：远端列表 + 保留本地占位卡（断网兜底：未确认送达服务端的卡不丢）
            val localPlaceholders = ideas.value.filter { it.id.startsWith(LOCAL_ID_PREFIX) }
            ideas.value = (remote + localPlaceholders).distinctBy { it.id }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun submitCapture(audioPath: String?, durationMs: Long): Result<String> = runCatching {
        // 1) 本地先落 QUEUED 占位卡（断网兜底：无网时它留在列表里显示「整理中」）
        val localId = "${LOCAL_ID_PREFIX}${System.currentTimeMillis()}"
        val placeholder = IdeaCard(
            id = localId,
            title = "整理中…",
            summary = "",
            transcript = "",
            status = CardStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
        )
        mutex.withLock { ideas.value = listOf(placeholder) + ideas.value }

        // 2) 提交后端 → 成功后用服务端卡片替换占位卡
        val resp = api.submitCapture(
            AvenloApi.CaptureRequest(
                ts = System.currentTimeMillis(),
                durationMs = durationMs,
                gestures = listOf(AvenloApi.CaptureRequest.Gesture(0, "pinch"), AvenloApi.CaptureRequest.Gesture(durationMs, "pinch")),
                audioUrl = audioPath,
            )
        )
        val serverCard = api.getIdea(resp.ideaId)
        mutex.withLock {
            ideas.value = ideas.value.map { if (it.id == localId) serverCard else it }
        }
        resp.ideaId
    }

    override suspend fun confirmIdea(id: String): Result<Unit> = runCatching {
        api.confirmIdea(id)
        mutex.withLock {
            ideas.value = ideas.value.map {
                if (it.id == id) it.copy(status = CardStatus.OK) else it
            }
        }
    }

    override suspend fun deleteIdea(id: String): Result<Unit> = runCatching {
        api.deleteIdea(id)
        mutex.withLock { ideas.value = ideas.value.filterNot { it.id == id } }
    }

    override suspend fun feedbackRelation(id: String, relatedId: String, good: Boolean): Result<Unit> = runCatching {
        api.feedbackRelation(id, relatedId, good)
    }
}
