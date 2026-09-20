"""轻量 Android 环境装配脚本：JDK17(Temurin) + cmdline-tools + SDK 组件
用法（Git Bash, 已激活 7897 代理）:
  python setup_android_env.py download   # 仅下载（可长时间后台跑）
  python setup_android_env.py install    # 解压装配 + sdkmanager 装组件
"""
import os
import ssl
import sys
import urllib.request

PROXY = "http://127.0.0.1:7897"
DL_DIR = r"C:\Users\huangshaozheng\Downloads\android-env"
SDK_DIR = os.path.join(os.environ["LOCALAPPDATA"], "Android", "Sdk")
JDK_DIR = os.path.join(DL_DIR, "jdk17")

# Adoptium Temurin 17 (Windows x64 zip)。不要硬编码下载 URL——release API 每次给出最新的 17.x。
TEMURIN_API = "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse"
CMDTOOLS_URL = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

# 代理下证书校验会挂，这里全局关掉（与既有实践一致）；再加 UA 防少数 CDN 拒绝。
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE


def opener(direct=False):
    handlers = []
    if not direct:
        handlers.append(urllib.request.ProxyHandler({"http": PROXY, "https": PROXY}))
    return urllib.request.build_opener(*handlers)


def fetch(url, dest, direct=False):
    if os.path.exists(dest) and os.path.getsize(dest) > 10_000_000:
        print(f"[skip] {dest} 已存在")
        return dest
    print(f"[get ] {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "curl/8"})
    with opener(direct).open(req, timeout=60) as resp, open(dest, "wb") as f:
        total = int(resp.headers.get("Content-Length") or 0)
        done = 0
        while True:
            chunk = resp.read(1 << 20)
            if not chunk:
                break
            f.write(chunk)
            done += len(chunk)
            if total:
                print(f"\r      {done/1e6:.0f}/{total/1e6:.0f} MB", end="", flush=True)
    print(f"\n[ok  ] {dest} ({done/1e6:.0f} MB)")
    return dest


def fetch_any(url, dest):
    """实测：本机 Steam++ 已劫持 adoptium/dl.google.com 域名且转发正常，
    直连（经 127.0.0.1:443 Envoy）可用；7897 代理对 adoptium 反而不通。
    策略：直连优先，失败退回 7897 代理。"""
    try:
        return fetch(url, dest, direct=True)
    except Exception as e:
        print(f"[warn] 直连失败（{e}），改走代理 {PROXY}")
        return fetch(url, dest, direct=False)


def cmdtools_url():
    return CMDTOOLS_URL


def download():
    os.makedirs(DL_DIR, exist_ok=True)
    # JDK17
    print("== 查询 Temurin 17 最新版 ==")
    req = urllib.request.Request(TEMURIN_API, headers={"User-Agent": "curl/8"})
    with opener(direct=True).open(req, timeout=60) as resp:
        import json
        assets = json.loads(resp.read())
    jdk_zip = next(a for a in assets if a["binary"]["package"]["name"].endswith(".zip"))
    pkg = jdk_zip["binary"]["package"]
    print(f"      -> {pkg['name']} ({pkg['size']/1e6:.0f} MB)")
    fetch_any(pkg["link"], os.path.join(DL_DIR, pkg["name"]))
    # cmdline-tools
    fetch_any(CMDTOOLS_URL, os.path.join(DL_DIR, "commandlinetools.zip"))


def install():
    import zipfile
    os.makedirs(SDK_DIR, exist_ok=True)
    # 1) JDK
    if not os.path.isdir(JDK_DIR):
        zips = [f for f in os.listdir(DL_DIR) if f.startswith("OpenJDK17U") and f.endswith(".zip")]
        assert zips, "先运行 download"
        z = os.path.join(DL_DIR, zips[0])
        print(f"== 解压 {z} ==")
        with zipfile.ZipFile(z) as zf:
            zf.extractall(DL_DIR)
        top = next(n for n in os.listdir(DL_DIR) if n.startswith("jdk-17"))
        os.rename(os.path.join(DL_DIR, top), JDK_DIR)
    print(f"[ok  ] JDK17 -> {JDK_DIR}")
    # 2) cmdline-tools (Google 要求放在 Sdk/cmdline-tools/latest 下)
    ct_root = os.path.join(SDK_DIR, "cmdline-tools")
    latest = os.path.join(ct_root, "latest")
    if not os.path.isdir(latest):
        z = os.path.join(DL_DIR, "commandlinetools.zip")
        assert os.path.exists(z), "先运行 download"
        print(f"== 解压 {z} ==")
        with zipfile.ZipFile(z) as zf:
            zf.extractall(ct_root)
        # zip 内顶层是 cmdline-tools/，改成 latest/
        os.rename(os.path.join(ct_root, "cmdline-tools"), latest)
    print(f"[ok  ] cmdline-tools -> {latest}")
    # 3) sdkmanager 装组件
    jdk_java = os.path.join(JDK_DIR, "bin", "java.exe")
    sdkman_bat = os.path.join(latest, "bin", "sdkmanager.bat")
    assert os.path.isfile(jdk_java), jdk_java
    assert os.path.isfile(sdkman_bat), sdkman_bat
    print("== sdkmanager 安装 platform-tools + android-34 + build-tools 34.0.0 ==")
    env = dict(os.environ, JAVA_HOME=JDK_DIR, JAVA_OPTS=f'-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897')
    for comp in ("platform-tools", "platforms;android-34", "build-tools;34.0.0"):
        print(f"-- {comp}")
        r = os.system(f'cmd /c "set JAVA_HOME={JDK_DIR}&& "{sdkman_bat}" --install "{comp}" < nul')
        # sdkmanager 需要多次 y/n 确认许可，用管道喂 y；上面 < nul 在拒绝交互时快速失败
        if r != 0:
            print(f"[retry] {comp}: 用 yes 管道重试")
            r = os.system(f'cmd /c "set JAVA_HOME={JDK_DIR}&& echo y | "{sdkman_bat}" --install "{comp}"')
        if r != 0:
            print(f"[FAIL] {comp} 安装失败，退出码 {r}")
            sys.exit(1)
    print("[done] SDK 装配完成")
    print(f"JAVA_HOME = {JDK_DIR}")
    print(f"ANDROID_HOME = {SDK_DIR}")


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "download"
    if action == "download":
        download()
    elif action == "install":
        install()
    else:
        print(__doc__)
