#!/bin/bash
# Avenlo 全链路复验脚本：等设备 -> 启动 App -> 验证前台
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"

# 1. 等设备就绪（最多 60s）
for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then echo "device ready (try $i)"; break; fi
  sleep 5
done

if [ "$ST" != "device" ]; then echo "FATAL: device not ready"; exit 1; fi

# 2. 启动 App
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity"

# 3. 等冷启动 + refresh 生效
sleep 6

# 4. 验证前台
"$ADB" shell "dumpsys activity activities | grep topResumedActivity" | tail -1
