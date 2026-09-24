#!/usr/bin/env bash
# =============================================================
# 演示前 30 秒自检（第三十九轮）——Demo Day Preflight
# 用途：评委进场前跑一遍，5 项全绿即可开演；红灯按提示修复
# 用法：bash scripts/verify/preflight.sh [设备序列号]
#   不带参数 = 自动选第一台在线设备（模拟器/真机均可）
# =============================================================
set -u
PROJ="$(cd "$(dirname "$0")/../.." && pwd)"
# adb 定位：与 verify_all.sh 同款（沙箱 PATH 无 adb，直接指向 SDK）+ 兜底 PATH
if [ -n "${LOCALAPPDATA:-}" ] && [ -x "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" ]; then
  ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
else
  ADB="${ADB:-adb}"
fi
PASS=0; FAIL=0

green() { printf '\033[32m%s\033[0m\n' "$1"; }
red()   { printf '\033[31m%s\033[0m\n' "$1"; }

# ---------- 设备识别（含 offline→device 恢复等待） ----------
# 沙箱实测：每次 bash 会话的首次 adb 调用会触发 daemon 重启，设备短暂 offline，~2s 恢复
WAITED=0
SERIAL=""
while :; do
  SERIAL="$("$ADB" devices 2>/dev/null | awk 'NR>1 && $2=="device"{print $1; exit}')"
  [ -n "$SERIAL" ] && break
  WAITED=$((WAITED+1))
  if [ "$WAITED" -ge 5 ]; then
    red "✗ 没有在线设备——请插好真机(USB调试)或启动模拟器"
    exit 1
  fi
  sleep 2
done
DEV_MODEL="$("$ADB" -s "$SERIAL" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
IS_EMU=false
case "$SERIAL" in
  emulator-*) IS_EMU=true ;;
esac
if [ -z "$DEV_MODEL" ]; then DEV_MODEL="$SERIAL"; fi
echo "设备: $DEV_MODEL ($SERIAL) $([ "$IS_EMU" = true ] && echo '[模拟器]' || echo '[真机]')"

# ---------- 检查 1：Mock Server 存活（/health + 种子数） ----------
echo ""
echo "── 检查 1/5：Mock Server 存活 ──"
# App 的 server 地址由 gradle.properties 的 avenlo.api.base 决定：
#   模拟器版 = http://10.0.2.2:8000（默认）
#   真机版   = http://192.168.1.40:8000（avenlo.api.base 覆盖）
if [ "$IS_EMU" = true ]; then
  API_HOST="10.0.2.2"; APP_BASE="http://10.0.2.2:8000"
else
  API_HOST="192.168.1.40"; APP_BASE="http://192.168.1.40:8000"
fi
# curl 本机侧探测（10.0.2.2 是模拟器视角的宿主机，本机侧等价 127.0.0.1）
HEALTH_URL="http://127.0.0.1:8000/health"
HEALTH_JSON="$(curl -s --max-time 4 "$HEALTH_URL" 2>/dev/null)"
if echo "$HEALTH_JSON" | grep -q '"ok":true'; then
  IDEAS_N="$(echo "$HEALTH_JSON" | grep -o '"ideas":[0-9]*' | grep -o '[0-9]*$')"
  COLL_N="$(echo "$HEALTH_JSON" | grep -o '"collections":[0-9]*' | grep -o '[0-9]*$')"
  green "✓ Server 存活（种子 $IDEAS_N 灵感 / $COLL_N 灵感集）"
  PASS=$((PASS+1))
else
  red "✗ Server 未启动或 /health 不通 → 修复: 在 mock-server 目录跑 run_in_background 常驻 uvicorn"
  red "  注意：真机演示时 App 侧需 avenlo.api.base=http://$API_HOST:8000 重打包（gradle.properties 第 7 行改后 assembleDebug）"
  FAIL=$((FAIL+1))
fi

# ---------- 检查 2：App 冷启动归位首页 ----------
echo ""
echo "── 检查 2/5：App 冷启动（CLEAR_TASK）→ 首页 ──"
"$ADB" -s "$SERIAL" shell "am force-stop com.hotfix.avenlo" 2>/dev/null; sleep 1
"$ADB" -s "$SERIAL" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" >/dev/null 2>&1
HOME_OK=false
DUMP=""
for I in $(seq 1 15); do
  sleep 2
  # 沙箱实测：adb daemon 频繁重启导致偶发 offline，cat 结果为空——按字节数判断重拉
  DUMP="$("$ADB" -s "$SERIAL" shell "cat /sdcard/ui_preflight.xml" 2>/dev/null | tr -d '\r')"
  SZ=$(printf '%s' "$DUMP" | wc -c)
  if [ "$SZ" -lt 500 ]; then
    "$ADB" -s "$SERIAL" shell "uiautomator dump /sdcard/ui_preflight.xml" >/dev/null 2>&1
    DUMP="$("$ADB" -s "$SERIAL" shell "cat /sdcard/ui_preflight.xml" 2>/dev/null | tr -d '\r')"
  fi
  if printf '%s' "$DUMP" | grep -q 'Hey, Runel\|搜索灵感、关键词、标签'; then
    HOME_OK=true; break
  fi
done
if [ "$HOME_OK" = true ]; then
  green "✓ 冷启动归位首页（CLEAR_TASK 生效，等待 ${I}x2s）"
  PASS=$((PASS+1))
else
  red "✗ 冷启动未到首页 → 排查: server 是否通 / APK 是否为最新构建（gradle.properties 的 avenlo.api.base 是否匹配设备）"
  FAIL=$((FAIL+1))
fi

# ---------- 检查 3：种子数据在屏 ----------
echo ""
echo "── 检查 3/5：首页种子数据在屏 ──"
# 断言收紧（实测教训）：首页 dump 里可见「旅行」标签、「Hey, Runel」问候、时长后缀等
# 这里只验「灵感卡确实渲染」——用时长后缀/「今天·N条」分组头等强特征，防假阳性
if printf '%s' "$DUMP" | grep -q '今天·[0-9]*条\|·[0-9]*分[0-9]*秒\|本周·[0-9]*条'; then
  green "✓ 种子卡在屏（server 数据已下发）"
  PASS=$((PASS+1))
else
  red "✗ 首屏未见种子卡 → 排查: server /ideas 响应 / App 断网兜底种子（SeedData.kt）是否被触发（IP 不匹配时 App 会走本地种子，也需确认 APK 构建版本）"
  FAIL=$((FAIL+1))
fi

# ---------- 检查 4：录音权限已授予 ----------
echo ""
echo "── 检查 4/5：录音权限 ──"
PERM="$("$ADB" -s "$SERIAL" shell "dumpsys package com.hotfix.avenlo | grep -A 1 'RECORD_AUDIO'" 2>/dev/null | tr -d '\r')"
if echo "$PERM" | grep -q 'granted=true'; then
  green "✓ RECORD_AUDIO 已授予"
  PASS=$((PASS+1))
else
  red "✗ RECORD_AUDIO 未授予 → 修复: adb -s $SERIAL shell pm grant com.hotfix.avenlo android.permission.RECORD_AUDIO"
  FAIL=$((FAIL+1))
fi

# ---------- 检查 5：模拟器/真机识别与显示说明 ----------
echo ""
echo "── 检查 5/5：设备与网络配置匹配 ──"
if [ "$IS_EMU" = true ]; then
  if grep -q 'avenlo.api.base=http://10.0.2.2:8000' "$PROJ/avenlo-android/gradle.properties" 2>/dev/null; then
    green "✓ 模拟器模式：gradle.properties 指向 10.0.2.2:8000（当前 APK 兼容）"
    PASS=$((PASS+1))
  else
    red "✗ 模拟器模式但 gradle.properties 不是 10.0.2.2 → 检查 avenlo.api.base 是否被改成真机 IP 后未还原"
    FAIL=$((FAIL+1))
  fi
else
  # 真机：验证手机侧能否到达宿主机 8000 端口（用 App 内 HTTP 不易做，退而检查 gradle.properties + 提示）
  if grep -q "avenlo.api.base=http://$API_HOST:8000" "$PROJ/avenlo-android/gradle.properties" 2>/dev/null; then
    green "✓ 真机模式：gradle.properties 已指向 $API_HOST:8000"
    PASS=$((PASS+1))
  else
    red "✗ 真机模式但 gradle.properties 未指向 $API_HOST:8000 → 修复: 改 gradle.properties 第 7 行后重新 assembleDebug 安装"
    red "  另外确认手机与电脑同一 WiFi，且 Windows 防火墙放行 8000 端口（首次会弹窗）"
    FAIL=$((FAIL+1))
  fi
fi

# ---------- 汇总 ----------
echo ""
echo "═════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
  green "演示就绪：5/5 全绿 ✓  （$(date '+%H:%M:%S')）"
  exit 0
else
  red "未就绪：$FAIL 项红灯 —— 按上面提示逐项修复后重跑"
  exit 1
fi
