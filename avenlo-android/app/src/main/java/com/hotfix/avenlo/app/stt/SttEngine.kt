package com.hotfix.avenlo.app.stt

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 端侧 STT 引擎（sherpa-onnx v1.12.29 流式 zipformer 中英 int8）——进程级单例。
 *
 * 第四十九轮修复（native crash 根因）：
 * 旧版引擎挂在 CaptureScreen 生命周期上，屏幕 popBackStack 触发 onDispose → release()
 * 把 native ptr 置 0；而转写协程（先解码 AAC 再推理，60s 音频全链路约 20s）往往比屏幕
 * 活得久，后续任何 JNI 调用（createStream 等）都作用在空指针上 → SIGSEGV 崩溃。
 *
 * 现在引擎只在进程内创建一次、绝不随屏释放；所有 JNI 入口 synchronized 串行化，
 * 且每次调用前用反射读 ptr 做防御（0 = 已释放/未初始化，直接降级返回）。
 */
class SttEngine private constructor() {

    /** native 识别器；@Volatile 保证跨线程可见性（prewarm 线程写，UI 线程读） */
    @Volatile
    private var recognizer: OnlineRecognizer? = null

    /** 预热完成标记（true = 已尝试过初始化，成败都算，避免反复重试） */
    @Volatile
    var prewarmed = false
        private set

    private val ptrField = OnlineRecognizer::class.java.getDeclaredField("ptr").apply {
        isAccessible = true
    }

    /** 当前是否可用（已加载且未被释放） */
    val isReady: Boolean
        get() = synchronized(this) { recognizer != null && ptrOf(recognizer!!) != 0L }

    private fun ptrOf(r: OnlineRecognizer): Long = runCatching { ptrField.getLong(r) }.getOrDefault(-1L)

    /**
     * 进程内首次调用时初始化（assets 模型拷到 filesDir + newFromFile）。
     * 失败静默降级——云端链路照常兜底；prewarmed 置 true 后不再重试。
     */
    fun prewarm(context: Context) {
        synchronized(this) {
            if (prewarmed || recognizer != null) return
            val appCtx = context.applicationContext
            // 初始化在持锁状态下做（约 15s 模型加载），期间 UI 读 isReady 只会拿到 false
            runCatching {
                val dir = File(appCtx.filesDir, MODEL_DIR)
                if (!dir.exists()) dir.mkdirs()
                val files = listOf(
                    "encoder-epoch-99-avg-1.int8.onnx",
                    "decoder-epoch-99-avg-1.int8.onnx",
                    "joiner-epoch-99-avg-1.int8.onnx",
                    "tokens.txt",
                )
                for (name in files) {
                    val out = File(dir, name)
                    if (out.exists() && out.length() > 0) continue
                    appCtx.assets.open("$MODEL_DIR/$name").use { input ->
                        FileOutputStream(out).use { output -> input.copyTo(output) }
                    }
                }
                val config = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = SAMPLE_RATE),
                    modelConfig = OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = File(dir, "encoder-epoch-99-avg-1.int8.onnx").absolutePath,
                            decoder = File(dir, "decoder-epoch-99-avg-1.int8.onnx").absolutePath,
                            joiner = File(dir, "joiner-epoch-99-avg-1.int8.onnx").absolutePath,
                        ),
                        tokens = File(dir, "tokens.txt").absolutePath,
                        numThreads = 2,
                        debug = false,   // 诊断期已过：Validate 失败会打 logcat tag=sherpa-onnx，无需冗余刷屏
                    ),
                )
                // assetManager 传 null：模型已落盘，走 newFromFile(config) 文件路径加载
                val r = OnlineRecognizer(assetManager = null, config = config)
                if (ptrOf(r) == 0L) {
                    // newFromFile 校验失败 → ptr=0，后续任何 JNI 调用都会 SIGSEGV
                    Log.e(TAG, "sherpa-onnx newFromFile failed (ptr=0)，详情见 logcat tag=sherpa-onnx")
                    runCatching { r.release() }
                } else {
                    recognizer = r
                    Log.i(TAG, "SttEngine prewarm OK（zipformer zh-en int8, threads=2）")
                }
            }.onFailure { Log.e(TAG, "prewarm failed: ${it.message}") }
            prewarmed = true
        }
    }

    /** 整段离线转写 m4a/AAC 文件：AAC→PCM(16k mono) → sherpa 全量解码。失败返回 null。 */
    suspend fun transcribeFile(context: Context, file: File): String? = withContext(Dispatchers.Default) {
        val appCtx = context.applicationContext
        if (!isReady) prewarm(appCtx)
        val r = synchronized(this@SttEngine) { recognizer } ?: return@withContext null
        runCatching {
            val pcm = AacToPcm.decode(file)
            if (pcm.isEmpty()) return@runCatching null
            // 全链路持锁：createStream/feed/finish 期间引擎不会被并发调用
            synchronized(this@SttEngine) {
                if (ptrOf(r) == 0L) return@synchronized null
                val stream: OnlineStream = r.createStream()
                val floats = FloatArray(pcm.size) { pcm[it] / 32768f }
                stream.acceptWaveform(floats, SAMPLE_RATE)
                stream.inputFinished()
                while (r.isReady(stream)) r.decode(stream)
                val text = r.getResult(stream).text.trim()
                runCatching { stream.release() }
                Log.i(TAG, "转写完成(${pcm.size}采样, ${"%.1f".format(pcm.size / 16000f)}s): ${text.ifBlank { "(空)" }}")
                text.ifBlank { null }
            }
        }.onFailure { Log.e(TAG, "transcribeFile failed: ${it.message}") }.getOrNull()
    }

    companion object {
        private const val TAG = "AvenloStt"
        const val SAMPLE_RATE = 16000
        private const val MODEL_DIR = "stt-model"

        /** 进程级单例（Demo 阶段不引 Hilt，与 ServiceLocator 同款手写风格） */
        @Volatile
        private var instance: SttEngine? = null

        fun get(): SttEngine = instance ?: synchronized(this) {
            instance ?: SttEngine().also { instance = it }
        }
    }
}
