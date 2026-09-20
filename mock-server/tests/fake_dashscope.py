"""假通义 Dashscope 服务 —— 用于本地自测 DashscopeStt 全链路逻辑

模拟通义录音文件识别的三段式协议：
  POST /api/v1/services/audio/asr/transcription
    → {"output": {"task_id": "task_xxx", "task_status": "PENDING"}}
  GET  /api/v1/tasks/{task_id}
    → 前两次 {"output": {"task_status": "RUNNING"}}
    → 之后  {"output": {"task_status": "SUCCEEDED",
                        "results": [{"transcription_url": ".../results/task_xxx.json"}]}}
  GET  /results/{task_id}.json   （结果文件，下载后才是真正的转写文本）
    → {"transcripts": [{"text": "假STT：这是测试音频的转写文本。"}]}

特殊行为（测试用）：
  - file_urls 含 "data:" → 返回 400（模拟真实服务端拒收 base64，验证本地文件兜底路径）
  - file_urls 含 "FAILAUDIO" → 任务状态走 FAILED（测异常回落）
"""
import json
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI()

# task_id → 提交计数（同一任务第几次被轮询）
poll_counts: dict[str, int] = {}
submit_fail: set[str] = set()


@app.post("/api/v1/services/audio/asr/transcription")
async def submit(req: Request):
    body = await req.json()
    file_url = body["input"]["file_urls"][0]

    if file_url.startswith("data:"):
        # 真实通义会拒收 base64 data URL（file_urls 仅支持公网 URL）
        return JSONResponse(
            status_code=400,
            content={"code": "InvalidParameter", "message": "file_urls only supports public http(s) url"},
        )
    if "FAILAUDIO" in file_url:
        task_id = "task_fail"
        submit_fail.add(task_id)
    else:
        task_id = "task_ok"
    poll_counts[task_id] = 0
    return {"output": {"task_id": task_id, "task_status": "PENDING"}}


@app.get("/api/v1/tasks/{task_id}")
async def poll(task_id: str):
    if task_id in submit_fail:
        return {"output": {"task_id": task_id, "task_status": "FAILED", "message": "audio decode error"}}
    poll_counts[task_id] = poll_counts.get(task_id, 0) + 1
    if poll_counts[task_id] <= 2:
        return {"output": {"task_id": task_id, "task_status": "RUNNING"}}
    return {
        "output": {
            "task_id": task_id,
            "task_status": "SUCCEEDED",
            "results": [{"transcription_url": f"http://127.0.0.1:18777/results/{task_id}.json"}],
        }
    }


@app.get("/results/{task_id}.json")
async def result(task_id: str):
    if task_id == "task_fail":
        return {"transcripts": []}
    return {"transcripts": [{"text": "假STT：这是测试音频的转写文本，约三秒钟。"}]}


@app.get("/health")
async def health():
    return {"ok": True, "fake": "dashscope"}
