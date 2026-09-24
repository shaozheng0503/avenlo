# -*- coding: utf-8 -*-
"""Round50: 解析新版 Hotfix.fig —— 解压 zstd、提取字符串表与内嵌图片资产。
与 Round46 链路相同，输出到 figma-export/r50/ 目录。"""
import re
import json
import zipfile
import os
import hashlib

FIG = "C:/Users/huangshaozheng/xwechat_files/wxid_afjhzsd2mtqf22_d8a4/temp/RWTemp/2026-09/9e20f478899dc29eb19741386f9343c8/Hotfix.fig"
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, 'figma-export', 'r50')
os.makedirs(OUT, exist_ok=True)
os.makedirs(os.path.join(OUT, 'images'), exist_ok=True)

z = zipfile.ZipFile(FIG)
names = z.namelist()
print("== zip entries ==")
for n in names[:40]:
    info = z.getinfo(n)
    print(f"  {n}  {info.file_size}B")

data = z.read("canvas.fig")
print(f"\ncanvas.fig: {len(data)} bytes, zstd blocks: {data.count(bytes.fromhex('28b52ffd'))}")

import zstandard as zstd
decomp = zstd.ZstdDecompressor()
chunks = []
pos = 0
found = 0
while True:
    idx = data.find(bytes.fromhex("28b52ffd"), pos)
    if idx < 0:
        break
    found += 1
    try:
        reader = decomp.stream_reader(data[idx:])
        raw = reader.read()
        if len(raw) > 0:
            chunks.append(raw)
    except Exception:
        pass
    pos = idx + 4

big = b"".join(chunks)
print(f"zstd blocks found: {found}, decompressed ok: {len(chunks)}, total: {len(big)} bytes")
open(os.path.join(OUT, "canvas_decompressed.bin"), "wb").write(big)

# 提取中文 UI 文案 + 有意义的英文串（带 offset，供树序关联）
strings = []
for m in re.finditer(rb'(?:[\xe4-\xe9][\x80-\xbf][\x80-\xbf]|[\x20-\x7e]){2,80}', big):
    try:
        s = m.group().decode('utf-8')
    except Exception:
        continue
    if re.search(r'[\u4e00-\u9fff]', s):
        strings.append((m.start(), s))

zh_uniq = []
seen = set()
for o, s in strings:
    if s not in seen:
        seen.add(s)
        zh_uniq.append((o, s))
print(f"\nchinese strings: {len(strings)}, unique: {len(zh_uniq)}")

json.dump([{"offset": o, "text": s} for o, s in zh_uniq],
          open(os.path.join(OUT, "strings_zh.json"), "w", encoding="utf-8"),
          ensure_ascii=False, indent=1)

# 打印全部中文串（新版全量，人工比对找新增/改动）
for o, s in zh_uniq:
    print(o, s)
print("\nsaved -> r50/strings_zh.json + canvas_decompressed.bin")
