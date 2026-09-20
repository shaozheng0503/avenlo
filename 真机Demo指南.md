# Avenlo 真机 Demo 指南（3 分钟跑通）

## 准备清单
- 真机一台（Android 8.0+），与电脑**同一 WiFi**
- 电脑端 Mock Server 已启动（绑 0.0.0.0，局域网可达）

## 步骤

### 1. 启动 Mock Server（电脑端）

```powershell
cd mock-server
C:/Users/huangshaozheng/.workbuddy/binaries/python/envs/avenlo-mock/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

验证（任意浏览器打开）：`http://192.168.1.40:8000/health` → 应返回 `{"ok":true,...}`

> IP 说明：192.168.1.40 是当前 WiFi 下电脑的局域网 IP，换网络后用 `ipconfig` 查 WLAN IPv4 并重编 APK（见文末「换网络重建」）。

### 2. 安装 APK（手机端）

方式 A（USB，推荐）：
```powershell
adb install -r avenlo-android/app/build/outputs/apk/debug/app-debug.apk
```

方式 B（无 USB）：把 APK 通过微信/QQ 发到手机，点击安装（允许未知来源）。

### 3. 跑 Demo 流程

1. 打开 Avenlo → 首页出现 11 条种子灵感卡
2. **长按**右下角「轻捏」按钮 300ms（进度环走满）→ 自动进入录音（首次需授权麦克风；短按则进准备页再点一次开始）→ 说话
3. **停说 3 秒** → 自动结束并保存（或点「再次轻捏·结束并保存」）
4. 自动回首页 → 顶部出现「整理中…」卡（QUEUED）
5. ~3 秒后自动变成完整卡（标题/摘要/标签）
6. 点卡片进详情页 → AI 摘要 + 相关想法（可点击跳转其详情）+ 参考资源
7. 底部导航 4 Tab：首页 / **记录**（按日期分组的时间线，捕捉提交后自动出卡）/ 统计（今日回顾）/ 我的（真实统计 + 电量环）
8. 灵感集页右上「+」可新建灵感集（server 落卡），2×2 照片网格

### 4. 验证写入是否真实

电脑端浏览器打开 `http://192.168.1.40:8000/ideas`，列表最上面应出现你刚录的那条（标题来自 MockLlm 从转写文本提炼）。灵感集新建也可在 `http://192.168.1.40:8000/collections` 验证（manual=true 为 App 新建）。

## 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 首页空列表 | 电脑防火墙拦了 8000 端口 | Windows 安全中心 → 防火墙 → 允许 Python/8000 端口入站 |
| 卡片一直「整理中」 | server 没重启或 IP 变了 | 查 `/health`；确认 APK 内 base URL 与当前 IP 一致 |
| 按钮无反应 | 录音权限被拒 | 系统设置 → 应用 → Avenlo → 权限 → 麦克风 |
| 静默不自动结束 | 麦克风灵敏度差异 | `CaptureScreen.kt` 搜 `SILENCE_AMP_THRESHOLD`（默认 800）调小 |

## 换网络重建 APK

IP 变了（换了 WiFi/热点）需要重编：

```powershell
cd avenlo-android
./gradlew assembleDebug -Pavenlo.api.base=http://<新IP>:8000
```

不重编的偷懒办法：电脑开热点固定 IP，或路由器给电脑做 IP 绑定。
