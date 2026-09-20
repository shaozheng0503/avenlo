package com.hotfix.avenlo.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.hotfix.avenlo.domain.capture.HapticEvent

/**
 * 震动语言：domain 层语义表 → Android 波形实现。
 * （iOS 侧 Core Haptics 按同一张 HapticEvent 语义表实现，见方案 5.3）
 */
object Haptics {

    private fun vibrator(ctx: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    /** 波形表：[延迟, 震动强度, 间隔, 强度, ...] 毫秒 */
    private val patterns: Map<HapticEvent, LongArray> = mapOf(
        // 开始=轻震 1 次（30ms）
        HapticEvent.STARTED to longArrayOf(0, 30),
        // 结束/已存下=双震（30ms + 60ms 间隔 + 30ms）
        HapticEvent.SAVED to longArrayOf(0, 30, 60, 30),
        // 撤销=短双震（20ms + 40ms + 20ms）
        HapticEvent.UNDO to longArrayOf(0, 20, 40, 20),
        // 失败=急促三连（20ms × 3，间隔 40ms）
        HapticEvent.FAILED to longArrayOf(0, 20, 40, 20, 40, 20),
        // 低电量=轻提示（推迟到下次录音开始时，规格站定义）
        HapticEvent.LOW_BATTERY to longArrayOf(0, 15, 30, 15),
    )

    fun fire(ctx: Context, event: HapticEvent) {
        val vb = vibrator(ctx)
        val pattern = patterns[event] ?: return
        vb.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}

/** Compose 侧便捷入口 */
@Composable
fun rememberHaptics(): (HapticEvent) -> Unit {
    val ctx = LocalContext.current
    return remember(ctx) { { event -> Haptics.fire(ctx, event) } }
}
