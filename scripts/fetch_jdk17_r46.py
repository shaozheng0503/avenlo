# -*- coding: utf-8 -*-
"""下载 Temurin JDK17 (TUNA 镜像) 到 ~/.jdks/ 供 Gradle 工具链使用"""
import urllib.request, ssl, zipfile, os, io, sys

dest_root = os.path.expanduser('~/.jdks')
zip_path = os.path.join(dest_root, 'temurin17.zip')
os.makedirs(dest_root, exist_ok=True)

ctx = ssl.create_default_context(); ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
url = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip'

if not os.path.exists(zip_path) or os.path.getsize(zip_path) < 100_000_000:
    print('downloading ...')
    req = urllib.request.Request(url, headers={'User-Agent': 'curl/8'})
    with urllib.request.urlopen(req, timeout=60, context=ctx) as resp, open(zip_path, 'wb') as f:
        total = 0
        while True:
            chunk = resp.read(1 << 20)
            if not chunk: break
            f.write(chunk); total += len(chunk)
            if total % (20 << 20) < (1 << 20):
                print(f'  {total/1e6:.0f} MB', flush=True)
    print(f'downloaded {total/1e6:.0f} MB')

print('extracting ...')
with zipfile.ZipFile(zip_path) as z:
    z.extractall(dest_root)
names = [n for n in os.listdir(dest_root) if 'jdk-17' in n.lower()]
print('extracted:', names)
for n in names:
    exe = os.path.join(dest_root, n, 'bin', 'java.exe')
    print('java.exe exists:', os.path.exists(exe), exe)
