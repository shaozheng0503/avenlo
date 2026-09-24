# -*- coding: utf-8 -*-
"""R48: 模拟器守护脚本——由 Bash run_in_background 托管，进程不脱离会话"""
import subprocess, time, os, sys

EMU = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/emulator/emulator.exe')
ADB = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe')

def sh(*args, timeout=40):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

# 清理旧 adb server
sh('kill-server')
time.sleep(1)

p = subprocess.Popen(
    [EMU, '-avd', 'avenlo_test', '-no-snapshot-load', '-no-boot-anim', '-gpu', 'auto'],
    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
)
print('emulator pid:', p.pid, flush=True)

for i in range(100):
    time.sleep(3)
    out = sh('shell', 'getprop', 'sys.boot_completed', timeout=10)
    if '1' in out:
        print('BOOT DONE at ~%ds' % (3 * (i + 1)), flush=True)
        break
    if i % 10 == 0:
        print('waiting... %ds' % (3 * (i + 1)), flush=True)
else:
    print('BOOT TIMEOUT', flush=True)
    sys.exit(1)

print(sh('devices'), flush=True)
# 装新 APK
print(sh('install', '-r', r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/avenlo-android/app/build/outputs/apk/debug/app-debug.apk'), flush=True)
print('READY', flush=True)

# 保持存活（守护）：模拟器进程挂了才退出
while True:
    if p.poll() is not None:
        print('emulator exited', flush=True)
        break
    time.sleep(10)
