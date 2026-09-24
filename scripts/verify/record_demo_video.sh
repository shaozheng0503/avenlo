#!/bin/bash
# 备份演示视频录制：按 30 秒 Demo 分镜走完整 P0 链路，adb screenrecord 录屏
# 产物：emulator-screens/demo_backup.mp4
# 前置：模拟器已 boot、server 跑在 127.0.0.1:8000、已装最新 APK
# 第四十三轮（v7）：两段录制 + ffmpeg 无损拼接——弹层段加入后总时长远超
#   screenrecord 180s 硬上限；段1 = P0 链路 + 详情 + 相关想法弹层（第四十一轮），
#   段2 = 搜索 / 灵感集 / 今日回顾 / 我的页（冷启动黑屏处切分最自然）
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens/demo_backup.mp4"
SERVER="http://127.0.0.1:8000"
FFMPEG="C:/ProgramData/chocolatey/bin/ffmpeg.exe"

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

stop_rec() {
  # 关键（第四十三轮实锤）：kill 本地 PID 只断 adb 包装，设备侧 screenrecord 继续录到
  # time-limit 上限（产物带长静止尾巴，v7 首录 329s 的根因）——必须对设备侧发 SIGINT。
  # 且单次 pkill 会因 adb 抖动静默失败（v7 二录段2 实锤跑到 175s 上限）——
  # 带存活验证重试：ps 里 screenrecord 清零才算停妥（[d] 防自匹配）。
  kill $REC_PID 2>/dev/null
  for i in 1 2 3 4 5; do
    "$ADB" shell "pkill -INT screenrecord" 2>/dev/null
    sleep 2
    N=$("$ADB" shell "ps -A | grep -c screenrecor[d]" 2>/dev/null | tr -d '\r')
    if [ "$N" = "0" ]; then sleep 2; return 0; fi
  done
  echo "  WARN: screenrecord 5 次尝试后仍存活，视频可能带尾巴"
}

wait_home_ready() {
  # 冷启动后轮询等首页真正渲染（am start TotalTime ~7s，固定 sleep 6 不够——33 轮教训）
  for i in 1 2 3 4 5 6; do
    sleep 5
    dump_to_tmp
    if grep -q '搜索灵感、关键词、标签\|Hey, Runel' "$PROJ/ui_dump_tmp.xml" 2>/dev/null; then
      echo "  首页就绪（${i}x5s）"; return 0
    fi
  done
  echo "  WARN: 首页未就绪"; return 1
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step0: 重置种子态 ==="
curl -s --noproxy '*' -X POST "$SERVER/admin/reset" > /dev/null
sleep 1

echo "=== step1: 冷启动（视频从首页开始） ==="
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 7

echo "=== step2: 开始录屏·段1（P0 链路 + 详情 + 弹层，180s 上限） ==="
"$ADB" shell "rm -f /sdcard/demo.mp4"
"$ADB" shell "screenrecord --time-limit 175 /sdcard/demo.mp4" &
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
  sleep 4  # 详情上半屏停留（AI 摘要）
else
  echo "  WARN: 新卡未定位到，跳过详情"
fi

echo "=== step7.4: 相关想法弹层演示（第四十一/四十三轮，v7 新增） ==="
# 新卡 related 数随 MockStt 转写轮换（1~2 条）——「共N条 ›」弹性匹配
dump_to_tmp
SHEET=$(find_tap "
for m in re.finditer(r'text=\"共\d+条 ›\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$SHEET" ]; then
  # 相关想法区在详情页下半屏——先滚一屏再重新定位（坐标随滚动上移）
  "$ADB" shell "input swipe 540 1600 540 800 400"; sleep 2
  dump_to_tmp
  SHEET=$(find_tap "
for m in re.finditer(r'text=\"共\d+条 ›\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
fi
if [ -n "$SHEET" ]; then
  "$ADB" shell "input tap $SHEET"; sleep 2
  echo "  弹层已打开（关联原因胶囊展示）"
  sleep 6  # 弹层停留：完整列表 + 关联原因胶囊（可解释性瞬间）
  # 点弹层内关联卡 → 跳其详情（第四十一轮动线）
  dump_to_tmp
  RELCARD=$(find_tap "
for pat in ['清晨的露水', '播客选题', '夜骑的城市观察', '咖啡馆与工作', '城市与人']:
    for m in re.finditer(r'text=\"' + pat + r'\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
        x1,y1,x2,y2 = map(int, m.groups())
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
        break
    else:
        continue
    break
")
  if [ -n "$RELCARD" ]; then
    "$ADB" shell "input tap $RELCARD"; sleep 3
    echo "  已跳转关联卡详情"
    sleep 5  # 关联卡详情停留（延展 + 参考资源）
  else
    echo "  WARN: 弹层内关联卡未定位，跳过跳转"
  fi
else
  echo "  WARN: 「共N条 ›」未定位，跳过弹层段"
fi

echo "=== step7.5: 停段1 + 段2 录屏（搜索 / 灵感集 / 回顾 / 我的） ==="
stop_rec
"$ADB" shell "rm -f /sdcard/demo2.mp4"
"$ADB" shell "screenrecord --time-limit 175 /sdcard/demo2.mp4" &
REC_PID=$!
sleep 2
# 搜索段（冷启动归零，规避状态恢复残留）——必须等首页就绪再进搜索（33 轮教训）
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 1
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
wait_home_ready
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
  wait_home_ready && break
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

echo "=== step7.7: 今日回顾三跳演示（第三十四轮新功能） ==="
# 冷启动归零 → 统计 Tab → 今日最佳大卡跳详情 → 返回 → 意外关联左卡跳详情
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 1
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
wait_home_ready
# 统计 Tab
TJ=$(python "$PROJ/scripts/verify/tap_node.py" "text:统计")
echo "  统计tab: $TJ"
sleep 3
# 今日最佳大卡（金句区域固定坐标——E2E 已验证 540,679）
"$ADB" shell "input tap 540 679"
sleep 5   # 详情停留（AI 摘要 + 相关想法）
# 返回今日回顾
"$ADB" shell "input keyevent 4"; sleep 2
# 滚动到意外关联
"$ADB" shell "input swipe 540 1600 540 900 400"; sleep 2
dump_to_tmp
ZK=$(python "$PROJ/scripts/verify/tap_node.py" "text:城市与人")
if [ -n "$ZK" ]; then
  echo "  意外关联左卡: $ZK"
  sleep 5  # 详情停留
  "$ADB" shell "input keyevent 4"; sleep 2
else
  echo "  WARN: 意外关联左卡未定位"
fi

echo "=== step7.8: 我的页动线演示（第三十五轮新功能） ==="
# 冷启动归零 → 我的 Tab → 灵感集统计格 → 灵感集列表
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 1
"$ADB" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
wait_home_ready
WD=$(python "$PROJ/scripts/verify/tap_node.py" "text:我的")
echo "  我的tab: $WD"
sleep 3
# 滚动到统计三格可见
"$ADB" shell "input swipe 540 1500 540 1000 300"; sleep 2
dump_to_tmp
TG=$(python "$PROJ/scripts/verify/tap_node.py" "text:灵感集")
if [ -n "$TG" ]; then
  echo "  灵感集统计格: $TG"
  sleep 5  # 灵感集列表停留
else
  echo "  WARN: 灵感集统计格未定位"
fi

echo "=== step8: 停录屏 + 拉取两段 + ffmpeg 拼接 ==="
stop_rec
"$ADB" pull /sdcard/demo.mp4 "$PROJ/emulator-screens/part1.mp4" > /dev/null 2>&1 \
  && echo "  part1 pulled" || { echo "FATAL: 段1拉取失败"; exit 1; }
"$ADB" pull /sdcard/demo2.mp4 "$PROJ/emulator-screens/part2.mp4" > /dev/null 2>&1 \
  && echo "  part2 pulled" || { echo "FATAL: 段2拉取失败"; exit 1; }
# concat demuxer 无损拼接（同参数两段 mp4）
printf "file 'part1.mp4'\nfile 'part2.mp4'\n" > "$PROJ/emulator-screens/concat.txt"
"$FFMPEG" -y -f concat -safe 0 -i "$PROJ/emulator-screens/concat.txt" -c copy "$OUT" 2>&1 | tail -2
rm -f "$PROJ/emulator-screens/part1.mp4" "$PROJ/emulator-screens/part2.mp4" "$PROJ/emulator-screens/concat.txt"
echo "saved: $OUT"
ls -la "$OUT" 2>/dev/null | awk '{print $5, $NF}'
ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1 "$OUT" 2>/dev/null || "$FFMPEG" -i "$OUT" 2>&1 | grep Duration
