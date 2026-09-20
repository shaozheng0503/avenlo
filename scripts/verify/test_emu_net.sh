#!/bin/bash
# 模拟器内部网络连通性测试
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"

for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then break; fi
  sleep 5
done
if [ "$ST" != "device" ]; then echo "FATAL: device not ready"; exit 1; fi

echo "=== emulator -> 10.0.2.2:8000/health ==="
"$ADB" shell "curl -s --max-time 5 http://10.0.2.2:8000/health" 2>&1
echo ""
