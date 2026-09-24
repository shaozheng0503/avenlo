package com.hotfix.avenlo.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 重试队列文件持久化（第四十五轮 M3-lite+）：根治第四十轮边界——
 * 断网期间进程被杀 → 内存 pending 队列丢 → 占位卡永久滞留「整理中」。
 *
 * 设计：JSON 文件原子写（tmp + rename），个位数条目全量序列化。
 * 不引 Room：3 字节字段 × 个位数条目，KSP/schema/migration 的构建面与收益不成比例
 * （本机代理网络环境拉新依赖链有风险，方案 8.2 既定「单用户低频写入不做冲突合并」延续）。
 *
 * 持久化内容 = 队列本身 + 占位卡重建信息（createdAt——重启后「整理中」卡要回到列表顶部）。
 * audioPath 是绝对路径（cacheDir 下）：进程重启后文件仍在即真补交；App 数据清除则丢弃（诚实降级）。
 */
/** 落盘条目（localId + 原始提交参数 + 占位卡重建信息） */
class PendingStore(private val dir: File) {

    @Serializable
    data class PendingEntry(
        val localId: String,
        val audioPath: String?,
        val durationMs: Long,
        val createdAt: Long,
    )

    private val file = File(dir, "pending_queue.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    init { dir.mkdirs() }

    /** 读全部（文件缺失/损坏返回空——首次运行与降级路径） */
    suspend fun load(): List<PendingEntry> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@withContext emptyList()
            json.decodeFromString(ListSerializer, file.readText())
        }.getOrDefault(emptyList())
    }

    /** 全量原子写（个位数条目，全量比增量简单且足够） */
    suspend fun save(entries: List<PendingEntry>) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(dir, "pending_queue.json.tmp")
            tmp.writeText(json.encodeToString(ListSerializer, entries))
            if (!tmp.renameTo(file)) {           // Windows/AVD 上 rename 覆盖不稳：回退删+写
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    private val ListSerializer = kotlinx.serialization.builtins.ListSerializer(PendingEntry.serializer())
}
