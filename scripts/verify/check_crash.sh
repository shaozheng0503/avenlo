#!/bin/bash
# 查 App 崩溃/ANR 日志
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
echo "=== crash logs ==="
"$ADB" logcat -d -b crash -t 50 2>&1 | tail -40
