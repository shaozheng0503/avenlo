# -*- coding: utf-8 -*-
"""桌面冒烟：sherpa-onnx + zipformer bilingual int8 模型转写官方测试 wav。
验证模型四件套完整 + 转写质量，再上 Android。"""
import wave, sys, time
import numpy as np
import sherpa_onnx

DIR = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/stt-models/zipformer-zh-en-int8'
t0 = time.time()
rec = sherpa_onnx.OnlineRecognizer.from_transducer(
    tokens=f'{DIR}/tokens.txt',
    encoder=f'{DIR}/encoder-epoch-99-avg-1.int8.onnx',
    decoder=f'{DIR}/decoder-epoch-99-avg-1.int8.onnx',
    joiner=f'{DIR}/joiner-epoch-99-avg-1.int8.onnx',
    num_threads=2,
    sample_rate=16000,
    feature_dim=80,
)
print('init: %.1fs' % (time.time() - t0))

for wav_name in ['test_wavs/0.wav', 'test_wavs/1.wav']:
    with wave.open(f'{DIR}/{wav_name}') as w:
        assert w.getframerate() == 16000, w.getframerate()
        assert w.getnchannels() == 1
        samples = w.readframes(w.getnframes())
    pcm = np.frombuffer(samples, dtype=np.int16)
    stream = rec.create_stream()
    t1 = time.time()
    # 分块喂（模拟流式），每 0.5s 一块
    chunk = 8000
    text = ''
    for i in range(0, len(pcm), chunk):
        stream.accept_waveform(16000, pcm[i:i+chunk].astype(np.float32) / 32768.0)
        while rec.is_ready(stream):
            rec.decode_stream(stream)
        text = rec.get_result(stream)
    stream.input_finished()
    while rec.is_ready(stream):
        rec.decode_stream(stream)
    text = rec.get_result_all(stream).text
    dur = len(pcm) / 16000
    print(f'{wav_name}: [{dur:.1f}s 音频, 转写 {time.time()-t1:.1f}s]')
    print('  →', text.strip())
print('SMOKE OK')
