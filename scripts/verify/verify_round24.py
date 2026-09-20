#!/usr/bin/env python
# 第二十四轮 App 端 E2E：验证新卡自动关联在详情页的正确渲染
# 前置：模拟器在线 + server 跑着修复后代码 + 首页顶卡是带 related 的新卡
import re
import subprocess
import sys
import time
import urllib.request

ADB = r"C:\Users\huangshaozheng\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PKG = "com.hotfix.avenlo"
ACT = "com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity"

_json = {"Cache-Control": "no-cache"}


def sh(*args, timeout=20):
    """执行 adb 命令；device offline 时等待重试"""
    for attempt in range(3):
        r = subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout)
        if "offline" in (r.stdout + r.stderr):
            wait_device()
            continue
        return r
    return r


def wait_device():
    for _ in range(30):
        r = subprocess.run([ADB, "get-state"], capture_output=True, text=True)
        if r.stdout.strip() == "device":
            return True
        time.sleep(2)
    return False


def dump(path):
    """uiautomator dump 到本地（3 次重试 + 文件大小校验）"""
    for _ in range(3):
        subprocess.run([ADB, "shell", "rm", "-f", "/sdcard/ui.xml"], capture_output=True)
        subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True, timeout=30)
        r = subprocess.run([ADB, "pull", "/sdcard/ui.xml", path], capture_output=True, text=True)
        if r.returncode == 0:
            try:
                with open(path, encoding="utf-8") as f:
                    if len(f.read()) > 500:
                        return True
            except OSError:
                pass
        time.sleep(2)
    return False


def tap_text(xml_path, pattern, settle=3):
    """按文本找节点点击；返回坐标"""
    with open(xml_path, encoding="utf-8") as f:
        xml = f.read()
    m = re.search(rf'text="{pattern}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        return None
    x = (int(m.group(1)) + int(m.group(3))) // 2
    y = (int(m.group(2)) + int(m.group(4))) // 2
    sh("shell", "input", "tap", str(x), str(y))
    time.sleep(settle)
    return (x, y)


def main():
    # 0) 设备就绪
    if not wait_device():
        print("FAIL: 设备不在线"); sys.exit(1)
    print("设备在线")

    # 1) 冷启动（force-stop 后重新拉起，清除内存缓存）
    sh("shell", "am", "force-stop", PKG)
    time.sleep(2)
    sh("shell", "am", "start", "-n", ACT)
    time.sleep(8)
    if not dump("/tmp/ui_r24_home.xml"):
        print("FAIL: 首页 dump 失败"); sys.exit(1)

    # 2) 首页应该显示新卡（15:45 的 0分5秒 卡，即 server 上带 2 条 related 的卡）
    with open("/tmp/ui_r24_home.xml", encoding="utf-8") as f:
        home = f.read()
    if "今天通勤路上想到一个想法" not in home:
        print("FAIL: 首页未见新卡"); sys.exit(1)
    print("首页 OK：新卡可见")

    # 3) 点新卡进详情
    if not tap_text("/tmp/ui_r24_home.xml", "今天通勤路上想到一个想法"):
        print("FAIL: 找不到新卡节点"); sys.exit(1)
    if not dump("/tmp/ui_r24_detail.xml"):
        print("FAIL: 详情页 dump 失败"); sys.exit(1)

    # 4) 验证详情页 related 渲染
    with open("/tmp/ui_r24_detail.xml", encoding="utf-8") as f:
        det = f.read()
    texts = [t for t in re.findall(r'text="([^"]{1,60})"', det) if t]
    print("详情页 texts:", texts)

    checks = {
        "标题": "灵感详情" in det,
        "相关想法区块存在": "相关想法" in det,
        "共2条": "共2条" in det,
        "播客选题：慢生活 (similar_theme)": "播客选题：慢生活" in det,
        "夜骑的城市观察 (same_collection)": "夜骑的城市观察" in det,
    }
    ok = True
    for name, passed in checks.items():
        print(f"  [{'PASS' if passed else 'FAIL'}] {name}")
        ok &= passed
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
