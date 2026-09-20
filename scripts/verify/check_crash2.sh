#!/bin/bash
# 取 crash 日志的头部（异常类型 + 消息 + 栈顶）
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then break; fi
  sleep 5
done
"$ADB" logcat -d -b crash -t 200 2>&1 | grep -E "FATAL|AndroidRuntime: (Process|java|kotlin|Caused|com.hotfix)" | head -20
