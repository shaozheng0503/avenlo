"""Avenlo Mock Server —— Idea Card V2.1 契约参考实现

端点一览（完整 schema 见 /docs）:
  POST /captures                     提交捕捉（触发事件）→ 返回新卡片 id（模拟 STT+LLM 整理，延迟可配）
  GET  /ideas?query=&tag=&range=     卡片列表 / 搜索（阻断项 #2 定稿参数）
  GET  /ideas/{id}                   卡片详情
  POST /ideas/{id}/confirm           待确认 → ok
  DELETE /ideas/{id}                 软删（30 天保留）
  POST /ideas/{id}/feedback          关联误判反馈
  GET  /collections                  灵感集列表
  POST /collections                  手动新建（阻断项 #2 定稿）
  GET  /review/today                 今日回顾
"""
import asyncio
import copy
import uuid

from fastapi import FastAPI, HTTPException, Query

from .models import (
    CaptureRequest, CaptureResponse, Collection, DailyReview,
    IdeaCard, Extension, ReferenceItem, ExtItem,
)
from . import seed
from .pipeline import run_pipeline, stt_provider, llm_provider

app = FastAPI(title="Avenlo Mock Server", version="0.1.0")

# 内存态（Demo 用；M3 换 SQLite + 文件目录）
_ideas: dict[str, dict] = {c["id"]: copy.deepcopy(c) for c in seed.IDEAS}
_collections: list[dict] = copy.deepcopy(seed.COLLECTIONS)
_lock = asyncio.Lock()

PROCESS_DELAY_S = 3.0   # 模拟 STT+LLM 整理延迟；Demo 可调 0


def _new_card(req: CaptureRequest) -> dict:
    return {
        "id": f"idea_{uuid.uuid4().hex[:12]}",
        "title": "新的灵感",
        "summary": "（模拟 AI 摘要）这条记录已被转写并完成初步整理。",
        "transcript": "（模拟转写文本）……",
        "tags": ["生活"],
        "status": "queued",
        "related": [],
        "extension": {},
        "collectionId": None,
        "audioUrl": req.audioUrl,
        "createdAt": req.ts,
        "durationMs": req.durationMs,
    }


async def _process(card: dict):
    """后台 AI 四层递进：queued → 走可插拔 pipeline（真 STT/LLM 或 mock）→ ok"""
    from .pipeline import MockLlm, MockStt
    mock_mode = isinstance(stt_provider, MockStt) and isinstance(llm_provider, MockLlm)
    if mock_mode:
        await asyncio.sleep(PROCESS_DELAY_S)   # 仅 mock 模式补演示延迟；真链路本身有耗时
    draft = await run_pipeline(card.get("audioUrl"), card.get("durationMs", 0))
    card["title"] = draft.title
    card["summary"] = draft.summary
    card["tags"] = draft.tags
    card["transcript"] = draft.transcript
    card["status"] = "ok"


@app.post("/captures", response_model=CaptureResponse)
async def submit_capture(req: CaptureRequest):
    card = _new_card(req)
    async with _lock:
        _ideas[card["id"]] = card
    asyncio.create_task(_process(card))
    return CaptureResponse(ideaId=card["id"], status=card["status"])


@app.get("/ideas", response_model=list[IdeaCard])
async def list_ideas(
    query: str | None = None,
    tag: str | None = None,
    range: str | None = Query(None, pattern="^(all|today|week|favorite)$"),
):
    cards = [c for c in _ideas.values() if c["status"] != "deleted"]
    if query:
        cards = [c for c in cards if query in c["title"] or query in c["summary"]]
    if tag:
        cards = [c for c in cards if tag in c["tags"]]
    if range == "today":
        cards = [c for c in cards if c["createdAt"] > seed.NOW - 86_400_000]
    elif range == "week":
        cards = [c for c in cards if c["createdAt"] > seed.NOW - 7 * 86_400_000]
    return sorted(cards, key=lambda c: -c["createdAt"])


@app.get("/ideas/{idea_id}", response_model=IdeaCard)
async def get_idea(idea_id: str):
    card = _ideas.get(idea_id)
    if not card or card["status"] == "deleted":
        raise HTTPException(404, "idea not found")
    return card


@app.post("/ideas/{idea_id}/confirm")
async def confirm_idea(idea_id: str):
    card = _ideas.get(idea_id) or (_ for _ in ()).throw(HTTPException(404))
    card["status"] = "ok"


@app.delete("/ideas/{idea_id}")
async def delete_idea(idea_id: str):
    card = _ideas.get(idea_id)
    if not card:
        raise HTTPException(404)
    card["status"] = "deleted"   # 软删：30 天保留（后台任务略）


@app.post("/ideas/{idea_id}/feedback")
async def feedback(idea_id: str, related_id: str, good: bool):
    if idea_id not in _ideas:
        raise HTTPException(404)
    return {"ok": True, "idea": idea_id, "related": related_id, "good": good}


@app.get("/collections", response_model=list[Collection])
async def list_collections():
    return _collections


@app.post("/collections", response_model=Collection)
async def create_collection(name: str):
    """阻断项 #2 定稿：手动新建灵感集（P1 简化：仅名称，聚合仍自动）"""
    col = {"id": f"col_{uuid.uuid4().hex[:8]}", "name": name, "subtitle": "", "count": 0, "tone": "sage", "manual": True}
    _collections.append(col)
    return col


@app.get("/review/today", response_model=DailyReview)
async def daily_review():
    return seed.DAILY_REVIEW


@app.get("/health")
async def health():
    return {"ok": True, "ideas": len(_ideas), "collections": len(_collections)}


@app.get("/pipeline")
async def pipeline_status():
    """当前 STT/LLM 供应商配置（便于 Demo 前快速查验，不泄露 key）"""
    return {
        "stt": type(stt_provider).__name__,
        "llm": type(llm_provider).__name__,
    }
