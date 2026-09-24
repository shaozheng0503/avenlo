"""第四十六轮：解析 canvas.fig（Kiwi+zstd）提取逐屏精确样式 token。

复用第一轮会话验证过的链路：canvas.fig 内含 zstd 块（magic 28b52ffd），
解压后是结构化二进制，字符串表可读。本轮目标：从结构化数据中提取
每个 frame（屏幕）下的 text 节点 fontSize/fills/布局框，输出 JSON。
"""
import re
import json
import zipfile
import struct

FIG = "C:/Users/huangshaozheng/Downloads/Hackathon.fig"
OUT = "C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/figma-export/tokens_raw.json"

z = zipfile.ZipFile(FIG)
data = z.read("canvas.fig")
print(f"canvas.fig: {len(data)} bytes, zstd blocks: {data.count(bytes.fromhex('28b52ffd'))}")

# 定位 zstd 块并解压（第一轮链路：块尾有四字节原始长度小端序）
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
    # 尝试流式解压（zstd 块自包含）
    try:
        reader = decomp.stream_reader(data[idx:])
        raw = reader.read()
        chunks.append(raw)
    except Exception:
        pass
    pos = idx + 4

print(f"zstd blocks found: {found}, decompressed ok: {len(chunks)}")
big = b"".join(chunks)
print(f"total decompressed: {len(big)} bytes")

open("C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/figma-export/canvas_decompressed.bin", "wb").write(big)

# 提取全部 UTF-8 可读字符串（长度>=2）
strings = re.findall(rb"[\x20-\x7e\xe4-\xe9][\x20-\x7e\x80-\xbf]{1,}", big)
uniq = []
seen = set()
for s in strings:
    try:
        t = s.decode("utf-8")
    except Exception:
        continue
    if t not in seen and len(t) >= 2:
        seen.add(t)
        uniq.append(t)

print(f"unique strings: {len(uniq)}")
json.dump(uniq, open("C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/figma-export/strings.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("saved strings.json + canvas_decompressed.bin")
