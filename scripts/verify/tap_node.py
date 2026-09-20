#!/usr/bin/env python
"""回归辅助：从 ui_dump_tmp.xml 找节点坐标并 tap。
用法: python tap_node.py <模式: search_box|collections_icon|text:xxx>
规避 bash 内嵌 python -c 的转义问题（第三十三轮踩坑）。
"""
import re
import subprocess
import sys
import time

ADB = r"C:\Users\huangshaozheng\AppData\Local\Android\Sdk\platform-tools\adb.exe"
XML = r"C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml"


def bounds_center(pattern):
    with open(XML, encoding="utf-8") as f:
        xml = f.read()
    m = re.search(rf'text="{pattern}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        return None
    return (int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "search_box":
        pos = bounds_center("搜索灵感、关键词、标签")
    elif mode == "collections_icon":
        c = bounds_center("搜索灵感、关键词、标签")
        pos = (c[0] + 240, c[1]) if c else None   # 搜索框右侧图标（搜索框宽~400，中心右侧 240）
    elif mode.startswith("text:"):
        pos = bounds_center(re.escape(mode[5:]))
    else:
        print("usage: tap_node.py search_box|collections_icon|text:xxx"); sys.exit(2)

    if not pos:
        print("NOT_FOUND"); sys.exit(1)
    # 设备在线检查
    for _ in range(10):
        r = subprocess.run([ADB, "get-state"], capture_output=True, text=True)
        if r.stdout.strip() == "device":
            break
        time.sleep(2)
    subprocess.run([ADB, "shell", "input", "tap", str(pos[0]), str(pos[1])], check=True)
    print(f"TAPPED {pos[0]} {pos[1]}")


if __name__ == "__main__":
    main()
