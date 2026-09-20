#!/bin/bash
# 取 crash 栈中 com.hotfix 帧
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then break; fi
  sleep 5
done
"$ADB" logcat -d -b crash -t 200 2>&1 | grep -A 3 "FATAL EXCEPTION" | head -8
echo "=== app frames ==="
"$ADB" logcat -d -b crash -t 200 2>&1 | grep "com.hotfix" | head -10
echo "=== all frames between FATAL and ViewGroup ==="
"$ADB" logcat -d -b crash -t 200 2>&1 | sed -n '/FATAL EXCEPTION/,/ViewGroup/p' | head -25
