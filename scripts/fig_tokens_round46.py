"""第四十六轮 Phase2：Kiwi 二进制结构化解析——提取 text 节点样式。

Kiwi schema（fig-kiwij）：payload 里每条消息有 field-ids。已知 figma kiwi
schema 关键 field（反编译 .fig 的通用知识）：
  - TEXT 节点: fontSize, fontFamily, fontWeight, fills, characters
  - FrameNode/RectangleNode: width/height/cornerRadius/fills
策略：不做完整 schema 还原（工程量大），走「结构共现」——
在解压后的二进制里，浮点数以 f64 和 f32 两种形式存。
fontSize（如 16.0）与圆角（如 12.0）都是 double。
颜色以 RGBA u32 或 4×f32 存储。

实用路线：对已知 token 候选值做全文定位 + 邻近字符串共现分析，
交叉验证 design spec 站已有结论，产出可信 token 表。
"""
import re
import json
import struct
from collections import Counter, defaultdict

BIN = "C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/figma-export/canvas_decompressed.bin"
big = open(BIN, "rb").read()

# ---- 1. 提取所有 f32 浮点值 + 频次（字号/间距/圆角都是常见小数值）----
f32s = Counter()
usable = len(big) - (len(big) % 4)
for (v,) in struct.iter_unpack("<f", big[:usable]):
    if v != 0 and abs(v) < 10000 and v == int(v):
        f32s[int(v)] += 1

print("f32 整数值 Top40（字号/间距/圆角候选）:")
for v, c in f32s.most_common(40):
    print(f"  {v}: {c}")

# ---- 2. RGBA 颜色：kiwi 里 color 是 {r,g,b,a} 四个 f32 (0..1) ----
# 扫描连续 4×f32 且都在 0..1 的序列，转 hex 统计
colors = Counter()
n = len(big) // 4
arr = struct.unpack(f"<{n}f", big[: n * 4])
for i in range(n - 3):
    r, g, b, a = arr[i], arr[i+1], arr[i+2], arr[i+3]
    if all(0.0 <= x <= 1.0 for x in (r, g, b, a)) and a > 0.01:
        # 排除全 0/全 1 的平凡序列
        if (r, g, b, a) != (0, 0, 0, 1) and (r, g, b) != (1, 1, 1):
            hexv = "#{:02X}{:02X}{:02X}".format(int(r*255+.5), int(g*255+.5), int(b*255+.5))
            colors[hexv] += 1

print("\nRGBA f32 颜色 Top30:")
for v, c in colors.most_common(30):
    print(f"  {v}: {c}")

json.dump(
    {"f32_int_top": dict(f32s.most_common(120)), "colors_top": dict(colors.most_common(60))},
    open("C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/figma-export/tokens_raw.json", "w"),
    indent=1,
)
print("\nsaved tokens_raw.json")
