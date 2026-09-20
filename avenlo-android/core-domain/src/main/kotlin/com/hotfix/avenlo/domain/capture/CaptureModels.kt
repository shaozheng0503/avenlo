package com.hotfix.avenlo.domain.capture

/** 震动语言（规格站定义，domain 层语义表 —— iOS 侧 Core Haptics 按同一张表实现） */
enum class HapticEvent { STARTED, SAVED, UNDO, FAILED, LOW_BATTERY }

/** 捕捉状态机：戒指/按钮触发 → 录音 → 提交 → 整理中 → 完成 */
sealed interface CaptureState {
    data object Idle : CaptureState
    data class Recording(val startedAt: Long, val gestureMs: Long) : CaptureState
    /** 静默 3s 自动结束 or 再次轻捏结束 → 提交 */
    data class Submitting(val durationMs: Long) : CaptureState
    /** 后端 STT+LLM 整理中（对应卡片 queued 状态） */
    data class Processing(val ideaId: String) : CaptureState
    data class Done(val ideaId: String) : CaptureState
    data class Error(val message: String) : CaptureState
}

/** 交互参数（规格站口径） */
object CaptureSpec {
    const val PINCH_MIN_MS = 300L        // 防误触
    const val SILENCE_AUTO_STOP_MS = 3000L
    const val MAX_DURATION_MS = 60_000L
    const val UNDO_WINDOW_MS = 3000L     // 结束后 3 秒内快速双捏=撤销
}
