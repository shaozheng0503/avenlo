# -*- coding: utf-8 -*-
"""从 HF 国内镜像下载 zipformer bilingual zh-en int8 模型四件套 + 测试 wav。
（GitHub release 整包 436MB 走加速器仅 27KB/s；hf-mirror 可直连且按文件拉，只取 int8 ≈ 34MB）"""
import urllib.request, ssl, sys, os, time

DIR = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/stt-models/zipformer-zh-en-int8'
REPO = 'csukuangfj/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20'
BASE = f'https://hf-mirror.com/{REPO}/resolve/main'
FILES = [
    'encoder-epoch-99-avg-1.int8.onnx',
    'decoder-epoch-99-avg-1.int8.onnx',
    'joiner-epoch-99-avg-1.int8.onnx',
    'tokens.txt',
    'bpe.model',
    # 测试音频（转写验证用，不进 APK）
    'test_wavs/0.wav',
    'test_wavs/1.wav',
]
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE

os.makedirs(DIR, exist_ok=True)
os.makedirs(os.path.join(DIR, 'test_wavs'), exist_ok=True)

def fetch(rel, retries=6):
    dest = os.path.join(DIR, rel)
    if os.path.exists(dest) and os.path.getsize(dest) > 1000:
        print('exists, skip:', rel)
        return
    url = f'{BASE}/{rel}'
    for attempt in range(1, retries + 1):
        print(f'downloading {rel} (attempt {attempt})', flush=True)
        t0 = time.time()
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'curl/8'})
            with urllib.request.urlopen(req, timeout=90, context=CTX) as r, open(dest, 'wb') as f:
                total = int(r.headers.get('Content-Length') or 0)
                got = 0
                while True:
                    chunk = r.read(1 << 20)
                    if not chunk:
                        break
                    f.write(chunk)
                    got += len(chunk)
                    if total:
                        print(f'\r  {got*100//total}% {got//(1<<20)}MB/{total//(1<<20)}MB {time.time()-t0:.0f}s', end='', flush=True)
            print(f'\n  done {got//(1<<20)}MB in {time.time()-t0:.0f}s')
            if total and got != total:
                raise IOError(f'size mismatch {got}/{total}')
            return
        except Exception as e:
            print(f'\n  retry after error: {e}')
            time.sleep(3 * attempt)
    raise RuntimeError(f'{rel} failed after {retries} attempts')

for n in FILES:
    try:
        fetch(n)
    except Exception as e:
        print('FAILED:', n, e)
        sys.exit(1)
print('ALL OK')
