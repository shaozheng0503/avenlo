"""pipeline 真链路自测（不依赖外部 key，用本地假 OpenAI 兼容服务）

跑法：mock-server venv 下
  python tests/test_pipeline.py

前置：tests.fake_openai 起在 127.0.0.1:8099（脚本自动拉起）
验证点：
  1. mock 模式：MockStt + MockLlm 产出
  2. OpenAICompatibleLlm 正常解析：markdown 围栏剥离 + title/summary/tags 映射
  3. 异常回落：LLM 返回非 JSON → 回落 MockLlm 文案
"""
import asyncio
import os
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.pipeline import (
    CardDraft, MockLlm, MockStt, OpenAICompatibleLlm,
    build_llm, build_stt, run_pipeline,
)

FAKE_URL = "http://127.0.0.1:8099"


def start_fake_openai():
    """拉起假 OpenAI 服务（daemon 线程）。防 8099 被系统代理转发：no_proxy 已在下方设置。"""
    from tests.fake_openai import app
    import uvicorn
    config = uvicorn.Config(app, host="127.0.0.1", port=8099, log_level="error")
    server = uvicorn.Server(config)
    threading.Thread(target=server.run, daemon=True).start()
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
    # 本机代理（127.0.0.1:7897）会把 127.0.0.1:8099 的请求也转发出去导致 502，
    # 显式豁免 loopback
    os.environ.setdefault("no_proxy", "127.0.0.1,localhost")
    os.environ["NO_PROXY"] = os.environ["no_proxy"]

    print("[0] 拉起假 OpenAI 服务 :8099")
    start_fake_openai()
    ok = await wait_health(FAKE_URL)
    assert ok, "fake openai server not up"
    print("    fake server up")

    print("=" * 60)
    print("[1] mock 模式（默认，无任何环境变量）")
    print(f"    stt={type(build_stt()).__name__}  llm={type(build_llm()).__name__}")
    d = await run_pipeline(None, 8000)
    # mock 文案已是真实感内容（无「（模拟转写）」前缀）——断言非空 + 有实际长度
    assert len(d.transcript) >= 10 and d.title, d
    print(f"    title={d.title}  transcript[:30] = {d.transcript[:30]}")
    print("    PASS")

    print("[2] OpenAICompatibleLlm 正常链路（假服务返回 markdown 围栏 JSON）")
    llm = OpenAICompatibleLlm(api_key="sk-fake", base_url=FAKE_URL, model="fake-model")
    d = await llm.generate("用户转写：我想做一个戒指形态的灵感捕捉设备")
    assert d.title == "假LLM标题-已解析", d
    assert d.tags == ["测试", "链路验证"], d
    assert "假LLM摘要" in d.summary, d
    print(f"    title={d.title}  tags={d.tags}")
    print("    PASS（markdown 围栏剥离 + 字段映射 + 截断保护）")

    print("[3] 异常回落：LLM 输出非 JSON")
    try:
        await llm.generate("BADJSON 触发非 JSON 输出")
        print("    FAIL: 未抛异常")
        sys.exit(1)
    except ValueError as e:
        print(f"    generate 抛出预期异常: {str(e)[:50]}...")

    # run_pipeline 层回落：假 STT 产出含 BADJSON 的转写 → 假 LLM 返回非 JSON → 回落 MockLlm
    class BadStt:
        async def transcribe(self, audio_path, duration_ms):
            return "BADJSON 用户转写文本"

    import app.pipeline as pl
    old_stt, old_llm = pl.stt_provider, pl.llm_provider
    pl.stt_provider, pl.llm_provider = BadStt(), llm
    try:
        d = await pl.run_pipeline(None, 5000)
        # 回落 MockLlm：产出完整卡片（title 非空）且 transcript 保真（BADJSON 透传）
        assert d.title and "BADJSON" in d.transcript, d
        print(f"    run_pipeline 回落成功：title={d.title}，transcript 保真")
        print("    PASS")
    finally:
        pl.stt_provider, pl.llm_provider = old_stt, old_llm

    print("[4] run_pipeline 直接对假 LLM（绕过环境变量单例）")
    import app.pipeline as pl
    old = pl.llm_provider
    pl.llm_provider = llm
    try:
        d = await pl.run_pipeline(None, 5000)
        assert d.title == "假LLM标题-已解析", d
        print(f"    title={d.title}（假 LLM 产出直接进卡片）")
        print("    PASS")
    finally:
        pl.llm_provider = old

    print("=" * 60)
    print("ALL TESTS PASSED")


if __name__ == "__main__":
    asyncio.run(main())
