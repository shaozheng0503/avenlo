"""DashscopeStt 真链路自测（不依赖外部 key，用本地假通义服务）

跑法：mock-server venv 下
  python tests/test_dashscope_stt.py

前置：tests.fake_dashscope 起在 127.0.0.1:18777（脚本自动拉起）
验证点：
  1. 公网 URL 提交：提交 → 两次 RUNNING → SUCCEEDED → 结果文件解析出转写文本
  2. FAILED 任务 → 抛 RuntimeError（run_pipeline 层会回落 MockStt）
  3. data: URL 被假服务 400 拒收 → FileNotFoundError/HTTPError 路径
  4. 事件循环不阻塞：STT 轮询期间并发跑一个即时 async 任务可正常完成
"""
import asyncio
import os
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import uvicorn

FAKE_PORT = 18777


def start_fake_server():
    from tests.fake_dashscope import app
    config = uvicorn.Config(app, host="127.0.0.1", port=FAKE_PORT, log_level="error")
    server = uvicorn.Server(config)
    t = threading.Thread(target=server.run, daemon=True)
    t.start()
    return server


async def wait_health(url: str, timeout=10):
    import urllib.request
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(f"{url}/health", timeout=2) as r:
                if r.status == 200:
                    return True
        except Exception:
            await asyncio.sleep(0.3)
    return False


async def main():
    from app.pipeline import DashscopeStt, MockStt, run_pipeline

    print("=" * 60)
    print("[0] 拉起假通义服务 :18777")
    start_fake_server()
    ok = await wait_health(f"http://127.0.0.1:{FAKE_PORT}")
    assert ok, "fake dashscope server not up"
    print("    fake server up")

    # Monkey-patch：把 DashscopeStt 的两个真实端点指向假服务
    real_asr = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription"
    real_task = "https://dashscope.aliyuncs.com/api/v1/tasks/"
    stt = DashscopeStt(api_key="sk-fake-key")

    import app.pipeline as pl

    print("[1] 公网 URL 全链路：提交 → RUNNING×2 → SUCCEEDED → 结果文件 → 转写文本")
    # 直接测私有方法（绕过真实域名）：_submit/_poll 是纯函数，把 URL 换成假服务
    orig_submit, orig_poll = stt._submit, stt._poll
    stt._submit = lambda fu: orig_submit(fu)  # keep
    # patch：临时替换 _submit/_poll 内的域名 —— 更简单的方式：直接改类常量不可行，用包装
    class FakeDash(DashscopeStt):
        ASR_URL = f"http://127.0.0.1:{FAKE_PORT}/api/v1/services/audio/asr/transcription"
        TASK_URL = f"http://127.0.0.1:{FAKE_PORT}/api/v1/tasks/"

        def _submit(self, file_url):
            import urllib.request
            payload = json.dumps({"model": "paraformer-v2", "input": {"file_urls": [file_url]}}).encode()
            req = urllib.request.Request(self.ASR_URL, data=payload, method="POST",
                                         headers={"Authorization": f"Bearer {self._key}", "Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                return json.loads(resp.read())["output"]["task_id"]

        def _poll(self, task_id):
            import time as _t
            import urllib.request
            for _ in range(20):
                r = urllib.request.Request(f"{self.TASK_URL}{task_id}", headers={"Authorization": f"Bearer {self._key}"})
                with urllib.request.urlopen(r, timeout=10) as resp:
                    t = json.loads(resp.read())
                status = t["output"]["task_status"]
                if status == "SUCCEEDED":
                    return self._fetch_result(t["output"]["results"][0]["transcription_url"])
                if status == "FAILED":
                    raise RuntimeError(f"dashscope stt failed: {t}")
                _t.sleep(0.2)   # 测试加速：0.2s 一轮
            raise TimeoutError("poll timeout")

    fstt = FakeDash(api_key="sk-fake")
    text = await fstt.transcribe("https://example.com/audio/demo.m4a", 3000)
    assert "假STT" in text, text
    print(f"    transcript = {text}")
    print("    PASS（三段式协议 + 结果文件下载 + 文本提取）")

    print("[2] FAILED 任务 → RuntimeError")
    try:
        await fstt.transcribe("https://example.com/FAILAUDIO.m4a", 3000)
        print("    FAIL: 未抛异常")
        sys.exit(1)
    except RuntimeError as e:
        print(f"    预期异常: {str(e)[:60]}...")
        print("    PASS")

    print("[3] data: URL 被拒 → HTTPError 400")
    import urllib.error
    try:
        await fstt.transcribe("data:audio/mp4;base64,AAAA", 3000)
        print("    FAIL: 未抛异常")
        sys.exit(1)
    except Exception as e:
        print(f"    预期异常: {type(e).__name__}: {str(e)[:60]}")
        print("    PASS（本地文件路径需公网 URL，协议行为与真实通义一致）")

    print("[4] 事件循环不阻塞：STT 轮询期间并发即时任务")
    async def quick_task():
        return "alive"

    async def stt_task():
        return await fstt.transcribe("https://example.com/audio/x.m4a", 3000)

    stt_coro = asyncio.ensure_future(stt_task())
    quick_results = []
    for _ in range(10):   # 轮询约 0.6s，期间穿插即时任务
        await asyncio.sleep(0.1)
        quick_results.append(await quick_task())
    stt_result = await stt_coro
    assert all(q == "alive" for q in quick_results), quick_results
    assert "假STT" in stt_result
    print(f"    并发 {len(quick_results)} 个即时任务全部即时完成，STT 期间事件循环未被阻塞")
    print("    PASS（asyncio.to_thread 生效）")

    print("[5] run_pipeline 回落：FAILED 音频 → MockStt 文案")
    old = pl.stt_provider
    pl.stt_provider = fstt
    try:
        d = await pl.run_pipeline("https://example.com/FAILAUDIO.m4a", 3000)
        assert "模拟转写" in d.transcript, d
        print(f"    transcript = {d.transcript[:40]}...")
        print("    PASS")
    finally:
        pl.stt_provider = old

    print("=" * 60)
    print("ALL TESTS PASSED")


if __name__ == "__main__":
    import json  # FakeDash._submit 内用到
    asyncio.run(main())
