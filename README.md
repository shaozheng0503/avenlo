<div align="center">

# 🎙️ Avenlo —— 灵感捕捉戒指

**轻捏戒指 → 录音 → AI 转写 / 摘要 / 关联 / 延展 → 灵感卡片**

Anker 首届黑客松挑战赛 · Hotfix 队参赛作品

[![Platform](https://img.shields.io/badge/platform-Android%2010%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-Mock%20Server-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![License](https://img.shields.io/badge/license-MIT-green)]()

*Demo 模式用 App 内按钮模拟戒指轻捏，无需硬件即可完整体验。*

</div>

---

## ✨ 功能特性

### 🎯 核心链路
- **戒指轻捏捕捉** —— FAB 短按启动录音（长按 300ms 模拟真实戒指轻捏手势，对齐规格站 `PINCH_MIN_MS`）
- **AI 灵感加工** —— 语音 → 文本（STT）→ 摘要 / 标签 / 关联 / 延展（LLM），一条龙产出结构化「灵感卡片」
- **灵感脉络** —— 详情页可视化关联想法，「为什么关联」（relation 文案）可解释
- **灵感集** —— 主题化收藏（旅行 / 生活 / 成长 / 家居…），支持新建与二级内页浏览

### 🛡️ 工程韧性
- **断网补交重试队列** —— 录音先落盘，断网时占位卡不滞留「整理中」，恢复后自动补交
- **全屏真实数据源** —— 搜索 / 回顾 / 灵感集 / 详情 server 优先 + 断网回落本地种子数据
- **新卡自动关联** —— 新灵感 processed 后自动挂载既有卡片的 `related` 引用
- **一键重置** —— `POST /admin/reset` 秒回种子态，Demo 现场免重启

### 🧪 质量守护
- **回归保护网 18 项** —— `verify_all.sh` 一键全量回归（起止自动重置种子态）
- **端到端连贯性验证 33 项** —— 冷启动到统计筛选联动全链路 PASS
- **演示前 30 秒自检** —— `preflight.sh` 五项检查全绿再上台

---

## 🏗️ 项目结构

```
avenlo/
├── avenlo-android/          # Android App（Kotlin + Compose，三模块为 KMP 预留）
│   ├── app/                 #   UI：9 屏 + 导航 + 主题 Token + 震动语言
│   ├── core-domain/         #   纯 Kotlin：IdeaCard V2.1 模型 + 捕捉状态机
│   └── core-data/           #   Ktor API client + 内存态 Repository
├── mock-server/             # FastAPI 后端（Idea Card V2.1 契约参考实现）
│   ├── app/main.py          #   13 端点 + 捕捉状态机 + 自动关联
│   ├── app/pipeline.py      #   STT/LLM 可插拔（mock / dashscope / openai_compatible）
│   └── tests/               #   假 OpenAI + 假 Dashscope 四组自测
├── scripts/
│   ├── verify/              #   回归脚本 28 个（一键回归 18 项 + E2E + 自检 + 录视频）
│   └── download_stt_models.py  #   STT 大模型下载（见下方说明）
├── emulator-screens/        # 9 屏验证截图 + 备份演示视频
├── 30秒Demo分镜脚本.md       # 现场演示分镜（含口播词与彩排清单）
├── 真机Demo指南.md           # 3 分钟真机跑通指南
├── 队友确认清单.md           # 待队友确认的设计决策
└── Avenlo-Android开发方案与设计规划.md   # 总方案（优先级/排期/风险/逐轮实绩）
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Android SDK | 34 |
| Python | 3.10+ |
| Android Studio | Hedgehog 及以上（可选） |

> 💡 一键装配环境：`python setup_android_env.py`

### 1️⃣ 启动 Mock Server

```bash
cd mock-server
pip install -r requirements.txt          # fastapi + uvicorn
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

验证：浏览器打开 <http://localhost:8000/health>，或访问 <http://localhost:8000/docs> 查看交互式 API 文档。

### 2️⃣ 构建 Android App

```bash
cd avenlo-android

# 模拟器版（API 指向 10.0.2.2）
./gradlew assembleDebug

# 真机版（指向电脑局域网 IP）
./gradlew assembleDebug -Pavenlo.api.base=http://<电脑局域网IP>:8000

# 产物：app/build/outputs/apk/debug/app-debug.apk
```

### 3️⃣ 跑回归验证（可选）

```bash
bash scripts/verify/verify_all.sh    # 一键全量回归，18 项检查 PASS/FAIL 汇总
python scripts/verify_r52_e2e.py     # 端到端连贯性验证，33 项
bash scripts/verify/preflight.sh     # 演示前 30 秒自检
```

---

## 🔌 接入真实 STT / LLM（可选）

无任何 key 时自动回落 mock，**Demo 永远能跑**。拿到 key 后切换：

```bash
export AVENLO_STT_PROVIDER=dashscope           # 通义 Paraformer
export AVENLO_STT_API_KEY=sk-xxx
export AVENLO_LLM_PROVIDER=openai_compatible   # 任意 OpenAI 兼容接口
export AVENLO_LLM_API_KEY=sk-xxx
export AVENLO_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AVENLO_LLM_MODEL=qwen-plus
```

查当前供应商：`GET /pipeline`。

### 📦 本地 STT 模型下载

App 内置离线语音识别（sherpa-onnx zipformer 中英双语 int8）。模型文件较大（~190MB）不入库，克隆后执行：

```bash
python scripts/download_stt_models.py
# 会下载并放置到 avenlo-android/app/src/main/assets/stt-model/
```

或手动下载 [sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16.tar.bz2)，解压后将 `encoder/decoder/joiner *.onnx + tokens.txt + bpe.model` 放入 `avenlo-android/app/src/main/assets/stt-model/`。

---

## 📡 API 契约

`mock-server/API_CONTRACT.md` 是 App ↔ Server 唯一事实源（**Idea Card V2.1**），核心端点：

| 端点 | 说明 |
|------|------|
| `POST /capture/start` · `/capture/stop` | 捕捉状态机（录音上传） |
| `GET /cards` · `GET /cards/{id}` | 灵感卡片查询 |
| `POST /cards/{id}/related` | 关联引用 |
| `GET /collections` · `POST /collections` | 灵感集管理 |
| `POST /admin/reset` | 一键重置种子态 |
| `GET /pipeline` | 查看当前 STT/LLM 供应商 |

---

## 🤝 分支约定（现场 24h）

- `main` 随时可演示；功能开发拉 `feat/<名字>`，演示验证过才合回
- P0 链路（捕捉→卡片）改动必须本机过一遍完整 Demo 再合：

```bash
bash scripts/verify/verify_all.sh    # 模拟器 + server 起好后，18 项检查
```

- 回归跑挂先查设备状态：模拟器连续多轮回归后可能进入 offline 态，重启即恢复

---

<div align="center">

**Hotfix 队** · Anker 首届黑客松挑战赛

轻捏一下，灵感不再溜走 ✨

</div>
