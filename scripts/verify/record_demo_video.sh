#!/bin/bash
# 备份演示视频录制：按 30 秒 Demo 分镜走完整 P0 链路，adb screenrecord 录屏
# 产物：emulator-screens/demo_backup.mp4
# 前置：模拟器已 boot、server 跑在 127.0.0.1:8000、已装最新 APK
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens/demo_backup.mp4"
SERVER="http://127.0.0.1:8000"

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}

find_tap() {
  python << PYEOF
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
$1
PYEOF
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

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step0: 重置种子态 ==="
curl -s --noproxy '*' -X POST "$SERVER/admin/reset" > /dev/null
sleep 1

echo "=== step1: 冷启动（视频从首页开始） ==="
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 7

echo "=== step2: 开始录屏（分段保护：3 分钟上限） ==="
"$ADB" shell "rm -f /sdcard/demo.mp4"
"$ADB" shell "screenrecord --time-limit 170 /sdcard/demo.mp4" &
REC_PID=$!
sleep 2

echo "=== step3: 首页停留 3 秒（问候头 + 种子卡） ==="
sleep 3

echo "=== step4: 长按 FAB（300ms 进度环）→ 直达捕捉屏 ==="
# 长按 = swipe 同点 600ms（input swipe 短距离模拟长按）
"$ADB" shell "input swipe 934 1980 934 1980 600"
sleep 4

echo "=== step5: 点「轻捏开始」→ 录音（模拟器静默 3s 自动保存） ==="
dump_to_tmp
BTN=$(find_tap "
for m in re.finditer(r'text=\"轻捏开始\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$BTN" ]; then
  "$ADB" shell "input tap $BTN"
  echo "  录音中（模拟器走静默自动保存或 60s 上限）..."
  # 轮询等 App 回首页（捕捉流程完成）——检查窗口内容出现「整理中」或首页特征
  for i in $(seq 1 20); do
    sleep 5
    dump_to_tmp
    if grep -q '今天\|整理中\|已保存' "$PROJ/ui_dump_tmp.xml" 2>/dev/null; then
      echo "  已回首页（${i}x5s）"; break
    fi
  done
else
  echo "  WARN: 「轻捏开始」未找到，可能已直接进录音态"
fi

echo "=== step6: 等 QUEUED 卡变成完整卡（2s 轮询在 App 内） ==="
sleep 8

echo "=== step7: 点顶部新卡进详情 ==="
dump_to_tmp
NEWCARD=$(find_tap "
import re as _re
# 找「整理中」或真实感新卡标题（mock 文案已真实感化：通勤/洗碗/咖啡馆/爵士/睡前/散步）
for pat in ['整理中', '通勤', '洗碗', '咖啡馆', '爵士', '睡前', '散步']:
    for m in re.finditer(r'text=\"' + pat + r'[^\"]*\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
        x1,y1,x2,y2 = map(int, m.groups())
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
        break
    else:
        continue
    break
")
if [ -n "$NEWCARD" ]; then
  "$ADB" shell "input tap $NEWCARD"; sleep 3
  echo "  详情页已打开"
  sleep 6  # 详情停留（AI 摘要 + 相关想法 共2条——「旧想法和新想法联系起来」的叙事瞬间）+ 延展
else
  echo "  WARN: 新卡未定位到，跳过详情"
fi

echo "=== step7.5: 搜索演示（标签点击 → 真实结果） ==="
"$ADB" shell "input keyevent 4"; sleep 2
# 回首页后进搜索屏（冷启动归零，规避状态恢复残留）
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 1
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 6
# 搜索框坐标自适应（第三十三轮：硬编码 135,310 已失效）
dump_to_tmp
SB=$(python "$PROJ/scripts/verify/tap_node.py" search_box)
echo "  search_box: $SB"
case "$SB" in TAPPED*) ;; *) echo "  WARN: 搜索框定位失败";; esac
sleep 2
# 处理残留查询
dump_to_tmp
if grep -q 'text="清空"' "$PROJ/ui_dump_tmp.xml" 2>/dev/null; then
  CL=$(python "$PROJ/scripts/verify/tap_node.py" "text:清空")
  echo "  cleared: $CL"
  sleep 1
fi
dump_to_tmp
TAG=$(python "$PROJ/scripts/verify/tap_node.py" "text:#摄影")
if [ -n "$TAG" ]; then
  echo "  tag: $TAG"
  sleep 3
else
  echo "  WARN: #摄影 标签未定位"
fi

echo "=== step7.6: 灵感集内页演示（第三十三轮新功能） ==="
# CLEAR_TASK 冷启动 + 轮询等首页真正渲染（am start TotalTime ~7s，固定 sleep 6 不够）
for TRY in 1 2 3; do
  "$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 1
  "$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  for i in 1 2 3 4 5 6; do  # 最多再等 30s，每 5s 查一次
    sleep 5
    dump_to_tmp
    if grep -q '搜索灵感、关键词、标签\|Hey, Runel' "$PROJ/ui_dump_tmp.xml" 2>/dev/null; then
      echo "  首页就绪（第${TRY}轮·${i}x5s）"; break 2
    fi
  done
  echo "  首页未就绪，重试（第${TRY}轮）"
done
# tap 前再 dump 一次（拿到的是「当前屏」坐标，防上一轮 dump 过期）
dump_to_tmp
CI=$(python "$PROJ/scripts/verify/tap_node.py" collections_icon)
echo "  collections_icon: $CI"
sleep 3
# 点第一张灵感集卡（「旅行灵感」或按 bounds 找含"灵感集"字样的卡片）
dump_to_tmp
COL=$(python "$PROJ/scripts/verify/tap_node.py" "text:旅行灵感")
if [ -n "$COL" ]; then
  echo "  collection: $COL"
  sleep 6  # 内页停留：顶栏 + N条 + 卡片列表
else
  echo "  WARN: 灵感集卡片未定位"
fi

echo "=== step8: 停止录屏并拉取 ==="
kill $REC_PID 2>/dev/null
sleep 3
"$ADB" pull /sdcard/demo.mp4 "$OUT" && echo "saved: $OUT" || echo "FATAL: 录屏拉取失败"
ls -la "$OUT" 2>/dev/null | awk '{print $5, $NF}'
