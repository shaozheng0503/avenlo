# -*- coding: utf-8 -*-
"""Round46: 全屏位采集流水线 —— 8 屏导航（首页/捕捉入口/详情/灵感集/搜索/回顾/我的 + 录音）"""
import subprocess, time, os, sys

ADB = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/platform-tools/adb.exe'
OUT = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emulator-screens/round46'
os.makedirs(OUT, exist_ok=True)
PKG = 'com.hotfix.avenlo'

def adb(*args, timeout=90):
    return subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout).stdout

def wait_boot():
    for i in range(90):
        if adb('shell', 'getprop', 'sys.boot_completed').strip() == '1':
            print(f'boot done ({i*3}s)'); return True
        time.sleep(3)
    return False

def tap(x, y, wait=1.5):
    adb('shell', 'input', 'tap', str(x), str(y)); time.sleep(wait)

def back(wait=1.2):
    adb('shell', 'input', 'keyevent', '4'); time.sleep(wait)

def shot(name, wait_after=0.8):
    time.sleep(wait_after)
    p = f'/sdcard/{name}.png'
    adb('shell', 'screencap', '-p', p)
    subprocess.run([ADB, 'pull', p, os.path.join(OUT, f'{name}.png')], capture_output=True, timeout=60)
    adb('shell', 'rm', p)
    print('shot', name, os.path.getsize(os.path.join(OUT, f'{name}.png')))

def dump_ui(name):
    adb('shell', 'uiautomator', 'dump', '/sdcard/u.xml', timeout=30)
    out = adb('shell', 'cat', '/sdcard/u.xml')
    with open(os.path.join(OUT, f'{name}.xml'), 'w', encoding='utf-8') as f:
        f.write(out)
    return out

def find_bounds(ui, text):
    """从 dump xml 中找含 text 的 node 的 bounds"""
    import re
    for m in re.finditer(r'<node[^>]*text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', ui):
        if text in m.group(1):
            x1, y1, x2, y2 = map(int, m.groups()[1:])
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None

def wait_home(timeout=25):
    for i in range(timeout // 3):
        ui = dump_ui('tmp')
        if '搜索灵感' in ui or 'Hey' in ui or '灵感' in ui:
            return ui
        time.sleep(3)
    return ui

if __name__ == '__main__':
    if not wait_boot():
        print('BOOT TIMEOUT'); sys.exit(1)
    time.sleep(6)
    # 冷启动
    adb('shell', 'am', 'force-stop', PKG); time.sleep(2)
    adb('shell', 'am', 'start', '-n', f'{PKG}/.app.MainActivity')
    time.sleep(12)
    ui = wait_home()
    shot('S01_home')
    # 从首页找灵感集入口
    pos = find_bounds(ui, '灵感集')
    print('灵感集 at', pos)
    if pos:
        tap(*pos, 3); shot('S02_collections'); ui2 = dump_ui('S02')
        back(); time.sleep(1.5)
    # 搜索
    pos = find_bounds(ui, '搜索灵感')
    print('搜索 at', pos)
    if pos:
        tap(*pos, 3); shot('S03_search'); dump_ui('S03')
        back(); time.sleep(1.5)
    # 今日回顾 tab
    ui2 = dump_ui('tmp2')
    pos = find_bounds(ui2, '回顾')
    print('回顾 at', pos)
    if pos:
        tap(*pos, 3); shot('S04_review'); dump_ui('S04')
        back(); time.sleep(1.5)
    # 我的 tab
    pos = find_bounds(ui2, '我的')
    print('我的 at', pos)
    if pos:
        tap(*pos, 3); shot('S05_mine'); dump_ui('S05')
        back(); time.sleep(1.5)
    # 首页第一张卡进详情
    ui3 = dump_ui('tmp3')
    pos = find_bounds(ui3, '阳光透过') or find_bounds(ui3, '咖啡馆') or find_bounds(ui3, '分')
    print('第一卡 at', pos)
    if pos:
        tap(*pos, 3); shot('S06_detail'); dump_ui('S06')
        back(); time.sleep(1.5)
    # 记录 tab
    pos = find_bounds(ui3, '记录')
    print('记录 at', pos)
    if pos:
        tap(*pos, 3); shot('S07_records'); dump_ui('S07')
    print('ALL DONE')
