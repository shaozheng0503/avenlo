"""Idea Card V2.1 契约模型 —— 与 Android 端 core-domain 模型一一对应"""
from datetime import datetime, timezone
from typing import Literal, Optional
from pydantic import BaseModel, Field


class RelatedRef(BaseModel):
    id: str
    title: str
    relation: Literal["same_collection", "similar_theme", "time_space"]
    coverUrl: Optional[str] = None
    durationMs: int = 0
    tag: Optional[str] = None


class ExtItem(BaseModel):
    title: str
    desc: str = ""


class ReferenceItem(BaseModel):
    """阻断项 #2 定稿：references 明确为 {title, url} 数组"""
    title: str
    url: str


class Extension(BaseModel):
    perspectives: list[ExtItem] = []
    references: list[ReferenceItem] = []
    directions: list[ExtItem] = []


CardStatus = Literal["ok", "needs_review", "queued", "syncing", "deleted"]


class IdeaCard(BaseModel):
    id: str
    title: str
    summary: str
    transcript: str
    tags: list[str] = []
    status: CardStatus = "ok"
    related: list[RelatedRef] = []
    extension: Extension = Field(default_factory=Extension)
    collectionId: Optional[str] = None
    audioUrl: Optional[str] = None
    createdAt: int = 0          # epoch millis
    durationMs: int = 0


class Gesture(BaseModel):
    t: int
    ev: Literal["pinch"]


class CaptureRequest(BaseModel):
    """触发事件（戒指 Plan A/B 或 App 模拟上报）"""
    ts: int
    durationMs: int
    gestures: list[Gesture] = []
    audioUrl: Optional[str] = None


class CaptureResponse(BaseModel):
    ideaId: str
    status: CardStatus


class Collection(BaseModel):
    id: str
    name: str
    subtitle: str = ""
    count: int = 0
    coverUrl: Optional[str] = None
    tone: Literal["sage", "peach", "gold", "blue"] = "sage"
    manual: bool = False


class DailyReview(BaseModel):
    date: str
    bestQuote: str
    bestTags: list[str]
    serendipityDesc: str
    pairLeft: dict
    pairRight: dict
    directions: list[ExtItem]
