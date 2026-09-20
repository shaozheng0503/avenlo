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
import os
import uuid

from fastapi import FastAPI, HTTPException, Query, UploadFile, File

from .models import (
    CaptureRequest, CaptureResponse, Collection, DailyReview,
    IdeaCard, Extension, ReferenceItem, ExtItem,
)
from . import seed
from .pipeline import run_pipeline, stt_provider, llm_provider

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

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
    # audioUrl 是相对路径（uploads/xxx.m4a）→ 转绝对路径给 STT 读
    audio = card.get("audioUrl")
    if audio and not os.path.isabs(audio):
        audio = os.path.join(os.path.dirname(UPLOAD_DIR), audio)
    draft = await run_pipeline(audio, card.get("durationMs", 0))
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


@app.post("/captures/audio")
async def upload_audio(file: UploadFile = File(...)):
    """音频直传（真机链路）：multipart 文件 → server 落盘 → 返回相对路径（填进 capture 的 audioUrl）"""
    ext = os.path.splitext(file.filename or "audio.m4a")[1] or ".m4a"
    if ext not in {".m4a", ".aac", ".mp3", ".wav", ".ogg"}:
        raise HTTPException(415, f"unsupported audio type: {ext}")
    rel = f"uploads/{uuid.uuid4().hex[:12]}{ext}"
    dst = os.path.join(os.path.dirname(UPLOAD_DIR), rel)
    with open(dst, "wb") as f:
        while chunk := await file.read(1 << 16):
            f.write(chunk)
    return {"audioUrl": rel, "size": os.path.getsize(dst)}


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


@app.post("/admin/reset")
async def admin_reset():
    """一键重置为种子态（Demo 现场/彩排间快速恢复，免重启 server）

    清除：所有捕捉产生的卡（保留种子 11 张）、手动新建的灵感集、uploads 音频。
    """
    async with _lock:
        _ideas.clear()
        _ideas.update({c["id"]: copy.deepcopy(c) for c in seed.IDEAS})
        _collections.clear()
        _collections.extend(copy.deepcopy(seed.COLLECTIONS))
    # uploads 音频文件
    cleared_files = 0
    for name in os.listdir(UPLOAD_DIR):
        if name != ".gitkeep":
            try:
                os.remove(os.path.join(UPLOAD_DIR, name))
                cleared_files += 1
            except OSError:
                pass
    return {"ok": True, "ideas": len(_ideas), "collections": len(_collections), "clearedUploads": cleared_files}
