# -*- coding: utf-8 -*-
"""Round50: 深挖 canvas_decompressed.bin 的 Kiwi 树结构 + 导出全部图片资产。
Kiwi schema 特征（第一轮已验证）：节点记录含 'schema:id'、'Background_W_H_X' 命名、
shared color/symbol 字段。目标是拼出 frame 层级与每帧内文本/几何。"""
import re, os, json, struct

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, 'figma-export', 'r50')
big = open(os.path.join(OUT, 'canvas_decompressed.bin'), 'rb').read()

# ---- 1) 全部节点记录：Kiwi 变长整数前导 + 字符串表引用 ----
# fig 用 string table：索引引用。先重建 string table（连续出现的字符串块）
# 实用策略：按顺序扫所有字符串及其 offset，将文本节点与其后跟随的几何数据配对。
# 文本节点模式: [text]\x00\x15\x83\x00\x00`!\x01(\x85\x00\x00\x90\x02)(font)\x00(style)\x00
#   \x15\x83\x00\x00 = float fontSize? 试解码: fig 里 fontSize 是 float32 LE
# 抓: text 后 120 字节内的 float32（合理字号 8-72）与颜色（0-1 float RGBA）

def f32s(blob, base):
    """列出 blob 内所有合理 float32 值"""
    out = []
    for i in range(0, len(blob) - 3, 1):
        v = struct.unpack_from('<f', blob, i)[0]
        if 4.0 <= v <= 96.0 and abs(v * 2 - round(v * 2)) < 1e-6:  # 字号一般是整数或 .5
            out.append((base + i, v))
    return out

texts = []
for m in re.finditer(rb'(?:[\xe4-\xe9][\x80-\xbf][\x80-\xbf]|[\x20-\x7e]){2,80}\x00', big):
    try:
        s = m.group()[:-1].decode('utf-8')
    except Exception:
        continue
    if not (re.search(r'[\u4e00-\u9fff]{2}', s) or re.fullmatch(r'[A-Za-z0-9 #.%/:，。！？…—·()（）]{2,80}', s)):
        continue
    if s in ('Inter', 'Microsoft YaHei', 'Regular', 'Medium', 'Bold', 'schema:id'):
        continue
    tail = big[m.end():m.end() + 160]
    sizes = f32s(tail, m.end())
    # 颜色：float 0-1 四元组，主色 #BF6940 ≈ (0.749, 0.412, 0.251)
    texts.append({
        'offset': m.start(), 'text': s,
        'possible_fontsize': [v for _, v in sizes[:6]],
    })

print('text nodes:', len(texts))
for t in texts:
    if len(t['text']) >= 2 and not re.match(r'^[0-9a-f]{20,}$', t['text']):
        fs = t['possible_fontsize'][:3]
        print(f"{t['offset']:7d} {t['text'][:44]!r:48s} fs={fs}")

json.dump(texts, open(os.path.join(OUT, 'text_nodes.json'), 'w', encoding='utf-8'),
          ensure_ascii=False, indent=1)
print('saved -> r50/text_nodes.json')
