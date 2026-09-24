# -*- coding: utf-8 -*-
"""Round46 final: 启动更新后的 App，重截 4 主 Tab（视觉回归基准）"""
import subprocess, time, os

ADB = r'C:\Users\huangshaozheng\AppData\Local\Android\Sdk\platform-tools\adb.exe'
OUT = r'C:\Users\huangshaozheng\WorkBuddy\2026-09-20-14-20-48\anker-hackathon\emulator-screens\round46_final'
os.makedirs(OUT, exist_ok=True)

def adb(*args, timeout=20):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout).stdout

def shot(name):
    adb('exec-out', 'screencap -p', timeout=30)  # warm
    r = subprocess.run([ADB, 'exec-out', 'screencap', '-p'], capture_output=True, timeout=30)
    with open(os.path.join(OUT, name), 'wb') as f:
        f.write(r.stdout)
    print('shot', name, len(r.stdout), 'bytes')

def tap(x, y):
    adb('shell', 'input', 'tap', str(x), str(y))

# 冷启动
adb('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
time.sleep(1)
adb('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
print('launched, waiting splash...')
time.sleep(6)
# 跳过 splash（点 CTA：屏幕中下方）
tap(540, 1850)
time.sleep(3)
shot('F01_home.png')

# 记录 tab（底导 y≈2337, 4 tab x = 135/405/675/945 @1080）
tap(405, 2337); time.sleep(2); shot('F02_records.png')
# 统计 tab
tap(675, 2337); time.sleep(2); shot('F03_review.png')
# 我的 tab
tap(945, 2337); time.sleep(2); shot('F04_mine.png')
# 灵感集（首页搜索框右侧图标 → 回首页先进搜索再点灵感集入口不可靠，直接深链不可用则手动：回首页点搜索）
tap(135, 2337); time.sleep(2)
tap(540, 560)   # 搜索框位置（首屏搜索条 y≈ 400~600 区域）
time.sleep(2)
shot('F05_search.png')
print('done')
