"""假 OpenAI 兼容服务 —— 用于本地自测 pipeline 的 LLM 真链路逻辑

行为：
  POST /chat/completions
    - user content 含 "BADJSON" → 返回非 JSON 文本（测异常回落）
    - 否则 → 返回 ```json 围栏包裹的 JSON（测 markdown 剥离 + 解析 + 字段映射）
"""
import json
from fastapi import FastAPI, Request

app = FastAPI()


@app.post("/chat/completions")
async def chat(req: Request):
    body = await req.json()
    user_content = body["messages"][-1]["content"]

    if "BADJSON" in user_content:
        content = "抱歉，我今天状态不好，无法输出结构化内容。"
    else:
        content = (
            "```json\n"
            "{\"title\": \"假LLM标题-已解析\", \"summary\": \"假LLM摘要：这段转写讲了测试内容\", \"tags\": [\"测试\", \"链路验证\"]}\n"
            "```"
        )

    return {
        "choices": [{"message": {"role": "assistant", "content": content}}],
        "usage": {"total_tokens": 42},
    }


@app.get("/health")
async def health():
    return {"ok": True, "fake": "openai-compatible"}
