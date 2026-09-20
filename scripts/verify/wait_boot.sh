#!/bin/bash
# 等模拟器 boot 完成
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
for i in $(seq 1 24); do
  BOOT=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$BOOT" = "1" ]; then echo "boot completed (try $i, ~$((i*5))s)"; exit 0; fi
  sleep 5
done
echo "FATAL: boot timeout"; exit 1
