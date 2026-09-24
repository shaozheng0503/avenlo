# -*- coding: utf-8 -*-
"""Round46: 模拟器截图采集工具（adb 全走 Python subprocess，绕开 shell 输出通道问题）"""
import subprocess, sys, time, os

ADB = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/platform-tools/adb.exe'
OUT = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emulator-screens/round46'
os.makedirs(OUT, exist_ok=True)

def adb(*args, timeout=30):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout)
    return r.stdout

def wait_boot():
    for i in range(60):
        v = adb('shell', 'getprop', 'sys.boot_completed').strip()
        if v == '1':
            print(f'boot completed ({i*3}s)')
            return True
        time.sleep(3)
    print('boot timeout')
    return False

def wait_window():
    for i in range(20):
        out = adb('shell', 'dumpsys', 'window', '|', 'grep', '-E', 'mCurrentFocus')
        if 'avenlo' in out:
            print('avenlo focused')
            return True
        time.sleep(2)
    return False

def tap(x, y):
    adb('shell', 'input', 'tap', str(x), str(y))

def swipe(x0, y0, x1, y1, ms=300):
    adb('shell', 'input', 'swipe', str(x0), str(y0), str(x1), str(y1), str(ms))

def back():
    adb('shell', 'input', 'keyevent', '4')

def home():
    adb('shell', 'input', 'keyevent', '3')

def shot(name):
    p = f'/sdcard/{name}.png'
    adb('shell', 'screencap', '-p', p)
    subprocess.run([ADB, 'pull', p, os.path.join(OUT, f'{name}.png')], capture_output=True, timeout=60)
    adb('shell', 'rm', p)
    sz = os.path.getsize(os.path.join(OUT, f'{name}.png'))
    print(f'shot {name}.png {sz}B')

def dump(name):
    out = adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    out = adb('shell', 'cat', '/sdcard/ui.xml')
    with open(os.path.join(OUT, f'{name}.xml'), 'w', encoding='utf-8') as f:
        f.write(out)
    print(f'dump {name}.xml {len(out)}B')

if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'all'
    if cmd in ('boot', 'all'):
        wait_boot()
        time.sleep(5)
    if cmd in ('capture', 'all'):
        # 冷启动 App
        adb('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
        time.sleep(1)
        adb('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
        time.sleep(8)
        shot('R46_home')
        dump('R46_home')
    print('done')
