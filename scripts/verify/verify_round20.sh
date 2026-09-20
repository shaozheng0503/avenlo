#!/bin/bash
# 第二十轮验证：详情页删除链路（更多菜单 → 确认对话框 → server 软删）
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens"
APK="$PROJ/avenlo-android/app/build/outputs/apk/debug/app-debug.apk"
SERVER="http://127.0.0.1:8000"

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}
dump_to_tmp() {
  for i in 1 2 3; do
    "$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
    "$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
    [ -s "$PROJ/ui_dump_tmp.xml" ] && return 0
    sleep 2
  done
  return 1
}
dump_first() { dump_to_tmp; grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -12; }
find_tap() {
  python << PYEOF
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
$1
PYEOF
}
shot() { "$ADB" shell "screencap -p /sdcard/s.png"; "$ADB" pull /sdcard/s.png "$OUT/$1.png" > /dev/null 2>&1; echo "saved $1"; }

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step0: 重置 + 记录基线 ==="
curl -s --noproxy '*' -X POST "$SERVER/admin/reset" > /dev/null
BASE=$(curl -s --noproxy '*' "$SERVER/ideas" | python -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "基线卡数: $BASE"

echo "=== step1: 装新 APK + 冷启动 ==="
"$ADB" install -r "$APK" 2>&1 | tail -1
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 7

echo "=== step2: 进「山野徒步」详情（idea_13，种子最底部卡） ==="
# 滚动到底部找山野徒步
for i in 1 2 3 4 5; do
  dump_to_tmp
  T=$(find_tap "
for m in re.finditer(r'text=\"山野徒步\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  [ -n "$T" ] && break
  "$ADB" shell "input swipe 540 1800 540 700 300"; sleep 2
done
if [ -z "$T" ]; then echo "FATAL: 找不到山野徒步"; dump_first; exit 1; fi
"$ADB" shell "input tap $T"; sleep 3
dump_first

echo "=== step3: 点「更多」菜单 ==="
dump_to_tmp
MORE=$(find_tap "
for m in re.finditer(r'content-desc=\"更多\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -z "$MORE" ]; then
  # content-desc 可能不可见，用顶栏右侧固定坐标兜底（1080x2400 屏，MoreHoriz 在右上）
  MORE="1010 175"
fi
echo "more at: $MORE"
"$ADB" shell "input tap $MORE"; sleep 2
dump_first
shot "25_more_menu"

echo "=== step4: 点「删除这条灵感」 ==="
dump_to_tmp
DEL=$(find_tap "
for m in re.finditer(r'text=\"删除这条灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -z "$DEL" ]; then echo "FATAL: 删除菜单项未出现"; dump_first; exit 1; fi
"$ADB" shell "input tap $DEL"; sleep 2
dump_first
shot "26_delete_dialog"

echo "=== step5: 确认删除 ==="
dump_to_tmp
CONFIRM=$(find_tap "
for m in re.finditer(r'text=\"删除\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$CONFIRM" ]; then
  "$ADB" shell "input tap $CONFIRM"; sleep 3
  echo "已点确认，等回列表..."
  sleep 3
else
  echo "FATAL: 确认按钮未找到"; dump_first; exit 1
fi

echo "=== step6: 验证 server 软删（卡数应 $((BASE-1))） ==="
AFTER=$(curl -s --noproxy '*' "$SERVER/ideas" | python -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "删除后卡数: $AFTER"
if [ "$AFTER" -eq $((BASE-1)) ]; then
  echo ">>> PASS: server 软删生效（$BASE → $AFTER）"
else
  echo ">>> FAIL: server 卡数异常（$BASE → $AFTER）"
fi
dump_first
shot "27_after_delete"

echo "=== step7: 恢复种子态 ==="
curl -s --noproxy '*' -X POST "$SERVER/admin/reset" > /dev/null
echo "done"
