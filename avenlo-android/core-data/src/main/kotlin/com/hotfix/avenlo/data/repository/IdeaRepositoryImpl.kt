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
 * 内存态 Repository + 文件持久化重试队列（第四十五轮）。
 * 单用户低频写入，Demo 阶段不做冲突合并（方案 8.2 既定决策）。
 *
 * 第四十轮（M3-lite）：断网捕捉的占位卡入内存 pending 队列，refresh() 成功即自动补交。
 * 第四十五轮（M3-lite+）：pending 队列 + 占位卡经 PendingStore 落盘——
 * **进程被杀后重启，队列与「整理中」占位卡完整恢复**，网络恢复照常补交。
 */
class IdeaRepositoryImpl(
    private val api: AvenloApi,
    seed: List<IdeaCard> = emptyList(),
    private val store: PendingStore? = null,   // null = 纯内存（单测/无 Context 场景）
) : IdeaRepository {

    private val mutex = Mutex()
    private val ideas = MutableStateFlow(seed.sortedByDescending { it.createdAt })

    /** 第四十轮 M3-lite 重试队列：占位卡 id → 原始提交参数（断网补交用） */
    private data class PendingCapture(val audioPath: String?, val durationMs: Long, val createdAt: Long)
    private var pending = LinkedHashMap<String, PendingCapture>()

    companion object {
        private const val LOCAL_ID_PREFIX = "idea_local_"
    }

    /** 第四十五轮：构造时恢复持久化队列 + 重建占位卡（suspend——ServiceLocator.init 需 runBlocking 包裹） */
    suspend fun restorePending() {
        val s = store ?: return
        val entries = s.load()
        if (entries.isEmpty()) return
        mutex.withLock {
            for (e in entries) {
                pending[e.localId] = PendingCapture(e.audioPath, e.durationMs, e.createdAt)
                // 重建占位卡（仅当列表里没有——正常补交完成后不会留队）
                if (ideas.value.none { it.id == e.localId }) {
                    ideas.value = listOf(e.toPlaceholder()) + ideas.value
                }
            }
        }
    }

    private fun PendingStore.PendingEntry.toPlaceholder() = IdeaCard(
        id = localId,
        title = "整理中…",
        summary = "",
        transcript = "",
        status = CardStatus.QUEUED,
        createdAt = createdAt,
        durationMs = durationMs,
    )

    /** 队列变更后落盘（失败不阻断主流程——最多退回第四十轮的进程内语义） */
    private suspend fun persist() {
        store ?: return
        val snapshot = mutex.withLock {
            pending.map { (id, p) -> PendingStore.PendingEntry(id, p.audioPath, p.durationMs, p.createdAt) }
        }
        store.save(snapshot)
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
        // 网络已恢复（listIdeas 成功）→ 自动补交滞留占位卡（第四十轮 M3-lite）
        // 注意：补交失败不影响 refresh 本身（列表已成功拉取），滞留卡留队列下次再试
        retryPending()
        Unit
    }

    override suspend fun retryPending(): Result<Int> = runCatching {
        // 快照当前待补交项（避免遍历时并发修改）；逐条用原始参数重提交
        val snapshot: List<Pair<String, PendingCapture>> = mutex.withLock { pending.toList() }
        var submitted = 0
        for ((localId, p) in snapshot) {
            val resp = try {
                submitOnce(p.audioPath, p.durationMs)
            } catch (t: Throwable) {
                null   // 网络仍未恢复/单条失败：留在队列里下次再试
            } ?: continue
            mutex.withLock {
                pending.remove(localId)
            }
            persist()
            // 与 submitCapture 相同：服务端卡替换本地占位卡
            val serverCard = try { api.getIdea(resp) } catch (t: Throwable) { null }
            if (serverCard != null) {
                mutex.withLock {
                    ideas.value = ideas.value.map { if (it.id == localId) serverCard else it }
                }
            }
            submitted++
        }
        submitted
    }

    /** 单次提交（submitCapture 与 retryPending 共用）；返回服务端 ideaId */
    private suspend fun submitOnce(audioPath: String?, durationMs: Long): String {
        // 音频直传 server（真机链路：STT 在 server 侧读文件）；失败回落本地路径字符串（同机 Demo 可读）
        val audioUrl: String? = audioPath?.let { path ->
            runCatching { api.uploadAudio(java.io.File(path)).audioUrl }.getOrDefault(path)
        }
        val resp = api.submitCapture(
            AvenloApi.CaptureRequest(
                ts = System.currentTimeMillis(),
                durationMs = durationMs,
                gestures = listOf(AvenloApi.CaptureRequest.Gesture(0, "pinch"), AvenloApi.CaptureRequest.Gesture(durationMs, "pinch")),
                audioUrl = audioUrl,
            )
        )
        return resp.ideaId
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
        mutex.withLock {
            ideas.value = listOf(placeholder) + ideas.value
            pending[localId] = PendingCapture(audioPath, durationMs, placeholder.createdAt)   // 入重试队列（第四十轮）
        }
        persist()

        // 2) 提交后端 → 成功出队并用服务端卡片替换占位卡；失败留在队列（refresh 时补交）
        try {
            val resp = submitOnce(audioPath, durationMs)
            mutex.withLock { pending.remove(localId) }
            persist()
            val serverCard = api.getIdea(resp)
            mutex.withLock {
                ideas.value = ideas.value.map { if (it.id == localId) serverCard else it }
            }
            resp
        } catch (t: Throwable) {
            // 失败即返回——占位卡保留 + 队列保留（已落盘），网络恢复后 refresh() 自动补交
            throw t
        }
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
