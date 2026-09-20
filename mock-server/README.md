# Avenlo Mock Server

Anker 黑客松 Hotfix 队 · Idea Card V2.1 契约参考实现（M1 冻结基准），App 端与后端从这里对齐。

## 启动

```bash
cd mock-server
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- 契约文档（Swagger）: http://localhost:8000/docs
- 依赖：`fastapi`、`uvicorn[standard]`（见 requirements，Python 3.11+）

## 核心行为

### Pipeline（可插拔）

`app/pipeline.py` 按环境变量自动选择实现：

| 环境变量 | 行为 |
|---|---|
| 无（默认） | **MockStt / MockLlm**——5 条真实感转写按 `durationMs` 轮换（`idx = (durationMs // 1000) % 5`），标题/摘要/标签从转写提炼（第二十二轮真实感化） |
| `DASHSCOPE_API_KEY` 设置 | 真链路：通义 STT（paraformer）+ qwen（LLM 整理），失败自动回落 mock |

### 自动关联（第二十四轮上线）

新卡 processed 后自动关联既有卡——「旧想法和新想法联系起来了」叙事的 server 侧支撑：

- **mock 模式**：`_MOCK_RELATED` 预置语义映射（5 条 mock 转写 → 语义关联的种子卡），转写含关键句即命中（子串匹配，非开头）
- **真链路**：`_match_related` 注释预留 embedding 相似度替换位
- 关联写入新卡 `related` 字段（title/relation/durationMs/tag），App 详情页「相关想法」区块渲染

### 其他端点行为

- `POST /admin/reset`：一键重置种子态（Demo 现场恢复，免重启）
- `DELETE /ideas/{id}`：软删（30 天保留语义）
- `POST /captures/audio`：真机链路 multipart 音频直传（server 落盘，返回 `uploads/` 相对路径）

## 测试

```bash
# 独立端口、daemon 线程模式，不依赖外部服务（各自可单独跑）
python tests/test_admin_reset.py     # reset 端点 3 用例
python tests/test_pipeline.py        # pipeline 回落/markdown 解析 4 用例
python tests/test_dashscope_stt.py   # 通义 STT 链路 5 用例（假服务）
python tests/test_auto_related.py    # 自动关联 8 用例（5 转写全命中 + HTTP 全链路）
```

## App 端联动注意

- App `IdeaRepositoryImpl` 为内存态（M3 接 Room）：**server 重启/reset 后 App 需冷启动**拉新数据，否则详情页显示旧缓存
- App 详情页轮询 QUEUED 卡（2s）直至 ok——mock 延迟 `PROCESS_DELAY_S=3s` 后即完成
