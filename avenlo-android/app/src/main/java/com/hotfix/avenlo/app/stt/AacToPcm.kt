package com.hotfix.avenlo.app.stt

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder

/**
 * AAC(m4a) → PCM 16k mono 解码。
 *
 * MediaExtractor 定轨 → MediaCodec 同步模式解码 → 输出 short[]
 * （sherpa-onnx 吃 16k mono PCM；MediaRecorder 录的是 AAC 44.1k，必须转）。
 *
 * 注意：不做重采样 —— MediaFormat 里请求 16000Hz 后多数解码器输出即 16k
 * （模拟器/主流真机均支持）；若遇到只出 44.1k 的设备，输出侧再做线性插值降采样。
 */
object AacToPcm {

    /** 解码整个 m4a 文件为 16k mono PCM；失败返回空数组 */
    fun decode(file: File, targetSampleRate: Int = SttEngine.SAMPLE_RATE): ShortArray = runCatching {
        val extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        check(trackIndex >= 0) { "no audio track" }
        extractor.selectTrack(trackIndex)

        val mime = format!!.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        // 请求 16k mono 输出（解码器支持时生效）
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, targetSampleRate)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        codec.configure(format, null, null, 0)
        codec.start()

        val out = ArrayList<Short>(16000)  // ~1s 起步
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        var outSampleRate = targetSampleRate   // 第四十九轮修复：真实输出采样率在 FORMAT_CHANGED 时才知道
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx >= 0 -> {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    // PCM16 little-endian → short[]
                    val shorts = ShortArray(info.size / 2)
                    buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                    // 双声道 → mono（解码器没按请求转 mono 时兜底）
                    val ch = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val mono = if (ch >= 2) {
                        ShortArray(shorts.size / ch) { j ->
                            var acc = 0
                            for (c in 0 until ch) acc += shorts[j * ch + c]
                            (acc / ch).toShort()
                        }
                    } else shorts
                    out.ensureCapacity(out.size + mono.size)
                    mono.forEach { out.add(it) }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // 解码器真实输出格式在这里拿（configure 里的 16k/mono 只是请求，可能被忽略）
                    val real = codec.outputFormat
                    outSampleRate = runCatching {
                        real.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }.getOrDefault(targetSampleRate)
                }
            }
        }
        codec.stop(); codec.release()
        extractor.release()

        // 输出采样率非 16k 时线性插值降采样（44.1k 兜底路径）
        val samples = ShortArray(out.size) { out[it] }
        if (outSampleRate == targetSampleRate) samples
        else resample(samples, outSampleRate, targetSampleRate)
    }.getOrDefault(ShortArray(0))

    /** 线性插值重采样（质量足够 ASR 用） */
    private fun resample(input: ShortArray, from: Int, to: Int): ShortArray {
        if (input.isEmpty() || from == to) return input
        val outLen = input.size.toLong() * to / from
        val out = ShortArray(outLen.toInt())
        for (i in out.indices) {
            val srcPos = i.toDouble() * from / to
            val i0 = srcPos.toInt()
            val i1 = (i0 + 1).coerceAtMost(input.size - 1)
            val frac = srcPos - i0
            out[i] = (input[i0] * (1 - frac) + input[i1] * frac).toInt().toShort()
        }
        return out
    }
}
