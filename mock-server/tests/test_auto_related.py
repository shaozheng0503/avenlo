#!/usr/bin/env python
"""自动关联回归测试：新卡 processed 后 related 应按 _MOCK_RELATED 语义映射填充。

背景（第二十四轮踩坑）：_match_related 首版用 startswith，但转写实际以
「今天」「刚才洗碗的时候…」等开头，5 条映射里 3 条永远匹配不上。
本测试锁住「子串匹配 + 全部 5 条转写均命中」的行为。

跑法：python tests/test_auto_related.py（与既有 tests 同风格，独立拉起 daemon 线程 server）
"""
import asyncio
import copy
import json
import os
import sys
import threading
import time
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import uvicorn
from app import seed
from app.main import _match_related, _MOCK_RELATED
from app.pipeline import CardDraft, MockStt

PORT = 8903
BASE = f"http://127.0.0.1:{PORT}"

results = []


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(f"[{'PASS' if cond else 'FAIL'}] {name}" + (f"  {detail}" if detail else ""))


def main():
    # ── 单元级：_match_related 直接对 5 条 mock 转写 ──
    ideas = {c["id"]: copy.deepcopy(c) for c in seed.IDEAS}
    import app.main as m
    orig = m._ideas
    m._ideas = ideas
    try:
        stt = MockStt()
        for i, transcript in enumerate(stt._TRANSCRIPTS):
            draft = CardDraft(title="t", summary="s", tags=["生活"], transcript=transcript)
            rel = _match_related({"id": "x"}, draft)
            expected_pairs = _MOCK_RELATED.get(next((k for k in _MOCK_RELATED if k in transcript), ""), [])
            check(f"转写[{i}]「{transcript[:12]}…」命中 {len(expected_pairs)} 条关联",
                  len(rel) == len(expected_pairs),
                  f"got: {[(r['title'], r['relation']) for r in rel]}")
    finally:
        m._ideas = orig

    # ── 集成级：真实 HTTP 链路（POST /captures → 等 processed → related 填充）──
    config = uvicorn.Config("app.main:app", host="127.0.0.1", port=PORT, log_level="error")
    server = uvicorn.Server(config)
    t = threading.Thread(target=server.run, daemon=True)
    t.start()
    for _ in range(50):
        try:
            urllib.request.urlopen(f"{BASE}/health", timeout=1); break
        except Exception:
            time.sleep(0.2)
    else:
        print("FAIL: server 未启动"); sys.exit(1)

    try:
        # durationMs=5000 → MockStt idx=0 → 通勤转写
        req = urllib.request.Request(
            f"{BASE}/captures",
            data=b'{"ts":' + str(int(time.time() * 1000)).encode() + b',"durationMs":5000,"audioUrl":null}',
            headers={"Content-Type": "application/json"}, method="POST")
        resp = urllib.request.urlopen(req, timeout=5)
        idea_id = json.loads(resp.read())["ideaId"]
        time.sleep(4.5)  # PROCESS_DELAY_S=3 + 余量

        detail = json.loads(urllib.request.urlopen(f"{BASE}/ideas/{idea_id}", timeout=5).read())
        titles = [r["title"] for r in detail.get("related", [])]
        check("HTTP 链路：通勤新卡 related=2",
              len(detail.get("related", [])) == 2, f"titles={titles}")
        check("HTTP 链路：关联含「播客选题：慢生活」", "播客选题：慢生活" in titles)
        check("HTTP 链路：关联含「夜骑的城市观察」", "夜骑的城市观察" in titles)
    finally:
        server.should_exit = True
        t.join(timeout=5)

    print("=" * 56)
    if all(ok for _, ok in results):
        print("ALL TESTS PASSED")
    else:
        print("SOME TESTS FAILED"); sys.exit(1)


if __name__ == "__main__":
    main()
