#!/bin/bash
# 查 App 崩溃日志 + 前台状态
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then break; fi
  sleep 5
done
echo "=== topResumedActivity ==="
"$ADB" shell "dumpsys activity activities | grep topResumedActivity" | tail -1
echo "=== crash buffer ==="
"$ADB" logcat -d -b crash -t 30 2>&1 | tail -30
echo "=== Avenlo process ==="
"$ADB" shell "ps -A | grep avenlo"
