"""POST /admin/reset 一键重置端点自测（不依赖外部服务）

跑法：mock-server venv 下
  python tests/test_admin_reset.py

验证点：
  1. 基础重置：脏数据（捕捉卡 + 手动灵感集）→ reset → 恢复种子态
  2. 软删卡后 reset：被删的种子卡恢复
  3. reset 幂等：连续 reset 状态一致
"""
import json
import threading
import time
import urllib.request
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import uvicorn
from app.main import app

PORT = 8098
BASE = f"http://127.0.0.1:{PORT}"


def _get(path):
    with urllib.request.urlopen(f"{BASE}{path}", timeout=5) as r:
        return json.loads(r.read())


def _post(path, data=None):
    req = urllib.request.Request(
        f"{BASE}{path}", method="POST",
        data=json.dumps(data or {}).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=5) as r:
        return json.loads(r.read())


def start_server():
    t = threading.Thread(
        target=uvicorn.run, args=(app,),
        kwargs={"host": "127.0.0.1", "port": PORT, "log_level": "error"},
        daemon=True,
    )
    t.start()
    for _ in range(20):
        try:
            _get("/health")
            return
        except Exception:
            time.sleep(0.3)
    raise RuntimeError("test server not up")


def main():
    os.environ["no_proxy"] = "127.0.0.1,localhost"
    start_server()
    seed_n = len(_get("/ideas"))
    print(f"[0] server up，种子卡数 {seed_n}")

    # ---- 1. 基础重置：脏数据 → 恢复 ----
    _post("/captures", {"audioUrl": None, "ts": int(time.time() * 1000), "durationMs": 3000})
    col = _post("/collections?name=%E8%84%8F%E6%95%B0%E6%8D%AE%E6%B5%8B%E8%AF%95")  # URL 编码「脏数据测试」
    n_ideas = len(_get("/ideas"))
    n_cols = len(_get("/collections"))
    assert n_ideas == seed_n + 1 and n_cols >= 5, f"脏数据制造失败: {n_ideas} ideas {n_cols} cols"
    r = _post("/admin/reset")
    assert r["ok"] and r["ideas"] == seed_n, f"reset 响应异常: {r}"
    n_after = len(_get("/ideas"))
    cols_after = [c["name"] for c in _get("/collections")]
    assert n_after == seed_n, f"重置后卡数 {n_after} != 种子 {seed_n}"
    assert "脏数据测试" not in cols_after, "手动灵感集未被清除"
    print(f"[1] PASS 基础重置：{n_ideas}→{n_after} 卡，手动灵感集已清")

    # ---- 2. 软删种子卡 → reset 恢复 ----
    req = urllib.request.Request(f"{BASE}/ideas/idea_13", method="DELETE")
    with urllib.request.urlopen(req, timeout=5) as r:
        json.loads(r.read())
    n_del = len(_get("/ideas"))
    assert n_del == seed_n - 1, f"软删后 {n_del} != {seed_n - 1}"
    _post("/admin/reset")
    n_restored = len(_get("/ideas"))
    ids = [i["id"] for i in _get("/ideas")]
    assert n_restored == seed_n and "idea_13" in ids, f"reset 未恢复软删卡: {n_restored}"
    print(f"[2] PASS 软删恢复：{n_del}→{n_restored}，idea_13 回来了")

    # ---- 3. reset 幂等 ----
    _post("/admin/reset")
    r2 = _post("/admin/reset")
    n1 = len(_get("/ideas"))
    assert r2["ideas"] == seed_n and n1 == seed_n, "连续 reset 状态漂移"
    print(f"[3] PASS 幂等：连续 reset 稳定在 {seed_n} 卡")

    print("=" * 50)
    print("ALL TESTS PASSED")


if __name__ == "__main__":
    main()
