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

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.pipeline import (
    CardDraft, MockLlm, MockStt, OpenAICompatibleLlm,
    build_llm, build_stt, run_pipeline,
)

FAKE_URL = "http://127.0.0.1:8099"


async def main():
    print("=" * 60)
    print("[1] mock 模式（默认，无任何环境变量）")
    print(f"    stt={type(build_stt()).__name__}  llm={type(build_llm()).__name__}")
    d = await run_pipeline(None, 8000)
    assert "模拟转写" in d.transcript, d
    print(f"    transcript[:30] = {d.transcript[:30]}")
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
        assert "模拟 AI 摘要" in d.summary and "BADJSON" in d.transcript, d
        print(f"    run_pipeline 回落成功：summary 仍为 mock 文案，transcript 保真")
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
