# -*- coding: utf-8 -*-
"""R48: 重启模拟器（avenlo_test 或 pixel_6 API34），等待 boot 完成"""
import subprocess, time, os, sys

EMU = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/emulator/emulator.exe')
ADB = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe')

def sh(*args, timeout=40):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

# 启动 AVD（avenlo_test 优先，带 -no-snapshot-load 冷启）
avd = 'avenlo_test'
p = subprocess.Popen(
    [EMU, '-avd', avd, '-no-snapshot-load', '-no-boot-anim', '-gpu', 'auto'],
    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    creationflags=0x00000008,  # DETACHED_PROCESS，脱离本进程生命周期
)
print('emulator pid:', p.pid)

# 等 boot
for i in range(90):
    time.sleep(3)
    out = sh('shell', 'getprop', 'sys.boot_completed', timeout=10)
    if '1' in out:
        print('BOOT DONE at ~%ds' % (3 * (i + 1)))
        break
    if i % 10 == 0:
        print('waiting... %ds' % (3 * (i + 1)))
else:
    print('BOOT TIMEOUT')
    sys.exit(1)

print(sh('devices'))
