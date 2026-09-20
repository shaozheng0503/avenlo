# Avenlo —— 把灵感装进一次轻捏
Anker 首届黑客松 · Hotfix 队 · 智能录音赛道

## 模块
- `app/` — Android 壳：Compose UI、导航、震动、前台服务
- `core-domain/` — 纯 Kotlin：IdeaCard 模型、状态机、用例（KMP 预备）
- `core-data/` — Repository、Ktor API client

## 构建
需 JDK 17 + Android SDK 34（环境装配见 `../setup_android_env.py`）。

```bash
# Git Bash 下构建（已验证 BUILD SUCCESSFUL）
export JAVA_HOME='C:\Users\huangshaozheng\Downloads\android-env\jdk17'
export ANDROID_HOME='C:\Users\huangshaozheng\AppData\Local\Android\Sdk'
export GRADLE_USER_HOME="$USERPROFILE/.gradle"   # 关键：绕开 Program Files 里的旧 init.gradle
./gradlew.bat assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ 本机 GRADLE_USER_HOME 默认指向 Gradle 7.6.1 安装目录，其 init.gradle 与
> 本工程的 settings-repos 模式冲突，必须如上覆盖。

## 真机联调 Mock Server
```bash
cd ../mock-server && uvicorn app.main:app --host 0.0.0.0 --port 8000
```
App 内 `Base URL` 可在设置中切换（默认 `http://10.0.2.2:8000` 模拟器）。
