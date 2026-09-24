# -*- coding: utf-8 -*-
"""Round46: 从 fig 字符串表挖各屏 UI 结构 —— 按画布树顺序关联文本与样式。"""
import json, os, struct, re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
big = open(os.path.join(BASE, 'canvas_decompressed.bin'), 'rb').read()

# 1) 提取全部可打印 UTF-8 串（>=2 字符），记录 offset
strings = []
for m in re.finditer(rb'[\x20-\x7e\xe0-\xef][\x20-\x7e\x80-\xbf]{1,120}', big):
    try:
        s = m.group().decode('utf-8')
        if len(s) >= 2 and not re.fullmatch(r'[\x20-\x7e]{2,}', s) or re.search(r'[\u4e00-\u9fff]', s):
            strings.append((m.start(), s))
    except Exception:
        pass
# 简化：直接用宽松规则
strings = []
for m in re.finditer(rb'(?:[\xe4-\xe9][\x80-\xbf][\x80-\xbf]|[\x20-\x7e]){2,60}', big):
    try:
        s = m.group().decode('utf-8')
    except Exception:
        continue
    if re.search(r'[\u4e00-\u9fff]', s) or re.fullmatch(r'[A-Za-z0-9 /%.,:;\-\u2014\u2018\u2019\u201c\u201d!?()]{3,60}', s):
        strings.append((m.start(), s))

print('candidate strings:', len(strings))
# 只保留中文串（UI 文案）
zh = [(o, s) for o, s in strings if re.search(r'[\u4e00-\u9fff]', s)]
print('chinese strings:', len(zh))
seen = set()
uniq = []
for o, s in zh:
    if s not in seen:
        seen.add(s)
        uniq.append((o, s))
print('unique chinese:', len(uniq))
for o, s in uniq[:150]:
    print(o, repr(s))
