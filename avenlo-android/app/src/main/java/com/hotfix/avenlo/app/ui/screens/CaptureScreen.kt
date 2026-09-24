package com.hotfix.avenlo.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.hotfix.avenlo.app.ServiceLocator
import com.hotfix.avenlo.app.haptics.rememberHaptics
import com.hotfix.avenlo.app.stt.SttEngine
import com.hotfix.avenlo.app.ui.theme.AvenloTokens
import com.hotfix.avenlo.domain.capture.CaptureSpec
import com.hotfix.avenlo.domain.capture.HapticEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用内捕捉屏（阻断项 #6 补丁方案：深色捕捉语言自设计，视觉待队伍评审）。
 *
 * 真实链路：点击开始（模拟戒指轻捏）→ 请求录音权限 → MediaRecorder 录音
 * → 再次点击 或 60s 上限 → 提交后端（queued 占位卡已在首页）→ 3s 撤销窗。
 */
@Composable
fun CaptureScreen(nav: NavController, autoStart: Boolean = false) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf<Phase>(Phase.Ready) }
    var elapsedSec by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var submittedId by remember { mutableStateOf<String?>(null) }

    // ---- 端侧 STT：进程级单例（第四十九轮修复：引擎不再随屏幕 dispose 释放，
    // 转写协程改走 App 级作用域，屏幕退出不中断、不崩溃）----
    var liveText by remember { mutableStateOf("") }        // 「正在聆听」下的实时增量文本
    var draftText by remember { mutableStateOf<String?>(null) }  // Done 态草稿转写
    val sttScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    val pulse = rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse",
    )

    // ---- 权限 ----
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording(context) { r, f -> recorder = r; audioFile = f; phase = Phase.Recording; haptics(HapticEvent.STARTED) }
        else phase = Phase.Denied
    }

    fun tryStart() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) startRecording(context) { r, f -> recorder = r; audioFile = f; phase = Phase.Recording; haptics(HapticEvent.STARTED) }
        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 长按 FAB 直达：进屏自动开录（权限已授予时无感；未授予弹窗，拒绝则回落 Ready 态）
    LaunchedEffect(autoStart) {
        if (autoStart && phase == Phase.Ready) tryStart()
    }

    // ---- 结束并提交（局部函数必须先于调用点声明）----
    fun stopAndSubmit() {
        runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
        phase = Phase.Submitting
        haptics(HapticEvent.SAVED)   // 已存下=双震
        val dur = elapsedSec * 1000L
        val path = audioFile?.absolutePath
        // 端侧草稿转写（sherpa-onnx 单例；失败静默——云端链路照常兜底）
        // 注意：sttScope 是 App 级作用域，屏幕 popBackStack 后转写继续、结果随 DraftState 落库
        if (path != null) {
            sttScope.launch {
                val full = SttEngine.get().transcribeFile(context, File(path))
                withContext(Dispatchers.Main) { draftText = full ?: "" }
            }
        }
        scope.launch {
            ServiceLocator.ideaRepo.submitCapture(path, dur)
                .onSuccess {
                    submittedId = it
                    phase = Phase.Done
                }
                .onFailure {
                    phase = Phase.Error(it.message ?: "提交失败")
                    haptics(HapticEvent.FAILED)
                }
        }
    }

    // ---- 计时 + 60s 上限 + 静默 3s 自动结束（200ms 振幅采样）----
    LaunchedEffect(phase) {
        if (phase == Phase.Recording) {
            var silenceMs = 0L
            var totalMs = 0L
            while (totalMs < CaptureSpec.MAX_DURATION_MS && phase == Phase.Recording) {
                delay(200)
                totalMs += 200
                elapsedSec = (totalMs / 1000).toInt()
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                silenceMs = if (amp > SILENCE_AMP_THRESHOLD) 0 else silenceMs + 200
                if (silenceMs >= CaptureSpec.SILENCE_AUTO_STOP_MS && totalMs >= 1000) {
                    stopAndSubmit()
                    break
                }
            }
            if (phase == Phase.Recording) stopAndSubmit()
        }
    }

    // ---- 撤销窗：提交 3 秒后自动返回首页（卡片已落库）----
    LaunchedEffect(submittedId) {
        if (submittedId != null) {
            delay(CaptureSpec.UNDO_WINDOW_MS)
            if (phase == Phase.Done) nav.popBackStack()
        }
    }

    Box(Modifier.fillMaxSize().background(AvenloTokens.CaptureDarkBg)) {
        IconButton(
            onClick = {
                runCatching { recorder?.release() }
                nav.popBackStack()
            },
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        ) { Icon(Icons.Filled.Close, "关闭", tint = Color.White.copy(alpha = 0.7f)) }

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val currentPhase = phase   // delegated property 不能智能转换，先捕获到局部 val
            when (currentPhase) {
                Phase.Ready, Phase.Denied -> {
                    Text("准备好了吗？", color = Color.White, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (phase == Phase.Denied) "需要麦克风权限\n请在系统设置中开启后重试" else "轻捏戒指开始记录\n（Demo 用按钮模拟轻捏）",
                        color = Color.White.copy(alpha = 0.6f), fontSize = AvenloTokens.FontSizeSm,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    BigButton(
                        label = if (phase == Phase.Denied) "重试权限" else "轻捏开始",
                        highlight = phase == Phase.Denied,
                        alpha = 1f,
                    ) { tryStart() }
                }

                Phase.Recording -> {
                    Box(
                        Modifier.size(120.dp).graphicsLayer { alpha = pulse.value }
                            .clip(CircleShape)
                            .background(AvenloTokens.Primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(AvenloTokens.Primary),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Mic, "录音中", tint = Color.White, modifier = Modifier.size(36.dp)) }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("正在聆听…", color = Color.White, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("再次轻捏结束 · 静默 3 秒自动保存", color = Color.White.copy(alpha = 0.6f), fontSize = AvenloTokens.FontSizeSm)
                    // 端侧 STT 实时预览（模型就绪才有）
                    if (liveText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            liveText,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = AvenloTokens.FontSizeSm,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "%d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                        color = AvenloTokens.Warning, fontSize = AvenloTokens.FontSize2xl, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(32.dp))
                    BigButton("再次轻捏 · 结束并保存", highlight = true, alpha = pulse.value) {
                        if (elapsedSec >= 1) stopAndSubmit()
                    }
                }

                Phase.Submitting -> {
                    CircularProgressIndicator(color = AvenloTokens.Warning, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("已存下，AI 正在整理…", color = Color.White, fontSize = AvenloTokens.FontSizeLg)
                }

                Phase.Done -> {
                    CircularProgressIndicator(color = AvenloTokens.Success, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("灵感已保存", color = AvenloTokens.Success, fontSize = AvenloTokens.FontSizeLg, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("3 秒内可撤销", color = Color.White.copy(alpha = 0.5f), fontSize = AvenloTokens.FontSizeSm)
                    // 端侧草稿转写（sherpa-onnx；空=引擎未就绪或转写中，云端精修后卡片会替换）
                    draftText?.let { draft ->
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "本地转写草稿：\n$draft",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = AvenloTokens.FontSizeSm,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    BigButton("撤销这条记录", highlight = false, alpha = 1f) {
                        val id = submittedId
                        if (id != null) {
                            haptics(HapticEvent.UNDO)
                            scope.launch {
                                ServiceLocator.ideaRepo.deleteIdea(id)
                                nav.popBackStack()
                            }
                        }
                    }
                }

                is Phase.Error -> {
                    Text("出错了", color = AvenloTokens.Error, fontSize = AvenloTokens.FontSizeXl, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(currentPhase.message, color = Color.White.copy(alpha = 0.6f), fontSize = AvenloTokens.FontSizeSm, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    BigButton("返回", highlight = true, alpha = 1f) { nav.popBackStack() }
                }
            }
        }

        Text(
            "上限 ${CaptureSpec.MAX_DURATION_MS / 1000} 秒 · 撤销窗 ${CaptureSpec.UNDO_WINDOW_MS / 1000} 秒",
            color = Color.White.copy(alpha = 0.35f), fontSize = AvenloTokens.FontSizeXs,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}

/** 捕捉屏内部状态机（UI 局部，与 domain 层 CaptureState 区分） */
private sealed interface Phase {
    data object Ready : Phase
    data object Denied : Phase
    data object Recording : Phase
    data object Submitting : Phase
    data object Done : Phase
    data class Error(val message: String) : Phase
}

/** 静默判定振幅阈值（MediaRecorder.maxAmplitude 0..32767，经验值） */
private const val SILENCE_AMP_THRESHOLD = 800

/** 大号捕捉按钮（深色屏唯一主控件） */
@Composable
private fun BigButton(label: String, highlight: Boolean, alpha: Float, onClick: () -> Unit) {
    val bg = if (highlight) AvenloTokens.Primary else Color.White.copy(alpha = 0.12f)
    val fg = if (highlight) Color.White else Color.White.copy(alpha = 0.85f)
    Text(
        label,
        color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 40.dp, vertical = 16.dp),
    )
}

/** MediaRecorder 启动（输出 AAC 到缓存目录） */
private fun startRecording(
    context: android.content.Context,
    onReady: (MediaRecorder?, File?) -> Unit,
) {
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.m4a")
    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
    runCatching {
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(96_000)
        mr.setAudioSamplingRate(44_100)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        onReady(mr, file)
    }.onFailure {
        runCatching { mr.release() }
        onReady(null, null)
    }
}
