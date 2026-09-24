# Avenlo —— 灵感捕捉戒指（Anker 黑客松 Hotfix 队）

> 轻捏戒指 → 录音 → AI 转写/摘要/关联/延展 → 灵感卡片。Demo 用 App 内按钮模拟戒指轻捏。

## 工程结构

```
avenlo-android/          # Android App（Kotlin + Compose，三模块为 KMP 预留）
  app/                   #   UI：9 屏 + 导航 + 主题 Token + 震动语言
  core-domain/           #   纯 Kotlin：IdeaCard V2.1 模型 + 捕捉状态机
  core-data/             #   Ktor API client + 内存态 Repository
mock-server/             # FastAPI 后端（Idea Card V2.1 契约参考实现）
  app/main.py            #   13 端点 + 捕捉状态机（/admin/reset 一键重置 + 新卡自动关联）
  app/pipeline.py        #   STT/LLM 可插拔（mock / dashscope / openai_compatible）
  tests/                 #   假 OpenAI + 假 Dashscope 四组自测（含自动关联回归）
scripts/verify/          # 验证脚本（43 轮迭代：一键回归 18 项 + 断网补交 E2E + 演示前 30 秒自检 + 两段式视频录制）
emulator-screens/        # 验证截图 + 备份演示视频（demo_backup.mp4）
30秒Demo分镜脚本.md       # 现场演示分镜（含口播词与彩排清单）
真机Demo指南.md           # 3 分钟真机跑通指南
队友确认清单.md           # 待队友确认的设计决策
Avenlo-Android开发方案与设计规划.md   # 总方案（优先级/排期/风险/逐轮实绩）
```

## 快速开始

### 1. Mock Server

```bash
cd mock-server
pip install -r requirements.txt   # fastapi uvicorn
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
# 验证：浏览器开 http://localhost:8000/health
```

### 2. Android

```bash
cd avenlo-android
./gradlew assembleDebug                                  # 模拟器版（10.0.2.2）
./gradlew assembleDebug -Pavenlo.api.base=http://<电脑局域网IP>:8000   # 真机版
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

环境要求：JDK 17 + Android SDK 34。一键装配见 `setup_android_env.py`。

### 3. 接真实 STT/LLM（可选，拿到 key 后）

```bash
export AVENLO_STT_PROVIDER=dashscope          # 通义 Paraformer
export AVENLO_STT_API_KEY=sk-xxx
export AVENLO_LLM_PROVIDER=openai_compatible # 任意 OpenAI 兼容接口
export AVENLO_LLM_API_KEY=sk-xxx
export AVENLO_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AVENLO_LLM_MODEL=qwen-plus
```

无任何 key 时自动回落 mock，Demo 永远能跑。查当前供应商：`GET /pipeline`。

新卡 processed 后自动关联既有卡（`related` 字段，App 详情页「相关想法」渲染）；
mock 模式为预置语义映射，真链路预留 embedding 相似度。详见 `mock-server/API_CONTRACT.md`。

灵感集支持二级页：列表点卡片进 `CollectionDetailScreen`（collectionId 精确匹配 + 语义标签兜底），
四灵感集均非空（3/5/3/5），徽标 count 与实际条数对齐。

## 契约

`mock-server/API_CONTRACT.md` 是 App ↔ Server 唯一事实源（Idea Card V2.1）。
OpenAPI 交互文档：server 起来后访问 `/docs`。

## 分支约定（现场 24h）

- `main` 随时可演示；功能开发拉 `feat/<名字>`，演示验证过才合回
- P0 链路（捕捉→卡片）改动必须本机过一遍完整 Demo 再合：模拟器 + server 起好后
  `bash scripts/verify/verify_all.sh`（18 项检查，起止自动重置种子态）
- 回归跑挂先查设备状态：模拟器连续多轮回归后可能进入 offline 态，重启即恢复
