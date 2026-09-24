# -*- coding: utf-8 -*-
"""Round48: 用 Python 直接跑 verify_all.sh（绕过 bash shim 的 tail/wsl 问题）"""
import subprocess, sys, os

BASE = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon'
# 找 Git bash（PortableGit 里的 bash.exe，不走 wsl）
bash = None
for cand in [
    r'C:/Users/huangshaozheng/.workbuddy/binaries/PortableGit/versions/1.2.0/bin/bash.exe',
    r'C:/Program Files/Git/bin/bash.exe',
]:
    if os.path.exists(cand):
        bash = cand
        break
if not bash:
    print('NO BASH FOUND'); sys.exit(1)

env = dict(os.environ)
env['PATH'] = r'C:/Users/huangshaozheng/.workbuddy/binaries/PortableGit/versions/1.2.0/bin;' + env.get('PATH', '')
env['ADB'] = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe')

p = subprocess.run(
    [bash, 'scripts/verify/verify_all.sh'],
    cwd=BASE, env=env, capture_output=True, text=True, timeout=560,
    encoding='utf-8', errors='replace',
)
out = (p.stdout or '') + '\n--- STDERR ---\n' + (p.stderr or '')
print(out[-6000:])
