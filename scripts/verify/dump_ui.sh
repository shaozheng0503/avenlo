#!/bin/bash
# 验证首页内容：等设备 -> UI dump -> 提取文本（覆盖旧文件防陈旧数据）
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"

# 1. 等设备就绪
for i in $(seq 1 12); do
  ST=$("$ADB" get-state 2>/dev/null)
  if [ "$ST" = "device" ]; then break; fi
  sleep 5
done
if [ "$ST" != "device" ]; then echo "FATAL: device not ready"; exit 1; fi

# 2. 删旧 dump 防陈旧数据，重新 dump
"$ADB" shell "rm -f /sdcard/ui.xml"
"$ADB" shell "uiautomator dump /sdcard/ui.xml"
"$ADB" pull /sdcard/ui.xml ./ui_dump.xml

# 3. 提取所有 text 属性
echo "=== UI text ==="
grep -o 'text="[^"]*"' ui_dump.xml | grep -v 'text=""' | head -40
