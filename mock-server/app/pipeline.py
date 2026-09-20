"""可插拔 STT / LLM 流水线（M2 Demo 契约不变，M3 换真实供应商不动业务代码）

设计：
  - SttProvider:  音频文件 → 转写文本
  - LlmProvider:  转写文本 → 卡片结构（title/summary/tags）
  - 环境变量驱动选择，无 key 时回落到内置模拟（Demo 永远能跑）
  - 队伍拿到 key 后只需 export 环境变量，代码零改动

环境变量：
  AVENLO_STT_PROVIDER   stt 供应商：mock（默认）| dashscope（通义 Paraformer）
  AVENLO_STT_API_KEY    dashscope API key（AVENLO_STT_PROVIDER=dashscope 时必填）
  AVENLO_LLM_PROVIDER   llm 供应商：mock（默认）| openai_compatible（OpenAI 兼容接口）
  AVENLO_LLM_API_KEY    API key（openai_compatible 必填）
  AVENLO_LLM_BASE_URL   兼容接口地址（默认 https://dashscope.aliyuncs.com/compatible-mode/v1）
  AVENLO_LLM_MODEL      模型名（默认 qwen-plus）
"""
from __future__ import annotations

import asyncio
import json
import os
import re
from dataclasses import dataclass
from typing import Protocol


@dataclass
class CardDraft:
    """LLM 产出的卡片骨架（与 IdeaCard 前四字段对齐）"""
    title: str
    summary: str
    tags: list[str]
    transcript: str


# ---------------------------------------------------------------- SttProvider

class SttProvider(Protocol):
    async def transcribe(self, audio_path: str | None, duration_ms: int) -> str:
        """音频 → 文本。失败抛异常，由调用方回落模拟文案。"""
        ...


class MockStt:
    """演示替身：按提交时间戳轮换真实感转写文案（Demo 上卡片内容可信）。

    真链路（dashscope key）接上后此路径不再命中——mock 只是「无 key 永远能跑」的兜底。
    """

    _TRANSCRIPTS = [
        "今天通勤路上想到一个想法，把每天听的播客里最打动我的一段，攒成一张张小卡片，"
        "周末翻看的时候就像和自己重新聊了一遍天。",
        "刚才洗碗的时候突然想起来，厨房收纳其实和知识管理很像，常用的放台面，"
        "备用的收进柜子，最重要的贴在冰箱上。",
        "路过楼下的咖啡馆，里面在放爵士乐。想到一个选题：城市里的背景音是怎么塑造我们情绪的，"
        "值得录一期节目聊聊。",
        "睡前脑子停不下来，想到白天开会时那个没聊完的点：用户的抱怨里其实藏着最好的需求文档。",
        "和朋友散步时聊到的，如果给爸妈做一个只有三个按键的手机，第三个按键应该是什么？"
        "我觉得是一键把今天的心情发到家庭群。",
    ]

    async def transcribe(self, audio_path: str | None, duration_ms: int) -> str:
        idx = (int(duration_ms) // 1000) % len(self._TRANSCRIPTS)
        return self._TRANSCRIPTS[idx]


class DashscopeStt:
    """通义 Paraformer 录音文件识别（异步提交 + 轮询 + 结果文件下载）。

    ⚠️ 接口限制：file_urls 仅支持**公网可访问 URL**（http/https）。
    - audioUrl 为 http(s):// 时直接提交；
    - 本地路径走 data:base64 会被服务端拒绝（保留路径仅为协议演示），
      真机链路需先把音频放 OSS / 任意公网静态服务。
    所有阻塞 HTTP 调用经 asyncio.to_thread 下放线程池，避免卡死事件循环。
    """

    def __init__(self, api_key: str):
        self._key = api_key

    async def transcribe(self, audio_path: str | None, duration_ms: int) -> str:
        if not audio_path:
            raise FileNotFoundError("audio url/path is empty")
        if audio_path.startswith(("http://", "https://")):
            file_url = audio_path                       # 公网 URL 直传
        else:
            if not os.path.exists(audio_path):
                raise FileNotFoundError(f"audio file not found: {audio_path}")
            import base64
            with open(audio_path, "rb") as f:
                b64 = base64.b64encode(f.read()).decode()
            file_url = f"data:audio/mp4;base64,{b64}"   # ⚠️ 仅协议演示，服务端会拒
        task_id = await asyncio.to_thread(self._submit, file_url)
        return await asyncio.to_thread(self._poll, task_id)

    def _submit(self, file_url: str) -> str:
        import urllib.request
        payload = json.dumps({
            "model": "paraformer-v2",
            "input": {"file_urls": [file_url]},
        }).encode()
        req = urllib.request.Request(
            "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription",
            data=payload, method="POST",
            headers={"Authorization": f"Bearer {self._key}", "Content-Type": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = json.loads(resp.read())
        return body["output"]["task_id"]

    def _poll(self, task_id: str) -> str:
        import time
        import urllib.request
        for _ in range(60):
            r = urllib.request.Request(
                f"https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}",
                headers={"Authorization": f"Bearer {self._key}"},
            )
            with urllib.request.urlopen(r, timeout=15) as resp:
                t = json.loads(resp.read())
            status = t["output"]["task_status"]
            if status == "SUCCEEDED":
                # transcription_url 是**结果文件 URL**，还要再 GET 一次拿 JSON
                result_url = t["output"]["results"][0]["transcription_url"]
                return self._fetch_result(result_url)
            if status == "FAILED":
                raise RuntimeError(f"dashscope stt failed: {t}")
            time.sleep(1)   # 已在 to_thread 线程内，同步 sleep 不影响事件循环
        raise TimeoutError("stt poll timeout")

    def _fetch_result(self, result_url: str) -> str:
        import urllib.request
        with urllib.request.urlopen(result_url, timeout=15) as resp:
            data = json.loads(resp.read())
        # 结果文件结构：{"transcripts": [{"text": "...", "sentences": [...]}]}
        return data["transcripts"][0]["text"]


# ---------------------------------------------------------------- LlmProvider

class LlmProvider(Protocol):
    async def generate(self, transcript: str) -> CardDraft:
        """转写文本 → 卡片骨架。失败抛异常，由调用方回落模拟文案。"""
        ...


class MockLlm:
    """演示替身：转写文本 → 真实感卡片（标题提炼 + 摘要压缩 + 标签推断）。"""

    _TITLES = ["关于工作的想法", "读书时想到的", "通勤路上记的", "睡前灵感", "和朋友聊出来的"]

    async def generate(self, transcript: str) -> CardDraft:
        clean = transcript.replace("\n", " ").strip()
        # 标题：优先从转写提炼关键短语（逗号/句号切分取首个完整短句，≤12 字）
        first_clause = re.split(r"[，。！？,.!?]", clean)[0].strip()
        title = (first_clause[:12] if first_clause else "") or self._TITLES[len(clean) % len(self._TITLES)]
        # 摘要：压缩到 60 字内（保留转写核心）
        summary = clean if len(clean) <= 60 else clean[:57] + "…"
        # 标签：简单关键词推断，命中则用，否则「生活」
        tags = []
        for kw, tag in [("播客", "创作"), ("通勤", "生活"), ("厨房", "生活"), ("咖啡", "观察"),
                        ("开会", "工作"), ("需求", "产品"), ("爸妈", "家庭"), ("选题", "创作"),
                        ("城市", "观察"), ("散步", "生活"), ("洗碗", "生活"), ("收纳", "生活")]:
            if kw in clean and tag not in tags:
                tags.append(tag)
            if len(tags) >= 2:
                break
        return CardDraft(
            title=title,
            summary=summary,
            tags=tags or ["生活"],
            transcript=transcript,
        )


class OpenAICompatibleLlm:
    """OpenAI 兼容接口（通义 qwen / DeepSeek / GLM 等皆可）。

    prompt 要求模型输出严格 JSON，解析失败抛异常回落 mock。
    """

    SYSTEM_PROMPT = (
        "你是灵感卡片整理助手。用户给你一段语音转写文本，请输出 JSON："
        '{"title": "≤12字标题", "summary": "≤60字摘要", "tags": ["1-3个中文标签"]}，'
        "只输出 JSON，不要任何其他文字。"
    )

    def __init__(self, api_key: str, base_url: str, model: str):
        self._url = f"{base_url.rstrip('/')}/chat/completions"
        self._key = api_key
        self._model = model

    async def generate(self, transcript: str) -> CardDraft:
        content = await asyncio.to_thread(self._chat, transcript)
        # 剥掉 markdown 代码块围栏（有的模型爱加 ```json）
        m = re.search(r"\{.*\}", content, re.S)
        if not m:
            raise ValueError(f"llm output not json: {content[:200]}")
        obj = json.loads(m.group(0))
        return CardDraft(
            title=str(obj.get("title", ""))[:24] or "新的灵感",
            summary=str(obj.get("summary", ""))[:120],
            tags=[str(t) for t in (obj.get("tags") or ["生活"])][:3],
            transcript=transcript,
        )

    def _chat(self, transcript: str) -> str:
        import urllib.request
        payload = json.dumps({
            "model": self._model,
            "messages": [
                {"role": "system", "content": self.SYSTEM_PROMPT},
                {"role": "user", "content": transcript[:2000]},
            ],
            "temperature": 0.3,
        }).encode()
        req = urllib.request.Request(
            self._url, data=payload, method="POST",
            headers={
                "Authorization": f"Bearer {self._key}",
                "Content-Type": "application/json",
            },
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = json.loads(resp.read())
        return body["choices"][0]["message"]["content"]


# ---------------------------------------------------------------- 工厂 + 入口

def build_stt() -> SttProvider:
    provider = os.environ.get("AVENLO_STT_PROVIDER", "mock").lower()
    if provider == "dashscope":
        key = os.environ.get("AVENLO_STT_API_KEY", "")
        if key:
            return DashscopeStt(key)
    return MockStt()


def build_llm() -> LlmProvider:
    provider = os.environ.get("AVENLO_LLM_PROVIDER", "openai_compatible").lower()
    if provider == "openai_compatible":
        key = os.environ.get("AVENLO_LLM_API_KEY", "")
        if key:
            return OpenAICompatibleLlm(
                api_key=key,
                base_url=os.environ.get(
                    "AVENLO_LLM_BASE_URL",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1",
                ),
                model=os.environ.get("AVENLO_LLM_MODEL", "qwen-plus"),
            )
    return MockLlm()


# 模块级单例（跟随进程生命周期）
stt_provider: SttProvider = build_stt()
llm_provider: LlmProvider = build_llm()


async def run_pipeline(audio_path: str | None, duration_ms: int) -> CardDraft:
    """完整流水线：STT → LLM → 卡片骨架。任一环节失败回落 Mock，保证 Demo 永远能出卡。"""
    # STT
    try:
        transcript = await stt_provider.transcribe(audio_path, duration_ms)
    except Exception:
        transcript = await MockStt().transcribe(audio_path, duration_ms)

    # LLM
    try:
        return await llm_provider.generate(transcript)
    except Exception:
        return await MockLlm().generate(transcript)
