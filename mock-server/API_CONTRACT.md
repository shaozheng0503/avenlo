# Avenlo API 契约（Idea Card V2.1 · M1 冻结基准）

> 本文件由 Mock Server 实现自描述生成，是 App 端与后端的唯一事实源。
> OpenAPI 交互文档：启动 Mock Server 后访问 `http://localhost:8000/docs`

## 端点

| Method | Path | 说明 | 状态码 |
|--------|------|------|--------|
| POST | `/captures` | 提交捕捉事件 → 返回新卡片 id（模拟 STT+LLM 整理 3s；processed 后**自动关联既有卡**填 related） | 200 |
| POST | `/captures/audio` | **音频直传**（multipart `file` 字段）→ 落盘 `uploads/` → 返回 `{audioUrl, size}`；audioUrl 为相对路径填进 capture | 200/415 |
| GET | `/ideas?query=&tag=&range=` | 卡片列表/搜索（**#2 定稿参数**）range∈all/today/week/favorite | 200 |
| GET | `/ideas/{id}` | 卡片详情（含 extension 三维度） | 200/404 |
| POST | `/ideas/{id}/confirm` | 待确认 → ok | 200/404 |
| DELETE | `/ideas/{id}` | 软删（30 天保留） | 200/404 |
| POST | `/ideas/{id}/feedback?related_id=&good=` | 关联误判反馈 | 200/404 |
| GET | `/collections` | 灵感集列表 | 200 |
| POST | `/collections?name=` | 手动新建（**#2 定稿**） | 200 |
| GET | `/review/today` | 今日回顾 | 200 |
| GET | `/health` | 健康检查 | 200 |
| GET | `/pipeline` | 当前 STT/LLM 供应商（mock / dashscope / openai_compatible，不泄露 key） | 200 |
| POST | `/admin/reset` | **一键重置种子态**（Demo 现场/彩排间恢复，免重启 server）：清捕捉卡、手动灵感集、uploads 音频 | 200 |

## STT/LLM 可插拔配置（pipeline.py）

环境变量驱动，无 key 自动回落 mock（Demo 永远能跑）：

| 变量 | 说明 | 默认 |
|------|------|------|
| `AVENLO_STT_PROVIDER` | `mock` \| `dashscope`（通义 Paraformer 文件识别） | `mock` |
| `AVENLO_STT_API_KEY` | dashscope key | — |
| `AVENLO_LLM_PROVIDER` | `mock` \| `openai_compatible` | `mock` |
| `AVENLO_LLM_API_KEY` | OpenAI 兼容接口 key | — |
| `AVENLO_LLM_BASE_URL` | 兼容接口地址 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `AVENLO_LLM_MODEL` | 模型名 | `qwen-plus` |

> 已用本地假 OpenAI 兼容服务自测通过（`tests/test_pipeline.py`）：markdown 围栏剥离、字段映射、截断保护、异常回落。

## Idea Card 字段（App ↔ Server 共同契约）

```jsonc
{
  "id": "idea_01",
  "title": "关于旅行的灵感",
  "summary": "……",              // AI 摘要
  "transcript": "……",           // 转写原文
  "tags": ["旅行", "生活"],
  "status": "ok",                 // ok | needs_review | queued | syncing | deleted
  "related": [                    // 关联想法
    { "id": "idea_11", "title": "清晨的露水", "relation": "similar_theme",
      "coverUrl": null, "durationMs": 22000, "tag": "自然" }
  ],
  "extension": {                  // 延展三维度（差异化核心）
    "perspectives": [ { "title": "旅行 × 当地文化体验", "desc": "……" } ],
    "references": [ { "title": "The Way We Travel", "url": "https://…" } ],  // #2 定稿：{title,url} 数组
    "directions": [ { "title": "城市观察笔记系列内容", "desc": "……" } ]
  },
  "collectionId": "col_01",
  "audioUrl": null,
  "createdAt": 1726800000000,     // epoch millis
  "durationMs": 136000
}
```

## 触发事件（戒指/模拟器 → Server）

```jsonc
{
  "ts": 1726800000000,
  "durationMs": 45000,
  "gestures": [ { "t": 0, "ev": "pinch" }, { "t": 45000, "ev": "pinch" } ],
  "audioUrl": null                 // 音频直传地址（M3 接真实存储）
}
```

## 状态机

```
capture_end ──▶ queued ──▶ ok
                  │
                  └──▶ needs_review ──(confirm)──▶ ok
任意 ──(delete)──▶ deleted（30 天后物理清除）
```

## 与方案文档的对应
- 方案 1.5 数据契约 → 本文件扩展定稿
- 方案 8.1 #2（契约缺 4 字段）→ 已全部补齐：collection_name / references 结构 / 搜索参数 / POST /collections

## 自动关联行为（第二十四轮上线）

新卡 processed 后 server 自动填充 `related`（App 详情页「相关想法」渲染）：

- **mock 模式**：`_MOCK_RELATED` 预置语义映射——5 条 mock 转写各指定语义关联的种子卡，转写含关键句即命中（子串匹配）
- **真链路**：预留 embedding 相似度替换位（`app/main.py::_match_related`）
- relation 枚举：`similar_theme`（相似主题）/ `same_collection`（同灵感集）/ `time_space`（时间与空间关联）；App 端 `relationLabel()` 渲染中文文案
